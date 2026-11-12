package com.example.auth.notification;

import com.example.auth.entity.*;
import com.example.auth.security.AccountPrincipal;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import com.example.auth.common.ApiException;

@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/auth/student/me/devices") @Transactional
public class StudentDeviceController {
 private final EntityManager em;
 @Value("${notifications.expo.project-id:}") private String projectId;
 public record Registration(@NotBlank @Pattern(regexp="(ExponentPushToken|ExpoPushToken)\\[[a-zA-Z0-9_-]{10,200}\\]") String pushToken,@NotBlank @Pattern(regexp="ANDROID|IOS") String platform,@NotNull UUID projectId) {}
 public record Binding(String installationId,long version,boolean enabled) {}
 private Account account(AccountPrincipal p){if(p==null)throw new ApiException(HttpStatus.UNAUTHORIZED,"Chưa đăng nhập");var a=em.find(Account.class,p.accountId(),LockModeType.PESSIMISTIC_WRITE);if(a==null||a.getRole()!=Role.STUDENT||!Boolean.TRUE.equals(a.getStatus())||a.isMustChangePassword()||a.getSessionVersion()!=p.sessionVersion())throw new ApiException(HttpStatus.FORBIDDEN,"Tài khoản không được đăng ký thiết bị");return a;}
 @PutMapping("/{installationId}") public Binding register(@PathVariable UUID installationId,@Valid @RequestBody Registration r,@AuthenticationPrincipal AccountPrincipal principal){var a=account(principal);if(projectId.isBlank())throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"Push project is not configured");if(!projectId.equals(r.projectId().toString()))throw new ApiException(HttpStatus.BAD_REQUEST,"Push project mismatch");String id=installationId.toString();var d=em.find(PushDevice.class,id,LockModeType.PESSIMISTIC_WRITE);boolean fresh=d==null;if(fresh){d=new PushDevice();d.setInstallationId(id);}var collisions=em.createQuery("select d from PushDevice d where d.pushToken=:token and d.installationId<>:id",PushDevice.class).setParameter("token",r.pushToken()).setParameter("id",id).getResultList();if(!collisions.isEmpty())throw new ApiException(HttpStatus.CONFLICT,"Push token belongs to another installation");if(!fresh&&d.getAccountId().equals(a.getId())&&d.getSessionVersion()==a.getSessionVersion()&&d.isEnabled()&&d.getPushToken().equals(r.pushToken())&&d.getPlatform().equals(r.platform())&&d.getProjectId().equals(projectId))return new Binding(id,d.getBindingVersion(),true);d.setAccountId(a.getId());d.setSessionVersion(a.getSessionVersion());d.setPushToken(r.pushToken());d.setPlatform(r.platform());d.setProjectId(projectId);d.setEnabled(true);d.setUpdatedAt(LocalDateTime.now(com.example.auth.common.BusinessTime.ZONE));if(fresh)em.persist(d);em.flush();return new Binding(id,d.getBindingVersion(),true);}
 @DeleteMapping("/{installationId}") @ResponseStatus(HttpStatus.NO_CONTENT) public void remove(@PathVariable UUID installationId,@AuthenticationPrincipal AccountPrincipal principal){var a=account(principal);var d=em.find(PushDevice.class,installationId.toString(),LockModeType.PESSIMISTIC_WRITE);if(d!=null&&d.getAccountId().equals(a.getId())){d.setEnabled(false);d.setUpdatedAt(LocalDateTime.now(com.example.auth.common.BusinessTime.ZONE));}}
}
