import { formatGap, formatTime, parseTime } from './pipes';

describe('parseTime', () => {
  const valid: [string, number][] = [
    ['205000', 125000],
    ['125000', 85000],
    ['1250000', 770000],
    ['123456', 83456],
    ['1234567', 754567],
    ['2.05.000', 125000],
    ['2:05.000', 125000],
    ['2:05', 125000],
    ['2.05', 125000],
    ['2,05', 125000],
    ['12:50.000', 770000],
    ['12:50', 770000],
    ['1.23.456', 83456],
    ['1:23.456', 83456],
    ['1 23 456', 83456],
    ['23.456', 23456],
    ['125.000', 125000],
    ['205', 125000],
    ['1234', 754000],
    ['1.2', 1200],
    ['1.23', 83000],
    ['23.4', 23400],
    ['1.234', 1234],
    ['45', 45000],
    ['0', 0],
  ];

  for (const [input, expected] of valid) {
    it(`parsea "${input}"`, () => {
      expect(parseTime(input)).toBe(expected);
    });
  }

  const invalid = ['', '   ', 'abc', '2:60', '99.999.999'];

  for (const input of invalid) {
    it(`rechaza "${input}"`, () => {
      expect(parseTime(input)).toBeNull();
    });
  }
});

describe('formatTime', () => {
  it('formatea minutos, segundos y milésimas', () => {
    expect(formatTime(125000)).toBe('02.05.000');
    expect(formatTime(770000)).toBe('12.50.000');
    expect(formatTime(null)).toBe('—');
  });
});

describe('formatGap', () => {
  it('formatea diferencias', () => {
    expect(formatGap(2500)).toBe('+2.500');
    expect(formatGap(65000)).toBe('+1.05.000');
    expect(formatGap(0)).toBe('—');
  });
});
