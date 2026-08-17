import { describe, expect, it } from 'vitest';
import { addDaysISO, formatDate, formatDateTime, formatDocument, formatMoney, formatNumber, formatRate, todayISO } from './format';

describe('formatMoney', () => {
  it('formata em BRL', () => {
    expect(formatMoney(1234.5)).toBe('R$\u00a01.234,50');
  });

  it('formata em USD', () => {
    expect(formatMoney(1234.5, 'USD')).toBe('US$\u00a01.234,50');
  });

  it('retorna travessão para nulos', () => {
    expect(formatMoney(null)).toBe('—');
    expect(formatMoney(undefined)).toBe('—');
    expect(formatMoney(NaN)).toBe('—');
  });
});

describe('formatNumber / formatRate', () => {
  it('formata número com casas decimais', () => {
    expect(formatNumber(0.015, 4)).toBe('0,0150');
  });

  it('formata taxa mensal (0.015 → 1,50% a.m.)', () => {
    expect(formatRate(0.015)).toBe('1,50% a.m.');
  });
});

describe('formatDate / formatDateTime', () => {
  it('converte ISO date para dd/mm/aaaa', () => {
    expect(formatDate('2026-11-11')).toBe('11/11/2026');
  });

  it('converte Instant ISO para data e hora', () => {
    expect(formatDateTime('2026-08-16T14:30:00Z')).toMatch(/\d{2}\/\d{2}\/\d{4}, \d{2}:\d{2}/);
  });

  it('retorna travessão para valores vazios', () => {
    expect(formatDate(null)).toBe('—');
    expect(formatDateTime(undefined)).toBe('—');
  });
});

describe('formatDocument', () => {
  it('formata CNPJ com máscara', () => {
    expect(formatDocument('11222333000181')).toBe('11.222.333/0001-81');
  });

  it('retorna valor original quando não tem 14 dígitos', () => {
    expect(formatDocument('123')).toBe('123');
  });

  it('retorna travessão para vazios', () => {
    expect(formatDocument(null)).toBe('—');
    expect(formatDocument(undefined)).toBe('—');
  });
});

describe('datas utilitárias', () => {
  it('todayISO retorna yyyy-MM-dd', () => {
    expect(todayISO()).toMatch(/^\d{4}-\d{2}-\d{2}$/);
  });

  it('addDaysISO soma dias', () => {
    const base = Date.parse(todayISO());
    const plus = Date.parse(addDaysISO(90));
    expect((plus - base) / 86_400_000).toBe(90);
  });
});
