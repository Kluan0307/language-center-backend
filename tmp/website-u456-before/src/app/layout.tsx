import type { Metadata } from "next";
import { Suspense } from "react";
import { getSiteNavigation } from "@/lib/navigation";
import { Header } from "@/components/header";
import { Footer } from "@/components/footer";
import { site, siteUrl, indexable } from "@/lib/site";
import localFont from "next/font/local";
import "./tokens.css";
import "./globals.css";
import "./header.css";
const sans = localFont({
  src: [
    { path: "./fonts/BeVietnamPro-Regular.ttf", weight: "400", style: "normal" },
    { path: "./fonts/BeVietnamPro-Medium.ttf", weight: "500", style: "normal" },
    { path: "./fonts/BeVietnamPro-SemiBold.ttf", weight: "600", style: "normal" },
    { path: "./fonts/BeVietnamPro-Bold.ttf", weight: "700", style: "normal" },
  ],
  variable: "--font-sans", display: "swap", fallback: ["Segoe UI", "Arial", "sans-serif"],
});
export const metadata: Metadata = {
  metadataBase: siteUrl(),
  title: {
    default: "Language Center — Học để vươn xa",
    template: "%s | Language Center",
  },
  description: site.description,
  robots: { index: indexable, follow: indexable },
  openGraph: {
    type: "website",
    locale: "vi_VN",
    siteName: site.name,
    title: "Language Center — Học để vươn xa",
    description: site.description,
    images: [
      {
        url: "/images/study-group.jpg",
        width: 1600,
        height: 1066,
        alt: "Ảnh minh họa học tập nhóm",
      },
    ],
  },
};
export default async function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const menus = await getSiteNavigation();
  return (
    <html lang="vi" className={sans.variable}>
      <body>
        <a className="skipLink" href="#main">
          Đến nội dung chính
        </a>
        <Suspense fallback={<div className="header" aria-label="Điều hướng đang tải" />}><Header menus={menus} /></Suspense>
        <main id="main">{children}</main>
        <Footer />
      </body>
    </html>
  );
}
