import "server-only";
import { audienceOptions, catalogOptions, getCatalog } from "./catalog";
import type { NavigationMenu } from "./navigation-model";

export async function getSiteNavigation(): Promise<NavigationMenu[]> {
  const menus: NavigationMenu[] = [];
  try {
    const [audiences, programs, courses] = await Promise.all([
      audienceOptions(), catalogOptions("PROGRAM"), getCatalog("COURSE", { size: 100 }),
    ]);
    const exists = (code: string) => audiences.some(a => a.code === code);
    const path = (program: string, audience?: string) => "/khoa-hoc?" + new URLSearchParams({ program, ...(audience ? { audience } : {}) });
    for (const [code, label] of [["PRESCHOOL", "Tiếng Anh Mẫu giáo"], ["PRIMARY", "Tiếng Anh Tiểu học"], ["SECONDARY", "Tiếng Anh THCS"]]) {
      if (exists(code)) menus.push({ label, href: "/khoa-hoc?audience=" + code });
    }
    if (exists("ADULT")) {
      const children = [{ label: "Tất cả khóa cho người lớn", href: "/khoa-hoc?audience=ADULT" }];
      for (const code of ["GE", "COMMUNICATION"]) {
        const p = programs.find(p => p.code === code);
        if (p) children.push({ label: p.name, href: path(p.slug, "ADULT") });
      }
      menus.push({ label: "Tiếng Anh Người lớn", children });
    }
    for (const code of ["IELTS", "TOEIC"]) {
      const p = programs.find(p => p.code === code);
      if (!p) continue;
      const children = [{ label: "Giới thiệu chương trình", href: "/chuong-trinh/" + p.slug }, { label: "Tất cả khóa " + p.name, href: path(p.slug) }];
      for (const a of audiences) {
        if (courses.items.some(c => c.programSlug === p.slug && c.classification?.audiences.some(g => g.code === a.code)))
          children.push({ label: p.name + " · " + a.name, href: path(p.slug, a.code) });
      }
      menus.push({ label: code, children });
    }
  } catch {
    menus.push({ label: "Tìm khóa học", href: "/khoa-hoc" });
  }
  menus.push({ label: "Giới thiệu", children: [
    { label: "Về trung tâm", href: "/gioi-thieu" },
    { label: "Đội ngũ giáo viên", href: "/giao-vien" },
    { label: "Các chi nhánh", href: "/chi-nhanh" },
    { label: "Tất cả chương trình", href: "/chuong-trinh" },
  ] });
  return menus;
}
