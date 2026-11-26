import fs from 'node:fs';

const filename = 'docs/assistant/readonly-scenarios.json';
const data = JSON.parse(fs.readFileSync(filename, 'utf8'));
const hostReport=fs.existsSync('docs/assistant/acceptance-host-results.json')?JSON.parse(fs.readFileSync('docs/assistant/acceptance-host-results.json','utf8')):null;
const liveReport=fs.existsSync('docs/assistant/acceptance-live-results.json')?JSON.parse(fs.readFileSync('docs/assistant/acceptance-live-results.json','utf8')):null;
const contracts = {
  personnel: ['AUTH personnel → personnel-profile/personnel-statistics; ACADEMIC teacher-classes', 'Teacher/Staff.id, qualification, certificate, specialization, AccountBranch.status, SchoolClass.teacherId', 'SA/A trong scope; S chỉ giáo viên active tối thiểu; T chỉ mình'],
  self: ['AUTH my-personnel-profile → ACADEMIC teacher-classes', 'Actor.accountId → Teacher.id, qualification/certificate/specialization, activeBranchIds, totalElements', 'TEACHER chính mình; lớp trong chi nhánh đang chọn'],
  organization: ['AUTH personnel-statistics cho staff count; các thống kê tổ chức khác chưa có tool', 'DISTINCT_PERSONNEL cho staff; branch/admin/account group chưa có metric MCP', 'SA hoặc A trong phân công; không chuyển role bằng prompt'],
  academic: ['ACADEMIC courses/classes/sessions/student-statistics', 'tuitionFee/status/currentSize/maxSize/startDate; ACTIVE_IN_CLASS; không có room availability/grouping', 'SA/A/S scoped; T chỉ quan hệ lớp/buổi được giao'],
  admissions: ['ACADEMIC courses/classes/sessions', 'level/type/tuitionFee/status/currentSize/maxSize; chưa lọc entry requirement hoặc ngày khai giảng tùy ý', 'STAFF chi nhánh hiện hành'],
  learning: ['ACADEMIC students/student-classes/student-statistics/attendance; TEACHING published-grades/current-grades', 'periods, ACTIVE_IN_CLASS, recorded/state, score/null, view; aggregate chuyên cần/thiếu điểm chưa có', 'A scoped; điểm published cho S; current cho T được giao'],
  people: ['ACADEMIC students/student-classes/classes/sessions; AUTH personnel-profile bị chặn với STAFF', 'studentId/classId/teacherName; qualification không được công khai qua quyền STAFF', 'STAFF chi nhánh hiện hành, published only'],
  finance: ['FINANCE tuitions/receipts; ACADEMIC priority-leads (các aggregate đề xuất chưa có tool)', 'balance/paidAmount/voidedAmount/refundedAmount; không cộng trang thành tổng kỳ/chi nhánh', 'SA/A/S trong scope; T không được xem'],
  operations: ['FINANCE tuitions/receipts; ACADEMIC priority-leads; support chưa có tool', 'overdue/balance; priorityReason; aggregate/date/status nâng cao cần bổ sung', 'A trong phân công; không mở quyền assignee qua model'],
  care: ['FINANCE tuitions/receipts; ACADEMIC priority-leads; TEACHING published-grades', 'billing root, balance, refundedAmount, priorityReason, score/null; support chưa có', 'STAFF chi nhánh hiện hành; lead chính mình; chỉ điểm công bố'],
  schedule: ['ACADEMIC sessions', 'actual teacherId/sessionDate/startTime/endTime/status; chưa có filter kind/substitute trong tool', 'TEACHER lớp/buổi chính mình, phạm vi ngày hữu hạn'],
  roster: ['ACADEMIC students/student-classes/attendance', 'studentId/recorded/state; distinct toàn bộ lớp/aggregate vắng/chưa ghi chưa có tool', 'TEACHER quan hệ lớp/buổi được giao, không mở cả lớp từ quyền dạy thay'],
  results: ['TEACHING current-grades; ACADEMIC attendance', 'score/null/maxScore/view; thiếu aggregate điểm/điểm danh và filter score', 'TEACHER đúng lớp/bài kiểm tra, không suy thiếu từ điểm 0'],
  FOLLOWUP: ['ChatService/ConversationStore/ReferenceIndex/TopicPolicy', 'selection/refs/scope/hasMore/nextPage; không dùng history làm chứng cứ quyền', 'Giữ actor, kiểm lại phiên; đổi scope phải tạo ngữ cảnh hợp lệ'],
  GUARD: ['TopicPolicy/ChatService/SessionVerifier và authorization tại API nghiệp vụ', 'Từ chối, hỏi rõ hoặc báo lỗi; không trả số 0 khi dependency lỗi', 'Không credential/mutation/SQL/ngoài phạm vi; quyền API quyết định'],
};
const ready = new Set([
  ...data.cases.filter(c => c.phase === 'R2').map(c => c.id),
  'SUPER_ADMIN-10','SUPER_ADMIN-13','SUPER_ADMIN-14','ADMIN-08','ADMIN-13','ADMIN-15','ADMIN-19',
  'STAFF-01','STAFF-02','STAFF-04','STAFF-06','STAFF-07','STAFF-08','STAFF-09','STAFF-10','STAFF-13','STAFF-14','STAFF-16','STAFF-19','STAFF-24',
  'TEACHER-07','TEACHER-08','TEACHER-11','TEACHER-19','TEACHER-22',
]);
const partial = new Set(['ADMIN-07','ADMIN-11','ADMIN-21','ADMIN-22','STAFF-03','STAFF-05','STAFF-11','STAFF-17','STAFF-20','STAFF-21','TEACHER-09','TEACHER-10','TEACHER-12','TEACHER-13','TEACHER-15','TEACHER-16','TEACHER-20','TEACHER-21']);
for (const c of data.cases) {
  const [api, fields, permission] = contracts[c.domain];
  c.audit = { api, fields, permission, source: c.domain === 'personnel' || c.domain === 'self' || c.domain === 'organization' ? 'r1-r2-completion.md#nguồn-và-quyền' : 'r1-r2-completion.md#ma-trận-các-nhóm-tra-cứu' };
  c.implementation = ready.has(c.id) ? 'READ_CONTRACT_AVAILABLE' : partial.has(c.id) ? 'PARTIAL_BASE_QUERY_ONLY' : c.domain === 'FOLLOWUP' || c.domain === 'GUARD' ? 'CROSS_CUTTING_ACCEPTANCE_PENDING' : 'MISSING_QUERY_OR_METRIC';
  if (c.id === 'STAFF-12') c.implementation = 'DENIED_BY_CURRENT_POLICY';
  c.execution = liveReport?.cases.find(r=>r.id===c.id)?.status ?? 'NOT_RUN';
  c.executionLayer = 'REAL_GEMINI_SYNTHETIC_MCP';
  if (c.domain === 'FOLLOWUP' || c.domain === 'GUARD') c.implementation = hostReport ? 'HOST_CONTRACT_TESTED' : c.implementation;
  if(hostReport)c.hostContractValidation=hostReport.cases.find(r=>r.id===c.id)?.status??'NOT_RUN';
  if (c.phase === 'R3' || c.phase === 'R4') {
    c.implementation = c.id === 'STAFF-12' ? 'DENIED_BY_CURRENT_POLICY' : c.id === 'ADMIN-18' ? 'UNSUPPORTED_GRADING_DEADLINE' : 'READ_CONTRACT_AVAILABLE';
    if (c.id === 'ADMIN-10') c.implementation = 'PARTIAL_PLANNED_END_DATE_ONLY';
    c.audit.source = 'r3-r4-completion.md';
    const academic = {
      admissions: ['courses/classes/sessions; course-admissions', 'tuitionFee, remainingSeats, accepted entryLevel theo policy mới nhất'],
      academic: ['courses/classes; academic-statistics; rooms; available-teachers', 'effectiveStudentCount, remainingSeats, group distinct theo branch; không trùng buổi đã lưu'],
      learning: ['students/student-classes/student-statistics; attendance-summary; missing-attendance; TEACHING exams', 'relationship RESERVED, ACTIVE_IN_CLASS, absentCount, notRecorded; deadline chưa có'],
      people: ['students/student-classes/student-profile/classes/sessions/attendance', 'hồ sơ tối thiểu, lớp/giáo viên phụ trách; S không có quyền bằng cấp giáo viên'],
      schedule: ['sessions với kind/substituteOnly/upcomingOnly; find_personnel trước teacherId', 'actual teacherId, roomName, start/endTime, status; cửa sổ tối đa 31 ngày'],
      roster: ['roster-statistics; students; attendance; attendance-summary', 'distinct học viên hiệu lực; studentId/missingOnly; ABSENT có ghi nhận và trong kỳ học'],
      results: ['TEACHING exams/current-grades', 'roster frozen, missingCount, scoreFilter ZERO/NOT_GRADED; không có deadline chấm'],
    }[c.domain];
    if (academic) { c.audit.api = academic[0]; c.audit.fields = academic[1]; }
    c.automatedEvidence = 'AssistantQueryTest (Academic/Teaching); AcademicExpansionTest; McpRuntimeTest; frontend assistant.test.mjs. Dữ liệu tổng hợp, không phải toàn bộ prompt evaluation.';
  }
  if (c.phase === 'R2') c.automatedEvidence = 'PersonnelQueryTest; PersonnelChatTest; AssistantQueryTest; McpRuntimeTest (synthetic contract tests, not full prompt evaluation)';
  if(c.phase==='R5'||c.phase==='R6') {
    c.implementation='READ_CONTRACT_AVAILABLE';c.audit.source='r5-r6-completion.md';
    if(c.domain==='organization') {
      c.audit.api='AUTH management-accounts/account-statistics/personnel-statistics; ACADEMIC center-statistics';
      c.audit.fields='Distinct account/role/status; branchIds active; chi nhánh thiếu ADMIN mở tài khoản và membership active. ADMIN chỉ STAFF/TEACHER khi đọc tài khoản.';
    } else {
      c.audit.api='FINANCE tuitions/receipts/debt-statistics/cash-flow/cash-comparison; ACADEMIC leads/care-statistics/support-requests/support-statistics; AUTH management-accounts';
      c.audit.fields='Billing-root/current outstanding; cash gross-void-refund theo ngày bút toán, có thể âm; hai kỳ bằng độ dài; STAFF lead assignee chính mình; support không có nội dung riêng.';
    }
    if(c.id==='STAFF-18'){c.audit.api='ACADEMIC usage-help topic PAYMENT_ACCOUNTING';c.audit.fields='Quy tắc đã duyệt: online PENDING chưa phải phiếu thu; không gọi model để suy trạng thái đơn.';}
    if(c.id==='STAFF-24'){c.audit.api='TEACHING published-grades';c.audit.fields='Bản công bố hợp lệ, studentId/enrollmentId được kiểm quyền; giữ quy tắc R4.';}
    c.automatedEvidence='PersonnelQueryTest; AssistantQueryTest (Academic/Finance); FinanceCareExpansionTest; McpRuntimeTest; frontend assistant.test.mjs. Không phải model evaluation toàn bộ prompt.';
  }
}
data.auditVersion = 'R1-R8-2026-11-13';
data.execution = liveReport?.status==='PASS'?'LIVE_SYNTHETIC_MATRIX_PASS_MYSQL_PENDING':'FULL_MODEL_ACCEPTANCE_INCOMPLETE';
fs.writeFileSync(filename, JSON.stringify(data, null, 2) + '\n');
let md = '# R1 — Đối chiếu 120 câu hỏi với hợp đồng tra cứu (cập nhật đến R8)\n\nĐây là audit hợp đồng. hostContractValidation là kiểm thử planner giả lập + MCP; execution là kết quả Gemini thật trên dữ liệu tổng hợp, NOT_RUN nghĩa chưa chạy. Xem acceptance-summary.json và r7-r8-completion.md để biết giới hạn. READ_CONTRACT_AVAILABLE chỉ xác nhận có API/field phù hợp; cách diễn đạt và nhiều lượt vẫn phải nghiệm thu R8. PARTIAL_BASE_QUERY_ONLY nghĩa là chưa đáp ứng đầy đủ câu mẫu.\n\n| ID | Câu hỏi / ý định | Hợp đồng / field | Quyền | Trạng thái |\n|---|---|---|---|---|\n';
for (const c of data.cases) md += `| ${c.id} | ${c.prompt} | ${c.audit.api}; ${c.audit.fields} | ${c.audit.permission}; ${c.scopeRule ?? 'Kiểm theo actor và scope thực tế'} | ${c.implementation} |\n`;
fs.writeFileSync('docs/assistant/readonly-audit.md', md);
console.log(`Audited ${data.cases.length} scenarios without claiming model evaluation PASS.`);
