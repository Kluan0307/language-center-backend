"use client";
import { useEffect, useRef, useState, type FormEvent } from "react";

type Option = { slug: string; name: string };
type Course = Option & { programSlug: string | null; branchSlug: string | null };
type Initial = { program: string; branch: string; course: string; schoolClass: string };

async function loadOptions(query: string, signal: AbortSignal): Promise<Course[]> {
  const list: Course[] = [], params = new URLSearchParams(query);
  for (let page = 0; page <= 10000; page++) {
    params.set("page", String(page));
    const response = await fetch("/api/catalog?" + params, { signal, cache: "no-store" });
    if (!response.ok) throw Error("Không tải được danh sách");
    const data = await response.json();
    list.push(...data.items);
    if (!data.hasNext) return list;
  }
  throw Error("Danh sách quá lớn");
}

// Results belong to one query. Old/aborted requests cannot replace the current selection.
function useOptions(query: string, enabled: boolean, revision: number) {
  const key = enabled ? query + ":" + revision : "";
  const [result, setResult] = useState<{ key: string; items: Course[]; failed: boolean }>({ key: "", items: [], failed: false });
  useEffect(() => {
    if (!enabled) return;
    const abort = new AbortController();
    loadOptions(query, abort.signal).then(items => {
      if (!abort.signal.aborted) setResult({ key, items, failed: false });
    }).catch(() => {
      if (!abort.signal.aborted) setResult({ key, items: [], failed: true });
    });
    return () => abort.abort();
  }, [query, enabled, key]);
  const current = enabled && result.key === key;
  return { items: current ? result.items : [], failed: current && result.failed, loading: enabled && !current };
}

export function ConsultationForm({ programs, branches, initial, initialNote = "" }: { initialNote?: string; programs: Option[]; branches: Option[]; initial: Initial }) {
  const [program, setProgram] = useState(initial.program), [branch, setBranch] = useState(initial.branch);
  const [selectedCourse, setCourse] = useState(initial.course), [selectedClass, setSchoolClass] = useState(initial.schoolClass);
  const [revision, setRevision] = useState(0), [busy, setBusy] = useState(false), [error, setError] = useState(""), [success, setSuccess] = useState(false);
  const courses = useOptions(new URLSearchParams({ kind: "COURSE", program, branch }).toString(), Boolean(program && branch), revision);
  const course = program && branch && (courses.loading || courses.items.some(c => c.slug === selectedCourse)) ? selectedCourse : "";
  const classes = useOptions(new URLSearchParams({ kind: "CLASS", course, branch }).toString(), Boolean(course && branch), revision);
  const schoolClass = course && branch && (classes.loading || classes.items.some(c => c.slug === selectedClass)) ? selectedClass : "";
  const attempt = useRef<{ payload: string; key: string } | null>(null), message = useRef<HTMLDivElement>(null);
  useEffect(() => { if (error || success) message.current?.focus(); }, [error, success]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy || courses.loading || classes.loading) return;
    const form = event.currentTarget;
    if (!form.reportValidity()) return;
    const data = new FormData(form);
    const payload = JSON.stringify({ fullName: String(data.get("fullName") || "").trim(), phone: String(data.get("phone") || "").trim(), email: String(data.get("email") || "").trim() || null, branchSlug: branch, programSlug: program || null, courseSlug: course || null, classSlug: schoolClass || null, note: String(data.get("note") || "").trim() || null, consent: data.get("consent") === "on", website: String(data.get("website") || "") });
    if (!attempt.current || attempt.current.payload !== payload) attempt.current = { payload, key: crypto.randomUUID() };
    setBusy(true); setError("");
    try {
      const response = await fetch("/api/consultations", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ ...JSON.parse(payload), requestId: attempt.current.key }), signal: AbortSignal.timeout(15000) });
      const result = await response.json();
      if (response.status !== 202) { if (response.status === 409) attempt.current = null; throw Error(result.message || "Chưa thể gửi yêu cầu. Vui lòng thử lại."); }
      setSuccess(true);
    } catch (cause) {
      setError(cause instanceof Error && cause.name !== "TimeoutError" && cause.name !== "TypeError" ? cause.message : "Kết nối bị gián đoạn. Vui lòng gửi lại; thông tin đã nhập vẫn được giữ.");
    } finally { setBusy(false); }
  }

  if (success) return <div className="formSuccess" ref={message} tabIndex={-1} role="status"><span className="successIcon" aria-hidden>✓</span><p className="eyebrow">ĐÃ GHI NHẬN YÊU CẦU</p><h2>Cảm ơn bạn đã quan tâm.</h2><p>Đội ngũ tư vấn tại <strong>{branches.find(b => b.slug === branch)?.name}</strong> sẽ liên hệ qua số điện thoại bạn cung cấp.</p><p>Bạn chưa cần tạo tài khoản hoặc thanh toán ở bước này.</p><button className="button light" onClick={() => { setSuccess(false); attempt.current = null; }}>Gửi yêu cầu khác</button></div>;

  return <form className="consultationForm" onSubmit={submit} aria-busy={busy}>
    <div className="formIntro"><p className="eyebrow">ĐĂNG KÝ TƯ VẤN</p><h2>Để lại thông tin, chúng tôi sẽ liên hệ.</h2><p>Các trường có dấu * là bắt buộc.</p></div>
    {error && <div role="alert" className="formError" tabIndex={-1} ref={message}>{error}</div>}
    {!branches.length && <p className="formHint" role="status">Thông tin chi nhánh tiếp nhận đang được cập nhật. Vui lòng quay lại sau hoặc liên hệ trung tâm qua kênh chính thức.</p>}
    <fieldset disabled={busy || !branches.length}>
      <legend className="srOnly">Đăng ký tư vấn chương trình học</legend>
      <fieldset className="formSection"><legend>Thông tin liên hệ</legend><div className="formGrid">
        <label>Họ và tên *<input name="fullName" required minLength={2} maxLength={100} autoComplete="name" placeholder="Nhập họ và tên" /></label>
        <label>Số điện thoại di động *<input name="phone" type="tel" required maxLength={30} autoComplete="tel" inputMode="tel" placeholder="Ví dụ: 0901234567" /></label>
        <label className="fullWidth">Email <span className="optional">(không bắt buộc)</span><input name="email" type="email" maxLength={150} autoComplete="email" placeholder="Địa chỉ email của bạn" /></label>
      </div></fieldset>
      <fieldset className="formSection"><legend>Nhu cầu học tập</legend><div className="formGrid">
        <label>Chi nhánh mong muốn *<select name="branch" required value={branch} onChange={e => { setBranch(e.target.value); setCourse(""); setSchoolClass(""); }}><option value="">Chọn chi nhánh</option>{branches.map(b => <option value={b.slug} key={b.slug}>{b.name}</option>)}</select></label>
        <label>Chương trình quan tâm<select name="program" value={program} onChange={e => { setProgram(e.target.value); setCourse(""); setSchoolClass(""); }}><option value="">Tôi cần được tư vấn thêm</option>{programs.map(p => <option key={p.slug} value={p.slug}>{p.name}</option>)}</select></label>
        <label>Khóa học<select name="course" value={course} disabled={!program || !branch || courses.loading} onChange={e => { setCourse(e.target.value); setSchoolClass(""); }} aria-describedby="course-help"><option value="">{courses.loading ? "Đang tải khóa học…" : "Nhờ trung tâm tư vấn khóa phù hợp"}</option>{courses.loading && course && <option value={course}>Đang tải lựa chọn của bạn…</option>}{courses.items.map(c => <option key={c.slug} value={c.slug}>{c.name}</option>)}</select><span id="course-help" className="optional">Chọn chương trình và chi nhánh để xem khóa học.</span></label>
        <label>Lớp học<select name="schoolClass" value={schoolClass} disabled={!course || classes.loading} onChange={e => setSchoolClass(e.target.value)}><option value="">{classes.loading ? "Đang tải lớp học…" : "Tư vấn lịch học phù hợp"}</option>{classes.loading && schoolClass && <option value={schoolClass}>Đang tải lựa chọn của bạn…</option>}{classes.items.map(c => <option key={c.slug} value={c.slug}>{c.name}</option>)}</select></label>
        <label className="fullWidth">Bạn mong muốn điều gì? <span className="optional">(không bắt buộc)</span><textarea name="note" defaultValue={initialNote} rows={3} maxLength={1000} placeholder="Mục tiêu học tập, thời gian thuận tiện hoặc điều bạn muốn hỏi…" /></label>
      </div></fieldset>
      {(courses.failed || classes.failed) && <div className="formHint" role="status">{courses.failed ? "Chưa tải được khóa học. Bạn vẫn có thể gửi yêu cầu tư vấn theo chương trình." : "Chưa tải được lớp học. Bạn vẫn có thể gửi yêu cầu tư vấn theo khóa học."} <button className="inlineButton" type="button" onClick={() => setRevision(v => v + 1)}>Tải lại danh sách</button></div>}
      <label className="websiteTrap" aria-hidden="true">Website<input name="website" tabIndex={-1} autoComplete="off" /></label>
      <label className="consent"><input name="consent" type="checkbox" required /><span>Tôi đồng ý để trung tâm sử dụng thông tin đã cung cấp để liên hệ tư vấn theo <a href="/chinh-sach-bao-mat" target="_blank" rel="noopener noreferrer">chính sách thông tin</a>. *</span></label>
      <button className="button submitButton" type="submit" disabled={!branches.length || busy || courses.loading || classes.loading}>{busy ? "Đang gửi yêu cầu…" : "Gửi yêu cầu tư vấn"}</button>
      <p className="formHint" role="status" aria-live="polite">{courses.loading || classes.loading ? "Đang cập nhật các lựa chọn học tập…" : "Đăng ký tư vấn không đồng nghĩa với ghi danh hoặc giữ chỗ trong lớp."}</p>
    </fieldset>
  </form>;
}
