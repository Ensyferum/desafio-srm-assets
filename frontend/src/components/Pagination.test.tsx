import { describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import { Pagination } from './Pagination';

describe('Pagination', () => {
  it('renders current page and disables previous on first page', () => {
    render(<Pagination page={0} totalPages={3} first last={false} onChange={() => {}} />);

    expect(screen.getByText('1/3')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Página anterior' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Próxima página' })).toBeEnabled();
  });

  it('calls onChange with next page', () => {
    const onChange = vi.fn();
    render(<Pagination page={1} totalPages={3} first={false} last={false} onChange={onChange} />);

    fireEvent.click(screen.getByRole('button', { name: 'Próxima página' }));
    expect(onChange).toHaveBeenCalledWith(2);

    fireEvent.click(screen.getByRole('button', { name: 'Página anterior' }));
    expect(onChange).toHaveBeenCalledWith(0);
  });

  it('disables next on last page', () => {
    render(<Pagination page={2} totalPages={3} first={false} last onChange={() => {}} />);

    expect(screen.getByText('3/3')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Próxima página' })).toBeDisabled();
  });
});
