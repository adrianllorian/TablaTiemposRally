import { Injectable, inject, signal } from '@angular/core';
import { Api } from './api';
import { RallyConfigRequest, RallyView, ResultRequest } from './models';

@Injectable({ providedIn: 'root' })
export class RallyStore {
  private readonly api = inject(Api);
  readonly view = signal<RallyView | null>(null);
  readonly connected = signal(false);
  private source: EventSource | null = null;

  async open(id: number): Promise<void> {
    this.close();
    this.view.set(await this.api.rally(id));
    this.connect(`/api/rallies/${id}/events`);
  }

  async openPublic(token: string): Promise<void> {
    this.close();
    this.view.set(await this.api.publicRally(token));
    this.connect(`/api/public/rallies/${token}/events`);
  }

  close(): void {
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
      this.view.set(data);
      this.connected.set(true);
    });
    this.source.onopen = () => this.connected.set(true);
    this.source.onerror = () => this.connected.set(false);
  }
}
