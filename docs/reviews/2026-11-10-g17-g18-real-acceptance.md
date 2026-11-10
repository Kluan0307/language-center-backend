# Nghiệm thu lại G17–G18 qua ứng dụng thật — 10/11/2026

**86/86 kiểm tra đạt** trên lượt tái tạo sạch `20261110-0365823a`, schema `lc_rehearsal_1794284386660_859a8d`. [Manifest bằng chứng tại bàn giao](2026-11-10-g17-g18-evidence.json), [acceptance-summary.json](../../storage/acceptance/20261110-0365823a/acceptance-summary.json).

Bộ chạy đầu `20261110-c9e02461` cũng đạt86 nhưng có các lần sửa harness; lượt sạch mới dùng prepare → restore → runtime → API → browser → SQL → stop bằng run.ps1. Không dùng các script mô phỏng cũ làm bằng chứng. Hướng dẫn: [chạy lại và demo](../../deployment/acceptance-g17-g18.md).

Các ID dưới đây lấy từ dữ liệu API thực của lượt mới: chi nhánh A#34, B#35; lớp#53; học viên A#52, B#53; enrollment A#43, B#44; học phí A#20, B#21. Thời lượng180phút, A85/PASS, B40/FAIL; thu3M, còn nợB3M.

## Expected và actual

Các giá trị ID/hash động phải bằng dữ liệu nhận trước từ API/bằng chứng trước khi thay đổi; số tiền, thời lượng, kết quả và HTTP kỳ vọng được đặt trước thao tác kiểm tra. Các race có thể đổi người thắng; invariant và status dự kiến phải giữ.

### g17 — 20 kiểm tra

| ID | Kịch bản | Expected | Actual |
|---|---|---|---|
|G17-01|Four actual roles, two isolated branches, valid sessions|`["SUPER_ADMIN","ADMIN","STAFF","TEACHER"]`|`["SUPER_ADMIN","ADMIN","STAFF","TEACHER"]`|
|G17-02|Contacted lead converts once to a real persisted Student|`true`|`true`|
|G17-03|Actual preview accepts B1 and rejects A2 by accepted level set|`[true,false]`|`[true,false]`|
|G17-04|Direct enrollment lacking academic eligibility is rejected|`409`|`409`|
|G17-05|Approval is really consumed once and replay returns the same enrollment|`true`|`true`|
|G17-06|After-commit tuition delivery is recorded and retry creates no duplicate|`true`|`true`|
|G17-07|First receipt 1M leaves 2M balance|`true`|`true`|
|G17-08|Same payment requestKey returns persisted receipt without another payment|`17`|`17`|
|G17-09|Two receipts total3M; paid status and zero balance|`[3000000,0,"PAID",2]`|`[3000000,0,"PAID",2]`|
|G17-10|Real scheduled minutes equal verified course plan|`180`|`180`|
|G17-11|Attendance really completes session and accumulates180 taught minutes|`[180,true,"COMPLETED"]`|`[180,true,"COMPLETED"]`|
|G17-12|Missing mandatory grade is INSUFFICIENT, not an invented PASS|`true`|`true`|
|G17-13|Provisional outcomes are PASS and FAIL with known attendance|`["PASS","FAIL"]`|`["PASS","FAIL"]`|
|G17-14|Final outcomes are persisted on two enrollment identities|`["PASS","FAIL"]`|`["PASS","FAIL"]`|
|G17-15|Grade modification after finalization is actually rejected|`409`|`409`|
|G17-16|PASS and FAIL both complete; unpaid B still has its original debt|`[true,true,3000000]`|`[true,true,3000000]`|
|G17-17|Class closed flag and closure proof are verified|`[true,"VERIFIED"]`|`[true,"VERIFIED"]`|
|G17-18|Real branch revenue matches predefined gross/net/debt|`{"grossCollected":3000000,"voided":0,"refunded":0,"collected":3000000,"outstanding":3000000}`|`{"grossCollected":3000000,"voided":0,"refunded":0,"collected":3000000,"outstanding":3000000}`|
|G17-19|Reload preserves Student profile, periods and final reference|`true`|`true`|
|G17-20|Search/filter/pagination returns exactly the two synthetic students|`[2,1,2]`|`[2,1,2]`|

### g17-crud — 8 kiểm tra

| ID | Kịch bản | Expected | Actual |
|---|---|---|---|
|CRUD-01|Create/read/update Student persists actual fields|`true`|`true`|
|CRUD-02|Unused Student can be deleted while historic Student remains protected|`[204,409]`|`[204,409]`|
|CRUD-03|Teacher profile update persists qualification/specialty|`true`|`true`|
|CRUD-04|Teacher without responsibilities can stop working explicitly|`"INACTIVE"`|`"INACTIVE"`|
|CRUD-05|Course metadata edit persists without inventing duration conversion|`"CRUD updated"`|`"CRUD updated"`|
|CRUD-06|Course stops new use through CLOSED status, not a fabricated DELETE route|`"CLOSED"`|`"CLOSED"`|
|CRUD-07|Class metadata edit and detail reload preserve the new name|`"CRUD class updated"`|`"CRUD class updated"`|
|CRUD-08|Unused class deletion works and class with enrollments cannot be deleted|`[204,409]`|`[204,409]`|

### g18 — 38 kiểm tra

| ID | Kịch bản | Expected | Actual |
|---|---|---|---|
|G18-01|Cross-branch object, query and X-Branch-Id are denied|`[404,404,403]`|`[404,404,403]`|
|G18-02|Foreign Teacher cannot access class and Teacher cannot open CRM|`[404,403]`|`[404,403]`|
|G18-03|Direct admin APIs reject Staff/Teacher|`[403,403,403,403]`|`[403,403,403,403]`|
|G18-04|Real JWT validation rejects altered signature and correctly signed expired token|`[401,401]`|`[401,401]`|
|G18-05|Revoked session cannot save; fresh login works|`[204,401,200]`|`[204,401,200]`|
|G18-06|Partial-scope Admin cannot edit global multi-branch Teacher profile|`[false,403]`|`[false,403]`|
|G18-07|Stale Lead write is rejected|`409`|`409`|
|G18-08|Stale Teacher profile version is rejected|`409`|`409`|
|G18-09|Two actual concurrent enrollments cannot consume the same last seat|`[200,409,1]`|`[200,409,1]`|
|G18-10|Concurrent same-intent payments return one persisted receipt|`[200,200,true,1]`|`[200,200,true,1]`|
|G18-11|Concurrent same-intent refund creates one journal entry|`[200,200,true,1]`|`[200,200,true,1]`|
|G18-12|No second refund beyond credit and ledger remains consistent|`[409,600,0,0,1000,400,600]`|`[409,600,0,0,1000,400,600]`|
|G18-13|Connection dropped after actual payment commit retries without duplication|`[200,true,true,1]`|`[200,true,true,1]`|
|G18-14|Real offboarding versus assignment leaves no responsibility on inactive Teacher|`[1,0]`|`[1,0]`|
|G18-15|Membership revocation races assignment without leaving work outside assignments|`[1,0]`|`[1,0]`|
|G18-16|Concurrent same final intent returns the same result identity|`[200,200,true]`|`[200,200,true]`|
|G18-17|Concurrent identical reopen intent does not create duplicate result changes|`[200,200]`|`[200,200]`|
|G18-18|Different concurrent final intents publish one current version|`[200,409]`|`[200,409]`|
|G18-19|Grade edit racing final confirmation cannot both commit|`[200,409]`|`[200,409]`|
|G18-20|Academic attendance racing Teaching confirmation is coordinated|`[200,409]`|`[200,409]`|
|G18-21|Changed source rejects an earlier preview fingerprint|`409`|`409`|
|G18-22|Original confirmed snapshot remains byte-identical after reopen and source corrections|`true`|`true`|
|G18-23|Old completion reference is rejected after reopening|`409`|`409`|
|G18-24|Old class status endpoint does not bypass explicit closure|`409`|`409`|
|G18-25|Teaching outage cannot partially change Academic completion|`[503,true]`|`[503,true]`|
|G18-26|Same Academic request/version succeeds after Teaching restarts|`5`|`5`|
|G18-27|Finance process outage preserves enrollment and marks tuition delivery failure|`true`|`true`|
|G18-28|Tuition retry after Finance recovery creates exactly one original bill|`[true,1]`|`[true,1]`|
|G18-29|Academic outage makes Teacher offboarding fail closed with no profile mutation|`[503,"ACTIVE"]`|`[503,"ACTIVE"]`|
|G18-30|Deleting Student/Class with established history is rejected|`[409,409]`|`[409,409]`|
|G18-31|Unapproved document is hidden from Staff and foreign Teacher|`[404,404]`|`[404,404]`|
|G18-32|Staff can download approved bytes but cannot access history/revisions|`[200,403,403]`|`[200,403,403]`|
|G18-33|Owner can retrieve unchanged old revision after approved replacement|`"8c39393da7af47d9bbd4b1732a375357bea8ef7e9caf8699c7a7663a34aab529"`|`"8c39393da7af47d9bbd4b1732a375357bea8ef7e9caf8699c7a7663a34aab529"`|
|G18-34|Same-intent concurrent VOID keeps the original receipt and one reversal|`[200,200,true,1,100,100]`|`[200,200,true,1,100,100]`|
|G18-35|Second VOID is blocked and gross-void-refund equals net|`[409,1100,100,400,600]`|`[409,1100,100,400,600]`|
|G18-36|Stale assessment cannot confirm over current evidence|`409`|`409`|
|G18-37|Stale approval decision cannot overwrite the accepted revision|`409`|`409`|
|G18-38|Changed assessment invalidates approval and blocks stale consumption|`["INVALIDATED",409]`|`["INVALIDATED",409]`|

### browser — 12 kiểm tra

| ID | Kịch bản | Expected | Actual |
|---|---|---|---|
|UI-01|Staff profile shows real periods, attendance and approved final summary|`true`|`true`|
|UI-02|Direct grade-management URL does not grant Staff score access|`true`|`true`|
|UI-03|Teacher can read own final but cannot finalize or reopen|`true`|`true`|
|UI-04|Final history renders persisted policy/score/minute snapshot|`true`|`true`|
|UI-05|Enrollment detail confirms actual synchronized result revision|`true`|`true`|
|UI-06|Class deep link shows explicit audited closure instead of invented CLOSED enum|`true`|`true`|
|UI-07|UI shows the real paid3M bill after split receipts|`true`|`true`|
|UI-08|Actual form submission and reload persist Student through backend|`true`|`true`|
|UI-09|Delayed real branchA response cannot replace branchB data after switch|`true`|`true`|
|UI-10|Session revoked while editing rejects actual save and creates no Student|`true`|`true`|
|UI-11|Mobile390 viewport contains profile without page overflow|`true`|`true`|
|UI-12|No uncaught JavaScript exceptions in these real UI journeys|`[]`|`[]`|

### database — 8 kiểm tra

| ID | Kịch bản | Expected | Actual |
|---|---|---|---|
|DB-01|Runtime uses separate target V013 schema with53 tables625 columns|`[true,53,625]`|`[true,53,625]`|
|DB-02|Both completed enrollments reference their actual current final revision|`[2,2]`|`[2,2]`|
|DB-03|Barrier counter matches exactly two current results with no duplicates|`[2,2,0]`|`[2,2,0]`|
|DB-04|Raw bills and receipts prove split payment without duplicate charges|`[2,2,3000000,3000000,0]`|`[2,2,3000000,3000000,0]`|
|DB-05|SQL confirms last-seat invariant beyond HTTP response codes|`[1,1,1]`|`[1,1,1]`|
|DB-06|SQL snapshot SHA equals earlier pre-reopen HTTP evidence|`"bbea971fadac5a27afec9955092002840336b66a22245c493a4324bd4eaefe4f"`|`"bbea971fadac5a27afec9955092002840336b66a22245c493a4324bd4eaefe4f"`|
|DB-07|Both document revisions exist in isolated file storage|`true`|`true`|
|DB-08|Source database fingerprint and row counts equal snapshot taken before test|`true`|`true`|

## Bằng chứng và giới hạn

Bản kê hash tại [acceptance-summary.json](../../storage/acceptance/20261110-0365823a/acceptance-summary.json); API [http.jsonl](../../storage/acceptance/20261110-0365823a/http.jsonl); [Student profile](../../storage/acceptance/20261110-0365823a/g17-student-profile.png), [class closure](../../storage/acceptance/20261110-0365823a/g17-class-closure.png), [branch switch](../../storage/acceptance/20261110-0365823a/g18-branch-switch.png), [mobile](../../storage/acceptance/20261110-0365823a/g17-mobile-profile.png). Raw dump/private-session không thuộc hồ sơ công khai.

[Log tái tạo](../../storage/acceptance-reproducibility-final.log), [ops9](../../storage/acceptance-operations-tests.log), [smoke19](../../storage/acceptance/20261110-0365823a/smoke.json). Source database không đổi theo DB-08; không DROP schema thử hoặc xóa lịch sử. Kết quả này không chứng nhận production/load hay toàn bộ release G20. [Bàn giao G17](../thesis-progress/G17.md), [G18](../thesis-progress/G18.md).
