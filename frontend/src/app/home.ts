import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Api } from './api';
import { Clock } from './clock';
import { RallyStore } from './rally-store';
import {
  Dictionaries,
  EditTarget,
  RallyConfigRequest,
  RallySummary,
  RallyView,
  RecordView,
  Stage,
} from './models';
import {
  DateLabelPipe,
  RaceTimePipe,
  formatDateLabel,
  formatTime,
  normalize,
  parseTime,
} from './pipes';
import { StagePanel } from './stage-panel';
import { RallyTable } from './table';
import { UiService } from './ui.service';

type Tab = 'rally' | 'records' | 'history';
type ConfigMode = 'edit' | 'new';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    FormsModule,
    Clock,
    RallyTable,
    StagePanel,
    RaceTimePipe,
    DateLabelPipe,
  ],
  templateUrl: './home.html',
})
export class Home implements OnInit, OnDestroy {
  readonly api = inject(Api);
  readonly store = inject(RallyStore);
  readonly ui = inject(UiService);

  readonly view = this.store.view;
  readonly connected = this.store.connected;

  readonly checking = signal(true);
  readonly authenticated = signal(false);
  readonly appName = signal('Tabla Tiempos Rally');
  readonly password = signal('');
  readonly loginError = signal('');
  readonly setupMode = signal(false);
  readonly setupPassword = signal('');
  readonly setupConfirm = signal('');
  readonly setupLogo = signal<File | null>(null);
  readonly setupError = signal('');
  readonly logoUrl = signal<string | null>(null);

  readonly settingsOpen = signal(false);
  readonly settingsCurrentPassword = signal('');
  readonly settingsNewPassword = signal('');
  readonly settingsConfirmPassword = signal('');
  readonly settingsError = signal('');
  readonly settingsLogo = signal<File | null>(null);

  readonly tab = signal<Tab>('rally');
  readonly showGaps = signal(false);
  readonly online = signal(navigator.onLine);

  readonly records = signal<RecordView[]>([]);
  readonly recordFilter = signal('');
  readonly history = signal<RallySummary[]>([]);
  readonly historyFilter = signal('');
  readonly dictionaries = signal<Dictionaries>({ stages: [], cars: [], pilots: [] });

  readonly stage = signal<Stage | null>(null);
  readonly edit = signal<EditTarget | null>(null);
  readonly editTime = signal('');
  readonly editError = signal('');

  readonly configOpen = signal(false);
  readonly configMode = signal<ConfigMode>('edit');
  readonly configName = signal('');
  readonly configStages = signal<string[]>(['']);
  readonly configEntrants = signal<{ name: string; car: string }[]>([{ name: '', car: '' }]);
  readonly configShakedown = signal(false);
  readonly configShakedownName = signal('Shakedown');
  readonly configPenaltyMode = signal<'WORST_PLUS' | 'FIXED'>('WORST_PLUS');
  readonly configPenaltyValue = signal('30');
  readonly configError = signal('');

  readonly inviteOpen = signal(false);
  readonly toast = signal('');
  readonly toastType = signal<'info' | 'record'>('info');
  private toastTimer: ReturnType<typeof setTimeout> | null = null;

  readonly filteredRecords = computed(() => {
    const filter = normalize(this.recordFilter().trim());
    if (!filter) {
      return this.records();
    }
    return this.records().filter(
      (record) =>
        normalize(record.name).includes(filter) ||
        normalize(record.holder).includes(filter) ||
        normalize(record.car).includes(filter),
    );
  });

  readonly filteredHistory = computed(() => {
    const filter = normalize(this.historyFilter().trim());
    if (!filter) {
      return this.history();
    }
    return this.history().filter(
      (item) =>
        normalize(item.name).includes(filter) ||
        normalize(item.leader ?? '').includes(filter) ||
        normalize(item.winner ?? '').includes(filter) ||
        normalize(formatDateLabel(item.date)).includes(filter) ||
        normalize(item.status).includes(filter),
    );
  });

  readonly currentStage = computed(() => {
    const view = this.view();
    return view?.stages.find((stage) => stage.id === view.currentStageId) ?? null;
  });

  readonly inviteUrl = computed(() => {
    const view = this.view();
    return view ? `${location.origin}/#/ver/${view.inviteToken}` : '';
  });

  private readonly onlineHandler = () => this.online.set(true);
  private readonly offlineHandler = () => this.online.set(false);

  ngOnInit(): void {
    window.addEventListener('online', this.onlineHandler);
    window.addEventListener('offline', this.offlineHandler);
    this.api
      .publicStatus()
      .then((status) => {
        this.appName.set(status.appName);
        this.logoUrl.set(status.logoUrl);
        if (!status.setupComplete) {
          this.setupMode.set(true);
          this.checking.set(false);
          return;
        }
        return this.api
          .me()
          .then((info) => {
            this.appName.set(info.appName);
            this.authenticated.set(true);
            return this.afterLogin();
          })
          .catch(() => this.authenticated.set(false))
          .finally(() => this.checking.set(false));
      })
      .catch(() => this.checking.set(false));
  }

  ngOnDestroy(): void {
    window.removeEventListener('online', this.onlineHandler);
    window.removeEventListener('offline', this.offlineHandler);
    this.store.close();
    if (this.toastTimer) {
      clearTimeout(this.toastTimer);
    }
  }

  login(event: Event): void {
    event.preventDefault();
    this.loginError.set('');
    this.api
      .login(this.password())
      .then((info) => {
        this.appName.set(info.appName);
        this.authenticated.set(true);
        this.password.set('');
        return this.afterLogin();
      })
      .catch(() => this.loginError.set('Contraseña incorrecta.'));
  }

  onSetupLogo(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.setupLogo.set(input.files && input.files.length ? input.files[0] : null);
  }

  async setup(): Promise<void> {
    this.setupError.set('');
    try {
      const status = await this.api.setup(
        this.setupPassword(),
        this.setupConfirm(),
        this.setupLogo(),
      );
      this.appName.set(status.appName);
      this.logoUrl.set(status.logoUrl);
      this.setupMode.set(false);
      this.authenticated.set(false);
      this.setupPassword.set('');
      this.setupConfirm.set('');
      this.setupLogo.set(null);
      this.showToast('Aplicación configurada. Entra con tu contraseña.');
    } catch {
      this.setupError.set('No se pudo configurar. Revisa la contraseña y la imagen.');
    }
  }

  openSettings(): void {
    this.settingsError.set('');
    this.settingsOpen.set(true);
  }

  onSettingsLogo(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.settingsLogo.set(input.files && input.files.length ? input.files[0] : null);
  }

  async changePassword(): Promise<void> {
    this.settingsError.set('');
    try {
      await this.api.changePassword(
        this.settingsCurrentPassword(),
        this.settingsNewPassword(),
        this.settingsConfirmPassword(),
      );
      this.settingsCurrentPassword.set('');
      this.settingsNewPassword.set('');
      this.settingsConfirmPassword.set('');
      this.showToast('Contraseña actualizada.');
    } catch {
      this.settingsError.set('No se pudo cambiar la contraseña. Revisa la actual.');
    }
  }

  async uploadLogo(): Promise<void> {
    this.settingsError.set('');
    const file = this.settingsLogo();
    if (!file) {
      this.settingsError.set('Selecciona una imagen.');
      return;
    }
    try {
      const status = await this.api.updateLogo(file);
      this.logoUrl.set(status.logoUrl);
      this.settingsLogo.set(null);
      this.showToast('Logo actualizado.');
    } catch {
      this.settingsError.set('No se pudo guardar el logo.');
    }
  }

  logout(): void {
    void this.api.logout().catch(() => undefined);
    this.store.clear();
    this.authenticated.set(false);
    this.password.set('');
    this.tab.set('rally');
    this.showToast('Sesión cerrada.');
  }

  async loadHistory(): Promise<void> {
    try {
      this.history.set(await this.api.rallies());
    } catch {
      this.history.set([]);
    }
  }

  async loadRecords(): Promise<void> {
    try {
      this.records.set(await this.api.records());
    } catch {
      this.records.set([]);
    }
  }

  async loadDictionaries(): Promise<void> {
    try {
      this.dictionaries.set(await this.api.dictionaries());
    } catch {
      this.dictionaries.set({ stages: [], cars: [], pilots: [] });
    }
  }

  async openRally(summary: RallySummary): Promise<void> {
    try {
      await this.store.open(summary.id);
      this.tab.set('rally');
    } catch {
      this.showToast('No se pudo abrir el rally.');
    }
  }

  async duplicateRally(summary: RallySummary): Promise<void> {
    try {
      const view = await this.api.rally(summary.id);
      this.openConfig('new', view);
    } catch {
      this.showToast('No se pudo cargar la configuración.');
    }
  }

  async reopenRally(summary: RallySummary): Promise<void> {
    try {
      await this.api.reopenRally(summary.id);
      await this.store.open(summary.id);
      this.tab.set('rally');
      await this.loadHistory();
      this.showToast('Rally reabierto.');
    } catch {
      this.showToast('No se pudo reabrir el rally.');
    }
  }

  async deleteRally(summary: RallySummary): Promise<void> {
    if (!confirm(`¿Seguro que quieres borrar "${summary.name}"? Sus tiempos desaparecerán de los récords.`)) {
      return;
    }
    try {
      const wasActive = this.view()?.id === summary.id;
      await this.api.deleteRally(summary.id);
      if (wasActive) {
        this.store.clear();
      }
      await this.loadHistory();
      await this.loadRecords();
      this.showToast('Rally borrado.');
    } catch {
      this.showToast('No se pudo borrar el rally.');
    }
  }

  openConfig(mode: ConfigMode, source?: RallyView | null): void {
    const base = source ?? this.view();
    this.configMode.set(mode);
    this.configError.set('');
    this.configName.set(
      mode === 'new'
        ? `Rally ${formatDateLabel(new Date().toISOString().slice(0, 10))}`
        : (base?.name ?? ''),
    );
    this.configStages.set(
      base ? base.stages.filter((stage) => stage.type === 'TC').map((stage) => stage.name) : [''],
    );
    this.configEntrants.set(
      base && base.entrants.length
        ? base.entrants.map((entrant) => ({ name: entrant.name, car: entrant.car }))
        : [{ name: '', car: '' }],
    );
    this.configShakedown.set(base?.shakedownEnabled ?? false);
    this.configShakedownName.set(base?.shakedownName ?? 'Shakedown');
    this.configPenaltyMode.set(base?.penaltyMode ?? 'WORST_PLUS');
    this.configPenaltyValue.set(
      base
        ? base.penaltyMode === 'FIXED'
          ? formatTime(base.penaltyValueMs)
          : String(base.penaltyValueMs / 1000)
        : '30',
    );
    this.configOpen.set(true);
  }

  updateStageName(index: number, value: string): void {
    this.configStages.update((stages) => stages.map((stage, i) => (i === index ? value : stage)));
  }

  addStage(): void {
    this.configStages.update((stages) => [...stages, '']);
  }

  removeStage(index: number): void {
    this.configStages.update((stages) => stages.filter((_, i) => i !== index));
  }

  updateEntrantName(index: number, value: string): void {
    this.configEntrants.update((entrants) =>
      entrants.map((entrant, i) => (i === index ? { ...entrant, name: value } : entrant)),
    );
  }

  updateEntrantCar(index: number, value: string): void {
    this.configEntrants.update((entrants) =>
      entrants.map((entrant, i) => (i === index ? { ...entrant, car: value } : entrant)),
    );
  }

  addEntrant(): void {
    this.configEntrants.update((entrants) => [...entrants, { name: '', car: '' }]);
  }

  removeEntrant(index: number): void {
    this.configEntrants.update((entrants) => entrants.filter((_, i) => i !== index));
  }

  async saveConfig(): Promise<void> {
    this.configError.set('');
    const name = this.configName().trim() || 'Rally';
    const stages = this.configStages()
      .map((stage) => stage.trim())
      .filter(Boolean)
      .map((stage) => ({ name: stage }));
    const entrants = this.configEntrants()
      .map((entrant) => ({ name: entrant.name.trim(), car: entrant.car.trim() }))
      .filter((entrant) => entrant.name);

    if (!stages.length) {
      this.configError.set('Añade al menos un tramo.');
      return;
    }
    if (!entrants.length) {
      this.configError.set('Añade al menos un piloto.');
      return;
    }

    const penaltyValueMs =
      this.configPenaltyMode() === 'FIXED'
        ? (parseTime(this.configPenaltyValue()) ?? 30_000)
        : Math.max(0, Number(this.configPenaltyValue()) || 30) * 1000;

    const request: RallyConfigRequest = {
      name,
      stages,
      entrants,
      shakedownEnabled: this.configShakedown(),
      shakedownName: this.configShakedownName().trim() || 'Shakedown',
      penaltyMode: this.configPenaltyMode(),
      penaltyValueMs,
    };

    try {
      if (this.configMode() === 'new') {
        if (
          !confirm(
            'Se guardará el rally actual en el historial y empezará uno nuevo con los tiempos a cero. ¿Continuar?',
          )
        ) {
          return;
        }
        const view = await this.api.create(request);
        await this.store.open(view.id);
        this.showToast('Nuevo rally creado. Tiempos a cero.');
      } else {
        const current = this.view();
        if (!current) {
          return;
        }
        const view = await this.store.updateConfig(current.id, request);
        this.store.view.set(view);
        this.showToast('Configuración guardada. Los tiempos no se tocan.');
      }
      this.configOpen.set(false);
      this.tab.set('rally');
      await this.loadHistory();
      await this.loadDictionaries();
    } catch {
      this.configError.set('No se pudo guardar la configuración.');
    }
  }

  openEdit(target: EditTarget): void {
    this.edit.set(target);
    this.editTime.set(target.cell.millis != null ? formatTime(target.cell.millis) : '');
    this.editError.set('');
  }

  closeEdit(): void {
    this.edit.set(null);
    this.editError.set('');
  }

  async saveEdit(kind: 'VALID' | 'PENALTY' | 'CLEAR'): Promise<void> {
    const target = this.edit();
    const view = this.view();
    if (!target || !view) {
      return;
    }
    this.editError.set('');
    try {
      if (kind === 'VALID') {
        const millis = parseTime(this.editTime());
        if (millis == null || millis <= 0) {
          this.editError.set('Formato de tiempo no válido. Usa 00.00.000 o 123456.');
          return;
        }
        await this.store.setResult(view.id, target.stage.id, target.row.entrantId, {
          kind: 'VALID',
          millis,
        });
      } else if (kind === 'PENALTY') {
        await this.store.setResult(view.id, target.stage.id, target.row.entrantId, {
          kind: 'PENALTY',
        });
      } else {
        await this.store.setResult(view.id, target.stage.id, target.row.entrantId, {
          kind: 'CLEAR',
        });
      }
      this.closeEdit();
      this.showToast(
        kind === 'VALID' ? 'Tiempo guardado.' : kind === 'PENALTY' ? 'Penalización aplicada.' : 'Celda vaciada.',
      );
    } catch {
      this.editError.set('No se pudo guardar el cambio.');
    }
  }

  async retireTarget(): Promise<void> {
    const target = this.edit();
    const view = this.view();
    if (!target || !view) {
      return;
    }
    try {
      await this.store.retire(view.id, target.row.entrantId, target.stage.id);
      this.closeEdit();
      this.showToast('Piloto retirado.');
    } catch {
      this.editError.set('No se pudo retirar al piloto.');
    }
  }

  async unretireTarget(): Promise<void> {
    const target = this.edit();
    const view = this.view();
    if (!target || !view) {
      return;
    }
    try {
      await this.store.unretire(view.id, target.row.entrantId);
      this.closeEdit();
      this.showToast('Piloto reintegrado.');
    } catch {
      this.editError.set('No se pudo reintegrar al piloto.');
    }
  }

  openStage(stage: Stage): void {
    this.stage.set(stage);
  }

  todayIso(): string {
    return new Date().toISOString().slice(0, 10);
  }

  onEditBackdrop(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.closeEdit();
    }
  }

  onConfigBackdrop(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.configOpen.set(false);
    }
  }

  onInviteBackdrop(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.inviteOpen.set(false);
    }
  }

  onSettingsBackdrop(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.settingsOpen.set(false);
    }
  }

  openInvite(): void {
    if (this.view()) {
      this.inviteOpen.set(true);
    }
  }

  async copyInvite(): Promise<void> {
    try {
      await navigator.clipboard.writeText(this.inviteUrl());
      this.showToast('Enlace copiado.');
    } catch {
      this.showToast('No se pudo copiar; selecciona el enlace a mano.');
    }
  }

  exportRally(): void {
    const view = this.view();
    if (view) {
      window.open(this.api.exportUrl(view.id), '_blank');
    }
  }

  exportSummary(summary: RallySummary): void {
    window.open(this.api.exportUrl(summary.id), '_blank');
  }

  async toggleWake(): Promise<void> {
    this.showToast(await this.ui.toggleWake());
  }

  showToast(message: string, type: 'info' | 'record' = 'info'): void {
    this.toast.set(message);
    this.toastType.set(type);
    if (this.toastTimer) {
      clearTimeout(this.toastTimer);
    }
    this.toastTimer = setTimeout(() => this.toast.set(''), 3400);
  }

  private async afterLogin(): Promise<void> {
    await Promise.all([this.loadHistory(), this.loadRecords(), this.loadDictionaries()]);
    const list = await this.api.rallies().catch(() => []);
    this.history.set(list);
    const active = list.find((rally) => rally.active);
    if (active) {
      try {
        await this.store.open(active.id);
      } catch {
        this.showToast('No se pudo abrir el rally activo.');
      }
    } else {
      this.store.clear();
    }
  }
}
