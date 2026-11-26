"use client";
import { useState, useSyncExternalStore, type ReactNode } from "react";
const query = "(max-width: 600px)";
function subscribe(callback: () => void) { const media = window.matchMedia(query); media.addEventListener("change", callback); return () => media.removeEventListener("change", callback); }
const snapshot = () => window.matchMedia(query).matches;
const serverSnapshot = () => false;
export function AdvancedFilters({ children, active }: { children: ReactNode; active: boolean }) {
  const mobile = useSyncExternalStore(subscribe, snapshot, serverSnapshot);
  const [expanded, setExpanded] = useState(active);
  return <details className="advancedFilters" open={!mobile || expanded} onToggle={event => { if (mobile) setExpanded(event.currentTarget.open); }}>
    <summary>Thêm bộ lọc{active ? " · Đang áp dụng" : ""}<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true"><path d="m6 9 6 6 6-6" /></svg></summary>
    <div className="filtersExtra">{children}</div>
  </details>;
}
