package com.example.teaching.grading;
import com.example.teaching.common.*;
import com.example.teaching.security.*;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
@Service @RequiredArgsConstructor @Transactional
public class GradingConversionService {
 private final EntityManager em;private final JsonMapper json;
 public record Mapping(@NotNull @Size(min=101,max=101) List<@NotNull Integer> listening,@NotNull @Size(min=101,max=101) List<@NotNull Integer> reading) {}
 public record Command(@NotBlank @Pattern(regexp="[A-Z][A-Z0-9_]{0,39}") String code,@NotNull @PositiveOrZero Long expectedRevision,@NotBlank @Size(max=160) String name,@NotBlank @Size(min=3,max=1000) String reference,@NotNull @Valid Mapping mapping) {}
 public record View(Long id,String code,long revision,String name,String reference,Mapping mapping) {}
 private void reader(){if(!Set.of(Role.SUPER_ADMIN,Role.ADMIN,Role.TEACHER).contains(Users.current().role()))throw ApiException.forbidden();}
 private View view(GradingConversion t){return new View(t.getId(),t.getCode(),t.getRevision(),t.getName(),t.getReference(),json.readValue(t.getMapping(),Mapping.class));}
 @Transactional(readOnly=true) public List<View> list(){reader();return em.createQuery("select t from GradingConversion t order by t.id desc",GradingConversion.class).setMaxResults(100).getResultList().stream().map(this::view).toList();}
 public View create(Command r){if(Users.current().role()!=Role.SUPER_ADMIN)throw ApiException.forbidden();
  for(var list:List.of(r.mapping().listening(),r.mapping().reading())){if(list.size()!=101)throw ApiException.bad("Bảng cần đủ số câu đúng từ 0 đến 100");int previous=0;for(var n:list){if(n==null||n<5||n>495||n%5!=0||n<previous)throw ApiException.bad("Điểm quy đổi phải tăng không giảm, từ 5–495, bước 5");previous=n;}}
  var existing=em.createQuery("select t from GradingConversion t where t.code=:c order by t.revision",GradingConversion.class).setParameter("c",r.code()).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();long latest=existing.isEmpty()?0:existing.getLast().getRevision();if(latest!=r.expectedRevision())throw ApiException.conflict("Bảng quy đổi đã thay đổi; tải lại");
  var t=new GradingConversion();t.setCode(r.code());t.setRevision(latest+1);t.setName(r.name().trim());t.setReference(r.reference().trim());t.setMapping(json.writeValueAsString(r.mapping()));t.setCreatedBy(Users.current().accountId());t.setCreatedAt(LocalDateTime.now(BusinessTime.ZONE));em.persist(t);return view(t);
 }
 public View require(Long id){var t=em.find(GradingConversion.class,id);if(t==null)throw ApiException.missing();return view(t);}
 public BigDecimal convert(Long id,String component,int raw){if(raw<0||raw>100)throw ApiException.bad("Số câu đúng phải từ 0 đến 100");var m=require(id).mapping();var list=switch(component){case "LISTENING"->m.listening();case "READING"->m.reading();default->throw ApiException.bad("Kỹ năng không hỗ trợ quy đổi TOEIC");};return BigDecimal.valueOf(list.get(raw));}
}
