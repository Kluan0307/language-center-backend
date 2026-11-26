package com.example.teaching.grading;

import com.example.teaching.evaluation.*;
import java.math.*;
import java.time.*;
import java.util.*;
import static com.example.teaching.grading.GradingModels.*;

/** Pure calculation. No numeric substitution for absent/missing results. */
public final class GradingCalculation {
 private GradingCalculation() {}
 public record Attempt(Long activityId,String name,LocalDate date,String status,BigDecimal score,Integer rawScore,Long conversionId,Level level,String comment,boolean selected) {}
 public record Item(Long rootId,Long activityId,String name,LocalDate date,String status,BigDecimal score,Integer rawScore,Long conversionId,Level level,String comment,List<Long> attempts,List<Attempt> history) {}
 public record Part(String code,String name,BigDecimal weight,BigDecimal score,BigDecimal maxScore,BigDecimal percent,Level level,String status,List<Item> assessments) {}
 public record Row(Long enrollmentId,Long studentId,String studentName,Kind kind,BigDecimal score,String unit,String academicStatus,String completionStatus,EvaluationDto.Attendance attendance,List<Part> components,List<String> reasons,Long mockActivityId) {}
 public record Report(Long classId,String templateId,String schemeName,Spec spec,String calculationVersion,LocalDate asOf,List<Row> students,List<String> rules) {}
 private static BigDecimal hundred(){return BigDecimal.valueOf(100);}
 private static BigDecimal rounded(BigDecimal v){return v==null?null:v.setScale(2,RoundingMode.HALF_UP);}
 public static BigDecimal ieltsOverall(List<BigDecimal> skills){if(skills.size()!=4||skills.stream().anyMatch(Objects::isNull))return null;return skills.stream().reduce(BigDecimal.ZERO,BigDecimal::add).divide(BigDecimal.valueOf(4),8,RoundingMode.HALF_UP).multiply(BigDecimal.TWO).setScale(0,RoundingMode.HALF_UP).divide(BigDecimal.TWO);}
 private static List<String> codes(GradingActivity a){return List.of(a.getComponents().split(","));}
 private static boolean applies(GradingActivity a,AcademicEvaluationContext.Learner l){return a.getRosterFrozenAt()!=null?a.getCandidates().stream().anyMatch(c->c.getStudentId().equals(l.studentId())&&c.getEnrollmentId().equals(l.enrollmentId())):l.periods()!=null&&l.periods().stream().anyMatch(p->p.includes(a.getAssessmentDate()));}
 private static Comparator<GradingActivity> order(){return Comparator.comparing(GradingActivity::getAssessmentDate).thenComparing(GradingActivity::getId);}
 private static GradingMark mark(List<GradingMark> marks,GradingActivity a,Long student,String code){return marks.stream().filter(m->m.getActivityId().equals(a.getId())&&m.getStudentId().equals(student)&&m.getComponent().equals(code)).findFirst().orElse(null);}
 private static List<Attempt> history(List<GradingActivity> attempts,List<GradingMark> marks,Long student,String code,Long selected){return attempts.stream().map(a->{var m=mark(marks,a,student,code);return new Attempt(a.getId(),a.getName(),a.getAssessmentDate(),m==null?"MISSING":m.getStatus().name(),m==null?null:m.getScore(),m==null?null:m.getRawScore(),m==null?null:m.getConversionId(),m==null?null:m.getLevel(),m==null?null:m.getComment(),a.getId().equals(selected));}).toList();}
 private static Item item(GradingActivity root,List<GradingActivity> all,List<GradingMark> marks,AcademicEvaluationContext.Learner l,String code,LocalDate today){
  var attempts=all.stream().filter(a->(a.getId().equals(root.getId())||Objects.equals(a.getRetakeOf(),root.getId()))&&applies(a,l)&&!a.getAssessmentDate().isAfter(today)).sorted(order()).toList();
  GradingActivity selected=root;GradingMark chosen=null;
  for(var a:attempts){var m=mark(marks,a,l.studentId(),code);if(m!=null&&(m.getStatus()==Status.GRADED||m.getStatus()==Status.EXEMPT)){selected=a;chosen=m;}}
  if(chosen==null){selected=attempts.isEmpty()?root:attempts.getLast();chosen=mark(marks,selected,l.studentId(),code);}
  String status=root.getAssessmentDate().isAfter(today)?"NOT_YET_DUE":chosen==null?"MISSING":chosen.getStatus().name();
  return new Item(root.getId(),selected.getId(),selected.getName(),selected.getAssessmentDate(),status,chosen==null?null:chosen.getScore(),chosen==null?null:chosen.getRawScore(),chosen==null?null:chosen.getConversionId(),chosen==null?null:chosen.getLevel(),chosen==null?null:chosen.getComment(),attempts.stream().map(GradingActivity::getId).toList(),history(attempts,marks,l.studentId(),code,selected.getId()));
 }
 public static Report calculate(GradingBinding binding,Spec spec,AcademicEvaluationContext context,List<GradingActivity> activities,List<GradingMark> marks,LocalDate today){
  var rows=new ArrayList<Row>();
  for(var learner:context.enrollments()){
   var reasons=new ArrayList<String>();var parts=new ArrayList<Part>();BigDecimal score=null;Long mockId=null;
   if(learner.periods()==null||learner.periods().isEmpty())reasons.add("Thiếu khoảng học của đăng ký");
   boolean mock=spec.kind()==Kind.IELTS||spec.kind()==Kind.TOEIC_LR;
   GradingActivity selectedMock=null;
   if(mock){var expected=new HashSet<>(spec.components().stream().map(Component::code).toList());
    var full=activities.stream().filter(a->a.getRetakeOf()==null&&new HashSet<>(codes(a)).equals(expected)&&applies(a,learner)).sorted(order()).toList();
    // Use the latest scheduled full mock, not a best-of mix across different tests.
    var due=full.stream().filter(a->!a.getAssessmentDate().isAfter(today)).toList();
    if(!due.isEmpty())selectedMock=due.getLast();else if(!full.isEmpty())selectedMock=full.getFirst();
    if(selectedMock!=null)mockId=selectedMock.getId();else reasons.add("Chưa có bài thi thử đầy đủ kỹ năng cho đăng ký này");
   }
   for(var component:spec.components()){
    var items=new ArrayList<Item>();
    // A full IELTS/TOEIC attempt is selected as one unit; partial retakes cannot
    // silently combine skill scores from different sittings.
    if(mock&&selectedMock!=null){var root=selectedMock;var eligible=activities.stream().filter(a->(a.getId().equals(root.getId())||Objects.equals(a.getRetakeOf(),root.getId()))&&applies(a,learner)&&!a.getAssessmentDate().isAfter(today)).sorted(order()).toList();
     GradingActivity chosen=null;for(var attempt:eligible)if(spec.components().stream().allMatch(c->{var m=mark(marks,attempt,learner.studentId(),c.code());return m!=null&&m.getStatus()==Status.GRADED;}))chosen=attempt;
     if(chosen==null)chosen=eligible.isEmpty()?root:eligible.getLast();var m=mark(marks,chosen,learner.studentId(),component.code());items.add(new Item(root.getId(),chosen.getId(),chosen.getName(),chosen.getAssessmentDate(),chosen.getAssessmentDate().isAfter(today)?"NOT_YET_DUE":m==null?"MISSING":m.getStatus().name(),m==null?null:m.getScore(),m==null?null:m.getRawScore(),m==null?null:m.getConversionId(),m==null?null:m.getLevel(),m==null?null:m.getComment(),eligible.stream().map(GradingActivity::getId).toList(),history(eligible,marks,learner.studentId(),component.code(),chosen.getId())));mockId=chosen.getId();
    }else if(!mock)for(var root:activities.stream().filter(a->a.getRetakeOf()==null&&codes(a).contains(component.code())&&applies(a,learner)).sorted(order()).toList())items.add(item(root,activities,marks,learner,component.code(),today));
    var graded=items.stream().filter(i->i.status().equals("GRADED")).toList();boolean allExempt=!items.isEmpty()&&items.stream().allMatch(i->i.status().equals("EXEMPT"));
    boolean missing=items.isEmpty()||items.stream().anyMatch(i->!Set.of("GRADED","EXEMPT").contains(i.status()));
    BigDecimal value=null,percent=null;Level level=null;
    if(spec.kind()==Kind.OBSERVATION){if(!graded.isEmpty())level=graded.getLast().level();}
    else if(!graded.isEmpty()){
     boolean valid=graded.stream().allMatch(i->i.score()!=null&&i.score().compareTo(component.minScore())>=0&&i.score().compareTo(component.maxScore())<=0&&i.score().subtract(component.minScore()).remainder(component.step()).signum()==0);
     if(valid){value=graded.stream().map(Item::score).reduce(BigDecimal.ZERO,BigDecimal::add).divide(BigDecimal.valueOf(graded.size()),16,RoundingMode.HALF_UP);percent=value.multiply(hundred()).divide(component.maxScore(),16,RoundingMode.HALF_UP);}else missing=true;
    }
    String status=allExempt?"EXEMPT":missing?"INCOMPLETE":spec.kind()==Kind.OBSERVATION&&level==null?"INCOMPLETE":"GRADED";
    if(component.required()&&!status.equals("GRADED")&&!status.equals("EXEMPT"))reasons.add("Chưa đủ kết quả bắt buộc: "+component.name());
    if(mock&&!status.equals("GRADED"))reasons.add("Chưa có điểm kỹ năng hợp lệ: "+component.name());
    parts.add(new Part(component.code(),component.name(),component.weight(),value,component.maxScore(),percent,level,status,List.copyOf(items)));
   }
   String academic="INSUFFICIENT",unit=spec.kind()==Kind.INTERNAL?"PERCENT":spec.kind()==Kind.IELTS?"BAND":spec.kind()==Kind.TOEIC_LR?"TOEIC":"OBSERVATION";
   if(reasons.isEmpty()){
    if(spec.kind()==Kind.INTERNAL){BigDecimal total=BigDecimal.ZERO,weight=BigDecimal.ZERO;for(var p:parts)if(p.percent()!=null){total=total.add(p.percent().multiply(p.weight()));weight=weight.add(p.weight());}if(weight.signum()>0){score=total.divide(weight,16,RoundingMode.HALF_UP);academic=score.compareTo(spec.passPercent())>=0?"PASS":"FAIL";}else reasons.add("Không có thành phần có điểm để tính tổng kết");}
    else if(mock){var scores=parts.stream().map(Part::score).toList();score=spec.kind()==Kind.IELTS?ieltsOverall(scores):scores.stream().reduce(BigDecimal.ZERO,BigDecimal::add);academic=spec.targetScore()==null?"RECORDED":score.compareTo(spec.targetScore())>=0&&parts.stream().allMatch(p->spec.minimumSkillScore()==null||p.score().compareTo(spec.minimumSkillScore())>=0)?"PASS":"FAIL";}
    else if(parts.stream().noneMatch(p->p.level()!=null))reasons.add("Chưa có nhận xét để đánh giá");
    else academic=parts.stream().anyMatch(p->p.level()==Level.NEEDS_SUPPORT)?"NEEDS_SUPPORT":parts.stream().anyMatch(p->p.level()==Level.DEVELOPING)?"DEVELOPING":"MEETS_EXPECTATIONS";
   }
   var attendance=EvaluationCalculator.attendanceFor(learner,context,spec.minAttendancePercent(),today);
   boolean attendanceKnown=spec.minAttendancePercent()==null||attendance.percent()!=null;
   boolean attendancePassed=spec.minAttendancePercent()==null||attendanceKnown&&attendance.percent().compareTo(spec.minAttendancePercent())>=0;
   boolean ended=context.duration()!=null&&context.duration().timeReadyForClosure();
   String completion=academic.equals("INSUFFICIENT")?"INSUFFICIENT":!ended?"IN_PROGRESS":!attendanceKnown?"INSUFFICIENT":!attendancePassed?"ATTENDANCE_NOT_MET":academic;
   if(!ended)reasons.add("Chưa đủ thời lượng hoặc còn buổi học chưa hoàn tất");if(!attendanceKnown)reasons.add("Chưa đủ dữ liệu chuyên cần");else if(!attendancePassed)reasons.add("Chuyên cần chưa đạt ngưỡng quy chế");
   rows.add(new Row(learner.enrollmentId(),learner.studentId(),learner.studentName(),spec.kind(),rounded(score),unit,academic,completion,attendance,List.copyOf(parts),List.copyOf(reasons),mockId));
  }
  return new Report(binding.getClassId(),binding.getTemplateId(),binding.getName(),spec,"DS4_V1",today,List.copyOf(rows),List.of("Điểm nội bộ: trung bình các bài có điểm trong từng thành phần, sau đó tính theo trọng số; so ngưỡng trước làm tròn.","Miễn được loại khỏi mẫu số; toàn bộ thành phần được miễn không tạo điểm tổng.","Thi lại giữ lịch sử; chọn lần có điểm hoặc miễn gần nhất. Vắng thi lại không xóa điểm hợp lệ trước đó.","IELTS/TOEIC: dùng bài thi thử đầy đủ gần nhất; không ghép kỹ năng giữa các lần thi. Điểm thi thử không phải chứng chỉ.","Chuyên cần và thời lượng là điều kiện riêng, không cộng vào band/điểm thi."));
 }
}
