import { Component, OnDestroy, OnInit, signal } from '@angular/core';

@Component({
  selector: 'app-clock',
  standalone: true,
  template: `
    <div class="clockbox">
      <div class="clock">{{ time() }}</div>
      <div
        class="countdown"
        [class.zero]="remaining() === 0"
        [class.warn]="remaining() >= 1 && remaining() <= 5"
        [class.hot]="remaining() >= 6"
      >
        <div class="num">{{ remaining().toString().padStart(2, '0') }}</div>
        <div class="lbl">Salida 20 s</div>
      </div>
    </div>
  `,
})
export class Clock implements OnInit, OnDestroy {
  readonly time = signal('');
  readonly remaining = signal(0);
  private timer: ReturnType<typeof setInterval> | null = null;

  ngOnInit(): void {
    this.tick();
    this.timer = setInterval(() => this.tick(), 250);
  }

  ngOnDestroy(): void {
    if (this.timer) {
      clearInterval(this.timer);
    }
  }

  private tick(): void {
    const now = new Date();
    this.time.set(now.toLocaleTimeString('es-ES', { hour12: false }));
    this.remaining.set((20 - (now.getSeconds() % 20)) % 20);
  }
}
