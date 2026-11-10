// Node 22, no dependencies. Generates documentation only; never connects to a database or edits application source.
const fs = require("node:fs"),
  path = require("node:path"),
  crypto = require("node:crypto");
const root = path.resolve(__dirname, ".."),
  design = path.join(root, "docs/thesis/design"),
  diagrams = path.join(root, "docs/thesis/diagrams");
const read = (p) => fs.readFileSync(path.join(root, p), "utf8");
const write = (p, s) => fs.writeFileSync(path.join(root, p), s);
const schema = JSON.parse(read("docs/thesis/design/schema-v013.metadata.json"));
const actual = JSON.parse(
  read("docs/thesis/design/schema-current.metadata.json"),
);
const walk = (p) =>
  fs
    .readdirSync(p, { withFileTypes: true })
    .flatMap((e) =>
      e.isDirectory() ? walk(path.join(p, e.name)) : [path.join(p, e.name)],
    );
const snake = (s) => s.replace(/([a-z0-9])([A-Z])/g, "$1_$2").toLowerCase();
const clean = (s) =>
  s.replace(/\/\*[\s\S]*?\*\/|\/\/[^\r\n]*/g, (x) =>
    x.replace(/[^\r\n]/g, " "),
  );
function parse(src, className) {
  const s = clean(src),
    start = s.indexOf(
      "{",
      s.search(new RegExp("\\bclass\\s+" + className + "\\b")),
    );
  if (start < 0) throw Error("Class missing " + className);
  const depths = new Array(s.length),
    memberStarts = new Array(s.length);
  let memberStart = start + 1,
    paren = 0,
    depth = 0,
    quote = "",
    escape = false;
  for (let i = start; i < s.length; i++) {
    depths[i] = depth;
    memberStarts[i] = memberStart;
    const c = s[i];
    if (quote) {
      if (escape) escape = false;
      else if (c === "\\") escape = true;
      else if (c === quote) quote = "";
      continue;
    }
    if (c === '"' || c === "'") {
      quote = c;
      continue;
    }
    if (c === "(") paren++;
    if (c === ")") paren--;
    if (
      (c === ";" && depth === 1 && paren === 0) ||
      (c === "}" && depth === 2 && paren === 0)
    )
      memberStart = i + 1;
    if (c === "{") depth++;
    if (c === "}") depth--;
  }
  const out = [];
  const re =
    /\b(private|protected|public)\s+((?:(?:static|final|transient)\s+)*)([\w.$]+(?:\s*<[^;{}=]+>)?(?:\[\])?)\s+(\w+)\s*(?:=[^;]*)?;/g;
  let match;
  while ((match = re.exec(s))) {
    if (depths[match.index] !== 1 || /\b(static|transient)\b/.test(match[2]))
      continue;
    const annotations = s.slice(memberStarts[match.index], match.index);
    if (/@Transient\b/.test(annotations)) continue;
    const type = match[3].replace(/\s+/g, ""),
      name = match[4],
      relation =
        annotations.match(
          /@(ManyToOne|OneToOne|OneToMany|ManyToMany|ElementCollection)\b/,
        )?.[1] ?? null;
    const column =
      annotations.match(
        /@(?:Column|JoinColumn)\s*\([\s\S]*?\bname\s*=\s*"([^"]+)"/,
      )?.[1] ?? snake(name);
    out.push({
      name,
      type,
      column,
      relation,
      id: /@Id\b/.test(annotations),
      version: /@Version\b/.test(annotations),
      annotations: annotations.trim().replace(/\s+/g, " "),
    });
  }
  return out;
}
const entities = [];
for (const service of ["auth", "academic", "teaching", "finance"])
  for (const p of walk(
    path.join(root, service + "-service/src/main/java"),
  ).filter((p) => p.endsWith(".java"))) {
    const source = fs.readFileSync(p, "utf8");
    if (!/@Entity\b/.test(source)) continue;
    const className = path.basename(p, ".java"),
      file = path.relative(root, p).replaceAll("\\", "/"),
      table = source.match(/@Table\s*\(\s*name\s*=\s*"([^"]+)"/)?.[1];
    if (!table) throw Error("Explicit table required " + file);
    const fields = parse(source, className);
    const collections = [
      ...source.matchAll(/@CollectionTable\s*\(\s*name\s*=\s*"([^"]+)"/g),
    ].map((x) => x[1]);
    const embedded = [
      ...source.matchAll(/@Embeddable[\s\S]*?\bclass\s+(\w+)\s*\{/g),
    ].map((x) => ({ name: x[1], fields: parse(source.slice(x.index), x[1]) }));
    entities.push({
      service,
      className,
      table,
      file,
      extends:
        source.match(
          new RegExp("class\\s+" + className + "\\s+extends\\s+(\\w+)"),
        )?.[1] ?? null,
      fields,
      collections,
      embedded,
      sha256: crypto.createHash("sha256").update(source).digest("hex"),
    });
  }
const owners = {},
  groups = {};
for (const e of entities) {
  owners[e.table] =
    e.table === "class_result_barriers" ? "shared-coordination" : e.service;
  for (const t of e.collections) owners[t] = e.service;
}
const catalogs = new Set([
  "branches",
  "rooms",
  "courses",
  "programs",
  "program_levels",
  "course_admission_policies",
  "course_admission_levels",
  "course_duration_plans",
  "class_duration_plans",
  "class_admission_events",
]);
const placements = new Set([
  "placement_assessments",
  "assessment_events",
  "placement_exceptions",
  "enrollment_admissions",
]);
for (const t of schema.tables) {
  if (!owners[t.table])
    throw Error("Table has no entity/collection owner " + t.table);
  groups[t.table] =
    owners[t.table] === "academic"
      ? catalogs.has(t.table)
        ? "academic-catalog"
        : placements.has(t.table)
          ? "academic-placement"
          : "academic-learning"
      : owners[t.table];
}
const descriptions = {
  accounts: "Tài khoản, role, trạng thái đăng nhập và phiên.",
  account_branches:
    "Phân công nhiều chi nhánh; primary là lựa chọn làm việc, không thay tập quyền.",
  account_branch_events: "Lịch sử phân công/thu hồi/đổi chi nhánh chính.",
  account_security_events: "Audit bảo mật, reset/đổi mật khẩu/thu hồi phiên.",
  admins: "Hồ sơ ADMIN liên kết account.",
  staffs: "Hồ sơ STAFF liên kết account.",
  super_admins: "Hồ sơ SUPER_ADMIN liên kết account.",
  teachers: "Hồ sơ chuyên môn, version và trạng thái làm việc Teacher.",
  teacher_profile_events:
    "Snapshot/audit thay đổi hồ sơ Teacher; scalar teacherId.",
  branches: "Chi nhánh Academic sở hữu; Auth chỉ tham chiếu ID.",
  rooms: "Phòng, sức chứa và tình trạng.",
  courses:
    "Khóa: giá, nội dung, trường duration cũ và expectedMinutes đã xác minh.",
  programs: "Chương trình/thang điểm cấu trúc.",
  program_levels:
    "Mức trong chương trình; rank để hiển thị, không quy đổi tự động.",
  course_admission_policies:
    "Phiên bản chính sách đầu vào và mục tiêu đầu ra của khóa.",
  course_admission_levels:
    "ElementCollection snapshot tập mức đầu vào; không phải entity độc lập.",
  class_admission_events: "Lịch sử áp dụng policy khóa cho lớp.",
  course_duration_plans: "Lịch sử xác nhận số phút khóa.",
  class_duration_plans:
    "Lịch sử kế hoạch phút lớp, snapshot nguồn khóa/custom.",
  students: "Hồ sơ học viên; currentLevel là text legacy riêng với assessment.",
  leads: "Tiếp nhận/quan tâm khóa/phụ trách/chuyển đổi thành học viên.",
  lead_activities:
    "Nhật ký chăm sóc, bàn giao, chuyển đổi; NOTE không là contact.",
  school_classes: "Lớp, khóa/phòng/Teacher, kế hoạch snapshot, sĩ số và lịch.",
  schedules: "Lịch tuần mẫu; khác buổi học thực tế.",
  class_sessions:
    "Buổi thực tế: trạng thái/ngày/giờ/actual/planned Teacher, liên kết nghỉ/bù.",
  session_events: "Snapshot trước/sau, lý do và người đổi buổi.",
  enrollments:
    "Đăng ký học, trạng thái, giá snapshot, billing root và refs kết quả.",
  enrollment_periods:
    "Khoảng học [activeFrom, activeUntil); legacy có thể không có dòng.",
  attendances:
    "Điểm danh theo enrollment/buổi; legacy class_session_id nullable.",
  attendance_changes:
    "Audit sửa điểm danh; giữ before/after và người thực hiện.",
  learning_events:
    "Lịch sử trạng thái học viên/đăng ký/chuyển/bảo lưu/hoàn thành.",
  placement_assessments:
    "Đánh giá đầu vào có nguồn, trạng thái/current key và version.",
  assessment_events:
    "Audit assessment trước/sau; không thay chứng cứ assessment.",
  placement_exceptions:
    "Đề nghị/duyệt/tiêu thụ ngoại lệ học thuật với context fingerprint.",
  enrollment_admissions:
    "Snapshot căn cứ chấp nhận cho ENROLL/RESUME/TRANSFER.",
  class_closures:
    "Trạng thái đóng/mở được đối soát, manual revision và epoch bằng chứng.",
  class_closure_events: "Intent/audit đóng mở lớp theo revision.",
  documents: "Metadata file giảng dạy; file vật lý bên ngoài MySQL.",
  document_events: "Lịch sử duyệt/gửi lại và snapshot các revision/file cũ.",
  exams: "Bài kiểm tra và freeze roster; thang/trọng số gợi ý.",
  exam_candidates:
    "Danh sách dự thi chụp theo ngày, enrollmentId có thể null ở legacy.",
  grades: "Điểm hiện tại theo exam/student; 0 khác chưa có grade; @Version.",
  grade_changes: "Audit hiệu chỉnh điểm, trước/sau/actor/reason.",
  evaluation_states:
    "Parent lock lớp, revision policy và teaching sourceRevision.",
  evaluation_policies:
    "Snapshot policy lớp, ngưỡng/chuyên cần và actor/reason.",
  evaluation_policy_exams:
    "ElementCollection quy tắc bài của policy: required/weight/maxScore/date.",
  final_results:
    "Phiên bản kết quả cuối theo enrollment; snapshot bất biến qua service.",
  result_reopens: "Intent/audit mở lại kết quả toàn lớp.",
  class_result_barriers:
    "Metadata phối hợp hẹp Academic/Teaching trong schema dùng chung.",
  tuition: "Khoản phải thu gắn enrollment billing root, số thu cached/version.",
  payments: "Phiếu thu gốc, requestKey/receipt; không xóa bằng hoàn/đảo.",
  tuition_adjustments: "Journal thay học phí/hạn, requestKey/version/reason.",
  payment_corrections: "Journal VOID/REFUND; giữ phiếu gốc.",
};
const groupedIndexes = (table) => {
  const out = {};
  for (const i of schema.indexes.filter((x) => x.table === table))
    (out[i.name] ??= {
      name: i.name,
      unique: !!i.unique,
      columns: [],
    }).columns.push(i.column);
  return Object.values(out);
};
const fkGroups = () => {
  const out = {};
  for (const f of schema.foreignKeys)
    ((out[f.table + "|" + f.name] ??= {
      ...f,
      columns: [],
      targetColumns: [],
    }).columns.push(f.column),
      out[f.table + "|" + f.name].targetColumns.push(f.targetColumn));
  return Object.values(out);
};
const fks = fkGroups();
const primary = (t) =>
  groupedIndexes(t).find((i) => i.name === "PRIMARY")?.columns ?? [];
function hint(c) {
  const n = c.name;
  const e = entities.find((x) => x.table === c.table);
  const f = e?.fields.find(
    (x) =>
      x.column === n &&
      !["OneToMany", "ElementCollection"].includes(x.relation),
  );
  if (c.generation) return "Cột generated MySQL; " + c.generation;
  if (n === "password") return "Hash BCrypt; không mật khẩu rõ.";
  if (n === "token" && c.table === "class_result_barriers")
    return "UUID kiểm cùng hạ tầng khóa; không JWT đăng nhập.";
  if (f?.version) return "@Version JPA: kiểm ghi đè optimistic.";
  if (/revision/.test(n))
    return "Revision/epoch do service quản lý; không tự coi là @Version.";
  if (n === "current_level")
    return "Text legacy chưa xác minh; không thay assessment.";
  if (n === "duration") return "Giá trị legacy; không tự coi là giờ/buổi/phút.";
  if (n === "expected_minutes")
    return "Số phút xác minh; null = chưa xác minh, không0.";
  if (n === "study_status") return "Trạng thái quá trình học, tách PASS/FAIL.";
  if (n === "status" && c.table === "school_classes")
    return "Trạng thái lưu; effective/dateStatus là dẫn xuất, closed khác hết ngày.";
  if (n === "billing_enrollment_id")
    return "ID đăng ký tính phí gốc; giữ khi chuyển cùng khóa.";
  if (n === "completion_result_id" || n === "completion_result_revision")
    return "Ref current Teaching khi đồng bộ hoàn thành; nullable ở legacy.";
  if (n === "active_until")
    return "Mốc kết thúc không bao gồm ngày này; null = còn mở.";
  if (n === "active_from") return "Mốc bắt đầu có bao gồm ngày này.";
  if (/snapshot|before_state|after_state/.test(n))
    return "Ảnh chụp/audit; không dùng dữ liệu hiện tại ghi đè lịch sử.";
  if (n === "request_id" || n === "request_key")
    return "Idempotency intent theo scope/index; giữ khi retry cùng ý định.";
  if (n === "fingerprint")
    return "Hash căn cứ/context; không bằng chứng khảo sát thật.";
  if (n === "paid_amount")
    return "Projection số thu ròng cached; đối chiếu journal khi cần.";
  if (n === "score") return "Điểm 0 hợp lệ; không có dòng/chưa chấm khác0.";
  if (n === "storage_key")
    return "Khóa file nội bộ; không đường dẫn public từ input.";
  if (/_by_name$|teacher_name|student_name/.test(n))
    return "Tên chụp theo nghiệp vụ, không FK định danh.";
  if (/_id$/.test(n))
    return schema.foreignKeys.some((x) => x.table === c.table && x.column === n)
      ? "ID có FK SQL thực tế, xem danh sách bên dưới."
      : "ID scalar/logic; không có FK SQL trong schema này.";
  if (/_at$|_date$|_since$/.test(n))
    return "Ngày/giờ nghiệp vụ; timezone ứng dụng Asia/Ho_Chi_Minh.";
  if (n === "reason" || /_reason$/.test(n))
    return "Lý do nghiệp vụ/audit; validation tùy endpoint.";
  return f
    ? "Ánh xạ " + e.className + "." + f.name + "."
    : "Xem đặc tả bảng và entity/collection tương ứng.";
}
let dict =
  "# Từ điển dữ liệu vật lý — V013\n\nSinh từ metadata MySQL trên bản sao V001–V013 ngày08/11/2026 và entity hiện tại. **53 bảng/625 cột**; không phải schema production đã nâng. [Nguồn JSON](schema-v013.metadata.json) · [Đối chiếu](schema-verification.md) · [Entity](entity-catalog.md). Không chứa dữ liệu nghiệp vụ.\n\nPK/FK/UK/index/nullability/default ở đây là thực tế SQL, không suy từ annotation. Default `∅` là không có default tường minh hoặc SQL NULL theo thông tin information_schema; extra/generation được ghi riêng. Check nghiệp vụ, @Version và immutable qua service không đồng nghĩa DB CHECK/trigger.\n\n## Danh mục\n\n| Bảng | Owner logic | Cột | Mục đích |\n|---|---|---|---|\n";
for (const t of schema.tables)
  dict += `| [${t.table}](#${t.table.replaceAll("_", "-")}) | ${owners[t.table]} | ${schema.columns.filter((x) => x.table === t.table).length} | ${descriptions[t.table] ?? ""} |\n`;
for (const t of schema.tables) {
  dict += `\n<a id="${t.table.replaceAll("_", "-")}"></a>\n\n## ${t.table}\n\n${descriptions[t.table]} Owner logic: **${owners[t.table]}**. Engine ${t.engine}; collation ${t.collation}.\n\n| Cột | Kiểu SQL | Nullable | Default | Key / extra | Ý nghĩa / lưu ý |\n|---|---|---|---|---|---|\n`;
  for (const c of schema.columns.filter((x) => x.table === t.table))
    dict += `| ${c.name} | ${c.type.replaceAll("|", "\\|")} | ${c.nullable} | ${c.default === null ? "∅" : String(c.default).replaceAll("|", "\\|")} | ${[c.key, c.extra].filter(Boolean).join("; ") || "—"} | ${hint(c).replaceAll("|", "\\|")} |\n`;
  dict += "\n**Index/unique:**\n\n";
  for (const i of groupedIndexes(t.table))
    dict += `- ${i.name}: ${i.unique ? "UNIQUE" : "INDEX"} (${i.columns.join(", ")}).\n`;
  if (!groupedIndexes(t.table).length)
    dict += "- Không có index trong metadata.\n";
  dict += "\n**FK SQL:**\n\n";
  const links = fks.filter((x) => x.table === t.table);
  if (!links.length)
    dict += "- Không có FK SQL; không suy ra FK từ tên cột hoặc ID qua API.\n";
  for (const f of links) {
    const rule = schema.rules.find(
      (r) => r.table === f.table && r.name === f.name,
    );
    dict += `- ${f.name}: (${f.columns.join(", ")}) → ${f.targetTable}(${f.targetColumns.join(", ")}); UPDATE ${rule?.update}, DELETE ${rule?.delete}.\n`;
  }
}
write("docs/thesis/design/data-dictionary.md", dict);
let catalog =
  "# Entity và thuộc tính JPA — G14\n\n52 lớp @Entity ánh xạ 51 bảng khác nhau; ResultBarrier được ánh xạ ở hai service vào cùng một bảng. Hai bảng collection bổ sung thành53 bảng. Các DTO, controller, Spring framework và enum không được gán thành bảng/entity riêng. @MappedSuperclass BaseEntity chỉ đóng góp id, không phải bảng độc lập.\n\nScalar ID trong Class Diagram không phải object association JPA; một số scalar vẫn có FK do SQL migration. FK vật lý xem ERD/từ điển; quan hệ miền nghiệp vụ xem sơ đồ logic. Thuộc tính collection/quan hệ là Java, không thêm cột cùng tên.\n\n";
for (const e of entities) {
  catalog += `## ${e.service}.${e.className}\n\nBảng **${e.table}**; [source](../../../${e.file})${e.extends ? `; extends ${e.extends} (id kế thừa)` : ""}.\n\n| Thuộc tính Java | Kiểu | Mapping |\n|---|---|---|\n`;
  for (const f of e.fields)
    catalog += `| ${f.name} | ${f.type} | ${[f.id ? "@Id" : "", f.version ? "@Version" : "", f.relation ?? "", !["OneToMany", "ManyToMany", "ElementCollection"].includes(f.relation) ? f.column : "không cột scalar riêng"].filter(Boolean).join("; ")} |\n`;
  for (const b of e.embedded)
    catalog += `\nEmbeddable ${b.name}: ${b.fields.map((f) => f.name + ":" + f.type).join(", ")}. Không phải entity độc lập.\n`;
  if (e.collections.length)
    catalog += `\nCollection tables: ${e.collections.join(", ")}.\n`;
}
write("docs/thesis/design/entity-catalog.md", catalog);
write(
  "docs/thesis/design/entity-mapping.json",
  JSON.stringify({ entities, owners, groups }, null, 2),
);
const header = (title) =>
  "@startuml\n!pragma layout smetana\nhide methods\nskinparam shadowing false\nskinparam packageStyle rectangle\n" +
  `title ${title}\n`;
function physical(name, selected, overview = false) {
  const set = new Set(selected),
    external = new Set();
  for (const f of fks.filter(
    (f) => set.has(f.table) && !set.has(f.targetTable),
  ))
    external.add(f.targetTable);
  let p = header("ERD vật lý " + name + " — FK thực tế MySQL V013");
  const groupNames = [...new Set(selected.map((t) => groups[t]))];
  for (const g of groupNames) {
    p += `package "${g}" {\n`;
    for (const t of selected.filter((t) => groups[t] === g)) {
      p += `entity "${t}" as T_${t} {\n`;
      for (const c of schema.columns.filter(
        (x) => x.table === t && (!overview || primary(t).includes(x.name)),
      ))
        p += ` {field} ${c.name} : ${c.type.replace(/enum\(.+\)/, "ENUM")} ${c.nullable === "YES" ? "[NULL]" : "[NOT NULL]"}${primary(t).includes(c.name) ? " <<PK>>" : ""}${c.extra.includes("GENERATED") ? " [GENERATED]" : ""}\n`;
      if (!primary(t).length) p += " note: no physical PK\n";
      p += "}\n";
    }
    p += "}\n";
  }
  for (const t of external)
    p += `entity "${t}" as T_${t} <<FK target ngoài module>>\n`;
  for (const f of fks.filter((f) => set.has(f.table))) {
    const nullable = f.columns.some(
        (c) =>
          schema.columns.find((x) => x.table === f.table && x.name === c)
            .nullable === "YES",
      ),
      unique = groupedIndexes(f.table).some(
        (i) =>
          i.unique &&
          i.columns.length === f.columns.length &&
          i.columns.every((c) => f.columns.includes(c)),
      );
    p += `T_${f.targetTable} "${nullable ? "0..1" : "1"}" -- "${unique ? "0..1" : "0..*"}" T_${f.table} : ${f.columns.join(",")}\n`;
  }
  p +=
    "legend bottom\n Liền = FK SQL có thật; cardinality dựa nullable và unique của FK.\n Không có đường nối không đồng nghĩa không có quan hệ nghiệp vụ.\n Scalar ID / HTTP / snapshot không được tự thêm thành FK.\n Chi tiết index/UK/default/generated: data-dictionary.md.\nendlegend\n@enduml\n";
  write("docs/thesis/diagrams/erd-" + name + ".puml", p);
}
physical(
  "physical-overview",
  schema.tables.map((x) => x.table),
  true,
);
for (const g of [...new Set(Object.values(groups))])
  physical(
    g,
    schema.tables.map((x) => x.table).filter((t) => groups[t] === g),
  );
function classModel(name, selected, overview = false) {
  let p = header("Class Diagram " + name + " — entity/domain hiện tại");
  const unique = selected.filter(
      (e) => e.table !== "class_result_barriers" || e.service === "academic",
    ),
    declaredBases = new Set();
  for (const owner of [...new Set(unique.map((e) => owners[e.table]))]) {
    p += `package "${owner}" {\n`;
    for (const e of unique.filter((e) => owners[e.table] === owner)) {
      const alias = e.service + "_" + e.className;
      p += `class "${e.className}" as ${alias} <<Entity: ${e.table}>> {\n`;
      const fields = overview
        ? e.fields.filter(
            (f) =>
              f.id ||
              f.version ||
              /Id$|^id$|version|role|username|fullName|revision|Revision|status|Status|expectedMinutes|snapshot|fingerprint|score|amount|Amount|activeFrom|activeUntil|requestKey|requestId|passPercent|minAttendancePercent|confirmedCount|closed|tuitionFee/.test(
                f.name,
              ),
          )
        : e.fields;
      if (overview && e.extends) p += " id : Long [inherited]\n";
      for (const f of fields) p += ` ${f.name} : ${f.type}\n`;
      p += "}\n";
      if (!overview && e.extends) {
        if (!declaredBases.has(e.service)) {
          p += `abstract class "${e.service}.BaseEntity" as ${e.service}_BaseEntity <<MappedSuperclass>>\n${e.service}_BaseEntity : id : Long\n`;
          declaredBases.add(e.service);
        }
        p += `${e.service}_BaseEntity <|-- ${alias}\n`;
      }
      for (const b of e.embedded) {
        const a = alias + "_" + b.name;
        p += `class "${e.className}.${b.name}" as ${a} <<Embeddable>> {\n`;
        for (const f of b.fields) p += ` ${f.name} : ${f.type}\n`;
        p += `}\n${alias} "1" *-- "0..*" ${a} : ElementCollection\n`;
      }
    }
    p += "}\n";
  }
  for (const e of unique)
    for (const f of e.fields.filter((f) =>
      ["ManyToOne", "OneToOne"].includes(f.relation),
    )) {
      const target = unique.find(
        (x) => x.service === e.service && x.className === f.type,
      );
      if (!target) continue;
      const nullable = !/optional\s*=\s*false|nullable\s*=\s*false/.test(
        f.annotations,
      );
      p += `${e.service}_${e.className} "${f.relation === "OneToOne" ? "0..1" : "0..*"}" --> "${nullable ? "0..1" : "1"}" ${target.service}_${target.className} : ${f.name}\n`;
    }
  if (overview) {
    const refs = [
      ["academic", "SchoolClass", "teacherId", "auth", "Teacher"],
      ["auth", "AccountBranch", "branchId", "academic", "Branch"],
      ["teaching", "Exam", "classId", "academic", "SchoolClass"],
      ["teaching", "Grade", "studentId", "academic", "Student"],
      ["teaching", "FinalResult", "enrollmentId", "academic", "Enrollment"],
      [
        "academic",
        "Enrollment",
        "completionResultId",
        "teaching",
        "FinalResult",
      ],
      ["finance", "Tuition", "enrollmentId", "academic", "Enrollment"],
      ["finance", "Payment", "tuitionId", "finance", "Tuition"],
    ];
    for (const [a, b, c, d, e] of refs)
      if (
        unique.some((x) => x.service === a && x.className === b) &&
        unique.some((x) => x.service === d && x.className === e)
      )
        p += `${a}_${b} ..> ${d}_${e} : ${c} [scalar ID; không JPA]\n`;
  }
  p +=
    "legend bottom\n Liền = object association JPA trong module; thoi = ElementCollection.\n Chấm = scalar ID nghiệp vụ, không phải object association/FK tự sinh.\n Scalar vẫn có thể có FK SQL từ migration: xem ERD vật lý.\n BaseEntity là mapped superclass; DTO/framework không vẽ thành entity.\n ResultBarrier Academic/Teaching là hai mapping cùng một bảng metadata.\nendlegend\n@enduml\n";
  write("docs/thesis/diagrams/class-" + name + ".puml", p);
  return p;
}
const major = new Set([
  "Account",
  "AccountBranch",
  "Teacher",
  "Branch",
  "Course",
  "Program",
  "ProgramLevel",
  "CourseAdmissionPolicy",
  "SchoolClass",
  "ClassSession",
  "Student",
  "Enrollment",
  "EnrollmentPeriod",
  "PlacementAssessment",
  "PlacementException",
  "Exam",
  "Grade",
  "EvaluationPolicy",
  "FinalResult",
  "ResultBarrier",
  "ClassClosure",
  "Tuition",
  "Payment",
  "Document",
]);
const overview = classModel(
  "overview",
  entities.filter((e) => major.has(e.className)),
  true,
);
for (const g of [...new Set(Object.values(groups))])
  classModel(
    g,
    entities.filter((e) => groups[e.table] === g),
  );
write("docs/domain-model.puml", overview);
let logical =
  header("ERD logic — nghiệp vụ, không thay ERD vật lý") +
  "left to right direction\n";
for (const t of [
  "accounts",
  "account_branches",
  "teachers",
  "branches",
  "programs",
  "program_levels",
  "courses",
  "school_classes",
  "class_sessions",
  "students",
  "enrollments",
  "placement_assessments",
  "placement_exceptions",
  "exams",
  "grades",
  "final_results",
  "tuition",
  "payments",
  "class_result_barriers",
])
  logical += `entity "${t}" as L_${t}\n`;
for (const [from, to, label] of [
  ["accounts", "account_branches", "phân công"],
  ["accounts", "teachers", "hồ sơ theo role"],
  ["branches", "school_classes", "scope ID"],
  ["teachers", "school_classes", "phân công ID/HTTP"],
  ["programs", "program_levels", "thang"],
  ["courses", "school_classes", "mở lớp"],
  ["school_classes", "class_sessions", "buổi thực tế"],
  ["students", "enrollments", "lượt học"],
  ["school_classes", "enrollments", "danh sách đăng ký"],
  ["students", "placement_assessments", "đầu vào"],
  ["placement_exceptions", "enrollments", "chấp nhận ngoại lệ"],
  ["school_classes", "exams", "ID/HTTP"],
  ["exams", "grades", "chấm điểm"],
  ["enrollments", "final_results", "kết quả theo lượt học"],
  ["enrollments", "tuition", "billing root ID/HTTP"],
  ["tuition", "payments", "phiếu thu ID"],
  ["class_result_barriers", "final_results", "khóa phối hợp shared schema"],
])
  logical += `L_${from} ..> L_${to} : ${label}\n`;
logical +=
  "legend bottom\n Các đường trong sơ đồ logic chỉ quan hệ nghiệp vụ, không tuyên bố FK.\n Cardinality / khóa / nullable chính xác: ERD vật lý và từ điển V013.\n Shared schema có owner logic; không vẽ thành bốn database độc lập.\nendlegend\n@enduml\n";
write("docs/thesis/diagrams/erd-logical.puml", logical);
// Registry-to-schema comparison, including scalar SQL FK and generated columns not mapped by JPA.
const missing = [],
  unmapped = [],
  scalarFK = [];
for (const e of entities) {
  const columns = schema.columns.filter((x) => x.table === e.table);
  for (const f of e.fields.filter(
    (f) =>
      !["OneToMany", "ManyToMany", "ElementCollection"].includes(f.relation),
  ))
    if (!columns.some((c) => c.name === f.column))
      missing.push(
        e.service + "." + e.className + "." + f.name + " => " + f.column,
      );
}
for (const t of schema.tables) {
  const ee = entities.filter((e) => e.table === t.table);
  if (!ee.length) continue;
  for (const c of schema.columns.filter((c) => c.table === t.table)) {
    if (c.name === "id" && ee.some((e) => e.extends)) continue;
    if (
      !ee.some((e) =>
        e.fields.some(
          (f) =>
            f.column === c.name &&
            !["OneToMany", "ManyToMany", "ElementCollection"].includes(
              f.relation,
            ),
        ),
      )
    )
      unmapped.push(t.table + "." + c.name);
  }
}
for (const f of fks) {
  const e = entities.find((e) => e.table === f.table);
  if (
    e &&
    f.columns.some((c) => e.fields.some((x) => x.column === c && !x.relation))
  )
    scalarFK.push(f.table + "." + f.columns.join(",") + " -> " + f.targetTable);
}
const newTables = schema.tables
  .map((x) => x.table)
  .filter((t) => !actual.tables.some((x) => x.table === t));
const missingColumns = schema.columns
  .filter(
    (c) =>
      actual.tables.some((t) => t.table === c.table) &&
      !actual.columns.some((a) => a.table === c.table && a.name === c.name),
  )
  .map((c) => c.table + "." + c.name);
const changed = schema.columns
  .filter((c) => {
    const a = actual.columns.find(
      (x) => x.table === c.table && x.name === c.name,
    );
    return a && (a.type !== c.type || a.nullable !== c.nullable);
  })
  .map((c) => {
    const a = actual.columns.find(
      (x) => x.table === c.table && x.name === c.name,
    );
    return (
      c.table +
      "." +
      c.name +
      ": " +
      a.type +
      "/" +
      a.nullable +
      " -> " +
      c.type +
      "/" +
      c.nullable
    );
  });
write(
  "docs/thesis/design/model-validation.json",
  JSON.stringify(
    {
      tables: schema.tables.length,
      columns: schema.columns.length,
      entityClasses: entities.length,
      uniqueEntityTables: new Set(entities.map((e) => e.table)).size,
      collectionTables: [...new Set(entities.flatMap((e) => e.collections))],
      mappedFieldsMissingColumns: missing,
      sqlColumnsNotMappedByEntity: unmapped,
      scalarFieldsWithPhysicalFK: scalarFK,
      currentSchema: {
        tables: actual.tables.length,
        columns: actual.columns.length,
        missingTables: newTables,
        missingColumns,
        typeOrNullabilityDifferences: changed,
      },
    },
    null,
    2,
  ),
);
if (missing.length)
  throw Error("Mapped fields missing columns: " + missing.join("; "));
console.log(
  JSON.stringify({
    entities: entities.length,
    tables: schema.tables.length,
    columns: schema.columns.length,
    unmapped,
    scalarFK: scalarFK.length,
    newTables: newTables.length,
    missingColumns: missingColumns.length,
    typeDifferences: changed.length,
  }),
);
