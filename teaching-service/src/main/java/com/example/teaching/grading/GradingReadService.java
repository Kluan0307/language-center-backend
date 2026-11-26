package com.example.teaching.grading;
import com.example.teaching.common.*;
import com.example.teaching.publication.ManagementProgressService;
import com.example.teaching.security.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class GradingReadService {
 private final GradingOutcomeService outcomes;private final GradingService grading;
 private final GradingWriteGuard guard;private final RemoteApi remote;private final BranchScope scope;private final JsonMapper json;
 @Value("${services.academic.url}") private String academic;
 public byte[] export(Long classId,boolean published){grading.binding(classId);if(!published){var p=outcomes.preview(classId);return GradingExport.csv("BẢN NHÁP · "+p.report().schemeName(),p.report().students());}
  var release=guard.latest(classId);if(release==null||!release.getState().equals("PUBLISHED"))throw ApiException.conflict("Chưa có công bố hiện hành");var report=json.readValue(release.getSnapshot(),GradingCalculation.Report.class);return GradingExport.csv("ĐÃ CÔNG BỐ v"+release.getRevision()+" · "+report.schemeName(),report.students());
 }
 public GradingOutcomeService.StudentView management(Long studentId,Long enrollmentId){
  if(!Set.of(Role.SUPER_ADMIN,Role.ADMIN,Role.STAFF).contains(Users.current().role()))throw ApiException.forbidden();
  var proof=remote.get(academic+"/api/v1/academic/students/"+studentId+"/learning-profile",ManagementProgressService.Profile.class);
  if(proof.student()==null||!Objects.equals(proof.student().id(),studentId)||proof.studies()==null)throw ApiException.missing();var e=proof.studies().stream().map(ManagementProgressService.Study::enrollment).filter(Objects::nonNull).filter(v->Objects.equals(v.id(),enrollmentId)&&Objects.equals(v.studentId(),studentId)).findFirst().orElseThrow(ApiException::missing);if(e.branchId()==null||e.classId()==null)throw ApiException.missing();scope.check(e.branchId());return outcomes.published(e.classId(),enrollmentId,studentId);
 }
 public byte[] studentExport(Long enrollmentId){var view=outcomes.student(enrollmentId);if(view.result()==null)throw ApiException.missing();return GradingExport.csv("ĐÃ CÔNG BỐ v"+view.revision()+" · "+view.templateName(),List.of(view.result()));}
}
