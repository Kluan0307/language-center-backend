package com.example.academic;

import com.example.academic.admission.*;
import com.example.academic.schoolclass.*;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Explicit OPEN_ENTRY evidence for tests of non-placement workflows, never a production fallback.
 */
@Component
@RequiredArgsConstructor
public class PlacementTestFixtures {
  private final EntityManager em;

  @Transactional
  public void open(Long classId) {
    var c = em.find(SchoolClass.class, classId);
    var program = new Program();
    program.setCode("P" + UUID.randomUUID());
    program.setName("Test program");
    em.persist(program);
    var level = new ProgramLevel();
    level.setProgram(program);
    level.setCode("OUT");
    level.setName("Test target");
    em.persist(level);
    em.flush();
    long revision =
        em.createQuery(
                    "select count(p) from CourseAdmissionPolicy p where p.courseId=:id", Long.class)
                .setParameter("id", c.getCourse().getId())
                .getSingleResult()
            + 1;
    var p = new CourseAdmissionPolicy();
    p.setCourseId(c.getCourse().getId());
    p.setRevision(revision);
    p.setMode(CourseAdmissionPolicy.Mode.OPEN_ENTRY);
    p.setProgramId(program.getId());
    p.setProgramCode(program.getCode());
    p.setProgramName(program.getName());
    p.setOutputLevelId(level.getId());
    p.setOutputLevelCode(level.getCode());
    p.setOutputLevelName(level.getName());
    p.setConfirmedBy(1L);
    p.setConfirmedByName("fixture");
    p.setConfirmedAt(LocalDateTime.now());
    p.setReason("Explicit open-entry fixture");
    em.persist(p);
    em.flush();
    c.setAdmissionPolicyId(p.getId());
    c.setAdmissionRevision(c.getAdmissionRevision() + 1);
    em.flush();
  }
}
