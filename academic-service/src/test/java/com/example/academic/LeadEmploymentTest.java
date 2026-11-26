package com.example.academic;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.example.academic.common.*;
import com.example.academic.lead.*;
import com.example.academic.security.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
class LeadEmploymentTest {
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 @Test void offboardedAccountCannotReceiveNewLeadsEvenWhenLoginIsUnlocked(){
  SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new CurrentUser(1L,1L,1L,Role.ADMIN,"admin"),null,List.of()));
  var api=mock(RemoteApi.class);var directory=new LeadAssigneeDirectory(api,mock(BranchScope.class),mock(org.springframework.jdbc.core.JdbcTemplate.class));
  when(api.get(anyString(),eq(LeadAssigneeDirectory.Page.class))).thenReturn(new LeadAssigneeDirectory.Page(List.of(new LeadAssigneeDirectory.Account(2L,"active","Active","STAFF",true,List.of(1L),"ACTIVE","NS001"),new LeadAssigneeDirectory.Account(3L,"inactive","Inactive","STAFF",true,List.of(1L),"INACTIVE","NS002")),1));
  var rows=directory.list(1L);assertEquals(1,rows.size());assertEquals(2L,rows.getFirst().accountId());
 }
}
