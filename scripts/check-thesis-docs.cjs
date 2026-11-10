// Static documentation verification; no network, no application or DB mutations.
const fs = require("node:fs"),
  path = require("node:path"),
  assert = require("node:assert/strict"),
  crypto = require("node:crypto");
const root = path.resolve(__dirname, ".."),
  base = path.join(root, "docs/thesis");
const walk = (p) =>
  fs
    .readdirSync(p, { withFileTypes: true })
    .flatMap((e) =>
      e.isDirectory() ? walk(path.join(p, e.name)) : [path.join(p, e.name)],
    );
const read = (p) => fs.readFileSync(p, "utf8"),
  sha = (p) =>
    crypto.createHash("sha256").update(fs.readFileSync(p)).digest("hex");
let checks = 0;
function check(value, message) {
  assert.ok(value, message);
  checks++;
}
const failures = [];
const documentFiles = walk(base).filter((x) => x.endsWith(".md"));
// Include the current handoffs: the original checker missed broken links here.
for (const stage of ["G15", "G16", "G19", "G20"])
  documentFiles.push(path.join(root, "docs/thesis-progress", stage + ".md"));
for (const p of documentFiles) {
  const s = read(p);
  for (const m of s.matchAll(/\[[^\]\r\n]*\]\(([^)]+)\)/g)) {
    const url = m[1].replace(/^<|>$/g, "").split(/\s+"/)[0];
    if (/^(https?:|mailto:)/.test(url)) continue;
    const [file, fragment] = url.split("#");
    const target = file
      ? path.resolve(path.dirname(p), decodeURIComponent(file))
      : p;
    if (!fs.existsSync(target)) {
      failures.push(path.relative(root, p) + " -> " + url);
      continue;
    }
    if (fragment && target.endsWith(".md")) {
      const body = read(target),
        headings = [...body.matchAll(/^#{1,6}\s+(.+)$/gm)].map((x) =>
          x[1]
            .toLowerCase()
            .replace(/<[^>]*>/g, "")
            .replace(/[^\p{L}\p{N}_\-\s]/gu, "")
            .trim()
            .replace(/\s/g, "-"),
        );
      if (
        !body.includes('id="' + fragment + '"') &&
        !headings.includes(fragment)
      )
        failures.push(path.relative(root, p) + " -> missing anchor " + url);
    }
    checks++;
  }
}
check(!failures.length, "Broken local links: " + failures.join("; "));
const meta = JSON.parse(
    read(path.join(base, "design/schema-v013.metadata.json")),
  ),
  mapping = JSON.parse(read(path.join(base, "design/entity-mapping.json"))),
  validation = JSON.parse(
    read(path.join(base, "design/model-validation.json")),
  ),
  dict = read(path.join(base, "design/data-dictionary.md"));
const tables = new Set(
  mapping.entities.flatMap((e) => [e.table, ...e.collections]),
);
check(tables.size === meta.tables.length, "Table coverage count");
for (const t of meta.tables) {
  check(tables.has(t.table), "Missing table owner " + t.table);
  const anchor = "## " + t.table + "\n",
    start = dict.indexOf(anchor);
  check(start >= 0, "Missing table dictionary " + t.table);
  const end = dict.indexOf("\n## ", start + anchor.length),
    section = dict.slice(start, end < 0 ? undefined : end);
  for (const c of meta.columns.filter((c) => c.table === t.table))
    check(
      section.includes("| " + c.name + " |"),
      "Missing dictionary column " + t.table + "." + c.name,
    );
}
// Verify rendered attribute text too: PlantUML can parse SQL types with parentheses as methods.
for (const t of meta.tables) {
  const svg = read(
    path.join(base, "diagrams/erd-" + mapping.groups[t.table] + ".svg"),
  );
  const start = svg.indexOf('data-entity="T_' + t.table + '"');
  check(start >= 0, "Missing rendered table " + t.table);
  const end = svg.indexOf("</g>", start),
    body = svg.slice(start, end);
  for (const c of meta.columns.filter((c) => c.table === t.table))
    check(
      body.includes(c.name + " : "),
      "Hidden rendered column " + t.table + "." + c.name,
    );
}
for (const e of mapping.entities) {
  if (e.table === "class_result_barriers" && e.service === "teaching") continue;
  const svg = read(
    path.join(base, "diagrams/class-" + mapping.groups[e.table] + ".svg"),
  );
  const start = svg.indexOf(
    'data-entity="' + e.service + "_" + e.className + '"',
  );
  check(start >= 0, "Missing rendered entity " + e.service + "." + e.className);
  const end = svg.indexOf("</g>", start),
    body = svg.slice(start, end);
  for (const f of e.fields)
    check(
      body.includes(f.name + " : "),
      "Hidden rendered JPA field " + e.className + "." + f.name,
    );
}
check(
  validation.mappedFieldsMissingColumns.length === 0,
  "JPA field missing SQL column",
);
check(
  JSON.stringify(validation.sqlColumnsNotMappedByEntity) ===
    JSON.stringify(["final_results.current_enrollment_id"]),
  "Unexpected unmapped SQL columns need explicit review",
);
for (const e of mapping.entities) {
  const p = path.join(root, e.file);
  check(fs.existsSync(p), "Entity source missing " + e.file);
  check(sha(p) === e.sha256, "Stale entity mapping " + e.file);
  for (const f of e.fields.filter(
    (f) =>
      !["OneToMany", "ManyToMany", "ElementCollection"].includes(f.relation),
  ))
    check(
      meta.columns.some((c) => c.table === e.table && c.name === f.column),
      "Mapped column missing " + e.className + "." + f.name,
    );
}
const sourceEntities=['auth','academic','teaching','finance'].flatMap(service=>walk(path.join(root,service+'-service/src/main/java')).filter(p=>p.endsWith('.java')&&/@Entity\b/.test(read(p))).map(p=>path.relative(root,p).replaceAll('\\','/')));check(sourceEntities.length===mapping.entities.length,'Entity registry count drift');for(const file of sourceEntities)check(mapping.entities.some(e=>e.file===file),'Entity missing registry '+file);
let count = 0;
const manifest = JSON.parse(
  read(path.join(base, "diagrams/render-manifest.json")),
);
for (const p of walk(path.join(base, "diagrams")).filter((p) =>
  p.endsWith(".puml"),
)) {
  const svg = p.replace(/\.puml$/, ".svg"),
    m = manifest.diagrams.find((x) => x.source === path.basename(p));
  check(fs.existsSync(svg), "Missing SVG " + svg);
  check(
    m && m.sourceSha256 === sha(p) && m.svgSha256 === sha(svg),
    "Stale render " + p,
  );
  check(
    /<svg\b/.test(read(svg)) &&
      !/Syntax Error|An error has occurred/.test(read(svg)),
    "Invalid SVG " + svg,
  );
  count++;
}
check(count === 32, "Expected32 canonical diagrams");
check(
  read(path.join(root, "docs/domain-model.puml")) ===
    read(path.join(base, "diagrams/class-overview.puml")),
  "Domain alias drift",
);
const ucs = read(path.join(base, "use-cases.md")),
  trace = read(path.join(base, "traceability.md"));
for (let i = 1; i <= 18; i++)
  check(
    ucs.includes('id="uc' + String(i).padStart(2, "0") + '"'),
    "UC missing " + i,
  );
for (let i = 1; i <= 12; i++)
  check(
    trace.includes("RQ" + String(i).padStart(2, "0")),
    "RQ mapping missing " + i,
  );
for (const p of walk(path.join(base, "diagrams")).filter((p) =>
  /use-case-.*\.puml$/.test(p),
)) {
  const s = read(p);
  check(
    !s.includes("<|--"),
    "Actor inheritance must not imply permissions " + p,
  );
  check(
    !s.includes("includeurl") && !s.includes("!include "),
    "Unexpected external diagram dependency " + p,
  );
}
const result = {
  passed: true,
  checks,
  diagrams: count,
  tables: meta.tables.length,
  columns: meta.columns.length,
  entityClasses: mapping.entities.length,
  scope:
    "Static documentation/links/source hashes/schema metadata coverage; not business regression or user survey",
};
if (process.argv.includes("--report"))
  fs.writeFileSync(
    path.join(root, "storage/g13-g14-doc-validation.json"),
    JSON.stringify(result, null, 2),
  );
console.log(JSON.stringify(result));
