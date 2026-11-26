// DCK5 acceptance verification — runs after migrate-local.cjs succeeds
// Tests: health, retained demo accounts, branch/role permissions,
//        RBAC enforcement, data counts, website catalog, and Gateway smoke.
const c = require('./data-common.cjs');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const GATEWAY = 'http://127.0.0.1:18080';
const ADMIN_WEB = 'http://127.0.0.1:15173';
const WEBSITE  = 'http://127.0.0.1:13000';
const TIMEOUT  = 10000;

async function get(url, headers = {}) {
  const r = await fetch(url, { headers, signal: AbortSignal.timeout(TIMEOUT) });
  return r;
}
async function post(url, body, headers = {}) {
  return fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...headers },
    body: JSON.stringify(body),
    signal: AbortSignal.timeout(TIMEOUT)
  });
}
async function login(username, password) {
  const r = await post(`${GATEWAY}/api/v1/auth/login`, { username, password });
  assert.equal(r.status, 200, `Login failed for ${username}: HTTP ${r.status}`);
  const j = await r.json();
  assert.ok(j.token, `No token for ${username}`);
  return j.token;
}

const checks = [];
function record(name, fn) {
  return fn().then(() => { checks.push({ name, status: 'passed' }); })
            .catch(e => { checks.push({ name, status: 'failed', error: e.message }); throw e; });
}

async function main() {
  // --- 1. Health checks for all 9 containers ---
  await record('All containers healthy', async () => {
    const ctx = c.context();
    const healthy = ctx.states.filter(s => s.State.Health?.Status === 'healthy').length;
    assert.equal(healthy, 9, `Only ${healthy}/9 containers healthy`);
  });

  // --- 2. Gateway readiness ---
  await record('Gateway readiness UP', async () => {
    const r = await get(`${GATEWAY}/actuator/health/readiness`);
    assert.equal(r.status, 200);
    const j = await r.json();
    assert.equal(j.status, 'UP');
  });

  // --- 3. Data presence in Docker MySQL ---
  await record('Docker DB matches current schema manifest with data', async () => {
    const rows = c.mysql("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_type='BASE TABLE'");
    const manifest=JSON.parse(fs.readFileSync(path.join(c.root,'docs/docker/schema-manifest.json'),'utf8'));
    assert.equal(Number(rows), manifest.tableCount);
    const accounts = Number(c.mysql("SELECT COUNT(*) FROM accounts"));
    assert.ok(accounts > 0, `No accounts in Docker DB`);
    const students = Number(c.mysql("SELECT COUNT(*) FROM students"));
    assert.ok(students > 0, `No students in Docker DB`);
  });

  // --- 4. Login: superadmin ---
  let superToken;
  await record('Login superadmin', async () => {
    superToken = await login('superadmin', '123456');
  });

  // --- 5. Login: admin1 ---
  let adminToken;
  await record('Login admin1', async () => {
    adminToken = await login('admin1', '123456');
  });

  let staffToken;
  await record('Login staff1', async () => {
    staffToken = await login('staff1', '123456');
  });

  // --- 6. Login: teacher1 ---
  let teacherToken;
  await record('Login teacher1', async () => {
    teacherToken = await login('teacher1', '123456');
  });

  // --- 7. Login: student hv0006 ---
  let studentToken;
  await record('Login student hv0006', async () => {
    studentToken = await login('hv0006', '123456');
  });

  // --- 8. Unauthenticated → 401 ---
  await record('Unauthenticated request returns 401', async () => {
    const r = await get(`${GATEWAY}/api/v1/academic/leads`);
    assert.equal(r.status, 401);
    const rid = r.headers.get('x-request-id');
    assert.ok(rid && /^[A-Za-z0-9-]{1,64}$/.test(rid), 'Missing x-request-id');
  });

  // --- 9. RBAC: teacher cannot access leads ---
  await record('Teacher cannot access leads (403)', async () => {
    const r = await get(`${GATEWAY}/api/v1/academic/leads`, { Authorization: `Bearer ${teacherToken}` });
    assert.equal(r.status, 403);
  });

  // --- 10. RBAC: student cannot access leads ---
  await record('Student cannot access leads (403)', async () => {
    const r = await get(`${GATEWAY}/api/v1/academic/leads`, { Authorization: `Bearer ${studentToken}` });
    assert.equal(r.status, 403);
  });

  // --- 11. Admin can list leads ---
  await record('Admin can list leads', async () => {
    const r = await get(`${GATEWAY}/api/v1/academic/leads`, { Authorization: `Bearer ${adminToken}` });
    assert.ok([200, 206].includes(r.status), `Expected 200 got ${r.status}`);
  });

  // --- 12. Superadmin can list accounts ---
  await record('Superadmin can list accounts', async () => {
    const r = await get(`${GATEWAY}/api/v1/auth/accounts`, { Authorization: `Bearer ${superToken}` });
    assert.ok([200].includes(r.status), `Expected 200 got ${r.status}`);
  });

  // --- 13. Teacher can view schedule ---
  await record('Teacher can view own schedule', async () => {
    const r = await get(`${GATEWAY}/api/v1/academic/schedules/my`, { Authorization: `Bearer ${teacherToken}` });
    assert.equal(r.status, 200);
    assert.ok(Array.isArray((await r.json()).content));
  });

  // --- 14. Admin web health ---
  await record('Web admin serves login page', async () => {
    const r = await get(ADMIN_WEB);
    assert.ok([200, 301, 302].includes(r.status), `Admin web HTTP ${r.status}`);
  });

  // --- 15. Website public homepage ---
  await record('Website serves homepage', async () => {
    const r = await get(WEBSITE);
    assert.ok([200, 301, 302].includes(r.status), `Website HTTP ${r.status}`);
  });

  // --- 16. Website API catalog (SSR data) ---
  await record('Gateway serves public catalog', async () => {
    const r = await get(`${GATEWAY}/api/v1/academic/public/catalog/COURSE?size=100`);
    assert.equal(r.status, 200);
    assert.ok((await r.json()).items.length > 0, 'Published courses missing');
  });

  await record('Staff can read own-branch students', async () => {
    const r = await get(`${GATEWAY}/api/v1/academic/students?branchId=1`, {Authorization:`Bearer ${staffToken}`,'X-Branch-Id':'1'});
    assert.equal(r.status,200);assert.ok((await r.json()).totalElements>0);
  });
  await record('Admin cannot read unassigned branch', async () => {
    const r = await get(`${GATEWAY}/api/v1/academic/students?branchId=3`, {Authorization:`Bearer ${adminToken}`,'X-Branch-Id':'3'});
    assert.equal(r.status,403);
  });
  await record('Student can read own enrollment', async () => {
    const r=await get(`${GATEWAY}/api/v1/academic/student/me/enrollments`,{Authorization:`Bearer ${studentToken}`});
    assert.equal(r.status,200);assert.ok((await r.json()).totalElements>0);
  });
  await record('Student tuition and receipts reach Finance', async () => {
    for(const route of ['tuitions','receipts']){const r=await get(`${GATEWAY}/api/v1/finance/student/me/${route}`,{Authorization:`Bearer ${studentToken}`});assert.equal(r.status,200);assert.ok(Array.isArray((await r.json()).content));}
  });
  await record('Next BFF loads published course data', async () => {
    const r=await get(`${WEBSITE}/api/catalog?kind=COURSE`);assert.equal(r.status,200);assert.ok((await r.json()).items.length>0);
  });

  // --- 17. Management endpoints not exposed ---
  await record('Actuator env endpoint is hidden (401/403/404)', async () => {
    const r = await get(`${GATEWAY}/actuator/env`);
    assert.ok([401, 403, 404].includes(r.status), `Management endpoint exposed: ${r.status}`);
  });

  // --- Collect result ---
  const passed = checks.filter(c => c.status === 'passed').length;
  const failed = checks.filter(c => c.status === 'failed').length;
  const result = {
    checkedAt: new Date().toISOString(),
    status: failed === 0 ? 'passed' : 'failed',
    passed,
    failed,
    checks,
    rowsInDocker: c.dockerSnapshot().rowCount,
    accounts: Number(c.mysql('SELECT COUNT(*) FROM accounts')),
    students: Number(c.mysql('SELECT COUNT(*) FROM students')),
    gateway: GATEWAY
  };
  fs.mkdirSync(path.join(c.root, 'docs/docker'), { recursive: true });
  fs.writeFileSync(path.join(c.root, 'docs/docker/runtime-functional-validation.json'), JSON.stringify(result, null, 2));
  console.log(JSON.stringify({ status: result.status, passed, failed, checks: checks.length }));
  if (failed > 0) { console.error('Failed checks:', checks.filter(c => c.status === 'failed')); process.exitCode = 1; }
}

main().catch(e => { fs.writeFileSync(path.join(c.root,'docs/docker/runtime-functional-validation.json'),JSON.stringify({checkedAt:new Date().toISOString(),status:'failed',checks,error:e.message},null,2));console.error('Docker functional verify error:', e.message); process.exitCode = 1; });
