import { Pipe, PipeTransform } from '@angular/core';

export function formatTime(ms: number | null | undefined): string {
  if (ms == null) {
    return '—';
  }
  const m = Math.floor(ms / 60000);
  const s = Math.floor((ms % 60000) / 1000);
  const t = ms % 1000;
  return `${String(m).padStart(2, '0')}.${String(s).padStart(2, '0')}.${String(t).padStart(3, '0')}`;
}

export function formatGap(ms: number | null | undefined): string {
  if (ms == null || ms === 0) {
    return '—';
  }
  const sign = ms > 0 ? '+' : '−';
  const value = Math.abs(ms);
  const m = Math.floor(value / 60000);
  const s = Math.floor((value % 60000) / 1000);
  const t = value % 1000;
  if (m === 0) {
    return `${sign}${s}.${String(t).padStart(3, '0')}`;
  }
  return `${sign}${m}.${String(s).padStart(2, '0')}.${String(t).padStart(3, '0')}`;
}

export function parseTime(raw: string): number | null {
  if (!raw) {
    return null;
  }
  const value = raw.trim();
  if (!value) {
    return null;
  }

  if (/^\d{5,6}$/.test(value)) {
    const digits = value.padStart(6, '0');
    const m = Number(digits.slice(0, digits.length - 5));
    const s = Number(digits.slice(-5, -3));
    const t = Number(digits.slice(-3));
    return m * 60000 + s * 1000 + t;
  }

  const parts = value.split(/[.:]/).map((part) => part.trim());
  if (parts.length === 2) {
    const s = Number(parts[0]);
    const t = Number(parts[1].padEnd(3, '0').slice(0, 3));
    if (Number.isNaN(s) || Number.isNaN(t)) {
      return null;
    }
    return s * 1000 + t;
  }
  if (parts.length === 3) {
    const m = Number(parts[0]);
    const s = Number(parts[1]);
    const t = Number(parts[2].padEnd(3, '0').slice(0, 3));
    if (Number.isNaN(m) || Number.isNaN(s) || Number.isNaN(t)) {
      return null;
    }
    return m * 60000 + s * 1000 + t;
  }
  return null;
}

export function formatDateLabel(value: string | null | undefined): string {
  if (!value) {
    return '';
  }
  const date = new Date(`${value}T00:00:00`);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleDateString('es-ES', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  });
}

export function normalize(value: string): string {
  return value
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase();
}

@Pipe({ name: 'raceTime', standalone: true })
export class RaceTimePipe implements PipeTransform {
  transform(value: number | null | undefined): string {
    return formatTime(value);
  }
}

@Pipe({ name: 'raceGap', standalone: true })
export class RaceGapPipe implements PipeTransform {
  transform(value: number | null | undefined): string {
    return formatGap(value);
  }
}

@Pipe({ name: 'dateLabel', standalone: true })
export class DateLabelPipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    return formatDateLabel(value);
  }
}
