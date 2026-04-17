import { useState, useMemo } from 'react';
import { Search, ChevronRight, X } from 'lucide-react';
import { cn } from '@/lib/cn';
import { Input } from './Input';
import { Badge } from './Badge';
import {
  SPECIALIZATION_TREE,
  searchSpecializations,
  type SpecLevel1,
  type SpecLevel2,
  type SpecLeaf,
} from '@/lib/specializations';

// ── Props ──────────────────────────────────────────────────────────────────

interface SpecializationPickerProps {
  value: string[];
  onChange: (value: string[]) => void;
  error?: string;
}

// ── Component ──────────────────────────────────────────────────────────────

export function SpecializationPicker({ value, onChange, error }: SpecializationPickerProps) {
  const [query, setQuery] = useState('');
  const [openL1, setOpenL1] = useState<string | null>(null);
  const [openL2, setOpenL2] = useState<string | null>(null);

  const searchResults = useMemo(() => searchSpecializations(query), [query]);
  const isSearching = query.trim().length > 0;

  function toggle(name: string) {
    onChange(
      value.includes(name) ? value.filter((v) => v !== name) : [...value, name],
    );
  }

  function remove(name: string) {
    onChange(value.filter((v) => v !== name));
  }

  return (
    <div className="flex flex-col gap-3">
      {/* Search */}
      <Input
        placeholder="전문 분야 검색..."
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        leftAddon={<Search size={16} />}
        className="bg-[#f9fafb]"
      />

      {/* Selected tags */}
      {value.length > 0 && (
        <div className="flex flex-wrap gap-1.5">
          {value.map((name) => (
            <Badge key={name} variant="primary" size="sm" className="gap-1 pr-1">
              {name}
              <button
                type="button"
                onClick={() => remove(name)}
                className="ml-0.5 rounded-full hover:bg-blue-200 p-0.5 transition-colors"
                aria-label={`${name} 제거`}
              >
                <X size={12} />
              </button>
            </Badge>
          ))}
        </div>
      )}

      {/* Tree / Search results */}
      <div className="border border-[#e0e2e6] rounded-card max-h-72 overflow-y-auto">
        {isSearching ? (
          <SearchResults
            results={searchResults}
            selected={value}
            onToggle={toggle}
            query={query}
          />
        ) : (
          <TreeBrowser
            selected={value}
            onToggle={toggle}
            openL1={openL1}
            setOpenL1={setOpenL1}
            openL2={openL2}
            setOpenL2={setOpenL2}
          />
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

// ── Tree browser (browsing mode) ───────────────────────────────────────────

interface TreeBrowserProps {
  selected: string[];
  onToggle: (name: string) => void;
  openL1: string | null;
  setOpenL1: (v: string | null) => void;
  openL2: string | null;
  setOpenL2: (v: string | null) => void;
}

function TreeBrowser({ selected, onToggle, openL1, setOpenL1, openL2, setOpenL2 }: TreeBrowserProps) {
  return (
    <div className="divide-y divide-[#e0e2e6]">
      {SPECIALIZATION_TREE.map((l1) => (
        <Level1Item
          key={l1.name}
          node={l1}
          isOpen={openL1 === l1.name}
          onToggleOpen={() => {
            setOpenL1(openL1 === l1.name ? null : l1.name);
            setOpenL2(null);
          }}
          openL2={openL2}
          setOpenL2={setOpenL2}
          selected={selected}
          onToggle={onToggle}
        />
      ))}
    </div>
  );
}

function Level1Item({
  node, isOpen, onToggleOpen, openL2, setOpenL2, selected, onToggle,
}: {
  node: SpecLevel1; isOpen: boolean; onToggleOpen: () => void;
  openL2: string | null; setOpenL2: (v: string | null) => void;
  selected: string[]; onToggle: (name: string) => void;
}) {
  const selectedCount = countSelected(node, selected);

  return (
    <div>
      <button
        type="button"
        onClick={onToggleOpen}
        className={cn(
          'w-full flex items-center justify-between px-4 py-3 text-left',
          'text-sm font-semibold text-[#16181d] hover:bg-gray-50 transition-colors',
        )}
      >
        <span className="flex items-center gap-2">
          <ChevronRight
            size={14}
            className={cn('text-[#575e6b] transition-transform', isOpen && 'rotate-90')}
          />
          {node.name}
        </span>
        {selectedCount > 0 && (
          <Badge variant="primary" size="sm">{selectedCount}</Badge>
        )}
      </button>

      {isOpen && (
        <div className="bg-gray-50/50">
          {node.children.map((l2) => (
            <Level2Item
              key={l2.name}
              node={l2}
              isOpen={openL2 === l2.name}
              onToggleOpen={() => setOpenL2(openL2 === l2.name ? null : l2.name)}
              selected={selected}
              onToggle={onToggle}
            />
          ))}
        </div>
      )}
    </div>
  );
}

function Level2Item({
  node, isOpen, onToggleOpen, selected, onToggle,
}: {
  node: SpecLevel2; isOpen: boolean; onToggleOpen: () => void;
  selected: string[]; onToggle: (name: string) => void;
}) {
  const selectedCount = node.children.filter((c) => selected.includes(c.name)).length;

  return (
    <div>
      <button
        type="button"
        onClick={onToggleOpen}
        className={cn(
          'w-full flex items-center justify-between pl-8 pr-4 py-2.5 text-left',
          'text-sm font-medium text-[#575e6b] hover:bg-gray-100/50 transition-colors',
        )}
      >
        <span className="flex items-center gap-2">
          <ChevronRight
            size={12}
            className={cn('text-[#575e6b] transition-transform', isOpen && 'rotate-90')}
          />
          {node.name}
        </span>
        {selectedCount > 0 && (
          <Badge variant="primary" size="sm">{selectedCount}</Badge>
        )}
      </button>

      {isOpen && (
        <div className="pb-1">
          {node.children.map((leaf) => (
            <LeafCheckbox
              key={leaf.name}
              leaf={leaf}
              checked={selected.includes(leaf.name)}
              onToggle={() => onToggle(leaf.name)}
              indent="pl-14"
            />
          ))}
        </div>
      )}
    </div>
  );
}

// ── Leaf checkbox (shared) ─────────────────────────────────────────────────

function LeafCheckbox({
  leaf, checked, onToggle, indent, breadcrumb,
}: {
  leaf: SpecLeaf; checked: boolean; onToggle: () => void;
  indent?: string; breadcrumb?: string;
}) {
  return (
    <label
      className={cn(
        'flex items-start gap-3 pr-4 py-2 cursor-pointer',
        'hover:bg-brand/5 transition-colors',
        indent ?? 'pl-4',
      )}
    >
      <input
        type="checkbox"
        checked={checked}
        onChange={onToggle}
        className="mt-0.5 w-4 h-4 rounded border-[#e0e2e6] text-brand focus:ring-brand/40 shrink-0"
      />
      <div className="min-w-0">
        <span className={cn('text-sm', checked ? 'font-medium text-brand' : 'text-[#16181d]')}>
          {leaf.name}
        </span>
        {breadcrumb && (
          <p className="text-xs text-[#575e6b] truncate mt-0.5">{breadcrumb}</p>
        )}
      </div>
    </label>
  );
}

// ── Search results (search mode) ───────────────────────────────────────────

function SearchResults({
  results, selected, onToggle, query,
}: {
  results: ReturnType<typeof searchSpecializations>;
  selected: string[]; onToggle: (name: string) => void; query: string;
}) {
  if (results.length === 0) {
    return (
      <div className="px-4 py-8 text-center text-sm text-[#575e6b]">
        "<span className="font-medium text-[#16181d]">{query}</span>"에 대한 검색 결과가 없습니다.
      </div>
    );
  }

  return (
    <div>
      <p className="px-4 py-2 text-xs text-[#575e6b] border-b border-[#e0e2e6]">
        {results.length}건의 결과
      </p>
      {results.map(({ leaf, path }) => (
        <LeafCheckbox
          key={leaf.name}
          leaf={leaf}
          checked={selected.includes(leaf.name)}
          onToggle={() => onToggle(leaf.name)}
          breadcrumb={`${path[0]} > ${path[1]}`}
        />
      ))}
    </div>
  );
}

// ── Helpers ─────────────────────────────────────────────────────────────────

function countSelected(l1: SpecLevel1, selected: string[]): number {
  return l1.children.reduce(
    (sum, l2) => sum + l2.children.filter((c) => selected.includes(c.name)).length,
    0,
  );
}
