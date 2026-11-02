param([string]$BaseUrl="http://localhost:8082/api/v1/academic")
$ErrorActionPreference="Stop"
if (-not $env:JWT_SECRET) { throw "Set JWT_SECRET before running this script." }
Add-Type -AssemblyName System.Net.Http
$client=[System.Net.Http.HttpClient]::new()
$script:checks=0
function Encode64([byte[]]$bytes) { [Convert]::ToBase64String($bytes).TrimEnd('=').Replace('+','-').Replace('/','_') }
function Token([string]$Role,$Branch,$Profile=71004,[int]$Lifetime=3600) {
 $now=[DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
 $claims=@{sub="smoke-$($Role.ToLower())";role=$Role;accountId=71001;profileId=$Profile;iat=$now;exp=($now+$Lifetime)}
 if($null-ne$Branch){$claims.branchId=$Branch}
 $header=Encode64 ([Text.Encoding]::UTF8.GetBytes('{"alg":"HS256","typ":"JWT"}'))
 $payload=Encode64 ([Text.Encoding]::UTF8.GetBytes(($claims|ConvertTo-Json -Compress)))
 $hmac=[Security.Cryptography.HMACSHA256]::new([Text.Encoding]::UTF8.GetBytes($env:JWT_SECRET))
 try { $signature=Encode64 ($hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes("$header.$payload"))) } finally { $hmac.Dispose() }
 return "$header.$payload.$signature"
}
function Send([string]$Method,[string]$Path,[string]$Jwt,$Body=$null) {
 $request=[Net.Http.HttpRequestMessage]::new([Net.Http.HttpMethod]::new($Method),"$BaseUrl$Path")
 if($Jwt){$request.Headers.Authorization=[Net.Http.Headers.AuthenticationHeaderValue]::new("Bearer",$Jwt)}
 if($null-ne$Body){$request.Content=[Net.Http.StringContent]::new(($Body|ConvertTo-Json -Depth 20 -Compress),[Text.Encoding]::UTF8,"application/json")}
 return $client.SendAsync($request)
}
function Call([string]$Method,[string]$Path,[string]$Jwt,$Body=$null,[int]$Expected=200) {
 $response=(Send $Method $Path $Jwt $Body).GetAwaiter().GetResult()
 try {
  $text=$response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
  if([int]$response.StatusCode-ne$Expected){throw "$Method $Path expected $Expected, received $([int]$response.StatusCode): $text"}
  $script:checks++
  if($text){return ($text|ConvertFrom-Json)}
 } finally {$response.Dispose()}
}
function Assert($Condition,[string]$Message){if(-not $Condition){throw $Message};$script:checks++}
$prefix="SMOKE"+[DateTime]::UtcNow.ToString("yyyyMMddHHmmss")
New-Item -ItemType Directory -Path "$PSScriptRoot/../target" -Force|Out-Null
[IO.File]::WriteAllText("$PSScriptRoot/../target/smoke-prefix.txt",$prefix)
$super=Token SUPER_ADMIN $null
$branchA=Call POST /branches $super @{code="$prefix-A";name="$prefix A";phone="0900000101"}
$branchB=Call POST /branches $super @{code="$prefix-B";name="$prefix B";phone="0900000102"}
$admin=Token ADMIN $branchA.id
$staff=Token STAFF $branchA.id
$teacher=Token TEACHER $branchA.id
$otherTeacher=Token TEACHER $branchA.id 71005
$adminB=Token ADMIN $branchB.id
$courseBody=@{code="$prefix-COURSE";name="Smoke course";duration=12;tuitionFee=1500000;maxStudents=5;level="A1";type="GENERAL";branchId=$branchB.id}
$course=Call POST /courses $admin $courseBody
Assert ($course.branchId-eq$branchA.id) "Client branch must be ignored"
$global=Call POST /courses $super @{code="$prefix-GLOBAL";name="Shared course";duration=12;tuitionFee=1000000;maxStudents=5}
$courseB=Call POST /courses $adminB $courseBody
$roomBody=@{code="$prefix-ROOM";name="Room";capacity=5;branchId=$branchA.id}
$room=Call POST /rooms $admin $roomBody
$roomB=Call POST /rooms $adminB $roomBody
$room2=Call POST /rooms $admin @{code="$prefix-ROOM2";name="Room 2";capacity=5}
$room3=Call POST /rooms $admin @{code="$prefix-ROOM3";name="Room 3";capacity=5}
function ClassBody([string]$Code,$Room,$Teacher,$Capacity=2) {
 return @{code="$prefix-$Code";name="Smoke $Code";courseId=$course.id;teacherId=$Teacher;teacherName="Smoke teacher";roomId=$Room;startDate=[DateTime]::Today.AddDays(-7).ToString("yyyy-MM-dd");endDate=[DateTime]::Today.AddDays(30).ToString("yyyy-MM-dd");maxSize=$Capacity;branchId=$branchA.id}
}
$classBody=ClassBody CLASS $room.id 71004
$class=Call POST /classes $admin $classBody
$classBodyB=ClassBody CLASS-B $roomB.id 71004
$classBodyB.courseId=$courseB.id;$classBodyB.branchId=$branchB.id
$classB=Call POST /classes $adminB $classBodyB
$class2=Call POST /classes $admin (ClassBody CLASS2 $room2.id 71005)
$raceClass=Call POST /classes $admin (ClassBody RACE $room3.id 71006 1)
$scheduleBody=@{classId=$class.id;dayOfWeek="MONDAY";startTime="18:00";endTime="19:00"}
$schedule=Call POST /schedules $admin $scheduleBody
$scheduleB=Call POST /schedules $adminB @{classId=$classB.id;dayOfWeek="MONDAY";startTime="18:00";endTime="19:00"}
$schedule2=Call POST /schedules $admin @{classId=$class2.id;dayOfWeek="MONDAY";startTime="18:00";endTime="19:00"}
$studentBody=@{fullName="$prefix Student";phone="$prefix-01";currentLevel="A1";branchId=$branchB.id}
$student=Call POST /students $admin $studentBody
$studentB=Call POST /students $adminB $studentBody
$student2=Call POST /students $staff @{fullName="$prefix Student2";phone="$prefix-02";currentLevel="A1"}
$student3=Call POST /students $admin @{fullName="$prefix Student3";phone="$prefix-03"}
$student4=Call POST /students $admin @{fullName="$prefix Student4";phone="$prefix-04"}
$enrollment=Call POST /enrollments $admin @{studentId=$student.id;classId=$class.id}
$enrollmentB=Call POST /enrollments $adminB @{studentId=$studentB.id;classId=$classB.id}
$leadBody=@{fullName="$prefix Lead";phone="$prefix-L1";desiredLevel="A2";interestedCourseId=$course.id;note="Initial"}
$lead=Call POST /leads $staff $leadBody
$leadB=Call POST /leads $adminB @{fullName="$prefix Lead B";phone="$prefix-LB";interestedCourseId=$courseB.id}
Write-Host "Fixtures ready: $prefix"
# Every declared endpoint must reject a missing token before body/query validation.
$endpoints=@(
 @("GET","/branches"),@("GET","/branches/$($branchA.id)"),@("POST","/branches"),@("PUT","/branches/$($branchA.id)"),@("PATCH","/branches/$($branchA.id)/toggle-status"),
 @("GET","/courses"),@("GET","/courses/$($course.id)"),@("POST","/courses"),@("PUT","/courses/$($course.id)"),@("PATCH","/courses/$($course.id)/status"),
 @("GET","/rooms"),@("GET","/rooms/$($room.id)"),@("POST","/rooms"),@("PUT","/rooms/$($room.id)"),@("PATCH","/rooms/$($room.id)/status"),@("DELETE","/rooms/$($room.id)"),
 @("GET","/students"),@("GET","/students/$($student.id)"),@("POST","/students"),@("PUT","/students/$($student.id)"),@("PATCH","/students/$($student.id)/status"),@("DELETE","/students/$($student.id)"),
 @("GET","/leads"),@("GET","/leads/$($lead.id)"),@("POST","/leads"),@("PUT","/leads/$($lead.id)"),@("PATCH","/leads/$($lead.id)/status"),@("POST","/leads/$($lead.id)/convert"),
 @("GET","/classes"),@("GET","/classes/my"),@("GET","/classes/$($class.id)"),@("GET","/classes/$($class.id)/students"),@("GET","/classes/$($class.id)/schedules"),@("POST","/classes"),@("PUT","/classes/$($class.id)"),@("PATCH","/classes/$($class.id)/status"),@("DELETE","/classes/$($class.id)"),
 @("GET","/schedules"),@("GET","/schedules/my"),@("POST","/schedules"),@("PUT","/schedules/$($schedule.id)"),@("DELETE","/schedules/$($schedule.id)"),@("POST","/schedules/check-conflict"),
 @("GET","/enrollments"),@("GET","/enrollments/$($enrollment.id)"),@("POST","/enrollments"),@("PATCH","/enrollments/$($enrollment.id)/status"),@("POST","/enrollments/$($enrollment.id)/transfer"),
 @("GET","/attendances/sheet?classId=$($class.id)&date=2026-01-01"),@("POST","/attendances/bulk"),@("GET","/attendances/history?classId=$($class.id)"),@("GET","/attendances/summary?classId=$($class.id)"),@("GET","/stats/overview")
)
foreach($endpoint in $endpoints){$null=Call $endpoint[0] $endpoint[1] "" $null 401}
$null=Call GET /branches (Token ADMIN $null) $null 403
$null=Call GET /branches (Token ADMIN $branchA.id 71004 -10) $null 401
$null=Call GET /branches "invalid" $null 401
Write-Host "Anonymous and invalid JWT checks passed for all 53 endpoints."
# Role restrictions, with valid bodies to reach method authorization.
$null=Call POST /branches $admin @{code="DENIED";name="Denied";phone="0"} 403
$null=Call PUT "/branches/$($branchA.id)" $staff @{code="DENIED";name="Denied";phone="0"} 403
$null=Call PATCH "/branches/$($branchA.id)/toggle-status" $teacher $null 403
foreach($path in @("/leads","/students","/rooms","/enrollments","/stats/overview")){$null=Call GET $path $teacher $null 403}
$null=Call POST /courses $teacher $courseBody 403
$null=Call PUT "/courses/$($global.id)" $admin $courseBody 403
$null=Call PATCH "/courses/$($global.id)/status" $staff @{status="CLOSED"} 403
$null=Call POST /classes $teacher $classBody 403
$null=Call POST /schedules $staff $scheduleBody 403
$null=Call PUT "/schedules/$($schedule.id)" $staff $scheduleBody 403
$null=Call DELETE "/schedules/$($schedule.id)" $staff $null 403
$null=Call POST /schedules/check-conflict $staff $scheduleBody 403
$null=Call GET /classes/my $admin $null 403
$null=Call GET /schedules/my $staff $null 403
# Other branches return 404.
foreach($path in @("/branches/$($branchB.id)","/courses/$($courseB.id)","/rooms/$($roomB.id)","/students/$($studentB.id)","/leads/$($leadB.id)","/classes/$($classB.id)","/enrollments/$($enrollmentB.id)","/classes/$($classB.id)/students","/classes/$($classB.id)/schedules","/attendances/sheet?classId=$($classB.id)&date=2026-01-01","/attendances/history?classId=$($classB.id)","/attendances/summary?classId=$($classB.id)")){$null=Call GET $path $admin $null 404}
$null=Call PUT "/schedules/$($scheduleB.id)" $admin $scheduleBody 404
$null=Call DELETE "/schedules/$($scheduleB.id)" $admin $null 404
$null=Call PUT "/students/$($studentB.id)" $admin $studentBody 404
$null=Call GET "/classes/$($class2.id)" $teacher $null 404
$null=Call GET "/attendances/sheet?classId=$($class2.id)&date=2026-01-01" $teacher $null 404
$null=Call POST /attendances/bulk $otherTeacher @{classId=$class.id;sessionDate=[DateTime]::Today.ToString("yyyy-MM-dd");records=@(@{enrollmentId=$enrollment.id;status="PRESENT"})} 403
$mine=Call GET /classes/my $teacher
Assert (@($mine.content|Where-Object {$_.teacherId-ne71004}).Count-eq0) "Teacher class isolation failed"
$mineSchedules=Call GET /schedules/my $teacher
Assert (@($mineSchedules.content|Where-Object {$_.teacherId-ne71004}).Count-eq0) "Teacher schedule isolation failed"
$filtered=Call GET "/students?branchId=$($branchB.id)" $admin
Assert (@($filtered.content|Where-Object {$_.branchId-ne$branchA.id}).Count-eq0) "Branch filter override failed"
Write-Host "Role and branch isolation checks passed."
# Successful CRUD/list operations and status filters not covered by fixture creation.
$null=Call GET /branches $admin
$null=Call GET "/branches/$($branchA.id)" $admin
$null=Call PUT "/branches/$($branchA.id)" $super @{code=$branchA.code;name=$branchA.name;phone=$branchA.phone}
$null=Call PATCH "/branches/$($branchA.id)/toggle-status" $super
$null=Call PATCH "/branches/$($branchA.id)/toggle-status" $super
$null=Call GET "/courses?keyword=Smoke&status=OPEN&level=A1&type=GENERAL" $admin
$null=Call GET "/courses/$($global.id)" $teacher
$null=Call PUT "/courses/$($course.id)" $admin $courseBody
$null=Call PATCH "/courses/$($course.id)/status" $admin @{status="CLOSED"}
$null=Call POST /classes $admin (ClassBody CLOSED $room.id 71999) 400
$null=Call PATCH "/courses/$($course.id)/status" $staff @{status="OPEN"}
$null=Call GET "/rooms?keyword=Room&status=AVAILABLE" $staff
$null=Call GET "/rooms/$($room.id)" $admin
$null=Call PUT "/rooms/$($room.id)" $admin $roomBody
$null=Call PATCH "/rooms/$($room.id)/status" $staff @{status="MAINTENANCE"}
$null=Call PATCH "/rooms/$($room.id)/status" $admin @{status="AVAILABLE"}
$null=Call GET "/students?keyword=$prefix&level=A1&status=STUDYING" $admin
$null=Call PUT "/students/$($student.id)" $staff $studentBody
$null=Call PATCH "/students/$($student.id)/status" $admin @{status="PAUSED"}
$null=Call PATCH "/students/$($student.id)/status" $staff @{status="STUDYING"}
$null=Call GET "/leads?keyword=$prefix&status=NEW&assignedTo=71001" $staff
$null=Call GET "/leads/$($lead.id)" $admin
$null=Call PUT "/leads/$($lead.id)" $staff $leadBody
$null=Call GET "/classes?status=ONGOING&courseId=$($course.id)&teacherId=71004" $admin
$null=Call GET "/classes/$($class.id)/students" $teacher
$null=Call GET "/classes/$($class.id)/schedules" $teacher
$null=Call PUT "/classes/$($class.id)" $admin $classBody
$null=Call GET "/schedules?classId=$($class.id)&dayOfWeek=MONDAY&teacherId=71004&roomId=$($room.id)" $staff
$null=Call PUT "/schedules/$($schedule.id)" $admin $scheduleBody
$null=Call GET "/enrollments?keyword=Smoke&studyStatus=STUDYING&classId=$($class.id)&studentId=$($student.id)" $staff
$freeRoom=Call POST /rooms $admin @{code="$prefix-FREE";name="Free";capacity=1}
$null=Call DELETE "/rooms/$($freeRoom.id)" $admin $null 204
$freeStudent=Call POST /students $admin @{fullName="$prefix Free";phone="$prefix-FREE"}
$null=Call DELETE "/students/$($freeStudent.id)" $admin $null 204
$freeClass=Call POST /classes $admin (ClassBody FREE $room3.id 71998)
$freeSlot=Call POST /schedules $admin @{classId=$freeClass.id;dayOfWeek="SUNDAY";startTime="08:00";endTime="09:00"}
$null=Call DELETE "/classes/$($freeClass.id)" $admin $null 204
$null=Call DELETE "/schedules/$($freeSlot.id)" $admin $null 404
# Validation, resource conflicts, lead conversion and deletion rules.
$null=Call POST /rooms $admin @{code="";name="";capacity=0} 400
$null=Call GET "/students?page=-1" $admin $null 400
$null=Call GET "/courses?status=UNKNOWN" $admin $null 400
$null=Call POST /students $admin $studentBody 409
$null=Call POST /courses $admin $courseBody 409
$null=Call POST /courses $super @{code="$prefix-GLOBAL";name="Duplicate";duration=1;tuitionFee=1;maxStudents=1} 409
$null=Call POST /schedules $admin @{classId=$class.id;dayOfWeek="MONDAY";startTime="19:00";endTime="18:00"} 400
$null=Call POST /schedules $admin $scheduleBody 409
$conflicts=Call POST /schedules/check-conflict $admin $scheduleBody
Assert (@($conflicts).Count-gt0) "Conflict preview must return conflicts"
$adjacent=Call POST /schedules $admin @{classId=$class.id;dayOfWeek="MONDAY";startTime="19:00";endTime="20:00"}
$null=Call DELETE "/schedules/$($adjacent.id)" $admin $null 204
$conflictingClass=ClassBody CLASS-CONFLICT $room.id 71004
$conflictingClass.schedules=@(@{dayOfWeek="MONDAY";startTime="18:00";endTime="19:00"})
$null=Call POST /classes $admin $conflictingClass 409
$null=Call POST /classes $staff $conflictingClass 403
$class2Change=ClassBody CLASS2 $room.id 71004
$null=Call PUT "/classes/$($class2.id)" $admin $class2Change 409
$null=Call DELETE "/rooms/$($room.id)" $admin $null 409
$null=Call DELETE "/students/$($student.id)" $admin $null 409
$null=Call DELETE "/classes/$($class.id)" $admin $null 409
$null=Call POST "/leads/$($lead.id)/convert" $admin $null 400
$lead=Call PATCH "/leads/$($lead.id)/status" $staff @{status="CONTACTED";note="Contact one"}
$lead=Call PATCH "/leads/$($lead.id)/status" $staff @{status="CONSULTING";note="Contact two"}
Assert ($lead.note-match"Initial" -and $lead.note-match"Contact one" -and $lead.note-match"Contact two") "Lead notes were overwritten"
$lead=Call POST "/leads/$($lead.id)/convert" $admin
Assert ($lead.status-eq"CONVERTED" -and $lead.convertedStudent.currentLevel-eq"A2") "Lead conversion failed"
$null=Call PUT "/leads/$($lead.id)" $admin $leadBody 400
$null=Call PATCH "/leads/$($lead.id)/status" $admin @{status="LOST"} 400
$null=Call POST "/leads/$($lead.id)/convert" $admin $null 400
$null=Call DELETE "/students/$($lead.convertedStudent.id)" $admin $null 409
$duplicateLead=Call POST /leads $admin @{fullName="$prefix Duplicate";phone=$student.phone}
$null=Call PATCH "/leads/$($duplicateLead.id)/status" $admin @{status="CONTACTED"}
$null=Call POST "/leads/$($duplicateLead.id)/convert" $admin $null 409
# Enrollment constraints and count recalculation.
$null=Call POST /enrollments $admin @{studentId=$student.id;classId=$class.id} 409
$null=Call POST /enrollments $admin @{studentId=$student.id;classId=$class2.id} 409
$null=Call POST /enrollments $super @{studentId=$studentB.id;classId=$class.id} 400
$enrollment2=Call POST /enrollments $admin @{studentId=$student2.id;classId=$class.id}
$full=Call GET "/classes/$($class.id)" $admin
Assert ($full.schoolClass.currentSize-eq2 -and $full.schoolClass.status-eq"FULL") "FULL or currentSize incorrect"
$null=Call POST /enrollments $admin @{studentId=$student3.id;classId=$class.id} 400
$null=Call PATCH "/enrollments/$($enrollment2.id)/status" $staff @{studyStatus="DROPPED"}
$available=Call GET "/classes/$($class.id)" $admin
Assert ($available.schoolClass.currentSize-eq1 -and $available.schoolClass.status-eq"ONGOING") "FULL was not released"
$null=Call PATCH "/enrollments/$($enrollment2.id)/status" $staff @{studyStatus="STUDYING"}
# Attendance default, whole-class enforcement, upsert, history and teacher scope.
$today=[DateTime]::Today.ToString("yyyy-MM-dd")
$sheet=Call GET "/attendances/sheet?classId=$($class.id)&date=$today" $teacher
Assert ($sheet.records.Count-eq2 -and @($sheet.records|Where-Object {$_.status-ne"PRESENT" -or $_.saved}).Count-eq0) "Default attendance sheet incorrect"
$null=Call POST /attendances/bulk $teacher @{classId=$class.id;sessionDate=$today;records=@(@{enrollmentId=$enrollment.id;status="ABSENT";hasPermission=$true})} 400
$records=@(@{enrollmentId=$enrollment.id;status="ABSENT";hasPermission=$true},@{enrollmentId=$enrollment2.id;status="LATE";hasPermission=$true})
$sheet=Call POST /attendances/bulk $teacher @{classId=$class.id;sessionDate=$today;records=$records}
Assert (@($sheet.records|Where-Object {$_.status-eq"LATE" -and $_.hasPermission}).Count-eq0) "LATE must store hasPermission=false"
$records[0].status="PRESENT"
$sheet=Call POST /attendances/bulk $teacher @{classId=$class.id;sessionDate=$today;records=$records}
$history=Call GET "/attendances/history?classId=$($class.id)" $staff
Assert (@($history)[0].present-eq1 -and @($history)[0].late-eq1) "Attendance upsert created duplicates"
$summary=Call GET "/attendances/summary?classId=$($class.id)" $teacher
Assert (@($summary|Where-Object {$_.totalSessions-ne1 -or $_.participationRate-ne100}).Count-eq0) "Attendance summary incorrect"
$future=[DateTime]::Today.AddDays(1).ToString("yyyy-MM-dd")
$null=Call POST /attendances/bulk $admin @{classId=$class.id;sessionDate=$future;records=$records} 400
# Transfer excludes the source class from student timetable checks.
$transferred=Call POST "/enrollments/$($enrollment.id)/transfer" $staff @{newClassId=$class2.id}
$old=Call GET "/enrollments/$($enrollment.id)" $admin
Assert ($old.studyStatus-eq"TRANSFERRED" -and $transferred.classId-eq$class2.id) "Transfer failed"
$detail=Call GET "/students/$($student.id)" $admin
Assert ($detail.enrollments.Count-eq2) "Student detail must contain registration history"
$null=Call PATCH "/classes/$($class2.id)/status" $admin @{status="FINISHED"}
$null=Call POST /enrollments $admin @{studentId=$student3.id;classId=$class2.id} 400
# Two HTTP requests race for the single seat.
$race1=Send POST /enrollments $admin @{studentId=$student3.id;classId=$raceClass.id}
$race2=Send POST /enrollments $admin @{studentId=$student4.id;classId=$raceClass.id}
$r1=$race1.GetAwaiter().GetResult();$r2=$race2.GetAwaiter().GetResult()
$codes=@([int]$r1.StatusCode,[int]$r2.StatusCode)|Sort-Object
Assert ($codes[0]-eq200 -and $codes[1]-eq400) "Concurrent enrollment must accept exactly one request"
$r1.Dispose();$r2.Dispose()
$race=Call GET "/classes/$($raceClass.id)" $admin
Assert ($race.schoolClass.currentSize-eq1) "Concurrent enrollment exceeded capacity"
$stats=Call GET /stats/overview $admin
Assert ($stats.overall.branchId-eq$branchA.id -and $stats.overall.studentsByStatus.STUDYING-ge5) "Stats scope or counts incorrect"
$groups=Call GET "/stats/overview?groupByBranch=true" $super
Assert (@($groups.branches).Count-ge2) "Grouped stats missing"
$null=Call GET "/stats/overview?groupByBranch=true" $staff $null 403
Write-Host "PASS: $script:checks checks; run prefix $prefix. No auth-service tables were modified."
$client.Dispose()
