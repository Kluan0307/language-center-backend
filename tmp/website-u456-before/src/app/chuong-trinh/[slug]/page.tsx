import type { Metadata } from "next";
import Link from "@/components/loading-link";
import { notFound } from "next/navigation";
import { findProgram } from "@/lib/programs";
import { CourseCatalog } from "@/components/course-catalog";
import { PublicImage } from "@/components/public-image";
import { Arrow } from "@/components/arrow";
export const dynamic = "force-dynamic";
export async function generateMetadata({
  params,
}: {
  params: Promise<{ slug: string }>;
}): Promise<Metadata> {
  const program = await findProgram((await params).slug);
  return program
    ? {
        title: program.name,
        description: program.description,
        alternates: { canonical: "/chuong-trinh/" + program.slug },
      }
    : { title: "Không tìm thấy chương trình" };
}
export default async function ProgramDetail({
  params,
}: {
  params: Promise<{ slug: string }>;
}) {
  const program = await findProgram((await params).slug);
  if (!program) notFound();
  return (
    <div className="shell">
      <section className="pageHero">
        <nav className="breadcrumbs" aria-label="Vị trí">
          <Link href="/">Trang chủ</Link>
          <span>/</span>
          <Link href="/chuong-trinh">Chương trình</Link>
          <span>/</span>
          <span>{program.name}</span>
        </nav>
        <p className="eyebrow">{program.tag}</p>
        <h1>{program.name}</h1>
        <p>{program.description}</p>
      </section>
      <PublicImage src={program.imageUrl} alt={program.name} />
      <CourseCatalog program={program.slug} />
      <div className="contentGrid">
        <div>
          <section className="contentSection">
            <p className="eyebrow">DÀNH CHO AI?</p>
            <h2>{program.lead}</h2>
            <p>{program.audience}</p>
          </section>
          <section className="contentSection">
            <p className="eyebrow">BẠN SẼ KHÁM PHÁ</p>
            <h2>Nội dung học tập</h2>
            <ul className="topicList">
              {program.topics.map((topic, index) => (
                <li key={topic}>
                  <span>0{index + 1}</span>
                  {topic}
                </li>
              ))}
            </ul>
          </section>
          <section className="contentSection">
            <h2>Bắt đầu như thế nào?</h2>
            <p>{program.approach}</p>
            <p>
              Thời lượng, lịch học và học phí được tư vấn theo khóa học, trình
              độ và chi nhánh phù hợp. Trung tâm xác nhận các thông tin này
              trước khi đăng ký học.
            </p>
          </section>
        </div>
        <aside className="infoPanel">
          <p className="eyebrow">LỘ TRÌNH CỦA BẠN</p>
          <h2>
            Cùng tìm điểm
            <br />
            bắt đầu phù hợp.
          </h2>
          <p>
            Chọn mục tiêu của bạn. Tìm hiểu cách đánh giá trình độ và lựa chọn
            lớp trước khi bắt đầu.
          </p>
          <Link
            href={"/tu-van?chuong-trinh=" + program.slug}
            className="button"
          >
            Tìm hiểu tư vấn <Arrow diagonal />
          </Link>
          <Link className="textLink" href="/chuong-trinh">
            ← Các chương trình khác
          </Link>
        </aside>
      </div>
    </div>
  );
}
