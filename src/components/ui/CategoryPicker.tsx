import { useState, useMemo, useCallback } from 'react';
import { Search, ChevronRight, ChevronDown, Check, X } from 'lucide-react';
import { cn } from '@/lib/cn';
import { Input } from './Input';
import { Badge } from './Badge';
import {
  LEGAL_CATEGORY_TREE,
  searchCategories,
  type CategoryNode,
  type CategoryLevel2,
  type CategoryLeaf,
  type CategorySelection,
  type CategorySearchResult,
} from '@/lib/legalCategories';

// ── Props ──────────────────────────────────────────────────────────────────

interface CategoryPickerProps {
  data?: CategoryNode[];
  value: CategorySelection[];
  onChange: (value: CategorySelection[]) => void;
  placeholder?: string;
  error?: string;
}

// ── Helpers ────────────────────────────────────────────────────────────────

function pathKey(path: string[]): string {
  return path.join(' > ');
}

function hasSelection(value: CategorySelection[], path: string[]): boolean {
  const key = pathKey(path);
  return value.some((s) => pathKey(s.path) === key);
}

// ── Component ──────────────────────────────────────────────────────────────

export function CategoryPicker({
  data = LEGAL_CATEGORY_TREE,
  value,
  onChange,
  placeholder = '카테고리 검색...',
  error,
}: CategoryPickerProps) {
  const [query, setQuery] = useState('');
  const [activeTab, setActiveTab] = useState<string>(data[0]?.name ?? '');
  const [openL2, setOpenL2] = useState<string | null>(null);

  const isSearching = query.trim().length > 0;
  const searchResults = useMemo(() => searchCategories(query, data), [query, data]);

  const toggle = useCallback(
    (name: string, path: string[]) => {
      const key = pathKey(path);
      if (value.some((s) => pathKey(s.path) === key)) {
        onChange(value.filter((s) => pathKey(s.path) !== key));
      } else {
        onChange([...value, { name, path }]);
      }
    },
    [value, onChange],
  );

  function remove(sel: CategorySelection) {
    onChange(value.filter((s) => pathKey(s.path) !== pathKey(sel.path)));
  }

  const handleTabClick = useCallback(
    (node: CategoryNode) => {
      setActiveTab(node.name);
      setOpenL2(null);
      toggle(node.name, [node.name]);
    },
    [toggle],
  );

  const activeNode = data.find((n) => n.name === activeTab);

  // ── Keyboard handler for tabs ──
  const handleTabKeyDown = useCallback(
    (e: React.KeyboardEvent, index: number) => {
      let nextIndex = index;
      if (e.key === 'ArrowDown' || e.key === 'ArrowRight') {
        e.preventDefault();
        nextIndex = (index + 1) % data.length;
      } else if (e.key === 'ArrowUp' || e.key === 'ArrowLeft') {
        e.preventDefault();
        nextIndex = (index - 1 + data.length) % data.length;
      } else if (e.key === 'Home') {
        e.preventDefault();
        nextIndex = 0;
      } else if (e.key === 'End') {
        e.preventDefault();
        nextIndex = data.length - 1;
      } else {
        return;
      }
      setActiveTab(data[nextIndex].name);
      setOpenL2(null);
      const tabList = (e.currentTarget as HTMLElement).parentElement;
      const buttons = tabList?.querySelectorAll<HTMLButtonElement>('[role="tab"]');
      buttons?.[nextIndex]?.focus();
    },
    [data],
  );

  return (
    <div className="flex flex-col gap-3">
      {/* Search */}
      <Input
        placeholder={placeholder}
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        leftAddon={<Search size={16} />}
        rightAddon={
          query ? (
            <button
              type="button"
              onClick={() => setQuery('')}
              className="p-0.5 rounded-full hover:bg-gray-200 transition-colors"
              aria-label="검색 초기화"
            >
              <X size={14} />
            </button>
          ) : undefined
        }
        className="bg-[#f9fafb]"
      />

      {/* Selected tags */}
      {value.length > 0 && (
        <div className="flex flex-wrap gap-1.5">
          {value.map((sel) => (
            <Badge key={pathKey(sel.path)} variant="primary" size="sm" className="gap-1 pr-1">
              {sel.path.length > 1 && (
                <span className="text-blue-300 font-normal">
                  {sel.path.slice(0, -1).join(' > ')} &gt;{' '}
                </span>
              )}
              {sel.name}
              <button
                type="button"
                onClick={() => remove(sel)}
                className="ml-0.5 rounded-full hover:bg-blue-200 p-0.5 transition-colors"
                aria-label={`${sel.name} 제거`}
              >
                <X size={12} />
              </button>
            </Badge>
          ))}
        </div>
      )}

      {/* Tree / Search results */}
      <div className="border border-[#e0e2e6] rounded-card overflow-hidden">
        {isSearching ? (
          <SearchResults
            results={searchResults}
            value={value}
            onToggle={toggle}
            query={query}
          />
        ) : (
          <div className="flex flex-col md:flex-row">
            {/* Tabs */}
            <div
              role="tablist"
              aria-label="법률 카테고리"
              aria-orientation="vertical"
              className={cn(
                'flex shrink-0 border-b border-[#e0e2e6]',
                'overflow-x-auto scrollbar-hide',
                'md:flex-col md:w-48 md:border-b-0 md:border-r md:overflow-x-visible md:overflow-y-auto md:max-h-96',
              )}
            >
              {data.map((node, index) => {
                const checked = hasSelection(value, [node.name]);
                return (
                  <button
                    key={node.name}
                    type="button"
                    role="tab"
                    id={`cat-tab-${index}`}
                    aria-selected={activeTab === node.name}
                    aria-controls={`cat-panel-${index}`}
                    tabIndex={activeTab === node.name ? 0 : -1}
                    onClick={() => handleTabClick(node)}
                    onKeyDown={(e) => handleTabKeyDown(e, index)}
                    className={cn(
                      'whitespace-nowrap px-4 py-3 text-sm font-medium text-left transition-colors',
                      'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/40 focus-visible:ring-inset',
                      'flex items-center gap-2',
                      activeTab === node.name
                        ? 'text-brand bg-blue-50 border-b-2 border-brand md:border-b-0 md:border-r-2'
                        : 'text-[#575e6b] hover:bg-gray-50 hover:text-[#16181d]',
                    )}
                  >
                    <Checkbox checked={checked} />
                    <span className="truncate">{node.name}</span>
                  </button>
                );
              })}
            </div>

            {/* Content panel */}
            <div
              role="tabpanel"
              id={`cat-panel-${data.findIndex((n) => n.name === activeTab)}`}
              aria-labelledby={`cat-tab-${data.findIndex((n) => n.name === activeTab)}`}
              className="flex-1 max-h-80 md:max-h-96 overflow-y-auto"
            >
              {activeNode ? (
                <div>
                  <div className="px-4 py-2.5 border-b border-[#e0e2e6] bg-gray-50/50">
                    <p className="text-xs text-[#575e6b]">
                      여러 분야를 선택할 수 있습니다. 대분류만 선택해도 상담을 시작할 수 있습니다.
                    </p>
                  </div>
                  <div className="divide-y divide-[#e0e2e6]">
                    {activeNode.children.map((l2) => (
                      <SubcategoryGroup
                        key={l2.name}
                        node={l2}
                        parentName={activeTab}
                        isOpen={openL2 === l2.name}
                        onToggle={() => setOpenL2(openL2 === l2.name ? null : l2.name)}
                        value={value}
                        onSelect={toggle}
                      />
                    ))}
                  </div>
                </div>
              ) : (
                <EmptyState message="카테고리를 선택해 주세요." />
              )}
            </div>
          </div>
        )}
      </div>

      {/* Error */}
      {error && (
        <p className="text-xs text-red-500 leading-snug" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}

// ── Checkbox ──────────────────────────────────────────────────────────────

function Checkbox({ checked }: { checked: boolean }) {
  return (
    <div
      className={cn(
        'w-4 h-4 rounded border-2 shrink-0 flex items-center justify-center transition-colors',
        checked ? 'border-brand bg-brand' : 'border-[#cdd0d5]',
      )}
    >
      {checked && <Check size={10} className="text-white" />}
    </div>
  );
}

// ── SubcategoryGroup (중분류) ──────────────────────────────────────────────

function SubcategoryGroup({
  node,
  parentName,
  isOpen,
  onToggle,
  value,
  onSelect,
}: {
  node: CategoryLevel2;
  parentName: string;
  isOpen: boolean;
  onToggle: () => void;
  value: CategorySelection[];
  onSelect: (name: string, path: string[]) => void;
}) {
  const l2Path = [parentName, node.name];
  const l2Checked = hasSelection(value, l2Path);
  const childCount = node.children.filter((c) =>
    hasSelection(value, [parentName, node.name, c.name]),
  ).length;

  return (
    <div>
      <div
        className={cn(
          'flex items-center transition-colors',
          l2Checked
            ? 'bg-brand/5'
            : childCount > 0
              ? 'bg-blue-50/50'
              : 'hover:bg-gray-50',
        )}
      >
        {/* 중분류 선택 — 체크박스만 토글 */}
        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation();
            onSelect(node.name, l2Path);
          }}
          className={cn(
            'pl-4 py-3 flex items-center',
            'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/40 focus-visible:ring-inset',
          )}
          aria-pressed={l2Checked}
          aria-label={`${node.name} ${l2Checked ? '선택 해제' : '선택'}`}
        >
          <Checkbox checked={l2Checked} />
        </button>

        {/* 중분류 이름 클릭 → 드롭다운 토글 */}
        <button
          type="button"
          onClick={onToggle}
          aria-expanded={isOpen}
          className={cn(
            'flex-1 flex items-center gap-2 py-3 pr-4 text-left',
            'text-sm font-medium transition-colors',
            'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/40 focus-visible:ring-inset',
            l2Checked ? 'text-brand' : 'text-[#16181d]',
          )}
        >
          <span>{node.name}</span>
          {childCount > 0 && !l2Checked && (
            <Badge variant="primary" size="sm">{childCount}</Badge>
          )}
          <span className="ml-auto text-[#575e6b]">
            {isOpen ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
          </span>
        </button>
      </div>

      {isOpen && (
        <div className="pb-1 border-t border-[#e0e2e6]/50">
          {node.children.map((leaf) => {
            const leafPath = [parentName, node.name, leaf.name];
            return (
              <LeafItem
                key={leaf.name}
                leaf={leaf}
                checked={hasSelection(value, leafPath)}
                onToggle={() => onSelect(leaf.name, leafPath)}
              />
            );
          })}
        </div>
      )}
    </div>
  );
}

// ── LeafItem (소분류 선택 항목) ────────────────────────────────────────────

function LeafItem({
  leaf,
  checked,
  onToggle,
  breadcrumb,
}: {
  leaf: CategoryLeaf;
  checked: boolean;
  onToggle: () => void;
  breadcrumb?: string;
}) {
  return (
    <button
      type="button"
      onClick={onToggle}
      className={cn(
        'w-full flex items-center gap-3 pl-10 pr-4 py-2.5 text-left',
        'text-sm transition-colors',
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand/40 focus-visible:ring-inset',
        checked
          ? 'text-brand font-medium bg-brand/5'
          : 'text-[#16181d] hover:bg-gray-50',
      )}
      aria-pressed={checked}
    >
      <Checkbox checked={checked} />
      <div className="min-w-0">
        <span className="block">{leaf.name}</span>
        {breadcrumb && (
          <span className="block text-xs text-[#575e6b] truncate mt-0.5">
            {breadcrumb}
          </span>
        )}
      </div>
    </button>
  );
}

// ── SearchResults ──────────────────────────────────────────────────────────

function SearchResults({
  results,
  value,
  onToggle,
  query,
}: {
  results: CategorySearchResult[];
  value: CategorySelection[];
  onToggle: (name: string, path: string[]) => void;
  query: string;
}) {
  if (results.length === 0) {
    return (
      <EmptyState>
        &ldquo;<span className="font-medium text-[#16181d]">{query}</span>&rdquo;에 대한 검색 결과가 없습니다.
      </EmptyState>
    );
  }

  return (
    <div className="max-h-80 md:max-h-96 overflow-y-auto">
      <p className="px-4 py-2 text-xs text-[#575e6b] border-b border-[#e0e2e6] sticky top-0 bg-white">
        {results.length}건의 결과
      </p>
      {results.map(({ leaf, path }) => (
        <LeafItem
          key={`${path[0]}-${path[1]}-${leaf.name}`}
          leaf={leaf}
          checked={hasSelection(value, [...path])}
          onToggle={() => onToggle(leaf.name, [...path])}
          breadcrumb={`${path[0]} > ${path[1]}`}
        />
      ))}
    </div>
  );
}

// ── EmptyState ─────────────────────────────────────────────────────────────

function EmptyState({
  message,
  children,
}: {
  message?: string;
  children?: React.ReactNode;
}) {
  return (
    <div className="px-4 py-8 text-center text-sm text-[#575e6b]">
      {children ?? message}
    </div>
  );
}
