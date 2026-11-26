package com.example.academic.student;
import com.example.academic.common.*;
import java.time.*;

public final class StudentProfileRules {
 private StudentProfileRules() {}
 public static String text(String value){return value==null||value.isBlank()?null:value.trim();}
 public static void phone(String value,boolean required){
   String p=text(value);
   if(p==null){if(required)throw ApiException.bad("Nhập số điện thoại liên hệ");return;}
   if(!p.matches("[+0-9() .-]{6,30}")||p.replaceAll("[^0-9]","").length()<6||p.replaceAll("[^0-9]","").length()>15)
     throw ApiException.bad("Số điện thoại phải có từ 6 đến 15 chữ số");
 }
 public static void validate(StudentDto.Request r){
   if(r.fullName()==null||r.fullName().isBlank())throw ApiException.bad("Nhập họ tên học viên");
   phone(r.phone(),true);phone(r.guardianPhone(),false);
   var today=LocalDate.now(BusinessTime.ZONE);
   if(r.dateOfBirth()!=null&&r.dateOfBirth().isAfter(today))throw ApiException.bad("Ngày sinh không được ở tương lai");
   boolean minor=r.dateOfBirth()!=null&&r.dateOfBirth().plusYears(18).isAfter(today);
   if(minor&&(text(r.guardianName())==null||text(r.guardianPhone())==null))
     throw ApiException.bad("Học viên dưới 18 tuổi cần họ tên và số điện thoại người giám hộ");
   if((text(r.guardianName())==null)!=(text(r.guardianPhone())==null))
     throw ApiException.bad("Nhập đầy đủ họ tên và số điện thoại người giám hộ");
 }
}
