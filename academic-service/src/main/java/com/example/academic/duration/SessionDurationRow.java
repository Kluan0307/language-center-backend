package com.example.academic.duration;

import com.example.academic.session.ClassSession;
import java.time.*;

public interface SessionDurationRow {
  Long getId();

  Long getClassId();

  LocalTime getStartTime();

  LocalTime getEndTime();

  LocalDate getSessionDate();

  ClassSession.Status getStatus();

  ClassSession.Kind getKind();

  Long getMakeupForId();

  Long getMakeupSessionId();
}
