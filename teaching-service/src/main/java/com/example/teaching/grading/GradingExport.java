package com.example.teaching.grading;
import java.nio.charset.StandardCharsets;
import java.util.*;
/** UTF-8 CSV suitable for spreadsheets; untrusted text cannot become formulas. */
public final class GradingExport {
 private GradingExport() {}
 public static String cell(Object input){String text=input==null?"":String.valueOf(input);String stripped=text.stripLeading();if(!stripped.isEmpty()&&"=+-@".indexOf(stripped.charAt(0))>=0)text="'"+text;return "\""+text.replace("\"","\"\"")+"\"";}
 private static void row(StringBuilder out,Object... values){out.append(Arrays.stream(values).map(GradingExport::cell).collect(java.util.stream.Collectors.joining(","))).append("\r\n");}
 public static byte[] csv(String label,List<GradingCalculation.Row> students){
  var out=new StringBuilder("\ufeff");row(out,"Bảng điểm",label);row(out,"Đăng ký","Học viên","Loại","Tổng","Đơn vị","Học thuật","Điều kiện hoàn thành","Chuyên cần (%)","Thành phần","Bài đánh giá","Ngày","Trạng thái","Điểm","Thang tối đa","Mức nhận xét","Số câu đúng","Bảng quy đổi","Nhận xét","Được tính");
  for(var student:students)for(var part:student.components()){
   if(part.assessments().isEmpty()){row(out,student.enrollmentId(),student.studentName(),student.kind(),student.score(),student.unit(),student.academicStatus(),student.completionStatus(),student.attendance().percent(),part.name(),"","",part.status(),"",part.maxScore(),part.level(),"","","","");continue;}
   for(var assessment:part.assessments()){
    var history=assessment.history();if(history==null||history.isEmpty())history=List.of(new GradingCalculation.Attempt(assessment.activityId(),assessment.name(),assessment.date(),assessment.status(),assessment.score(),assessment.rawScore(),assessment.conversionId(),assessment.level(),assessment.comment(),true));
    for(var attempt:history)row(out,student.enrollmentId(),student.studentName(),student.kind(),student.score(),student.unit(),student.academicStatus(),student.completionStatus(),student.attendance().percent(),part.name(),attempt.name(),attempt.date(),attempt.status(),attempt.score(),part.maxScore(),attempt.level(),attempt.rawScore(),attempt.conversionId(),attempt.comment(),attempt.selected()?"Có":"Không");
   }
  }
  return out.toString().getBytes(StandardCharsets.UTF_8);
 }
}
