// Synthetic fixtures only. Used as an extension of verify-mobile-ux.cjs.
const assert = require('node:assert/strict');
let fixture;
const orderId = '11111111-1111-4111-8111-111111111111', reviewId = '22222222-2222-4222-8222-222222222222';
const state = { noticesRead: new Set(), tickets: [], operations: new Map(), paymentPosts: [], supportPosts: [], failRead: false, orderCreated: false };
exports.setup = data => { fixture = data; };
const tuition = id => ({ id, billingRootId: id, enrollmentIds: [id], enrollmentLabel: id === 1 ? 'Giao tiếp A1' : 'IELTS B1', courseName: 'Tiếng Anh', branchId: 1, currency: 'VND', originalAmount: '5000000.00', discountAmount: '0.00', totalAmount: '5000000.00', netPaid: '0.00', outstanding: '5000000.00', credit: '0.00', dueDate: fixture.tomorrow, status: 'UNPAID', overdue: false, cancelled: false, verification: 'VERIFIED', reviewCode: null, version: 1 });
let order;
function orderView(id = orderId) { return id === orderId ? order ?? { id, tuitionId: 1, state: 'PENDING', amount: '500000.00', currency: 'VND', provider: 'VNPAY', environment: 'SANDBOX', createdAt: fixture.today + 'T09:00:00', expiresAt: fixture.tomorrow + 'T10:00:00', receiptId: null, message: '', reviewRequired: false, version: 1, checkoutUrl: null, displayCode: 'GD-001', className: 'Giao tiếp A1', courseName: 'Tiếng Anh' }
  : { ...orderView(), id: reviewId, state: 'REVIEW_REQUIRED', receiptId: null, reviewRequired: true, displayCode: 'GD-002' }; }
function receipt() { return { id: 11, tuitionId: 1, receiptNo: 'PT001', paidAt: fixture.today + 'T09:30:00', amount: '500000.00', netAmount: '500000.00', voidedAmount: '0.00', refundedAmount: '0.00', currency: 'VND', method: 'CARD', state: 'VALID', className: 'Giao tiếp A1', courseName: 'Tiếng Anh' }; }
function notices() { return [
  { id: 1, category: 'SCHEDULE', title: 'Lịch học đã thay đổi', body: 'Kiểm tra buổi học của bạn.', resourceType: 'SESSION', resourceId: '2' },
  { id: 2, category: 'GRADES', title: 'Có điểm mới', body: 'Điểm của lớp Giao tiếp A1.', resourceType: 'GRADES', resourceId: '1' },
  { id: 3, category: 'GENERAL', title: 'Thông báo chung', body: 'Nội dung thông báo từ trung tâm.', resourceType: 'GENERAL', resourceId: '1' },
].map(n => ({ ...n, readAt: state.noticesRead.has(n.id) ? fixture.today + 'T10:00:00' : null, createdAt: fixture.today + 'T09:00:00' })); }
async function body(req) { let raw = ''; for await (const part of req) raw += part; return raw ? JSON.parse(raw) : {}; }
exports.handle = async ({ req, url, route, reply }) => {
  const send = (data, status) => { reply(data, status); return true; };
  const paged = rows => fixture.page(rows, url);
  if (route === '/auth/me/logout-all') return send({});
  if (route.endsWith('/profile')) return send({ studentId: 901, studentCode: 'HV001', fullName: 'Học viên mẫu', dateOfBirth: '2004-01-01', gender: 'MALE', email: 'student@example.test', phone: '0900000000', address: 'Địa chỉ mẫu', status: 'STUDYING', branch: fixture.branch, version: 1 });
  if (route.endsWith('/notification-preferences')) return send({ pushEnabled: false, schedule: true, grades: true, tuition: true, payment: true, documents: true, support: true, version: 1, pushAvailable: false });
  if (route.endsWith('/notifications/unread-count')) return send({ count: notices().filter(n => !n.readAt).length });
  if (route.endsWith('/notifications')) return send(paged(notices().filter(n => !url.searchParams.has('unread') || Boolean(n.readAt) !== (url.searchParams.get('unread') === 'true'))));
  if (/\/notifications\/\d+\/read$/.test(route)) { if (state.failRead) return send({ message: 'Unavailable' }, 503); state.noticesRead.add(Number(route.split('/').at(-2))); return send({}); }
  if (/\/notifications\/\d+$/.test(route)) return send(notices().find(n => n.id === Number(route.split('/').pop())));
  if (route.endsWith('/enrollments')) return send(paged(url.searchParams.get('group') === 'HISTORY' ? [] : fixture.classes));
  if (/\/enrollments\/\d+$/.test(route)) return send({ enrollment: fixture.classes.find(c => c.id === Number(route.split('/').pop())), periods: [{ activeFrom: fixture.today, activeUntil: null }], coverage: 'VERIFIED_PERIODS', publicEvents: [] });
  if (route.endsWith('/grades')) return send(paged([{ examId: 1, examName: 'Kiểm tra giữa khóa', examType: 'MIDTERM', examDate: fixture.today, maxScore: 10, weight: 50, revision: 1, publishedAt: fixture.today + 'T09:00:00', score: 0, status: 'GRADED', studentComment: 'Cần luyện tập thêm.' }, { examId: 2, examName: 'Kiểm tra cuối khóa', examType: 'FINAL', examDate: fixture.tomorrow, maxScore: 10, weight: 50, revision: 1, publishedAt: fixture.today + 'T09:00:00', score: null, status: 'NOT_GRADED', studentComment: null }]));
  if (route.endsWith('/final-result')) return send({ state: 'PENDING', outcome: null, revision: null, confirmedAt: null });
  if (route.endsWith('/attendance')) return send({ ...paged([{ sessionId: 1, attendanceId: null, date: fixture.today, startTime: '18:00', endTime: '19:30', status: 'NOT_RECORDED', hasPermission: false, source: 'VERIFIED' }]), summary: { eligibleCompleted: 1, present: 0, late: 0, absent: 0, notRecorded: 1, legacyUnverified: 0, coverageComplete: false, attendancePercent: null } });
  if (route.endsWith('/documents')) return send(paged([{ id: 1, title: 'Tài liệu giao tiếp', description: 'Tài liệu của lớp.', filename: 'lesson.pdf', mimeType: 'application/pdf', size: 1024, revision: 1, sharedAt: fixture.today + 'T09:00:00' }]));
  if (route.endsWith('/tuitions/summary')) return send({ currency: 'VND', totalCount: 2, activeCount: 2, cancelledCount: 0, overdueCount: 0, reviewCount: 0, pendingBillingRoots: 0, coverageComplete: true, totalAmount: '10000000.00', netPaid: '0.00', outstanding: '10000000.00', credit: '0.00', asOf: new Date().toISOString() });
  if (route.endsWith('/tuitions')) return send(paged(url.searchParams.has('enrollmentId') ? [tuition(Number(url.searchParams.get('enrollmentId')))] : [tuition(1), tuition(2)]));
  if (/\/tuitions\/\d+$/.test(route)) return send({ tuition: tuition(Number(route.split('/').pop())), adjustments: [] });
  const paymentContext = id => ({ tuitionId: id, action: state.orderCreated && order?.state === 'PENDING' ? 'RESUME' : 'PAY', reason: null, message: 'Có thể thanh toán', outstanding: tuition(id).outstanding, currency: 'VND', order: state.orderCreated && order?.state === 'PENDING' ? order : null, sandbox: true });
  if (route.endsWith('/payment-contexts')) return send(url.searchParams.get('tuitionIds').split(',').map(Number).map(paymentContext));
  if (route.endsWith('/payment-context')) return send(paymentContext(Number(route.split('/').at(-2))));
  if (route.endsWith('/receipts')) return send(paged([receipt()]));
  if (/\/receipts\/\d+$/.test(route)) return send({ receipt: receipt(), tuition: tuition(1), corrections: [] });
  if (route.endsWith('/payment-orders') && req.method === 'POST') {
    const command = await body(req); state.paymentPosts.push(command); order = orderView(); order.amount = command.amount; state.orderCreated = true; return send(order);
  }
  if (route.endsWith('/payment-orders')) return send(paged([orderView(), orderView(reviewId)].filter(o => !url.searchParams.get('state') || o.state === url.searchParams.get('state'))));
  if (/\/payment-orders\/[a-f0-9-]+$/i.test(route)) return send(orderView(route.split('/').pop()));
  if (route.endsWith('/support-requests') && req.method === 'POST') {
    const command = await body(req); state.supportPosts.push(command);
    if (state.operations.has(command.requestId)) return send(state.operations.get(command.requestId));
    const detail = { request: { id: state.tickets.length + 1, category: command.category, subject: command.subject, status: 'OPEN', version: 1, createdAt: fixture.today + 'T09:00:00', updatedAt: fixture.today + 'T09:00:00', resourceType: command.resourceType ?? null, resourceId: command.resourceId ?? null, resourceKey: command.resourceKey ?? null }, messages: [{ id: 1, sender: 'STUDENT', message: command.description, createdAt: fixture.today + 'T09:00:00' }] };
    state.tickets.push(detail); state.operations.set(command.requestId, detail); return send(detail);
  }
  if (route.endsWith('/support-requests')) return send(paged(state.tickets.map(d => d.request)));
  if (/\/support-requests\/\d+\/(messages|close)$/.test(route)) {
    const command = await body(req), detail = state.tickets.find(d => d.request.id === Number(route.split('/').at(-2)));
    if (state.operations.has(command.requestId)) return send(state.operations.get(command.requestId));
    if (route.endsWith('/close')) detail.request.status = 'CLOSED';
    else detail.messages.push({ id: detail.messages.length + 1, sender: 'STUDENT', message: command.message, createdAt: fixture.today + 'T10:00:00' });
    detail.request.version++; state.operations.set(command.requestId, detail); return send(detail);
  }
  if (/\/support-requests\/\d+$/.test(route)) return send(state.tickets.find(d => d.request.id === Number(route.split('/').pop())));
  if (route.includes('/support-requests/operations/')) return send(state.operations.get(route.split('/').pop()));
  return false;
};
exports.run = async h => {
  const { cdp, evaluate, wait, click, check, screenshot, noOverflow, requests, checks } = h;
  const text = value => `document.body.innerText.includes(${JSON.stringify(value)})`;
  async function fill(label, value) {
    await evaluate(`(() => {const el = document.querySelector('[aria-label="${label}"]'); el.focus(); el.select();})()`);
    await cdp('Input.insertText', { text: value });
  }
  async function go(route, expected) {
    await cdp('Page.navigate', { url: 'http://127.0.0.1:18765' + route });
    await wait('document.querySelectorAll("input").length === 2', 'login for ' + route);
    for (let i = 0; i < 2; i++) { await evaluate(`document.querySelectorAll('input')[${i}].focus()`); await cdp('Input.insertText', { text: i ? 'synthetic-password' : 'ux-student' }); }
    await click('Đăng nhập'); await wait(text(expected), route);
  }
  await click('Học tập'); await wait(text('Giao tiếp A1'), 'class list');
  await click('Mở lớp Giao tiếp A1, L01'); await wait(text('Điểm và kết quả'), 'class hub');
  await check('class history collapsed by default', '!document.body.innerText.includes("Thời gian theo học")');
  await screenshot('class-hub-360'); await noOverflow('class hub fits phone');
  await click('Điểm và kết quả'); await wait(text('0 / 10'), 'published zero grade');
  await check('zero grade and ungraded exam differ', text('Chưa chấm') + ' && ' + text('Chưa xác nhận kết quả'));
  await check('grade screen retains class context', text('Giao tiếp A1')); await screenshot('grades-360');
  await click('Cần kiểm tra điểm?'); await wait(text('Bạn cần hỗ trợ gì?'), 'grade support');
  await check('support receives class automatically', 'location.search.includes("resourceType=ENROLLMENT") && location.search.includes("resourceId=1")');
  await fill('Nội dung yêu cầu', 'Nhờ trung tâm kiểm tra điểm giữa khóa giúp em.'); await click('Gửi yêu cầu'); await wait(text('Yêu cầu #1'), 'support saved');
  assert.equal(state.supportPosts.length, 1); assert.equal(state.supportPosts[0].category, 'GRADE'); assert.equal(state.supportPosts[0].resourceId, 1); checks.push('support sends owned class context once');
  await fill('Phản hồi yêu cầu', 'Em đã bổ sung thông tin, nhờ trung tâm kiểm tra.'); await click('Gửi phản hồi'); await wait(text('Em đã bổ sung thông tin'), 'support reply'); checks.push('support reply appears in thread');
  await click('Đóng yêu cầu'); await wait(text('Đóng yêu cầu hỗ trợ?'), 'close confirmation');
  assert.equal(state.tickets[0].request.status, 'OPEN'); checks.push('closing support requires explicit confirmation');
  await click('Xác nhận đóng yêu cầu'); await wait(text('Yêu cầu đã đóng'), 'support closed'); checks.push('confirmed support close succeeds');
  await go('/attendance/1', 'Chưa đủ dữ liệu để tính tỷ lệ');
  await check('missing attendance is not absence', text('Chưa cập nhật') + ' && !' + text('100%')); await screenshot('attendance-360');
  await go('/documents?enrollmentId=1', 'Tài liệu giao tiếp'); await check('documents retain enrollment filter', 'location.search.includes("enrollmentId=1")');
  await go('/tuition?enrollmentId=1', 'Còn phải đóng');
  await check('class fees exclude unrelated class', text('Giao tiếp A1') + ' && !' + text('IELTS B1'));
  await check('class fees do not show global sum', '!' + text('10.000.000 đ')); await screenshot('class-fees-360');
  await click('Thanh toán'); await wait(text('1. Chọn số tiền'), 'checkout amount');
  await click('Đóng một phần'); await fill('Số tiền muốn đóng', '6000000');
  await check('amount above balance cannot continue', 'Array.from(document.querySelectorAll("[role=button]")).some(e => e.innerText.trim() === "Tiếp tục" && e.getAttribute("aria-disabled") === "true")');
  await fill('Số tiền muốn đóng', '500000'); await click('Tiếp tục'); await wait(text('2. Xác nhận thanh toán'), 'checkout confirmation');
  await check('confirmation shows student and class', text('Học viên: Học viên mẫu') + ' && ' + text('Giao tiếp A1'));
  await screenshot('checkout-360'); await click('Thanh toán 500.000 đ'); await wait(text('Phiên thanh toán chưa hoàn tất'), 'pending order');
  assert.equal(state.paymentPosts.length, 1); assert.equal(state.paymentPosts[0].amount, '500000.00'); checks.push('checkout creates one frozen payment request');
  await check('pending is not reported as paid', '!' + text('Thanh toán thành công'));
  order = { ...order, state: 'SUCCEEDED', receiptId: 11, version: 2 };
  await click('Chi tiết giao dịch'); await click('Kiểm tra lại trạng thái'); await wait(text('Thanh toán thành công'), 'confirmed success'); checks.push('backend confirmation reveals receipt action');
  await click('Xem phiếu thu'); await wait(text('PT001'), 'receipt'); await check('receipt keeps original payment amount', text('500.000 đ')); await screenshot('receipt-360');
  await evaluate('history.back()'); await wait(text('Thanh toán thành công'), 'back to payment result');
  await click('Lịch sử thanh toán'); await wait(text('Phiếu thu đã ghi nhận'), 'history after class payment');
  await check('payment result clears the old class filter for full receipt history', '!location.search.includes("enrollmentId=")');
  await go('/tuition?tab=history', 'Phiếu thu đã ghi nhận');
  await check('history deep link survives login', 'location.search.includes("tab=history")');
  await check('finance uses one level of tabs', text('Phiếu thu') + ' && !' + text('Lịch sử')); await screenshot('finance-history-360');
  await click('Giao dịch online'); await wait(text('GD-002'), 'order history');
  await click('Kiểm tra giao dịch'); await wait(text('Giao dịch đang được kiểm tra'), 'review order');
  await click('Yêu cầu trung tâm kiểm tra'); await wait(text('Bạn cần hỗ trợ gì?'), 'order support');
  await check('payment support keeps UUID', 'location.search.includes(' + JSON.stringify(reviewId) + ') && !location.search.includes("resourceId=")');
  await go('/support?category=PAYMENT&resourceType=PAYMENT_ORDER&resourceKey=' + reviewId, 'Bạn cần hỗ trợ gì?');
  await check('payment support survives login', 'location.search.includes(' + JSON.stringify(reviewId) + ')');
  await go('/notifications', 'Lịch học đã thay đổi'); await screenshot('notifications-360');
  await click('Chưa đọc: Lịch học đã thay đổi'); await wait(text('Buổi học đã hủy'), 'notification target'); assert.ok(state.noticesRead.has(1)); checks.push('notification opens session and marks read');
  state.failRead = true; await go('/notifications', 'Có điểm mới'); await click('Chưa đọc: Có điểm mới'); await wait(text('0 / 10'), 'read outage target');
  assert.ok(!state.noticesRead.has(2)); checks.push('read marker failure does not block content or fake read state'); state.failRead = false;
  await go('/notifications/3', 'Nội dung thông báo từ trung tâm.'); await wait(text('Đã đọc'), 'notice details'); assert.ok(state.noticesRead.has(3)); checks.push('general announcement remains readable and marks read');
  await go('/settings', 'Thông báo trên thiết bị chưa khả dụng'); await check('inbox remains available without push', text('Hộp thông báo'));
  await go('/profile', 'HV001'); await check('profile details collapsed', '!' + text('student@example.test')); await screenshot('profile-360');
  await click('Thông tin cá nhân'); await wait(text('student@example.test'), 'profile disclosure'); await click('Yêu cầu cập nhật thông tin'); await wait(text('Bạn cần hỗ trợ gì?'), 'profile support');
  await check('profile support is prefilled', `location.search.includes('resourceType=PROFILE') && document.querySelector('[aria-label="Tiêu đề yêu cầu"]')?.value === 'Cập nhật thông tin hồ sơ'`);
  await cdp('Emulation.setDeviceMetricsOverride', { width: 320, height: 720, deviceScaleFactor: 1, mobile: true });
  await noOverflow('support form fits 320px'); await screenshot('support-320');
  await go('/profile', 'HV001'); await noOverflow('profile fits 320px'); await click('Đăng xuất'); await wait(text('Đăng xuất tài khoản?'), 'logout confirmation');
  assert.ok(!requests.some(r => r.path.endsWith('/logout-all'))); checks.push('logout waits for confirmation');
  await click('Đăng xuất tất cả thiết bị'); await wait(text('Đăng nhập học viên'), 'logged out'); checks.push('confirmed logout returns to login');
};
