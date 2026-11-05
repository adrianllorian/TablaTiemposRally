import { Injectable, signal } from '@angular/core';

type Theme = 'light' | 'dark';

@Injectable({ providedIn: 'root' })
export class UiService {
  readonly theme = signal<Theme>('light');
  readonly wake = signal(false);
  readonly wakeSupported = 'wakeLock' in navigator;
  private sentinel: WakeLockSentinel | null = null;

  constructor() {
    const stored = localStorage.getItem('tt-theme');
    this.theme.set(stored === 'dark' ? 'dark' : 'light');
    this.applyTheme();
    document.addEventListener('visibilitychange', () => {
      if (document.visibilityState === 'visible' && this.wake() && !this.sentinel) {
        void this.requestWake();
      }
    });
  }

  toggleTheme(): void {
    this.theme.set(this.theme() === 'dark' ? 'light' : 'dark');
    localStorage.setItem('tt-theme', this.theme());
    this.applyTheme();
  }

  async toggleWake(): Promise<string> {
    if (!this.wakeSupported) {
      return 'Este navegador no permite mantener la pantalla encendida.';
    }
    if (this.wake()) {
      this.wake.set(false);
      if (this.sentinel) {
        try {
          await this.sentinel.release();
        } catch {
          // ignorar
        }
        this.sentinel = null;
      }
      return 'Pantalla normal.';
    }
    this.wake.set(true);
    const ok = await this.requestWake();
    if (!ok) {
      this.wake.set(false);
      return 'No se pudo mantener la pantalla encendida.';
    }
    return 'Pantalla siempre encendida.';
  }

  private async requestWake(): Promise<boolean> {
    try {
      this.sentinel = await navigator.wakeLock.request('screen');
      this.sentinel.addEventListener('release', () => {
        this.sentinel = null;
      });
      return true;
    } catch {
      this.sentinel = null;
      return false;
    }
  }

  private applyTheme(): void {
    document.documentElement.setAttribute('data-theme', this.theme());
  }
}
