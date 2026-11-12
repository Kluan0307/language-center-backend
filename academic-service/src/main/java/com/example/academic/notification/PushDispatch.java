package com.example.academic.notification;

import com.example.academic.common.BusinessTime;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service @RequiredArgsConstructor
public class PushDispatch {
 private final EntityManager em; private final JdbcTemplate jdbc;
 @org.springframework.beans.factory.annotation.Value("${notifications.expo.project-id:}") private String projectId;
 public record Attempt(Long id,String token,Long account,Long notification) {}
 private LocalDateTime now(){return LocalDateTime.now(BusinessTime.ZONE);}
 @Transactional(propagation=Propagation.REQUIRES_NEW) public Attempt claim(Long id){var p=em.find(PushDelivery.class,id,LockModeType.PESSIMISTIC_WRITE);if(p==null||!p.getStatus().equals("PENDING"))return null;var n=em.find(StudentNotification.class,p.getNotificationId());var d=em.find(PushDevice.class,p.getInstallationId(),LockModeType.PESSIMISTIC_WRITE);var prefs=n==null?null:em.find(NotificationPreference.class,n.getAccountId());boolean valid=d!=null&&d.isEnabled()&&d.getProjectId().equals(projectId)&&n!=null&&d.getAccountId().equals(n.getAccountId())&&d.getBindingVersion()==p.getBindingVersion()&&prefs!=null&&prefs.allows(n.getCategory())&&n.getCreatedAt().isAfter(now().minusHours(24));if(valid)valid=jdbc.queryForObject("SELECT COUNT(*) FROM accounts a JOIN students s ON s.account_id=a.id WHERE a.id=? AND s.id=? AND a.status=1 AND a.role='STUDENT' AND a.must_change_password=0 AND a.session_version=?",Long.class,d.getAccountId(),n.getStudentId(),d.getSessionVersion())==1;if(!valid){p.setStatus("SUPPRESSED");p.setResultCode("OWNER_SESSION_OR_PREFERENCE_CHANGED");return null;}p.setStatus("SENDING");p.setAttemptedAt(now());em.flush();return new Attempt(id,d.getPushToken(),n.getAccountId(),n.getId());}
 @Transactional(propagation=Propagation.REQUIRES_NEW) public void complete(Long id,ExpoPushGateway.Result r){var p=em.find(PushDelivery.class,id,LockModeType.PESSIMISTIC_WRITE);if(p==null||!p.getStatus().equals("SENDING"))return;p.setTicketId(r.id());p.setStatus(r.status().equals("OK")&&r.id()!=null?"ACCEPTED":r.status().equals("ERROR")?"FAILED":"UNKNOWN");p.setResultCode(r.code());disable(p,r.code());}
 private void disable(PushDelivery p,String code){if(!"DeviceNotRegistered".equals(code))return;var d=em.find(PushDevice.class,p.getInstallationId(),LockModeType.PESSIMISTIC_WRITE);if(d!=null&&d.getBindingVersion()==p.getBindingVersion()){d.setEnabled(false);d.setUpdatedAt(now());}}
 @Transactional(propagation=Propagation.REQUIRES_NEW) public void receipt(Long id,ExpoPushGateway.Result r){var p=em.find(PushDelivery.class,id,LockModeType.PESSIMISTIC_WRITE);if(p==null||!p.getStatus().equals("ACCEPTED"))return;p.setReceiptCheckedAt(now());if(r.status().equals("OK"))p.setStatus("HANDED_OFF");else if(r.status().equals("ERROR")){p.setStatus("FAILED");p.setResultCode(r.code());disable(p,r.code());}}
}
