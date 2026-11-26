const fs = require('node:fs'), path = require('node:path'), assert = require('node:assert/strict');
const base = require('./verify-mobile-m456.cjs');
const baseline = process.env.LC_UX_BASELINE === '1';
exports.outputName = baseline ? 'student-m78-baseline' : 'student-m78-verification';
let fixture, offline = false, forbidden = false, authOffline = false, account = 901;
exports.setup = context => { fixture = context; base.setup(context); };
exports.handle = async args => {
  const { req, route, reply } = args;
  if (route === '/auth/me') { reply(authOffline ? {} : { accountId: account, username: account === 901 ? 'ux-student' : 'student-b', role: 'STUDENT', fullName: account === 901 ? 'Học viên mẫu' : 'Học viên B', mustChangePassword: false }, authOffline ? 503 : 200); return true; }
  if (route === '/auth/login') {
    let body = ''; for await (const chunk of req) body += chunk;
    if (JSON.parse(body).username === 'student-b') { account = 902; fixture.classes.forEach((c, i) => { c.className = 'Lớp riêng B' + (i + 1); }); }
    reply({ role: 'STUDENT', token: 'synthetic-session-' + account }); return true;
  }
  if (forbidden && route.endsWith('/grades')) { reply({}, 403); return true; }
  if (offline && (route.includes('/enrollments/') || route.endsWith('/enrollments'))) { reply({}, 503); return true; }
  return base.handle(args);
};
exports.run = async h => {
  const { click, wait, check, cdp, evaluate, screenshot, checks, requests, output } = h;
  const text = value => `document.body.innerText.includes(${JSON.stringify(value)})`;
  const classCount = () => requests.filter(r => r.path === '/api/v1/academic/student/me/enrollments/1').length;
  await click('Học tập'); await click('Mở lớp Giao tiếp A1, L01'); await wait(text('Xem lịch lớp này'), 'class detail');
  await click('Điểm và kết quả'); await wait(text('0 / 10'), 'grade');
  await click('Giao tiếp A1'); await wait(text('Xem lịch lớp này'), 'back to class');
  await click('Chuyên cần'); await wait(text('Chưa đủ dữ liệu để tính tỷ lệ'), 'attendance');
  await click('Giao tiếp A1'); await wait(text('Xem lịch lớp này'), 'class again');
  const measured = { scenario: 'Class -> grades -> class -> attendance -> class', classRequests: classCount(), mode: baseline ? 'before-M7' : 'after-M7' };
  fs.writeFileSync(path.join(output, 'performance.json'), JSON.stringify(measured, null, 2));
  checks.push('measured class detail request count: ' + measured.classRequests);
  if (baseline) return;
  assert.equal(measured.classRequests, 1); checks.push('shared class context reuses one owned response');
  offline = true; await click('Điểm và kết quả'); await wait(text('Chưa cập nhật được thông tin mới'), 'cached data warning');
  await check('last published grade stays readable during temporary outage', text('0 / 10') + ' && ' + text('Cập nhật gần nhất:')); await screenshot('cached-grades-offline');
  offline = false; await click('Thử cập nhật thông tin'); await wait('!' + text('Chưa cập nhật được thông tin mới'), 'retry recovered'); checks.push('manual refresh recovers without changing screens');
  await click('Giao tiếp A1'); await wait(text('Xem lịch lớp này'), 'class'); forbidden = true;
  await click('Điểm và kết quả'); await wait(text('Bạn không có quyền truy cập.'), 'permission revoked');
  await check('forbidden response removes previously cached grades', '!' + text('0 / 10')); forbidden = false;
  await click('Thử lại'); await wait(text('0 / 10'), 'permission retry');
  await click('Cần kiểm tra điểm?'); await wait(text('Bạn cần hỗ trợ gì?'), 'support draft');
  await evaluate(`document.querySelector('[aria-label="Nội dung yêu cầu"]').focus()`); await cdp('Input.insertText', { text: 'Bản nháp của em cần được giữ khi quay lại ứng dụng.' });
  // Browser emulates the same visibility events consumed by RN Web AppState.
  authOffline = true;
  await evaluate(`Object.defineProperty(document,'hidden',{configurable:true,get:()=>true});Object.defineProperty(document,'visibilityState',{configurable:true,get:()=>'hidden'});document.dispatchEvent(new Event('visibilitychange'));`);
  await evaluate(`Object.defineProperty(document,'hidden',{configurable:true,get:()=>false});Object.defineProperty(document,'visibilityState',{configurable:true,get:()=>'visible'});document.dispatchEvent(new Event('visibilitychange'));`);
  await wait(text('Chưa kết nối lại máy chủ'), 'session revalidation warning');
  await check('resume outage keeps the unsent draft', `document.querySelector('[aria-label="Nội dung yêu cầu"]')?.value === 'Bản nháp của em cần được giữ khi quay lại ứng dụng.'`);
  await check('resume does not return to login', '!' + text('Đăng nhập học viên')); await screenshot('resume-keeps-draft');
  authOffline = false; await click('Thử cập nhật thông tin'); await wait('!' + text('Chưa kết nối lại máy chủ'), 'resume recovered');
  await check('reconnection preserves the draft', `document.querySelector('[aria-label="Nội dung yêu cầu"]')?.value === 'Bản nháp của em cần được giữ khi quay lại ứng dụng.'`);
  // Navigate to profile using an internal action, preserving the authenticated session.
  await evaluate('history.back()'); await wait(text('0 / 10'), 'back to grades');
  await click('Giao tiếp A1'); await wait(text('Xem lịch lớp này'), 'back to class');
  await click('Xem lịch lớp này'); await wait(text('Giờ Việt Nam'), 'back to schedule tab');
  await click('Cá nhân'); await wait(text('HV001'), 'profile'); await click('Đăng xuất'); await click('Đăng xuất tất cả thiết bị'); await wait('document.querySelectorAll("input").length === 2', 'logout');
  for (let i=0;i<2;i++) { await evaluate(`document.querySelectorAll('input')[${i}].focus()`); await cdp('Input.insertText',{text:i?'synthetic-password':'student-b'}); }
  await click('Đăng nhập'); await wait(text('Chào Học viên B!'), 'second account');
  await click('Học tập'); await click('Mở lớp Lớp riêng B1, L01'); await wait(text('Xem lịch lớp này'), 'second account class');
  await check('new account cannot see previous account class data', text('Lớp riêng B1') + ' && !' + text('Giao tiếp A1'));
  assert.ok(classCount() > measured.classRequests); checks.push('new account fetches fresh private data');
};
