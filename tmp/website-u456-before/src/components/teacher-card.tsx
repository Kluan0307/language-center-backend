import Link from "@/components/loading-link";
import type {Teacher} from "@/lib/teachers";
import {PublicImage} from "./public-image";
export function TeacherCard({teacher:t}:{teacher:Teacher}){return <article className="teacherCard">{t.imageUrl?<PublicImage src={t.imageUrl} alt={t.name}/>:<div className="teacherInitials" aria-hidden>{t.name.split(/\s+/).slice(-2).map(s=>s[0]).join("")}</div>}<div className="teacherCardBody"><p className="eyebrow">{t.specialization||"ĐỘI NGŨ GIẢNG DẠY"}</p><h2>{t.name}</h2>{t.teachingFocus&&<p>{t.teachingFocus}</p>}{t.qualification&&<p className="qualification">{t.qualification}</p>}<Link className="textLink" href={"/giao-vien/"+t.slug}>Tìm hiểu giáo viên →</Link></div></article>;}
