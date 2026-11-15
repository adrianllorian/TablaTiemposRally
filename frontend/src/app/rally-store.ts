import { Injectable, inject, signal } from '@angular/core';
import { Api } from './api';
import { RallyConfigRequest, RallyView, ResultRequest } from './models';

@Injectable({ providedIn: 'root' })
export class RallyStore {
  private readonly api = inject(Api);
  readonly view = signal<RallyView | null>(null);
  readonly connected = signal(false);
  private source: EventSource | null = null;
  private pollTimer: ReturnType<typeof setInterval> | null = null;
  private pollMode: { kind: 'id'; id: number } | { kind: 'token'; token: string } | null = null;
  private polling = false;

  constructor() {
    // Al volver a la pestaña, refresca de inmediato por si el SSE estuvo suspendido.
    document.addEventListener('visibilitychange', () => {
      if (!document.hidden) {
        void this.poll();
      }
    });
  }

  async open(id: number): Promise<void> {
    this.close();
    this.view.set(await this.api.rally(id));
    this.pollMode = { kind: 'id', id };
    this.connect(`/api/rallies/${id}/events`);
    this.startPolling();
  }

  async openPublic(token: string): Promise<void> {
    this.close();
    this.view.set(await this.api.publicRally(token));
    this.pollMode = { kind: 'token', token };
    this.connect(`/api/public/rallies/${token}/events`);
    this.startPolling();
  }

  close(): void {
    this.stopPolling();
    this.pollMode = null;
    this.source?.close();
    this.source = null;
    this.connected.set(false);
  }

  clear(): void {
    this.close();
    this.view.set(null);
  }

  async create(request: RallyConfigRequest): Promise<RallyView> {
    const view = await this.api.create(request);
    this.view.set(view);
    return view;
  }

  async updateConfig(id: number, request: RallyConfigRequest): Promise<RallyView> {
    const view = await this.api.updateConfig(id, request);
    this.view.set(view);
    return view;
  }

  async setResult(
    id: number,
    stageId: number,
    entrantId: number,
    request: ResultRequest,
  ): Promise<RallyView> {
    const view = await this.api.setResult(id, stageId, entrantId, request);
    this.view.set(view);
    return view;
  }

  async retire(id: number, entrantId: number, stageId: number): Promise<RallyView> {
    const view = await this.api.retire(id, entrantId, stageId);
    this.view.set(view);
    return view;
  }

  async unretire(id: number, entrantId: number): Promise<RallyView> {
    const view = await this.api.unretire(id, entrantId);
    this.view.set(view);
    return view;
  }

  private connect(url: string): void {
    this.source = new EventSource(url);
    this.source.addEventListener('rally', (event) => {
      const data = JSON.parse((event as MessageEvent).data) as RallyView;
      this.apply(data);
      this.connected.set(true);
    });
    this.source.onopen = () => this.connected.set(true);
    this.source.onerror = () => this.connected.set(false);
  }

  /**
   * Refresco de seguridad. Aunque un proxy inverso bloquee o bufferee el SSE,
   * la tabla se actualiza igualmente cada pocos segundos.
   */
  private startPolling(): void {
    this.stopPolling();
    this.pollTimer = setInterval(() => void this.poll(), 5000);
  }

  private stopPolling(): void {
    if (this.pollTimer) {
      clearInterval(this.pollTimer);
      this.pollTimer = null;
    }
  }

  private async poll(): Promise<void> {
    const mode = this.pollMode;
    if (this.polling || !mode || document.hidden) {
      return;
    }
    this.polling = true;
    try {
      const view =
        mode.kind === 'id'
          ? await this.api.rally(mode.id)
          : await this.api.publicRally(mode.token);
      if (this.pollMode !== mode) {
        return;
      }
      this.apply(view);
      this.connected.set(true);
    } catch {
      // Si el polling falla, el SSE seguirá intentando reconectar.
    } finally {
      this.polling = false;
    }
  }

  private apply(view: RallyView): void {
    const current = this.view();
    if (!current || view.version > current.version) {
      this.view.set(view);
    }
  }
}
