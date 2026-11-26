"use client";
import Image from "next/image";
import Link from "@/components/loading-link";
import { usePathname, useSearchParams } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import type { NavigationMenu } from "@/lib/navigation-model";

export function Header({ menus }: { menus: NavigationMenu[] }) {
  const [open, setOpen] = useState(false), pathname = usePathname(), params = useSearchParams();
  const root = useRef<HTMLElement>(null), menuButton = useRef<HTMLButtonElement>(null);
  const close = () => {
    setOpen(false);
    root.current?.querySelectorAll<HTMLDetailsElement>("details[open]").forEach(d => d.open = false);
  };
  useEffect(() => {
    const outside = (event: PointerEvent) => {
      if (root.current && !root.current.contains(event.target as Node)) {
        setOpen(false);
        root.current.querySelectorAll<HTMLDetailsElement>("details[open]").forEach(d => d.open = false);
      }
    };
    document.addEventListener("pointerdown", outside);
    return () => document.removeEventListener("pointerdown", outside);
  }, []);
  function active(href: string) {
    const url = new URL(href, "https://navigation.local");
    if (pathname !== url.pathname && !pathname.startsWith(url.pathname + "/")) return false;
    if (!url.search) return pathname !== "/khoa-hoc" || !params.get("program") && !params.get("audience");
    return Array.from(url.searchParams).every(([key, value]) => params.get(key) === value);
  }
  function renderMenus() {
    return menus.map(menu => menu.children ? (
      <details className="navDropdown" key={menu.label} data-active={menu.children.some(c => active(c.href)) || undefined}>
        <summary>{menu.label}<span className="navChevron" aria-hidden="true">⌄</span></summary>
        <div className="navDropdownPanel">
          {menu.children.map(link => <Link key={link.href} href={link.href} onClick={close} aria-current={active(link.href) ? "page" : undefined}>{link.label}</Link>)}
        </div>
      </details>
    ) : <Link key={menu.label} href={menu.href!} onClick={close} aria-current={active(menu.href!) ? "page" : undefined}>{menu.label}</Link>);
  }
  return <header className="header catalogHeader" ref={root} onKeyDown={event => {
    if (event.key !== "Escape") return;
    const detail = (event.target as HTMLElement).closest<HTMLDetailsElement>("details[open]");
    if (detail) { detail.open = false; detail.querySelector<HTMLElement>("summary")?.focus(); }
    else { close(); menuButton.current?.focus(); }
  }}>
    <div className="shell headerInner">
      <Link href="/" className="brand" onClick={close} aria-label="Language Center — Trang chủ">
        <Image className="brandLogo" src="/brand/logo.svg" alt="" width={48} height={48} priority />
        <span>Language<br /><strong>Center</strong></span>
      </Link>
      <nav className="headerUtility" aria-label="Khám phá trung tâm">
        <Link href="/khoa-hoc" onClick={close}>Tất cả khóa học</Link>
        <Link href="/chuong-trinh" onClick={close}>Chương trình</Link>
        <Link href="/giao-vien" onClick={close}>Giáo viên</Link>
        <Link href="/chi-nhanh" onClick={close}>Chi nhánh</Link>
      </nav>
      <Link href="/tu-van" className="button small headerCta" onClick={close}>Tư vấn ngay <span aria-hidden="true">↗</span></Link>
      <button ref={menuButton} className="menuButton" aria-label={open ? "Đóng menu" : "Mở menu"} aria-expanded={open} aria-controls="mobile-navigation" onClick={() => setOpen(!open)}>{open ? "Đóng ×" : "Menu ☰"}</button>
    </div>
    <nav className="shell desktopNav catalogNav" aria-label="Chương trình và đối tượng học">{renderMenus()}</nav>
    {open && <nav id="mobile-navigation" className="mobileNav catalogMobileNav" aria-label="Điều hướng trên điện thoại">
      <Link href="/khoa-hoc" onClick={close}>Tất cả khóa học</Link>
      {renderMenus()}
      <Link className="button" href="/tu-van" onClick={close}>Tư vấn ngay ↗</Link>
    </nav>}
  </header>;
}
