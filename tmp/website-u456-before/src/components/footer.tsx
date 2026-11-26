import Image from "next/image";
import Link from "@/components/loading-link";
import { site } from "@/lib/site";
export function Footer() {
  return (
    <footer className="footer">
      <div className="shell footerGrid">
        <div>
          <Link href="/" className="footerBrand">
            <Image className="brandLogo" src="/brand/logo.svg" alt="" width={44} height={44} />Language Center
          </Link>
          <p>
            Học để kết nối.
            <br />
            Tự tin để bước xa hơn.
          </p>
        </div>
        <div>
          <h2>Khám phá</h2>
          <Link href="/khoa-hoc">Tìm khóa học</Link>
          <Link href="/giao-vien">Đội ngũ giáo viên</Link>
          <Link href="/chuong-trinh">Chương trình học</Link>
          <Link href="/gioi-thieu">Về trung tâm</Link>
          <Link href="/chi-nhanh">Chi nhánh</Link>
        </div>
        <div>
          <h2>Đồng hành cùng bạn</h2>
          <Link href="/tu-van">Tư vấn lộ trình</Link>
          <Link href="/chinh-sach-bao-mat">Chính sách thông tin</Link>
          {site.hotline && (
            <a href={"tel:" + site.hotline.replace(/[^+0-9]/g, "")}>
              {site.hotline}
            </a>
          )}
          {site.email && <a href={"mailto:" + site.email}>{site.email}</a>}
        </div>
      </div>
      <div className="shell footerBottom">
        <span>© {new Date().getFullYear()} Language Center</span>
        <span>Ảnh minh họa học tập: Unsplash</span>
        <a href="#main">Lên đầu trang ↑</a>
      </div>
    </footer>
  );
}
