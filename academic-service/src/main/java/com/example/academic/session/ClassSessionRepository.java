package com.example.academic.session;

import jakarta.persistence.LockModeType;
import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ClassSessionRepository
    extends JpaRepository<ClassSession, Long>, JpaSpecificationExecutor<ClassSession> {
  List<ClassSession> findBySchoolClassIdOrderBySessionDateAsc(Long classId);

  List<ClassSession> findBySchoolClassIdAndSessionDate(Long classId, LocalDate date);

  List<ClassSession> findByStatusNotAndSessionDateBetween(
      ClassSession.Status status, LocalDate from, LocalDate to);

  @Query(
      "select s from ClassSession s join fetch s.schoolClass c where s.status<>'CANCELLED' and"
          + " s.sessionDate between :from and :to and (c.branchId=:branch or s.teacherId in"
          + " :teachers)")
  List<ClassSession> conflictCandidates(
      @Param("branch") Long branch,
      @Param("teachers") List<Long> teachers,
      @Param("from") LocalDate from,
      @Param("to") LocalDate to);

  Optional<ClassSession> findByMakeupForId(Long id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from ClassSession s where s.id=:id")
  Optional<ClassSession> lock(@Param("id") Long id);

  @Query("select s.id as id,s.schoolClass.id as classId,s.startTime as startTime,s.endTime as endTime,s.sessionDate as sessionDate,s.status as status,s.kind as kind,s.makeupForId as makeupForId,s.makeupSessionId as makeupSessionId from ClassSession s where s.schoolClass.id in :ids")
  List<com.example.academic.duration.SessionDurationRow> durationRows(@Param("ids") Collection<Long> ids);

  boolean existsByRoomId(Long id);

  @Query(
      "select count(s)>0 from ClassSession s where s.roomId=:room and s.status='SCHEDULED' and"
          + " s.schoolClass.maxSize>:capacity")
  boolean exceedsCapacity(@Param("room") Long room, @Param("capacity") int capacity);

  boolean existsByRoomIdAndStatus(Long roomId, ClassSession.Status status);
}
