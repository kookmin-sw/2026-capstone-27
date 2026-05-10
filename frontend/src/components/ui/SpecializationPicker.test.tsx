import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { SpecializationPicker } from './SpecializationPicker';

describe('SpecializationPicker', () => {
  it('renders search input and all top-level categories', () => {
    render(<SpecializationPicker value={[]} onChange={() => {}} />);

    expect(screen.getByPlaceholderText('전문 분야 검색...')).toBeInTheDocument();
    expect(screen.getByText('부동산 거래')).toBeInTheDocument();
    expect(screen.getByText('이혼·위자료·재산분할')).toBeInTheDocument();
    expect(screen.getByText('기업·상사거래')).toBeInTheDocument();
  });

  it('expands level1 on click', async () => {
    const user = userEvent.setup();
    render(<SpecializationPicker value={[]} onChange={() => {}} />);

    await user.click(screen.getByText('부동산 거래'));

    expect(screen.getByText('부동산 매매')).toBeInTheDocument();
    expect(screen.getByText('부동산 임대차')).toBeInTheDocument();
  });

  it('expands level2 to show leaf checkboxes', async () => {
    const user = userEvent.setup();
    render(<SpecializationPicker value={[]} onChange={() => {}} />);

    await user.click(screen.getByText('부동산 거래'));
    await user.click(screen.getByText('부동산 매매'));

    expect(screen.getByText('계약 체결 및 효력')).toBeInTheDocument();
    expect(screen.getByText('하자담보책임')).toBeInTheDocument();
  });

  it('calls onChange when checkbox is toggled', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<SpecializationPicker value={[]} onChange={onChange} />);

    await user.click(screen.getByText('부동산 거래'));
    await user.click(screen.getByText('부동산 매매'));
    await user.click(screen.getByText('계약 체결 및 효력'));

    expect(onChange).toHaveBeenCalledWith(['계약 체결 및 효력']);
  });

  it('shows selected items as badges', () => {
    render(
      <SpecializationPicker
        value={['계약 체결 및 효력', '하자담보책임']}
        onChange={() => {}}
      />,
    );

    const badges = screen.getAllByRole('button', { name: /제거/ });
    expect(badges).toHaveLength(2);
  });

  it('removes item when badge X is clicked', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(
      <SpecializationPicker
        value={['계약 체결 및 효력', '하자담보책임']}
        onChange={onChange}
      />,
    );

    await user.click(screen.getByRole('button', { name: '계약 체결 및 효력 제거' }));

    expect(onChange).toHaveBeenCalledWith(['하자담보책임']);
  });

  it('switches to search mode when query is entered', async () => {
    const user = userEvent.setup();
    render(<SpecializationPicker value={[]} onChange={() => {}} />);

    await user.type(screen.getByPlaceholderText('전문 분야 검색...'), '보증금');

    expect(screen.getByText(/건의 결과/)).toBeInTheDocument();
    expect(screen.getByText('보증금 및 차임')).toBeInTheDocument();
  });

  it('shows breadcrumb path in search results', async () => {
    const user = userEvent.setup();
    render(<SpecializationPicker value={[]} onChange={() => {}} />);

    await user.type(screen.getByPlaceholderText('전문 분야 검색...'), '보증금');

    expect(screen.getByText('부동산 거래 > 부동산 임대차')).toBeInTheDocument();
  });

  it('shows "no results" for non-matching query', async () => {
    const user = userEvent.setup();
    render(<SpecializationPicker value={[]} onChange={() => {}} />);

    await user.type(screen.getByPlaceholderText('전문 분야 검색...'), 'xyzabc');

    expect(screen.getByText(/에 대한 검색 결과가 없습니다/)).toBeInTheDocument();
  });

  it('shows error message when error prop is set', () => {
    render(
      <SpecializationPicker
        value={[]}
        onChange={() => {}}
        error="전문분야를 1개 이상 선택해주세요"
      />,
    );

    expect(screen.getByText('전문분야를 1개 이상 선택해주세요')).toBeInTheDocument();
  });

  it('shows selected count badge on expanded category', () => {
    render(
      <SpecializationPicker
        value={['계약 체결 및 효력', '하자담보책임']}
        onChange={() => {}}
      />,
    );

    // 부동산 거래 should show count "2"
    expect(screen.getByText('2')).toBeInTheDocument();
  });
});
