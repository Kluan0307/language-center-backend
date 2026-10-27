package com.example.academic.schedule;

import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface ScheduleRepository
    extends JpaRepository<Schedule, Long>, JpaSpecificationExecutor<Schedule> {
  List<Schedule> findBySchoolClassId(Long id);

  List<Schedule> findBySchoolClassBranchIdAndDayOfWeek(Long branchId, java.time.DayOfWeek day);
}
