package com.example.academic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.example.academic.branch.*;
import com.example.academic.common.*;
import com.example.academic.lead.*;
import com.example.academic.security.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    properties = "spring.datasource.url=jdbc:h2:mem:lead-care-tests;MODE=MySQL;DB_CLOSE_DELAY=-1")
@Transactional
class LeadCareWorkflowTest {
  @Autowired LeadService service;
  @Autowired LeadRepository leads;
  @Autowired LeadActivityRepository events;
  @Autowired BranchRepository branches;
  @MockitoBean LeadAssigneeDirectory assignees;
  Long branch;
  LeadDto lead;

  void actor(Role role, long id, Long branchId) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                new CurrentUser(
                    id,
                    id,
                    branchId,
                    role,
                    "actor-" + id,
                    branchId == null ? List.of() : List.of(branchId)),
                null,
                List.of()));
  }

  @BeforeEach
  void setup() {
    var b = new Branch();
    b.setCode(UUID.randomUUID().toString());
    b.setName("Branch");
    b.setPhone("0900000000");
    branches.saveAndFlush(b);
    branch = b.getId();
    actor(Role.STAFF, 11, branch);
    lead =
        service.save(
            null,
            new LeadDto.Request(
                "Prospect", "0900000000", null, "Referral", null, "A1", "Original note", branch));
  }

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  LeadDto care(LeadDto.Channel channel, Lead.Status status, LocalDateTime date) {
    return service.interact(
        lead.id(),
        new LeadDto.Interaction(
            channel, "Actual contact outcome", status, date, service.get(lead.id()).version()));
  }

  @Test
  void internalNoteDoesNotInventContact() {
    var saved = care(LeadDto.Channel.NOTE, Lead.Status.NEW, null);
    assertNull(saved.lastContactedAt());
    assertTrue(saved.version() > lead.version());
    assertEquals("NOTE", service.history(lead.id()).getFirst().getChannel());
  }

  @Test
  void administrativeStatusChangeDoesNotInventContact() {
    var saved =
        service.status(
            lead.id(),
            new LeadDto.StatusRequest(
                Lead.Status.CONTACTED, "Administrative update", lead.version()));
    assertNull(saved.lastContactedAt());
  }

  @Test
  void actualContactSavesActorAndFutureAppointment() {
    var future = LocalDateTime.now(BusinessTime.ZONE).withNano(0).plusDays(1);
    var saved = care(LeadDto.Channel.CALL, Lead.Status.CONTACTED, future);
    assertNotNull(saved.lastContactedAt());
    assertEquals(future, saved.nextContactAt());
    assertEquals("actor-11", service.history(lead.id()).getFirst().getActorName());
    assertEquals("Original note", saved.note());
  }

  @Test
  void staleCareCannotOverwriteNewAppointment() {
    care(LeadDto.Channel.CALL, Lead.Status.CONTACTED, null);
    assertThrows(
        ApiException.class,
        () ->
            service.interact(
                lead.id(),
                new LeadDto.Interaction(
                    LeadDto.Channel.NOTE,
                    "Old browser note",
                    Lead.Status.LOST,
                    null,
                    lead.version())));
  }

  @Test
  void appointmentsInPastAndClosedLeadWithNewAppointmentAreRejected() {
    assertThrows(
        ApiException.class,
        () ->
            care(
                LeadDto.Channel.CALL,
                Lead.Status.CONTACTED,
                LocalDateTime.now(BusinessTime.ZONE).withNano(0).minusMinutes(1)));
    assertThrows(
        ApiException.class,
        () ->
            care(
                LeadDto.Channel.CALL,
                Lead.Status.LOST,
                LocalDateTime.now(BusinessTime.ZONE).withNano(0).plusDays(1)));
  }

  @Test
  void overdueQueueContainsOnlyOpenLeads() {
    var row = leads.findById(lead.id()).orElseThrow();
    row.setNextContactAt(LocalDateTime.now(BusinessTime.ZONE).withNano(0).minusMinutes(5));
    leads.flush();
    assertEquals(
        1,
        service.list(0, 20, null, null, null, branch, Lead.FollowUp.OVERDUE, null).totalElements());
    var closed =
        service.status(
            lead.id(),
            new LeadDto.StatusRequest(
                Lead.Status.LOST, "Prospect declined", service.get(lead.id()).version()));
    assertNull(closed.nextContactAt());
    assertEquals(
        0,
        service.list(0, 20, null, null, null, branch, Lead.FollowUp.OVERDUE, null).totalElements());
  }

  @Test
  void reassignmentPreservesAppointmentAndRemovesFormerStaffWriteAccess() {
    var date = LocalDateTime.now(BusinessTime.ZONE).withNano(0).plusDays(1);
    care(LeadDto.Channel.CALL, Lead.Status.CONTACTED, date);
    actor(Role.ADMIN, 1, branch);
    when(assignees.require(22L, branch))
        .thenReturn(
            new LeadAssigneeDirectory.Assignee(22L, "staff22", "Staff Twenty Two", "STAFF"));
    var changed =
        service.assign(
            lead.id(),
            new LeadDto.Assignment(22L, "Staff shift handover", service.get(lead.id()).version()));
    assertEquals(date, changed.nextContactAt());
    assertEquals("actor-11", service.history(lead.id()).getFirst().getOldAssigneeName());
    actor(Role.STAFF, 11, branch);
    assertThrows(ApiException.class, () -> care(LeadDto.Channel.CALL, Lead.Status.CONTACTED, null));
    assertThrows(
        ApiException.class, () -> service.convert(lead.id(), service.get(lead.id()).version()));
    actor(Role.STAFF, 22, branch);
    assertNotNull(care(LeadDto.Channel.CALL, Lead.Status.CONSULTING, null));
  }

  @Test
  void staffCannotReassign() {
    assertThrows(
        ApiException.class,
        () -> service.assign(lead.id(), new LeadDto.Assignment(22L, "Hand over", lead.version())));
    verify(assignees, never()).require(any(), any());
  }

  @Test
  void convertedLeadClearsAppointmentAndRetryDoesNotCreateAnotherStudent() {
    care(
        LeadDto.Channel.CALL,
        Lead.Status.CONSULTING,
        LocalDateTime.now(BusinessTime.ZONE).withNano(0).plusDays(1));
    var first = service.convert(lead.id(), service.get(lead.id()).version());
    int count = service.history(lead.id()).size();
    assertNull(first.nextContactAt());
    assertEquals(
        first.convertedStudent().id(),
        service.convert(lead.id(), service.get(lead.id()).version()).convertedStudent().id());
    assertEquals(count, service.history(lead.id()).size());
  }

  @Test
  void foreignBranchCannotReadHistory() {
    actor(Role.STAFF, 33, branch + 999);
    assertThrows(ApiException.class, () -> service.history(lead.id()));
  }

  @Test
  void staleConversionCannotCreateStudentFromUnseenChanges() {
    var prior = care(LeadDto.Channel.CALL, Lead.Status.CONTACTED, null);
    care(LeadDto.Channel.NOTE, Lead.Status.CONTACTED, null);
    assertThrows(ApiException.class, () -> service.convert(lead.id(), prior.version()));
    assertNull(service.get(lead.id()).convertedStudent());
  }
}
