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
  const value = raw.trim().replace(/\s+/g, ' ');
  if (!value) {
    return null;
  }

  // Entrada rápida sin separadores:
  // 123456 -> 1:23.456 | 1250000 -> 12:50.000 | 205 -> 2:05 | 45 -> 45 s
  if (/^\d+$/.test(value)) {
    if (value.length >= 5 && value.length <= 8) {
      const digits = value.padStart(6, '0');
      const m = Number(digits.slice(0, digits.length - 5));
      const s = Number(digits.slice(-5, -3));
      const t = Number(digits.slice(-3));
      if (Number.isNaN(m) || Number.isNaN(s) || Number.isNaN(t) || s >= 60) {
        return null;
      }
      return m * 60000 + s * 1000 + t;
    }
    if (value.length === 3 || value.length === 4) {
      const m = Number(value.slice(0, -2));
      const s = Number(value.slice(-2));
      if (Number.isNaN(m) || Number.isNaN(s) || s >= 60) {
        return null;
      }
      return m * 60000 + s * 1000;
    }
    if (value.length <= 2) {
      return Number(value) * 1000;
    }
    return null;
  }

  const hasColon = value.includes(':');
  const parts = value.split(/[.:,\s]+/).filter(Boolean);
  if (parts.length < 2 || parts.length > 3) {
    return null;
  }

  if (parts.length === 2) {
    const second = parts[1];
    // "2:05" o "2.05" con dos cifras se entiende como minutos:segundos.
    // "23.456" o "125.000" se entiende como segundos.milésimas.
    const minutesSeconds = hasColon || (second.length === 2 && parts[0].length <= 2);
    const first = Number(parts[0]);
    if (minutesSeconds) {
      const seconds = Number(second);
      if (Number.isNaN(first) || Number.isNaN(seconds) || seconds >= 60) {
        return null;
      }
      return first * 60000 + seconds * 1000;
    }
    const seconds = Number(parts[0]);
    const millis = Number(second.padEnd(3, '0').slice(0, 3));
    if (Number.isNaN(seconds) || Number.isNaN(millis)) {
      return null;
    }
    return seconds * 1000 + millis;
  }

  const m = Number(parts[0]);
  const s = Number(parts[1]);
  const t = Number(parts[2].padEnd(3, '0').slice(0, 3));
  if (Number.isNaN(m) || Number.isNaN(s) || Number.isNaN(t) || s >= 60) {
    return null;
  }
  return m * 60000 + s * 1000 + t;
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
