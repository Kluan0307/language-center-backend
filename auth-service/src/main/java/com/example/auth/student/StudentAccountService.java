package com.example.auth.student;

import com.example.auth.common.*;
import com.example.auth.entity.*;
import com.example.auth.repository.AccountRepository;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
@Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
public class StudentAccountService {
 private final JdbcTemplate jdbc;
 private final AccountRepository accounts;
 private final StudentProvisioningRepository provisions;
 private final PasswordEncoder encoder;
 public record Profile(Long id,Long accountId,Long branchId,String fullName,String email,String phone,long version) {}
 public record View(Long studentId,long studentVersion,boolean linked,Long accountId,String username,Boolean active,Boolean mustChangePassword,Long version) {}
 public record Create(@NotNull java.util.UUID requestId,@NotNull @Positive Long studentId,@NotNull @PositiveOrZero Long expectedStudentVersion,@NotBlank @Pattern(regexp="[A-Za-z0-9._-]{3,50}") String username,@NotBlank @Size(min=8,max=72) String temporaryPassword,@NotBlank String confirmPassword) {}
 public record Status(@NotNull Boolean active,@NotNull @PositiveOrZero Long version,@NotBlank @Size(min=3,max=255) String reason) {}
 public record Reset(@NotBlank @Size(min=8,max=72) String temporaryPassword,@NotBlank String confirmPassword,@NotNull @PositiveOrZero Long version,@NotBlank @Size(min=3,max=255) String reason) {}
 private Profile one(String clause,Long id,boolean lock) {
  var rows=jdbc.query("select id,account_id,branch_id,full_name,email,phone,version from students where "+clause+"=?"+(lock?" for update":""),(rs,n)->new Profile(rs.getLong("id"),(Long)rs.getObject("account_id"),rs.getLong("branch_id"),rs.getString("full_name"),rs.getString("email"),rs.getString("phone"),rs.getLong("version")),id);
  if(rows.size()!=1)throw ApiException.missing();return rows.getFirst();
 }
 @Transactional(readOnly=true)
 public Profile profile(Long accountId) { try { return one("account_id",accountId,false); } catch (ApiException error) { if(error.getStatus()==org.springframework.http.HttpStatus.NOT_FOUND) throw ApiException.forbidden(); throw error; } }
 private Account manager(String username, Profile student) {
  Account caller=accounts.findByUsername(username).orElseThrow(ApiException::missing);
  if(!Boolean.TRUE.equals(caller.getStatus()) || (caller.getRole()!=Role.SUPER_ADMIN && caller.getRole()!=Role.ADMIN))throw ApiException.forbidden();
  if(caller.getRole()!=Role.SUPER_ADMIN&&!caller.activeBranchIds().contains(student.branchId()))throw ApiException.missing();
  return caller;
 }
 private void password(String value,String confirm) {
  if(value==null || value.length()<8 || value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72 || !value.equals(confirm))throw ApiException.bad("Mật khẩu cần tối thiểu8ký tự, tối đa72byte và xác nhận khớp");
 }
 private View view(Profile p) {
  if(p.accountId()==null)return new View(p.id(),p.version(),false,null,null,null,null,null);
  var account=accounts.findById(p.accountId()).orElseThrow(ApiException::missing);
  if(account.getRole()!=Role.STUDENT)throw ApiException.conflict("Liên kết tài khoản không hợp lệ");
  var provision=provisions.findByAccountId(account.getId()).orElseThrow(ApiException::missing);
  return new View(p.id(),p.version(),true,account.getId(),account.getUsername(),account.getStatus(),account.isMustChangePassword(),provision.getVersion());
 }
 @Transactional(readOnly=true)
 public View get(Long studentId,String username) {var p=one("id",studentId,false);manager(username,p);return view(p);}
 @Transactional(readOnly=true)
 public View request(String id,String username) {var r=provisions.findById(id).orElseThrow(ApiException::missing);return get(r.getStudentId(),username);}
 public View create(Create r,String username) {
  var student=one("id",r.studentId(),true);var caller=manager(username,student);password(r.temporaryPassword(),r.confirmPassword());
  var prior=provisions.findById(r.requestId().toString());
  if(prior.isPresent()) {var p=prior.get();if(!p.getStudentId().equals(r.studentId())||!p.getUsername().equals(r.username())||p.getExpectedStudentVersion()!=r.expectedStudentVersion()||!encoder.matches(r.temporaryPassword(),p.getInitialPasswordHash()))throw ApiException.conflict("requestId đã dùng cho yêu cầu khác");return view(student);}
  if(student.accountId()!=null)throw ApiException.conflict("Học viên đã có tài khoản");
  if(student.version()!=r.expectedStudentVersion())throw ApiException.conflict("Hồ sơ đã thay đổi, hãy tải lại");
  if(accounts.existsByUsername(r.username()))throw ApiException.conflict("Tên đăng nhập đã tồn tại");
  String hash=encoder.encode(r.temporaryPassword());
  var account=Account.builder().username(r.username()).password(hash).role(Role.STUDENT).status(true).mustChangePassword(true).branchId(student.branchId()).build();
  account.recordSecurityEvent("STUDENT_ACCOUNT_CREATED",caller.getId(),"studentId="+student.id()+";requestId="+r.requestId());accounts.saveAndFlush(account);
  int updated=jdbc.update("update students set account_id=?,version=version+1 where id=? and account_id is null and version=?",account.getId(),student.id(),r.expectedStudentVersion());
  if(updated!=1)throw ApiException.conflict("Hồ sơ đã thay đổi");
  var p=new StudentProvisioning();p.setRequestId(r.requestId().toString());p.setStudentId(student.id());p.setAccountId(account.getId());p.setUsername(r.username());p.setExpectedStudentVersion(r.expectedStudentVersion());p.setInitialPasswordHash(hash);p.setCreatedBy(caller.getId());p.setCreatedAt(LocalDateTime.now(BusinessTime.ZONE));provisions.saveAndFlush(p);
  return view(one("id",student.id(),false));
 }
 private StudentProvisioning operation(Long accountId,Long version,Profile student) {
  if(!accountId.equals(student.accountId()))throw ApiException.missing();
  var p=provisions.findByAccountId(accountId).orElseThrow(ApiException::missing);
  if(version==null||p.getVersion()!=version)throw ApiException.conflict("Tài khoản đã thay đổi, hãy tải lại");
  p.setOperationCount(Math.addExact(p.getOperationCount(),1));return p;
 }
 public View status(Long id,Status r,String username) {
  if(r.reason()==null || r.reason().trim().length()<3)throw ApiException.bad("Reason is required");
  var student=one("account_id",id,true);var caller=manager(username,student);
  var p=operation(id,r.version(),student);var a=accounts.lock(id).orElseThrow(ApiException::missing);if(a.getRole()!=Role.STUDENT)throw ApiException.forbidden();
  if(!r.active().equals(a.getStatus())) {a.setStatus(r.active());a.setSessionVersion(Math.addExact(a.getSessionVersion(),1));a.recordSecurityEvent(r.active()?"ACCOUNT_UNLOCKED":"ACCOUNT_LOCKED",caller.getId(),r.reason().trim());}
  accounts.saveAndFlush(a);provisions.saveAndFlush(p);return view(student);
 }
 public View reset(Long id,Reset r,String username) {
  var student=one("account_id",id,true);var caller=manager(username,student);password(r.temporaryPassword(),r.confirmPassword());
  var p=operation(id,r.version(),student);var a=accounts.lock(id).orElseThrow(ApiException::missing);if(a.getRole()!=Role.STUDENT)throw ApiException.forbidden();
  if(encoder.matches(r.temporaryPassword(),a.getPassword()))throw ApiException.bad("Mật khẩu mới phải khác mật khẩu hiện tại");
  a.setPassword(encoder.encode(r.temporaryPassword()));a.setMustChangePassword(true);a.setPasswordChangedAt(LocalDateTime.now(BusinessTime.ZONE));a.setSessionVersion(Math.addExact(a.getSessionVersion(),1));a.recordSecurityEvent("PASSWORD_RESET",caller.getId(),r.reason().trim());accounts.saveAndFlush(a);provisions.saveAndFlush(p);return view(student);
 }
}
