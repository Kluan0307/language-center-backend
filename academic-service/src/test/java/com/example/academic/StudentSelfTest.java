package com.example.academic;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.example.academic.student.*;
import com.example.academic.branch.*;
import com.example.academic.security.*;
import com.example.academic.common.ApiException;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
class StudentSelfTest {
 StudentRepository students;BranchRepository branches;StudentSelfController controller;
 @BeforeEach void setup(){students=mock(StudentRepository.class);branches=mock(BranchRepository.class);controller=new StudentSelfController(students,branches);}
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 void actor(Long account,Long profile,Role role){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new CurrentUser(account,profile,999L,role,"student",List.of()),null,List.of()));}
 @Test void readsByAccountAndActualBranchNotHeader(){actor(5L,9L,Role.STUDENT);var s=new Student();s.setId(9L);s.setAccountId(5L);s.setFullName("Student A");s.setPhone("0900000000");s.setBranchId(11L);var b=new Branch();b.setId(11L);b.setName("Branch A");b.setPhone("0900000001");when(students.findByAccountId(5L)).thenReturn(Optional.of(s));when(branches.findById(11L)).thenReturn(Optional.of(b));assertEquals("Student A",controller.profile().fullName());assertEquals(11L,controller.profile().branch().id());verify(students,times(2)).findByAccountId(5L);}
 @Test void rejectsUnlinkedOrMismatchedProfile(){actor(5L,9L,Role.STUDENT);when(students.findByAccountId(5L)).thenReturn(Optional.empty());assertThrows(ApiException.class,controller::profile);var s=new Student();s.setId(10L);when(students.findByAccountId(5L)).thenReturn(Optional.of(s));assertThrows(ApiException.class,controller::profile);verifyNoInteractions(branches);}
 @Test void nonStudentCannotUseSelfProjection(){actor(5L,9L,Role.ADMIN);assertThrows(ApiException.class,controller::profile);verifyNoInteractions(students);}
}
