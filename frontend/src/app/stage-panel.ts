import { Component, computed, input, output } from '@angular/core';
import { RallyView, Stage } from './models';
import { DateLabelPipe, RaceGapPipe, RaceTimePipe } from './pipes';

@Component({
  selector: 'app-stage-panel',
  standalone: true,
  imports: [RaceTimePipe, RaceGapPipe, DateLabelPipe],
  template: `
    <div class="modal open" (click)="onBackdrop($event)">
      <div class="modal-card">
        <div class="modal-head">
          <div>
            <h2>{{ title() }}</h2>
            <div class="muted small" style="margin-top:5px">
              <span class="stage-chip" [class.closed]="stage().status === 'CLOSED'"
                [class.live]="stage().status === 'LIVE'"
                [class.pending]="stage().status === 'PENDING'"
                [class.void]="stage().status === 'VOID'">{{ statusLabel() }}</span>
            </div>
          </div>
          <button class="icon-btn" type="button" (click)="close.emit()" aria-label="Cerrar">✕</button>
        </div>

        @if (stage().recordMs != null) {
          <div class="record-callout">
            <div class="crown big">🏆</div>
            <div>
              <div class="muted small">Récord histórico</div>
              <div class="record-big">{{ stage().recordMs | raceTime }}</div>
              <div class="muted small">
                {{ stage().recordHolder }} · {{ stage().recordCar }} ·
                {{ stage().recordRally }} · {{ stage().recordDate | dateLabel }}
              </div>
            </div>
          </div>
        } @else {
          <div class="record-callout">
            <div>
              <div class="muted small">Récord histórico</div>
              <div class="record-big">Sin récord</div>
            </div>
          </div>
        }

        <table class="panel-table">
          <thead>
            <tr>
              <th>Pos</th>
              <th>Piloto</th>
              <th>Coche</th>
              <th>Tiempo</th>
              <th>Dif. mejor</th>
              <th>Dif. anterior</th>
              <th>Referencia</th>
            </tr>
          </thead>
          <tbody>
            @for (row of stage().rows; track row.entrantId) {
              <tr>
                <td>{{ row.position }}º</td>
                <td>{{ row.name }}</td>
                <td class="muted">{{ row.car }}</td>
                <td class="cell" [class.best]="row.best" [class.valid]="!row.best && row.kind === 'VALID'"
                  [class.penalty]="row.kind === 'PENALTY'">
                  {{ row.millis | raceTime }}
                  @if (row.record) { 🏆 }
                  @if (row.kind === 'PENALTY') { <span class="badge p">P</span> }
                </td>
                <td>{{ row.bestGapMs | raceGap }}</td>
                <td>{{ row.prevGapMs | raceGap }}</td>
                <td>{{ row.prevName ? (row.prevPosition + 'º ' + row.prevName) : '—' }}</td>
              </tr>
            }
            @for (name of stage().missingNames; track name) {
              <tr class="row-muted">
                <td>—</td>
                <td>{{ name }}</td>
                <td></td>
                <td>{{ stage().status === 'LIVE' ? 'En curso' : 'Pendiente' }}</td>
                <td>—</td>
                <td>—</td>
                <td>—</td>
              </tr>
            }
            @for (row of retiredRows(); track row.entrantId) {
              <tr class="row-muted">
                <td>—</td>
                <td>{{ row.name }}</td>
                <td class="muted">{{ row.car }}</td>
                <td>Retirado</td>
                <td>—</td>
                <td>—</td>
                <td>—</td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    </div>
  `,
})
export class StagePanel {
  readonly view = input.required<RallyView>();
  readonly stage = input.required<Stage>();
  readonly close = output<void>();

  readonly retiredRows = computed(() =>
    this.view().general.filter((row) => row.status === 'RETIRED'),
  );

  title(): string {
    return this.stage().type === 'SHAKEDOWN'
      ? `Shakedown · ${this.stage().name}`
      : `${this.stage().code} · ${this.stage().name}`;
  }

  statusLabel(): string {
    return {
      PENDING: 'Pendiente',
      LIVE: 'En curso',
      CLOSED: 'Cerrado',
      VOID: 'Anulado',
    }[this.stage().status];
  }

  onBackdrop(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.close.emit();
    }
  }
}
