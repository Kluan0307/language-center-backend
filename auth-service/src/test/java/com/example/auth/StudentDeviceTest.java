package com.example.auth;
import static org.junit.jupiter.api.Assertions.*;
import com.example.auth.entity.*;
import com.example.auth.notification.*;
import com.example.auth.security.AccountPrincipal;
import jakarta.persistence.EntityManager;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import com.example.auth.common.ApiException;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:student-devices;MODE=MySQL;DB_CLOSE_DELAY=-1","notifications.expo.project-id=99999999-9999-4999-8999-999999999999"}) @Transactional
class StudentDeviceTest {
 @Autowired EntityManager em; @Autowired StudentDeviceController service;
 Account a,b; UUID installation=UUID.fromString("11111111-1111-4111-8111-111111111111");
 @BeforeEach void setup(){a=account("student-device-a");b=account("student-device-b");}
 Account account(String name){var a=new Account();a.setUsername(name);a.setPassword("test-only-hash");a.setRole(Role.STUDENT);a.setStatus(true);em.persist(a);em.flush();return a;}
 StudentDeviceController.Registration request(){return new StudentDeviceController.Registration("ExpoPushToken[test-only-device-token]","ANDROID",UUID.fromString("99999999-9999-4999-8999-999999999999"));}
 @Test void registrationBindsCurrentAccountAndDoesNotReturnToken(){var d=service.register(installation,request(),new AccountPrincipal(a));assertTrue(d.enabled());assertFalse(d.toString().contains(request().pushToken()));assertEquals(a.getId(),em.find(PushDevice.class,installation.toString()).getAccountId());}
 @Test void accountSwitchChangesBindingAndOldDeleteCannotDisableNewOwner(){var first=service.register(installation,request(),new AccountPrincipal(a));var next=service.register(installation,request(),new AccountPrincipal(b));assertTrue(next.version()>first.version());service.remove(installation,new AccountPrincipal(a));assertTrue(em.find(PushDevice.class,installation.toString()).isEnabled());assertEquals(b.getId(),em.find(PushDevice.class,installation.toString()).getAccountId());}
 @Test void ownerCanUnregisterIdempotently(){service.register(installation,request(),new AccountPrincipal(a));service.remove(installation,new AccountPrincipal(a));service.remove(installation,new AccountPrincipal(a));assertFalse(em.find(PushDevice.class,installation.toString()).isEnabled());}
 @Test void sameTokenCannotBindTwoInstallations(){service.register(installation,request(),new AccountPrincipal(a));assertThrows(ApiException.class,()->service.register(UUID.randomUUID(),request(),new AccountPrincipal(b)));}
 @Test void revokedAndLockedAccountsCannotRegister(){var principal=new AccountPrincipal(a);a.setSessionVersion(1);em.flush();assertThrows(ApiException.class,()->service.register(installation,request(),principal));a.setStatus(false);em.flush();assertThrows(ApiException.class,()->service.register(installation,request(),new AccountPrincipal(a)));}
 @Test void projectAndRoleMustMatch(){assertThrows(ApiException.class,()->service.register(installation,new StudentDeviceController.Registration(request().pushToken(),"ANDROID",UUID.randomUUID()),new AccountPrincipal(a)));a.setRole(Role.TEACHER);em.flush();assertThrows(ApiException.class,()->service.register(installation,request(),new AccountPrincipal(a)));}
 @Test void identicalRegistrationDoesNotInvalidatePendingPushBinding(){var first=service.register(installation,request(),new AccountPrincipal(a));var retry=service.register(installation,request(),new AccountPrincipal(a));assertEquals(first.version(),retry.version());}
}
