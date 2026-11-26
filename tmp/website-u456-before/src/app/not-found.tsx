import Link from "@/components/loading-link";
export default function NotFound() {
  return (
    <section className="shell section">
      <p className="eyebrow">404 · KHÔNG TÌM THẤY TRANG</p>
      <h1 className="pageTitle">Có vẻ bạn đã đi xa một chút.</h1>
      <p>Quay lại để khám phá các chương trình học.</p>
      <Link href="/chuong-trinh" className="button">
        Xem chương trình →
      </Link>
    </section>
  );
}
