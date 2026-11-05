import {
  Component,
  ElementRef,
  effect,
  input,
  output,
  viewChild,
} from '@angular/core';
import { EditTarget, GeneralRow, RallyView, Stage, StageCell } from './models';
import { RaceGapPipe, RaceTimePipe } from './pipes';

@Component({
  selector: 'app-rally-table',
  standalone: true,
  imports: [RaceTimePipe, RaceGapPipe],
  templateUrl: './table.html',
})
export class RallyTable {
  readonly view = input.required<RallyView>();
  readonly editable = input(false);
  readonly showGaps = input(false);
  readonly editCell = output<EditTarget>();
  readonly openStage = output<Stage>();
  private readonly wrap = viewChild<ElementRef<HTMLDivElement>>('wrap');
  private lastVersion = -1;

  constructor() {
    effect(() => {
      const view = this.view();
      if (view.version !== this.lastVersion) {
        this.lastVersion = view.version;
        setTimeout(() => this.scrollToCurrent(), 0);
      }
    });
  }

  cell(stage: Stage, entrantId: number): StageCell | undefined {
    return stage.cells.find((cell) => cell.entrantId === entrantId);
  }

  cellClass(cell: StageCell): string {
    if (cell.kind === 'RETIRED') {
      return 'cell retired-cell';
    }
    if (cell.kind === 'EC') {
      return 'cell ec-cell';
    }
    if (cell.kind === 'PENALTY') {
      return 'cell penalty';
    }
    if (cell.kind === 'VALID') {
      return cell.best ? 'cell best' : 'cell valid';
    }
    return 'cell';
  }

  statusLabel(stage: Stage): string {
    return {
      PENDING: 'Pendiente',
      LIVE: 'En curso',
      CLOSED: 'Cerrado',
      VOID: 'Anulado',
    }[stage.status];
  }

  onCell(stage: Stage, row: GeneralRow, cell: StageCell): void {
    if (this.editable()) {
      this.editCell.emit({ stage, row, cell });
    }
  }

  onStage(stage: Stage): void {
    this.openStage.emit(stage);
  }

  private scrollToCurrent(): void {
    const wrap = this.wrap()?.nativeElement;
    if (!wrap) {
      return;
    }
    const th =
      (wrap.querySelector('th.stage-head.live') as HTMLElement | null) ??
      (wrap.querySelector('th.stage-head.pending') as HTMLElement | null);
    if (!th) {
      return;
    }
    const pos = wrap.querySelector('th.col-pos') as HTMLElement | null;
    const pilot = wrap.querySelector('th.col-pilot') as HTMLElement | null;
    const car = wrap.querySelector('th.col-car') as HTMLElement | null;
    const total = wrap.querySelector('th.col-total') as HTMLElement | null;
    const difs = wrap.querySelectorAll('th.col-dif');
    const left =
      (pos?.offsetWidth ?? 0) + (pilot?.offsetWidth ?? 0) + (car?.offsetWidth ?? 0);
    const rightSticky = window.matchMedia('(max-width: 760px)').matches;
    const right = rightSticky
      ? 0
      : (total?.offsetWidth ?? 0) +
        Array.from(difs).reduce((sum, item) => sum + (item as HTMLElement).offsetWidth, 0);
    const visible = Math.max(0, wrap.clientWidth - left - right);
    const target = th.offsetLeft - left - Math.max(0, (visible - th.offsetWidth) / 2);
    const max = Math.max(0, wrap.scrollWidth - wrap.clientWidth);
    wrap.scrollLeft = Math.max(0, Math.min(target, max));
  }
}
