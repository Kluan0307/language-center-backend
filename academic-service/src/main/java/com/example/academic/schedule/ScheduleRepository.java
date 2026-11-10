package com.example.academic.schedule;

import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface ScheduleRepository
    extends JpaRepository<Schedule, Long>, JpaSpecificationExecutor<Schedule> {
  @Query(
      "select s from Schedule s join fetch s.schoolClass c where s.dayOfWeek=:day and"
          + " (c.branchId=:branch or c.teacherId=:teacher)")
  List<Schedule> conflictCandidates(
      @org.springframework.data.repository.query.Param("branch") Long branch,
      @org.springframework.data.repository.query.Param("teacher") Long teacher,
      @org.springframework.data.repository.query.Param("day") java.time.DayOfWeek day);

  List<Schedule> findBySchoolClassId(Long id);

  List<Schedule> findBySchoolClassBranchIdAndDayOfWeek(Long branchId, java.time.DayOfWeek day);
}
