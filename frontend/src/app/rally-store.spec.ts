import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { Api } from './api';
import { RallyStore } from './rally-store';
import { RallyView } from './models';

class FakeEventSource {
  onopen: (() => void) | null = null;
  onerror: (() => void) | null = null;
  addEventListener(): void {}
  close(): void {}
}

function view(version: number): RallyView {
  return {
    version,
    stages: [],
    general: [],
    entrants: [],
  } as unknown as RallyView;
}

describe('RallyStore', () => {
  let currentVersion = 1;
  let api: {
    rally: ReturnType<typeof vi.fn>;
    publicRally: ReturnType<typeof vi.fn>;
  };
  let store: RallyStore;

  beforeEach(() => {
    vi.useFakeTimers();
    (globalThis as unknown as { EventSource: unknown }).EventSource = FakeEventSource;

    api = {
      rally: vi.fn(() => Promise.resolve(view(currentVersion))),
      publicRally: vi.fn(() => Promise.resolve(view(currentVersion))),
    };

    TestBed.configureTestingModule({
      providers: [{ provide: Api, useValue: api }],
    });
    store = TestBed.inject(RallyStore);
  });

  afterEach(() => {
    store.close();
    vi.useRealTimers();
    TestBed.resetTestingModule();
  });

  it('actualiza la vista por polling aunque el SSE no entregue eventos', async () => {
    currentVersion = 1;
    await store.open(1);
    expect(store.view()?.version).toBe(1);

    currentVersion = 2;
    await vi.advanceTimersByTimeAsync(5000);

    expect(store.view()?.version).toBe(2);
    expect(api.rally).toHaveBeenCalledTimes(2);
  });

  it('no retrocede a una versión anterior', async () => {
    currentVersion = 2;
    await store.open(1);

    currentVersion = 1;
    await vi.advanceTimersByTimeAsync(5000);

    expect(store.view()?.version).toBe(2);
  });
});
