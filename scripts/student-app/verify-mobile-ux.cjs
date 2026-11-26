// Isolated browser acceptance. Synthetic HTTP responses exist only in this process;
// it never connects to the project backend or changes database records.
const fs = require('node:fs'), path = require('node:path'), http = require('node:http');
const { spawn } = require('node:child_process');
const assert = require('node:assert/strict');
const webRoot = path.resolve(process.argv[2]);
const extension = process.argv[3] ? require(path.resolve(process.argv[3])) : null;
const outputName = extension?.outputName ?? (extension ? 'student-m456-verification' : 'student-m123-verification');
if (!/^[a-z0-9-]+$/.test(outputName)) throw Error('Invalid verification output name');
const output = path.resolve(__dirname, '../../tmp', outputName);
fs.mkdirSync(output, { recursive: true });
const checks = [], errors = [], requests = [];
const today = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Ho_Chi_Minh', year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date());
const date = new Date(today + 'T00:00:00Z');
const tomorrow = new Date(date.getTime() + 86400000).toISOString().slice(0, 10);
const branch = { id: 1, name: 'Chi nhánh mẫu', address: 'Địa chỉ kiểm thử', phone: '0900000000' };
const session = (id, changes = {}) => ({ sessionId: id, enrollmentId: 1, enrollmentIds: [1], classId: 1, classCode: 'L01', className: 'Giao tiếp A1', courseName: 'Giao tiếp', branch, date: today, startTime: '18:00', endTime: '19:30', status: 'SCHEDULED', kind: 'REGULAR', isMakeup: false, teacherChanged: false, teacherName: 'Giáo viên mẫu', roomName: 'P01', legacyEstimated: false, version: 1, updatedAt: null, ...changes });
const rows = [session(1), session(2, { startTime: '20:00', endTime: '21:30', status: 'CANCELLED', teacherChanged: true }), session(3, { enrollmentId: 2, enrollmentIds: [2], classId: 2, className: 'IELTS B1', date: tomorrow, isMakeup: true })];
const classes = [1, 2].map(id => ({ id, classId: id, classCode: 'L0' + id, className: id === 1 ? 'Giao tiếp A1' : 'IELTS B1', courseName: 'Tiếng Anh', branch, status: 'STUDYING', teacherName: 'Giáo viên mẫu', roomName: 'P01', classStatus: 'OPEN', startDate: today, endDate: tomorrow, enrolledAt: today }));
let feeMode = 'due', scheduleError = false, changesError = false;
function page(content, url) { const size = Number(url.searchParams.get('size') || 100); return { content, page: Number(url.searchParams.get('page') || 0), size, totalElements: content.length, totalPages: Math.ceil(content.length / size) }; }
extension?.setup?.({ today, tomorrow, branch, rows, classes, page });
const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, 'http://127.0.0.1:18765');
  if (url.pathname.startsWith('/api/v1/')) {
    requests.push({ path: url.pathname + url.search, method: req.method });
    res.setHeader('Content-Type', 'application/json');
    const reply = (data, status = 200) => { res.statusCode = status; res.end(JSON.stringify(data)); };
    const route = url.pathname.slice('/api/v1'.length);
    if (extension && await extension.handle({ req, res, url, route, reply })) return;
    if (route === '/auth/login') return reply({ role: 'STUDENT', token: 'local-synthetic-session' });
    if (route === '/auth/me') return reply({ accountId: 901, username: 'ux-student', role: 'STUDENT', fullName: 'Học viên mẫu', mustChangePassword: false });
    if (route.endsWith('/notifications/unread-count')) return reply({ count: 2 });
    if (route.endsWith('/next-session')) return reply({ session: rows[0], asOf: new Date().toISOString(), timezone: 'Asia/Ho_Chi_Minh' });
    if (route.endsWith('/notifications')) return changesError ? reply({ message: 'Unavailable' }, 503) : reply(page([{ id: 1, category: 'SCHEDULE', title: 'Thay đổi lịch học', body: 'Kiểm tra phòng học của buổi tối.', resourceType: 'SESSION', resourceId: '2', createdAt: today + 'T08:00:00', readAt: null }], url));
    if (route.endsWith('/tuitions/summary')) {
      if (feeMode === 'error') return reply({ message: 'Unavailable' }, 503);
      return reply({ currency: 'VND', totalCount: 1, activeCount: 1, cancelledCount: 0, overdueCount: 0, reviewCount: feeMode === 'review' ? 1 : 0, pendingBillingRoots: 0, coverageComplete: feeMode !== 'review', totalAmount: '5000000.00', netPaid: '0.00', outstanding: feeMode === 'review' ? null : feeMode === 'clear' ? '0.00' : '5000000.00', credit: '0.00', asOf: new Date().toISOString() });
    }
    if (route.endsWith('/enrollments')) return reply(page(classes, url));
    if (route.endsWith('/schedule')) {
      if (scheduleError) return reply({ message: 'Unavailable' }, 503);
      return reply(page(rows.filter(s => s.date >= url.searchParams.get('from') && s.date <= url.searchParams.get('to') && (!url.searchParams.get('enrollmentId') || String(s.enrollmentId) === url.searchParams.get('enrollmentId'))), url));
    }
    if (/\/sessions\/\d+$/.test(route)) return reply(rows.find(s => s.sessionId === Number(route.split('/').pop())));
    return reply({ message: 'No synthetic response for this route' }, 404);
  }
  let file = path.resolve(webRoot, '.' + decodeURIComponent(url.pathname));
  if (!file.startsWith(webRoot + path.sep) && file !== webRoot) { res.statusCode = 403; return res.end(); }
  if (!fs.existsSync(file) || !fs.statSync(file).isFile()) file = path.join(webRoot, 'index.html');
  res.setHeader('Content-Type', ({ '.js': 'text/javascript', '.html': 'text/html', '.ttf': 'font/ttf', '.png': 'image/png', '.json': 'application/json' })[path.extname(file)] || 'application/octet-stream');
  fs.createReadStream(file).pipe(res);
});
const delay = ms => new Promise(resolve => setTimeout(resolve, ms));
let browser, socket, sequence = 0;
const pending = new Map();
async function cdp(method, params = {}) {
  const id = ++sequence;
  return new Promise((resolve, reject) => { pending.set(id, { resolve, reject }); socket.send(JSON.stringify({ id, method, params })); });
}
async function evaluate(expression) {
  const result = await cdp('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true });
  if (result.exceptionDetails) throw Error(result.exceptionDetails.text);
  return result.result.value;
}
async function wait(expression, label) {
  for (let i = 0; i < 100; i++) { if (await evaluate(expression)) return; await delay(100); }
  throw Error('Timed out: ' + label + '\n' + await evaluate('document.body.innerText'));
}
async function click(text) {
  let clicked = false;
  for (let attempt = 0; attempt < 30; attempt++) {
    clicked = await evaluate(`(() => {const el = [...document.querySelectorAll('[role="button"],[role="tab"],[role="radio"],a,button')].filter(e => e.getBoundingClientRect().width > 0 && e.getBoundingClientRect().height > 0 && !e.closest('[aria-hidden="true"]')).find(e => e.getAttribute('aria-label') === ${JSON.stringify(text)} || e.innerText?.trim() === ${JSON.stringify(text)}); if (!el) return false; el.click(); return true;})()`);
    if (clicked) break; await delay(100);
  }
  assert.ok(clicked, 'Missing action ' + text + '\n' + await evaluate('document.body.innerText'));
  await delay(250);
}
async function check(name, expression) { assert.ok(await evaluate(expression), name); checks.push(name); }
async function screenshot(name) { const r = await cdp('Page.captureScreenshot', { format: 'png', captureBeyondViewport: false }); fs.writeFileSync(path.join(output, name + '.png'), Buffer.from(r.data, 'base64')); }
async function visitTab(name, includes) { await click(name); await wait(`document.body.innerText.includes(${JSON.stringify(includes)})`, name); }
async function noOverflow(name) { await check(name, 'document.documentElement.scrollWidth <= innerWidth + 1'); }
async function run() {
  await new Promise((resolve, reject) => { server.once('error', reject); server.listen(18765, '127.0.0.1', resolve); });
  browser = spawn('C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe', ['--headless=new', '--disable-gpu', '--no-first-run', '--no-default-browser-check', '--remote-debugging-port=19223', '--user-data-dir=' + path.join(output, 'chrome-profile'), 'about:blank'], { windowsHide: true, stdio: 'ignore' });
  browser.on('error', error => { errors.push(error.message); });
  let targets;
  for (let i = 0; i < 100; i++) { try { targets = await (await fetch('http://127.0.0.1:19223/json/list')).json(); if (targets.some(t => t.type === 'page')) break; } catch {} await delay(100); }
  const target = targets?.find(t => t.type === 'page');
  if (!target) throw Error('Chrome CDP did not start');
  socket = new WebSocket(target.webSocketDebuggerUrl);
  socket.onmessage = event => { const msg = JSON.parse(event.data); if (msg.id) { const entry = pending.get(msg.id); pending.delete(msg.id); if (entry) msg.error ? entry.reject(Error(msg.error.message)) : entry.resolve(msg.result); } else if (msg.method === 'Runtime.exceptionThrown') errors.push(msg.params.exceptionDetails.exception?.description ?? msg.params.exceptionDetails.text); };
  await new Promise((resolve, reject) => { socket.onopen = resolve; socket.onerror = reject; });
  await cdp('Runtime.enable'); await cdp('Page.enable');
  await cdp('Emulation.setDeviceMetricsOverride', { width: 360, height: 800, deviceScaleFactor: 1, mobile: true });
  await cdp('Page.navigate', { url: 'http://127.0.0.1:18765' });
  await wait('document.querySelectorAll("input").length === 2', 'login inputs');
  for (let i = 0; i < 2; i++) { await evaluate(`document.querySelectorAll('input')[${i}].focus()`); await cdp('Input.insertText', { text: i === 0 ? 'ux-student' : 'synthetic-password' }); }
  await click('Đăng nhập');
  await wait('document.body.innerText.includes("Buổi học tiếp theo") && document.body.innerText.includes("Giao tiếp A1")', 'home');
  if (extension) {
    await extension.run({ cdp, evaluate, wait, click, check, screenshot, visitTab, noOverflow, requests, checks, today, output });
    assert.equal(errors.length, 0, errors.join('\n')); checks.push('no uncaught browser runtime exceptions'); return;
  }
  await wait('document.body.innerText.includes("Thay đổi lịch cần chú ý") && document.body.innerText.includes("Học phí cần đóng")', 'home modules ready');
  await check('next session precedes changes and fees', 'document.body.innerText.indexOf("Buổi học tiếp theo") < document.body.innerText.indexOf("Thay đổi lịch cần chú ý") && document.body.innerText.indexOf("Buổi học tiếp theo") < document.body.innerText.indexOf("Học phí cần đóng")');
  await check('next session is visible before scrolling', '[...document.querySelectorAll("[role=button]")].some(e => e.getAttribute("aria-label")?.startsWith("Xem buổi Giao tiếp A1") && e.getBoundingClientRect().top < 500 && e.getBoundingClientRect().bottom < innerHeight - 60)');
  await check('five bottom tabs retained', '["Trang chủ","Lịch học","Học tập","Học phí","Cá nhân"].every(t => [...document.querySelectorAll("a,[role=tab]")].some(e => e.innerText.includes(t)))');
  await noOverflow('home fits 360px'); await screenshot('home-360');
  await click('Xem buổi Giao tiếp A1, ' + await evaluate(`new Intl.DateTimeFormat('vi-VN',{weekday:'long',day:'numeric',month:'numeric',timeZone:'UTC'}).format(new Date('${today}T00:00:00Z'))`) + ', 18:00, Theo lịch');
  await wait('document.body.innerText.includes("Giáo viên buổi này")', 'session detail');
  await check('session uses real room and teacher', 'document.body.innerText.includes("P01") && document.body.innerText.includes("Giáo viên mẫu")');
  await click('Xem lịch ngày này');
  await wait('document.body.innerText.includes("Buổi này đã hủy")', 'day agenda');
  await check('detail returns to the correct filtered day', 'location.search.includes("enrollmentId=1") && location.search.includes("view=day")');
  await click('Ngày'); await screenshot('schedule-360');
  await click('Tuần'); await check('week list preserves selected class', 'location.search.includes("enrollmentId=1") && location.search.includes("view=agenda")');
  await click('Giao tiếp A1'); await wait('document.body.innerText.includes("Lọc theo lớp học")', 'class sheet');
  await screenshot('class-filter-360');
  await check('class filter has checked radio choice', 'document.querySelector("[role=radio][aria-checked=true]") !== null || document.querySelector("input[type=radio]:checked") !== null');
  await click('Tất cả lớp của tôi'); await wait('!document.body.innerText.includes("Lọc theo lớp học")', 'sheet dismissal'); checks.push('clear class filter closes sheet');
  await click('Lưới giờ'); await wait('document.body.innerText.includes("Vuốt ngang")', 'time grid');
  await check('time grid has real events', 'document.body.innerText.includes("Giao tiếp A1")');
  await click('Tuần'); await click('Tuần sau'); await click('Tuần sau');
  await wait('document.body.innerText.includes("Không có buổi học trong tuần này")', 'empty week'); checks.push('week navigation and empty state');
  await click('Hôm nay'); await wait('document.body.innerText.includes("Giao tiếp A1")', 'today'); checks.push('today restores schedule');
  scheduleError = true;
  await visitTab('Trang chủ', 'Buổi học tiếp theo'); await visitTab('Lịch học', 'Giờ Việt Nam'); await click('Tuần trước');
  await wait('document.body.innerText.includes("Chưa tải được lịch")', 'new week error'); checks.push('schedule error has isolated retry');
  scheduleError = false; await click('Thử lại'); await wait('document.body.innerText.includes("Không có buổi học trong tuần này")', 'schedule retry');
  await click('Hôm nay'); await wait('document.body.innerText.includes("Giao tiếp A1")', 'schedule recovery'); checks.push('schedule retry recovers');
  feeMode = 'review'; await visitTab('Trang chủ', 'Học phí cần xác minh');
  await check('unverified debt is not zero', '!document.body.innerText.includes("0 đ")'); checks.push('unverified debt explanation');
  feeMode = 'clear'; await visitTab('Lịch học', 'Giao tiếp A1'); await visitTab('Trang chủ', 'Buổi học tiếp theo');
  await wait('!document.body.innerText.includes("Học phí cần xác minh")', 'zero debt updated');
  await check('verified zero hides actionable debt card', '!document.body.innerText.includes("Học phí cần đóng")');
  feeMode = 'error'; changesError = true;
  await visitTab('Lịch học', 'Giao tiếp A1'); await click('Trang chủ');
  await wait('document.body.innerText.includes("Chưa cập nhật được học phí") || document.body.innerText.includes("Chưa cập nhật được thông tin mới")', 'home service warning');
  await check('service failures keep next session usable', 'document.body.innerText.includes("Giao tiếp A1") && (document.body.innerText.includes("Chưa kiểm tra được thay đổi lịch") || document.body.innerText.includes("Chưa cập nhật được thông tin mới"))');
  await cdp('Emulation.setDeviceMetricsOverride', { width: 320, height: 720, deviceScaleFactor: 1, mobile: true });
  await delay(250); await noOverflow('home fits 320px');
  await visitTab('Lịch học', 'Giao tiếp A1'); await noOverflow('schedule fits 320px'); await screenshot('schedule-320');
  const tabGeometry = await evaluate(`Array.from(document.querySelectorAll('div,span')).filter(e => e.textContent === 'Trang chủ' && e.childElementCount === 0 && e.getBoundingClientRect().width > 0).map(e => [e, e.parentElement, e.parentElement?.parentElement].filter(Boolean).map(n => ({tag:n.tagName,role:n.getAttribute('role'),text:n.textContent,rect:n.getBoundingClientRect().toJSON(),font:getComputedStyle(n).fontSize,lineHeight:getComputedStyle(n).lineHeight,overflow:getComputedStyle(n).overflow})))`);
  fs.writeFileSync(path.join(output, 'tab-geometry.json'), JSON.stringify(tabGeometry, null, 2));
  assert.ok(tabGeometry.length > 0 && tabGeometry.every(ancestors => ancestors[0].rect.height >= 18), 'bottom tab labels must not be clipped'); checks.push('bottom tab labels have room for Vietnamese text');
  await check('week navigation targets at least 48px', '["Tuần trước","Tuần sau"].every(label => {const r = document.querySelector(`[aria-label="${label}"]`).getBoundingClientRect(); return r.width >= 48 && r.height >= 48})');
  assert.equal(errors.length, 0, errors.join('\n'));
  checks.push('no uncaught browser runtime exceptions');
}
run().then(() => { fs.writeFileSync(path.join(output, 'result.json'), JSON.stringify({ status: 'passed', checks, requests, errors }, null, 2)); process.stdout.write(JSON.stringify({ status: 'passed', count: checks.length, checks, output }, null, 2)); })
  .catch(error => { fs.writeFileSync(path.join(output, 'result.json'), JSON.stringify({ status: 'failed', error: error.stack, checks, requests, errors }, null, 2)); process.stderr.write(error.stack + '\n'); process.exitCode = 1; })
  .finally(async () => { if (socket?.readyState === WebSocket.OPEN) { try { await Promise.race([cdp('Browser.close'), delay(1000)]); } catch {} socket.close(); } if (browser && !browser.killed) browser.kill(); server.close(); });
