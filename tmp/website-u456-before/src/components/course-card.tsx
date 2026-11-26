import Link from "@/components/loading-link";
import type {CatalogItem} from "@/lib/catalog";
import {money} from "@/lib/catalog";
import {CourseClassification} from "./course-classification";
import {PublicImage} from "./public-image";
export function CourseCard({course:c,audience,branch}:{course:CatalogItem;audience?:string;branch?:string}){const context=new URLSearchParams({...audience?{audience}:{},...branch?{branch}:{}});const href="/khoa-hoc/"+c.slug+(context.size?"?"+context:"");return <article className="courseCard"><PublicImage src={c.imageUrl} alt={c.name}/><div className="courseCardBody"><div className="cardKicker"><span>{c.branchName||"Tư vấn địa điểm học"}</span><span>{c.code}</span></div><h2><Link href={href}>{c.name}</Link></h2><p>{c.summary}</p><CourseClassification value={c.classification}/><dl className="courseFacts"><div><dt>Thời lượng</dt><dd>{c.expectedMinutes!=null?c.expectedMinutes/60+" giờ":"Theo lộ trình tư vấn"}</dd></div><div><dt>Học phí</dt><dd>{c.tuitionFee!=null?money(c.tuitionFee):"Liên hệ tư vấn"}</dd></div></dl><Link className="textLink" href={href}>Khám phá khóa học →</Link></div></article>;}
