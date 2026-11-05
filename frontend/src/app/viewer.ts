import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Api } from './api';
import { Clock } from './clock';
import { Stage } from './models';
import { DateLabelPipe } from './pipes';
import { RallyStore } from './rally-store';
import { StagePanel } from './stage-panel';
import { RallyTable } from './table';
import { UiService } from './ui.service';

@Component({
  selector: 'app-viewer',
  standalone: true,
  imports: [Clock, RallyTable, StagePanel, DateLabelPipe],
  templateUrl: './viewer.html',
})
export class Viewer implements OnInit, OnDestroy {
  private readonly api = inject(Api);
  private readonly route = inject(ActivatedRoute);
  private readonly store = inject(RallyStore);
  readonly ui = inject(UiService);

  readonly view = this.store.view;
  readonly connected = this.store.connected;
  readonly appName = signal('Tabla Tiempos Rally');
  readonly logoUrl = signal<string | null>(null);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly showGaps = signal(false);
  readonly stage = signal<Stage | null>(null);
  readonly toast = signal('');
  readonly online = signal(navigator.onLine);
  private toastTimer: ReturnType<typeof setTimeout> | null = null;

  private readonly onlineHandler = () => this.online.set(true);
  private readonly offlineHandler = () => this.online.set(false);

  ngOnInit(): void {
    window.addEventListener('online', this.onlineHandler);
    window.addEventListener('offline', this.offlineHandler);
    const token = this.route.snapshot.paramMap.get('token');
    if (!token) {
      this.error.set('Enlace no válido.');
      this.loading.set(false);
      return;
    }
    Promise.all([
      this.api.publicStatus().catch(() => null),
      this.store.openPublic(token),
    ])
      .then(([status]) => {
        if (status) {
          this.appName.set(status.appName);
          this.logoUrl.set(status.logoUrl);
        }
      })
      .catch(() => this.error.set('Enlace no válido o rally no disponible.'))
      .finally(() => this.loading.set(false));
  }

  ngOnDestroy(): void {
    window.removeEventListener('online', this.onlineHandler);
    window.removeEventListener('offline', this.offlineHandler);
    this.store.close();
    if (this.toastTimer) {
      clearTimeout(this.toastTimer);
    }
  }

  toggleGaps(event: Event): void {
    this.showGaps.set((event.target as HTMLInputElement).checked);
  }

  async toggleWake(): Promise<void> {
    this.showToast(await this.ui.toggleWake());
  }

  showToast(message: string): void {
    this.toast.set(message);
    if (this.toastTimer) {
      clearTimeout(this.toastTimer);
    }
    this.toastTimer = setTimeout(() => this.toast.set(''), 3400);
  }
}
