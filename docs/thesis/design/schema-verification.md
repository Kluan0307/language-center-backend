# Đối chiếu mô hình, entity, migration và schema — G14

Ngày08/11/2026. Kiểm tra **chỉ đọc information_schema**, không truy vấn dòng nghiệp vụ, không chạy DDL/seed/migration vào database đang dùng và không sửa nghiệp vụ. [Metadata V013](schema-v013.metadata.json), [metadata hiện tại](schema-current.metadata.json), [mapping entity](entity-mapping.json), [kết quả so tên cột](model-validation.json), [fingerprints migration](migration-manifest.json).

## Nguồn và hai hiện trạng

| Nguồn | Quan sát | Cách dùng |
|---|---|---|
| Entity hiện tại Auth/Academic/Teaching/Finance |52 @Entity,51 bảng entity khác nhau;2 bảng ElementCollection; một ResultBarrier ánh xạ ở2 service | Class Diagram và owner logic |
| Bản sao đã apply/replay V001–V013 ở G11/G12 |53 bảng,625 cột; MySQL 8.0.46;35 FK đơn cột và 132 index/unique/PK theo tên | ERD vật lý và từ điển đầy đủ |
| Database ứng dụng hiện tại |33 bảng,388 cột;21 FK đơn cột và 87 index/unique/PK theo tên | Ghi chênh lệch; không tuyên bố đã nâng V013 |
| Migration source |13 file V001–V013; fingerprint lúc lập tài liệu | Đối chiếu ý định DDL, không thay execution ledger |

Schema bản sao được giữ từ G11/G12, nguồn chỉ đọc. G14 không tạo hoặc khôi phục bản sao mới. Các JSON xuất chỉ metadata, không chứa username/password/token giá trị, học viên hoặc số tiền thực tế. Schema vật lý không có bảng riêng cho từng DTO/report/projection.

## Chênh lệch cần xử lý khi triển khai mới

**20 bảng chưa có trong database hiện tại:**

- assessment_events
- class_admission_events
- class_closure_events
- class_closures
- class_duration_plans
- class_result_barriers
- course_admission_levels
- course_admission_policies
- course_duration_plans
- enrollment_admissions
- evaluation_policies
- evaluation_policy_exams
- evaluation_states
- final_results
- placement_assessments
- placement_exceptions
- program_levels
- programs
- result_reopens
- teacher_profile_events

**11 cột thiếu trên các bảng đã có:**

- courses.expected_minutes
- courses.duration_revision
- enrollments.completion_result_id
- enrollments.completion_result_revision
- exam_candidates.enrollment_id
- exams.version
- school_classes.admission_policy_id
- school_classes.admission_revision
- school_classes.expected_minutes
- school_classes.duration_revision
- teachers.version

**Khác kiểu/nullable đã quan sát:**

- courses.duration: int/NO -> int/YES; V011 cho duration legacy nullable, không bịa giá trị cho khóa mới.

Ngoài các cột/bảng trên, có 45 chữ ký index/unique/PK và 14 FK-column của target chưa thấy ở current (so theo cấu trúc, không chỉ tên). Một số nằm trên bảng mới nên không cộng thành lỗi độc lập. Không suy chính xác migration nào đã chạy chỉ từ hiện trạng; schema có thể đã có DDL thủ công/ddl-auto. Đọc từng migration, backup và diễn tập có ghi nhận trước khi nâng. G14 chỉ lập danh sách, không tự sửa để sơ đồ đẹp hơn.

| Index/unique target chưa có cùng cấu trúc | Cột |
|---|---|
| assessment_events.idx_assessment_event INDEX | assessment_id, id |
| assessment_events.PRIMARY UNIQUE | id |
| class_admission_events.idx_class_admission_event INDEX | class_id, id |
| class_admission_events.PRIMARY UNIQUE | id |
| class_closure_events.PRIMARY UNIQUE | request_id |
| class_closure_events.uk_closure_revision UNIQUE | class_id, revision |
| class_closures.PRIMARY UNIQUE | class_id |
| class_duration_plans.PRIMARY UNIQUE | id |
| class_duration_plans.uk_class_duration_revision UNIQUE | class_id, revision |
| class_result_barriers.PRIMARY UNIQUE | class_id |
| course_admission_levels.uk_admission_level UNIQUE | policy_id, level_id |
| course_admission_policies.PRIMARY UNIQUE | id |
| course_admission_policies.uk_course_admission_revision UNIQUE | course_id, revision |
| course_duration_plans.PRIMARY UNIQUE | id |
| course_duration_plans.uk_course_duration_revision UNIQUE | course_id, revision |
| enrollment_admissions.fk_enrollment_admission_approval INDEX | approval_id |
| enrollment_admissions.ix_enrollment_admission INDEX | enrollment_id, id |
| enrollment_admissions.PRIMARY UNIQUE | id |
| evaluation_policies.PRIMARY UNIQUE | id |
| evaluation_policies.uk_evaluation_policy_revision UNIQUE | class_id, revision |
| evaluation_policy_exams.fk_evaluation_exam INDEX | exam_id |
| evaluation_policy_exams.uk_evaluation_policy_exam UNIQUE | policy_id, exam_id |
| evaluation_states.PRIMARY UNIQUE | class_id |
| final_results.idx_final_class INDEX | class_id, id |
| final_results.idx_final_student INDEX | student_id, id |
| final_results.PRIMARY UNIQUE | id |
| final_results.uk_final_current UNIQUE | current_enrollment_id |
| final_results.uk_final_request UNIQUE | request_id |
| final_results.uk_final_revision UNIQUE | enrollment_id, revision |
| placement_assessments.idx_assessment_student_program INDEX | student_id, program_id, status |
| placement_assessments.PRIMARY UNIQUE | id |
| placement_assessments.uk_assessment_current UNIQUE | current_key |
| placement_exceptions.fk_placement_exception_class INDEX | class_id |
| placement_exceptions.fk_placement_exception_consumed INDEX | consumed_enrollment_id |
| placement_exceptions.fk_placement_exception_source INDEX | source_enrollment_id |
| placement_exceptions.fk_placement_exception_student INDEX | student_id |
| placement_exceptions.ix_placement_exception_branch INDEX | branch_id, id |
| placement_exceptions.PRIMARY UNIQUE | id |
| program_levels.PRIMARY UNIQUE | id |
| program_levels.uk_program_level_code UNIQUE | program_id, code |
| programs.PRIMARY UNIQUE | id |
| programs.uk_program_code UNIQUE | code |
| result_reopens.PRIMARY UNIQUE | request_id |
| teacher_profile_events.idx_teacher_event INDEX | teacher_id, id |
| teacher_profile_events.PRIMARY UNIQUE | id |

## Những khác biệt ánh xạ cần giải thích

- Không phát hiện field persistent scalar/owning join thiếu cột trên target theo bộ trích xuất. Đây là kiểm tên cột, **không** chứng nhận toàn bộ tương thích Hibernate hoặc mọi precision/nullable/index/default với Java. G11/G12 đã prod/validate theo bằng chứng của đợt đó; G14 không khởi động lại service để giả thành test runtime mới.
- final_results.current_enrollment_id là generated column MySQL không mapping thành field JPA. Đây là chủ đích: unique key chặn hai state CONFIRMED cùng enrollment; nhiều NULL cho các bản REOPENED được phép. Không thêm vào DTO để người dùng tự ghi.
- courses.scope_key và placement_assessments.current_key cũng là generated; entity đọc theo mapping read-only. Unique chứa NULL và generated key phải được hiểu theo MySQL, không thay bằng unique(student,program,status) làm mất lịch sử.
- course_admission_levels và evaluation_policy_exams không PK surrogate; có unique(policy_id,level_id/exam_id). Không vẽ id tự tưởng tượng. ElementCollection không là entity độc lập.
- Chín FK dưới đây do SQL migration giữ, còn field Java là Long/scalar (không object association):

- class_duration_plans.class_id -> school_classes
- course_admission_policies.course_id -> courses
- course_duration_plans.course_id -> courses
- enrollment_admissions.approval_id -> placement_exceptions
- enrollment_admissions.enrollment_id -> enrollments
- placement_exceptions.class_id -> school_classes
- placement_exceptions.consumed_enrollment_id -> enrollments
- placement_exceptions.source_enrollment_id -> enrollments
- placement_exceptions.student_id -> students

- FK evaluation_policy_exams.exam_id cũng là Long trong embeddable Rule; owning collection policy khác FK tới exam. course_admission_levels.level_id giữ snapshot và không có FK tới program_levels.
- Các ID liên service như teacher_id, exam.class_id, grade.student_id, tuition.enrollment_id, enrollment.completion_result_id **không FK SQL** trong target. Payment.tuition_id cũng là scalar không FK dù cùng Finance. Kiểm tra ở service/HTTP và index không thay thành ràng buộc FK trong sơ đồ.
- Snapshot/audit append-only là quy tắc service, không khẳng định MySQL có trigger ngăn UPDATE/DELETE trực tiếp. TeacherProfileEvent/DocumentEvent/GradeChange giữ scalar và bản chụp; giữ lịch sử do code, không suy cascade.
- Account có các bảng profile riêng; unique account_id trong từng bảng không tự ngăn một account xuất hiện ở hai loại profile khác nhau. Một profile khớp role là invariant của Auth, không cross-table CHECK SQL.
- Cardinality vật lý Enrollment→Period là0..* vì legacy có thể chưa có khoảng; enrollment mới do service tạo khoảng. Lead→Student từ FK/unique thực tế, không giả một profile chỉ có một enrollment hoặc một lớp suốt đời.
- Một class_result_barriers dùng chung schema; owner logic shared-coordination, không nhân đôi bảng hoặc vẽ mỗi service một database. Token/revision/counter chỉ là metadata khóa; không phải JWT hoặc kết quả học tập.

## Giới hạn và bước tiếp

Chưa kiểm dữ liệu orphan/range/nội dung JSON thực tế, chưa load test, chưa kiểm mọi trigger/check ngoài nguồn trích xuất, chưa xác nhận phục hồi production. Không chạy UPDATE/ALTER/DROP hay auto-migration; metadata của current chỉ đọc. Khi source/schema thay đổi phải snapshot metadata mới, chạy generator và render lại; không sửa JSON nhằm che drift. Migration/vận hành xem [hướng dẫn](../../migrations/README.md).

G15 tiếp tục Activity/Sequence và ranh giới transaction; G16 đồng bộ API/trace cuối; G17–G20 nghiệm thu/release. ERD và Class Diagram này là as-built target V013, không chứng nhận một trung tâm thật đã dùng chính sách đó.
