import type { Metadata } from "next";
import Link from "@/components/loading-link";
import { getPrograms, findProgram } from "@/lib/programs";
import { ProgramCard } from "@/components/program-card";
import { Cta } from "@/components/cta";
export const metadata: Metadata = {
  title: "Chương trình tiếng Anh",
  description:
    "Tìm chương trình tiếng Anh nền tảng, giao tiếp, IELTS và TOEIC theo mục tiêu học tập.",
  alternates: { canonical: "/chuong-trinh" },
};
export const dynamic = "force-dynamic";
export default async function Programs({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const params = await searchParams,
    goal = typeof params["muc-tieu"] === "string" ? params["muc-tieu"] : "";
  const page=Math.max(0,Math.min(10000,Math.floor(Number(params.page)||0)));
  const result=await getPrograms(Math.floor(page));
  const programs=result.items;
  const match=goal?await findProgram(goal):undefined;
  const selected=goal?(match?[match]:[]):programs;
  return (
    <>
      <div className="shell">
        <section className="pageHero">
          <nav className="breadcrumbs" aria-label="Vị trí">
            <Link href="/">Trang chủ</Link>
            <span>/</span>
            <span>Chương trình học</span>
          </nav>
          <p className="eyebrow">HỌC TIẾNG ANH THEO CÁCH CỦA BẠN</p>
          <h1>
            Mục tiêu khác nhau.
            <br />
            Điểm bắt đầu phù hợp.
          </h1>
          <p>
            Tìm hiểu từng chương trình để lựa chọn hướng học. Trình độ và lộ
            trình cụ thể được xác định sau khi đánh giá đầu vào.
          </p>
        </section>
        <p className="programBrowse"><Link className="textLink" href="/khoa-hoc">Tìm khóa học theo chi nhánh và học phí →</Link></p>
        <form className="filterForm" action="/chuong-trinh" method="get">
          <label>
            Mục tiêu của bạn
            <select name="muc-tieu" defaultValue={goal}>
              <option value="">Tất cả chương trình</option>
              {programs.map((program) => (
                <option key={program.slug} value={program.slug}>
                  {program.name}
                </option>
              ))}
            </select>
          </label>
          <button className="button" type="submit">
            Xem chương trình
          </button>
          {goal && (
            <Link className="textLink" href="/chuong-trinh">
              Bỏ bộ lọc ×
            </Link>
          )}
        </form>
        <div className="programGrid" style={{ paddingBottom: 70 }}>
          {selected.length ? (
            selected.map((program) => (
              <ProgramCard key={program.slug} program={program} />
            ))
          ) : (
            <div className="noResults">
              <h2>Chưa có chương trình phù hợp</h2>
              <p>Chọn một mục tiêu khác để tiếp tục khám phá.</p>
              <Link className="textLink" href="/chuong-trinh">
                Xem tất cả chương trình →
              </Link>
            </div>
          )}
        </div>
      </div>
      <nav className="shell pagination" aria-label="Phân trang">
        {page>0 && <Link className="button light" href={"/chuong-trinh?page="+(page-1)}>← Trang trước</Link>}
        {result.hasNext && <Link className="button" href={"/chuong-trinh?page="+(page+1)}>Trang tiếp →</Link>}
      </nav>
      <Cta />
    </>
  );
}
