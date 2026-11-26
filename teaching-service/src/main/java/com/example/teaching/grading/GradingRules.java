package com.example.teaching.grading;
import com.example.teaching.common.ApiException;
import java.math.BigDecimal;
import java.util.*;
import static com.example.teaching.grading.GradingModels.*;

public final class GradingRules {
 private GradingRules() {}
 private static BigDecimal n(String v){return new BigDecimal(v);}
 public static Spec standard(Kind kind){
  List<Component> components=switch(kind){
   case OBSERVATION -> List.of(observe("LISTENING","Nghe hiểu"),observe("SPEAKING","Phản xạ nói"),observe("VOCABULARY","Từ vựng"),observe("PARTICIPATION","Tham gia hoạt động"));
   case INTERNAL -> List.of(number("HOMEWORK","Bài tập",10,10),number("QUIZ","Kiểm tra định kỳ",10,20),number("MIDTERM","Giữa kỳ",10,30),number("FINAL","Cuối kỳ",10,40));
   case IELTS -> List.of(band("LISTENING","Listening"),band("READING","Reading"),band("WRITING","Writing"),band("SPEAKING","Speaking"));
   case TOEIC_LR -> List.of(toeic("LISTENING","Listening"),toeic("READING","Reading"));
  };
  return new Spec(kind,components,kind==Kind.INTERNAL?n("60"):null,kind==Kind.INTERNAL?n("80"):null);
 }
 private static Component observe(String c,String name){return new Component(c,name,true,null,null,null,null);}
 private static Component number(String c,String name,int max,int weight){return new Component(c,name,true,BigDecimal.ZERO,BigDecimal.valueOf(max),n("0.01"),BigDecimal.valueOf(weight));}
 private static Component band(String c,String name){return new Component(c,name,true,BigDecimal.ZERO,n("9"),n("0.5"),null);}
 private static Component toeic(String c,String name){return new Component(c,name,true,n("5"),n("495"),n("5"),null);}
 public static void validate(Spec s){
  if(s==null||s.kind()==null||s.components()==null||s.components().isEmpty()||s.components().size()>20)throw ApiException.bad("Quy chế phải có 1–20 thành phần");
  Set<String> codes=new HashSet<>();BigDecimal total=BigDecimal.ZERO;
  for(var c:s.components()){
   if(c==null||c.code()==null||!c.code().matches("[A-Z][A-Z0-9_]{0,39}")||!codes.add(c.code())||c.name()==null||c.name().isBlank()||c.name().length()>120||c.required()==null)throw ApiException.bad("Thành phần không hợp lệ hoặc trùng mã");
   if(s.kind()==Kind.OBSERVATION){if(c.minScore()!=null||c.maxScore()!=null||c.step()!=null||c.weight()!=null)throw ApiException.bad("Nhận xét trẻ nhỏ không dùng điểm hoặc trọng số");}
   else {
    if(c.minScore()==null||c.maxScore()==null||c.step()==null||c.minScore().signum()<0||c.maxScore().compareTo(c.minScore())<=0||c.maxScore().compareTo(n("10000"))>0||c.step().signum()<=0||c.step().compareTo(c.maxScore().subtract(c.minScore()))>0||c.minScore().scale()>2||c.maxScore().scale()>2||c.step().scale()>2)throw ApiException.bad("Thang điểm và bước điểm không hợp lệ");
    if(s.kind()==Kind.INTERNAL){if(c.weight()==null||c.weight().signum()<=0||c.weight().scale()>2)throw ApiException.bad("Trọng số phải dương và tối đa hai chữ số thập phân");total=total.add(c.weight());}
    else if(c.weight()!=null)throw ApiException.bad("IELTS/TOEIC không cộng trọng số điểm nội bộ");
   }
  }
  if(s.kind()==Kind.INTERNAL&&total.compareTo(n("100"))!=0)throw ApiException.bad("Tổng trọng số phải bằng 100%");
  if((s.kind()==Kind.IELTS||s.kind()==Kind.TOEIC_LR)){
   var expected=standard(s.kind()).components();if(!codes.equals(new HashSet<>(expected.stream().map(Component::code).toList())))throw ApiException.bad("Thiếu hoặc thừa kỹ năng của bài thi");
   for(var c:s.components()){var e=expected.stream().filter(x->x.code().equals(c.code())).findFirst().orElseThrow();if(c.minScore().compareTo(e.minScore())!=0||c.maxScore().compareTo(e.maxScore())!=0||c.step().compareTo(e.step())!=0||!c.required())throw ApiException.bad("Phải giữ đúng thang và kỹ năng bắt buộc IELTS/TOEIC");}
  }
  for(var p:Arrays.asList(s.passPercent(),s.minAttendancePercent()))if(p!=null&&(p.signum()<0||p.compareTo(n("100"))>0||p.scale()>2))throw ApiException.bad("Ngưỡng phần trăm phải từ 0 đến 100");
  if(s.kind()==Kind.INTERNAL&&s.passPercent()==null)throw ApiException.bad("Quy chế điểm nội bộ cần ngưỡng đạt");
  if(s.kind()!=Kind.INTERNAL&&s.passPercent()!=null)throw ApiException.bad("Không áp dụng ngưỡng phần trăm cho quy chế này");
  if(s.targetScore()!=null||s.minimumSkillScore()!=null){
   if(s.kind()!=Kind.IELTS&&s.kind()!=Kind.TOEIC_LR)throw ApiException.bad("Mục tiêu điểm chỉ dùng cho IELTS/TOEIC");
   if(s.targetScore()==null&&s.minimumSkillScore()!=null)throw ApiException.bad("Cần mục tiêu tổng khi quy định điểm kỹ năng tối thiểu");
   BigDecimal totalMax=s.kind()==Kind.IELTS?n("9"):n("990"),skillMax=s.kind()==Kind.IELTS?n("9"):n("495"),step=s.kind()==Kind.IELTS?n("0.5"):n("5");
   if(s.targetScore()!=null&&(s.targetScore().signum()<0||s.targetScore().compareTo(totalMax)>0||s.targetScore().remainder(step).signum()!=0)||s.minimumSkillScore()!=null&&(s.minimumSkillScore().signum()<0||s.minimumSkillScore().compareTo(skillMax)>0||s.minimumSkillScore().remainder(step).signum()!=0))throw ApiException.bad("Mục tiêu điểm không đúng thang hoặc bước điểm");
  }
 }
 public static void mark(Kind kind,Component c,Mark m){
  if(m.status()==null)throw ApiException.bad("Chọn trạng thái kết quả");
  if(m.status()!=Status.GRADED&&m.rawScore()!=null)throw ApiException.bad("Trạng thái chưa có điểm không được kèm số câu đúng");
  if(m.status()!=Status.GRADED){if(m.score()!=null||m.level()!=null)throw ApiException.bad("Vắng, miễn hoặc chưa nộp không được kèm điểm/mức đánh giá");if(m.comment()==null||m.comment().isBlank())throw ApiException.bad("Cần ghi chú cho trạng thái chưa có kết quả");return;}
  if(kind==Kind.OBSERVATION){if(m.score()!=null||m.level()==null||m.comment()==null||m.comment().isBlank())throw ApiException.bad("Nhận xét cần mức đánh giá và nội dung, không nhập điểm");return;}
  if(m.level()!=null||m.score()==null||m.score().compareTo(c.minScore())<0||m.score().compareTo(c.maxScore())>0||m.score().subtract(c.minScore()).remainder(c.step()).signum()!=0)throw ApiException.bad("Điểm phải đúng thang và bước điểm của thành phần");
 }
}
