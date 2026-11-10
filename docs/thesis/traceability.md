# Ma trận truy vết hiện hành — G16

Cập nhật10/11/2026, đối chiếu source và nghiệm thu thật G17/G18. Mã RQ giữ theo [yêu cầu gốc](requirements.md), UC theo [đặc tả](use-cases.md). requirements.md là snapshot G01 ngày 07/11: các mục thiếu trong đó không phải kết luận thiếu hiện tại. Mức đã có source, đã test và sẵn sàng nộp là ba mức khác nhau.

[API đầy đủ](../api-list.md) · [DTO/hợp đồng](../api-contracts.md) · [quyền](role-permissions.md) · [thiết kế động](design/dynamic-workflows.md) · [bằng chứng86 ca](../reviews/2026-11-10-g17-g18-real-acceptance.md). API viết đầy đủ prefix, path parameter được đặt theo controller; không dùng ví dụ endpoint rút gọn như contract.

| RQ | Yêu cầu / UC | Màn hình đang dùng | API tiêu biểu thực tế | Dữ liệu | Bằng chứng và giới hạn |
|---|---|---|---|---|---|
|RQ01|Học viên; [UC03](use-cases.md#uc03), [UC08](use-cases.md#uc08), [UC09](use-cases.md#uc09), [UC18](use-cases.md#uc18)|StudentManagement,Leads|GET /api/v1/academic/students/{id}/learning-profile|students,enrollments,enrollment_periods,learning_events,leads,lead_activities|G17-02/19/20; CRUD-01/02; UI-01/08/11; [source](../../academic-service/src/main/java/com/example/academic/student/StudentService.java)|
|RQ02|Giáo viên; [UC02](use-cases.md#uc02), [UC04](use-cases.md#uc04), [UC05](use-cases.md#uc05)|AccountManagement,TeacherDirectory|PATCH /api/v1/auth/teacher-profiles/{id}/work-status|teachers,accounts,account_branches,teacher_profile_events|CRUD-03/04; G18-06/08/14/15/29; [source](../../auth-service/src/main/java/com/example/auth/service/TeacherProfileService.java)|
|RQ03|Khóa học; [UC06](use-cases.md#uc06)|CourseManagement|PUT /api/v1/academic/courses/{id}|programs,program_levels,courses,course_admission_policies,course_duration_plans|CRUD-05/06; G17-03/10; AdmissionPolicyWorkflowTest,DurationWorkflowTest; [source](../../academic-service/src/main/java/com/example/academic/course/CourseService.java)|
|RQ04|Lớp và ghi danh; [UC07](use-cases.md#uc07), [UC09](use-cases.md#uc09), [UC10](use-cases.md#uc10), [UC13](use-cases.md#uc13)|ClassManagement,EnrollmentManagement|POST /api/v1/academic/enrollments|school_classes,enrollments,enrollment_admissions,placement_exceptions|G17-04/05/16/17; CRUD-07/08; G18-09/23/24; [source](../../academic-service/src/main/java/com/example/academic/enrollment/EnrollmentService.java)|
|RQ05|Phân lớp theo trình độ; [UC06](use-cases.md#uc06), [UC08](use-cases.md#uc08), [UC09](use-cases.md#uc09), [UC10](use-cases.md#uc10)|StudentManagement,EnrollmentManagement|POST /api/v1/academic/enrollments/preview|placement_assessments,assessment_events,course_admission_levels,placement_exceptions|G17-03–05; G18-36–38; PlacementAssessmentWorkflowTest; [source](../../academic-service/src/main/java/com/example/academic/placement/PlacementService.java)|
|RQ06|Lịch và buổi học; [UC07](use-cases.md#uc07), [UC11](use-cases.md#uc11)|ScheduleManagement,SessionManagement,AttendanceManagement|POST /api/v1/academic/attendances/bulk|schedules,class_sessions,session_events,attendances,attendance_changes|G17-10/11; G18-20; SessionWorkflowTest,DurationWorkflowTest; [source](../../academic-service/src/main/java/com/example/academic/session/SessionPlanner.java)|
|RQ07|Tiến độ/kết quả; [UC11](use-cases.md#uc11), [UC12](use-cases.md#uc12), [UC13](use-cases.md#uc13), [UC16](use-cases.md#uc16)|ExamManagement,GradeManagement,DocumentManagement|POST /api/v1/teaching/classes/{classId}/final-results/{enrollmentId}/confirm|exams,exam_candidates,grades,evaluation_policies,final_results,result_reopens,documents,document_events|G17-12–16; G18-16–26/31–33; UI-03/04/05; DB-02/03/06/07; [source](../../teaching-service/src/main/java/com/example/teaching/results/FinalResultService.java)|
|RQ08|Học phí/thanh toán; [UC09](use-cases.md#uc09), [UC14](use-cases.md#uc14)|TuitionManagement,PaymentManagement|POST /api/v1/finance/payments|tuition,payments,tuition_adjustments,payment_corrections|G17-06–09; G18-10–13/27/28/34/35; UI-07; DB-04; [source](../../finance-service/src/main/java/com/example/finance/payment/PaymentService.java)|
|RQ09|Tìm kiếm/thống kê; [UC03](use-cases.md#uc03), [UC12](use-cases.md#uc12), [UC15](use-cases.md#uc15), [UC18](use-cases.md#uc18)|Overview,Reports|GET /api/v1/finance/stats/overview|students,enrollments,payments,payment_corrections,tuition|G17-18/20; G18-35; StatsQueryTest,RevenueReportTest; [source](../../finance-service/src/main/java/com/example/finance/stats/RevenueController.java)|
|RQ10|Quyền/nhất quán/UI; [UC01](use-cases.md#uc01), [UC02](use-cases.md#uc02), [UC05](use-cases.md#uc05), [UC10](use-cases.md#uc10), [UC13](use-cases.md#uc13), [UC14](use-cases.md#uc14), [UC16](use-cases.md#uc16)|AccountManagement,ChangePassword|POST /api/v1/auth/me/logout-all|accounts,account_branches,account_security_events,class_result_barriers|G18-01–08/14–26; UI-02/03/09/10/12; DB-05; [source](../../auth-service/src/main/java/com/example/auth/security/AccountPrincipal.java)|
|RQ11|Phân tích/thiết kế; [UC17](use-cases.md#uc17)|Tài liệu|Không là API riêng|53 bảng target V013|32 hình; metadata/source/hash/link check; review G15; chưa phải khảo sát thực tế; [source](../../academic-service/src/main/java/com/example/academic/results/ClassClosureService.java)|
|RQ12|Phát triển/kiểm thử/bàn giao; [UC17](use-cases.md#uc17)|Tài liệu|Không là API riêng|schema/file/backup|Maven335,FE82,ops9,smoke19,acceptance86; G20 release cuối chưa chốt; [source](../../academic-service/src/main/java/com/example/academic/operations/ProductionSafety.java)|

## Quy tắc đọc bằng chứng

G17-xx/G18-xx/CRUD-xx/UI-xx/DB-xx trỏ từng kiểm tra trong báo cáo86 ca; không phải số test được suy từ tên giai đoạn. Các test Java nêu tên có trong source và nằm trong tổng335 đã chạy cùng ngày; không gán số ca riêng khi chưa đối soát. Frontend82 dùng node --test, không phải82component/DOM tests; UI12 dùng Edge thật. RQ11 có thiết kế và kiểm tra tĩnh, còn khảo sát cần chứng cứ người dùng. RQ12 có công cụ/diễn tập nhưng **G20 gói release cuối chưa nghiệm thu lại**.

## Các điều chỉnh contract cần giữ nhất quán

- Grade ghi bằng PUT /api/v1/teaching/grades; final chốt theo enrollment, mở lại theo class với expectedRevision.
- Ngoại lệ REQUESTED → APPROVED/REJECTED/INVALIDATED → CONSUMED; không TTL7ngày và không bỏ ràng buộc cứng.
- Học phí dùng tuition/paid_amount và payments/payment_corrections, không có bảng receipts hoặc tuitions.
- Account assignments là account_branches, lịch sử account_branch_events; giáo viên teacher_profile_events.
- Class FINISHED khác closure.closed và verification. Học xong FAIL vẫn COMPLETED; nợ không chặn học thuật.
- Gateway định tuyến, service kiểm JWT/live session; shared account/barrier là ngoại lệ điều phối SQL.
- TeacherManagement cũ không là bằng chứng route thực: App.tsx dùng AccountManagement cho quản trị Teacher, TeacherDirectory cho Staff; session nằm trong ScheduleManagement như cấu trúc hiện tại.

## Bằng chứng nguồn FE

[App.tsx](../../../../language-center-web-frontend/src/App.tsx), [navigation](../../../../language-center-web-frontend/src/config/navigation.ts), [API danh mục](../api-list.md). Tên màn hình trong bảng là nhóm component, không suy mọi component là một route độc lập. Các nhóm quyền chi tiết ở role-permissions; không ghi Staff không được điểm danh hoặc Admin được nhập điểm ban đầu thay Teacher.
