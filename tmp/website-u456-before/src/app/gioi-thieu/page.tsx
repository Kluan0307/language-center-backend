import type { Metadata } from "next";
import Image from "next/image";
import { Cta } from "@/components/cta";
export const metadata: Metadata = {
  title: "Về trung tâm",
  description:
    "Tìm hiểu định hướng học tiếng Anh, quy trình đánh giá đầu vào và cách theo dõi việc học tại Language Center.",
  alternates: { canonical: "/gioi-thieu" },
};
export default function About() {
  return (
    <>
      <div className="shell">
        <section className="pageHero">
          <p className="eyebrow">LANGUAGE CENTER</p>
          <h1>
            Ngôn ngữ mở ra cơ hội.
            <br />
            Việc học bắt đầu từ bạn.
          </h1>
          <p>
            Chúng tôi hướng đến trải nghiệm học tiếng Anh có mục tiêu rõ ràng,
            từ lựa chọn chương trình đến theo dõi quá trình học.
          </p>
        </section>
        <section className="aboutStrip" id="cach-hoc">
          <Image
            src="/images/study-group.jpg"
            alt="Ảnh minh họa những người học cùng trao đổi"
            width={800}
            height={530}
          />
          <div>
            <p className="eyebrow">CÁCH CHÚNG TÔI ĐỒNG HÀNH</p>
            <h2>
              Hiểu đầu vào.
              <br />
              Rõ lộ trình. Theo dõi tiến bộ.
            </h2>
            <p>
              Đánh giá trình độ là bước để hiểu kiến thức hiện tại và lựa chọn
              lớp phù hợp. Nội dung học được trao đổi dựa trên mục tiêu của từng
              người học.
            </p>
            <p>
              Trong quá trình học, học viên có thể theo dõi lịch, chuyên cần và
              kết quả đã công bố. Ứng dụng học viên giúp tiếp nhận thông báo và
              chủ động theo dõi các thông tin này.
            </p>
            <p>
              Trao đổi với trung tâm khi cần làm rõ lộ trình, lịch học hoặc
              thông tin đăng ký.
            </p>
          </div>
        </section>
      </div>
      <Cta />
    </>
  );
}
