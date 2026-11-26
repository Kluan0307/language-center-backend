package com.example.academic;
import com.example.academic.course.*;
import com.example.academic.schoolclass.SchoolClass;
import com.example.academic.admission.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class CourseClassificationViewTest {
 @Test void unclassifiedLegacyCourseDoesNotInventAnAudience(){var view=CourseClassificationView.of(new Course(),mock(EntityManager.class));assertNull(view.programName());assertTrue(view.audiences().isEmpty());assertFalse(view.complete());}
 @Test void enrollmentUsesItsClassPolicySnapshot(){var em=mock(EntityManager.class);var course=new Course();course.setProgramId(1L);var program=new Program();program.setName("Tên mới");program.setCode("GE");when(em.find(Program.class,1L)).thenReturn(program);var policy=new CourseAdmissionPolicy();policy.setProgramCode("GE");policy.setProgramName("Tên lúc phân lớp");when(em.find(CourseAdmissionPolicy.class,2L)).thenReturn(policy);var schoolClass=new SchoolClass();schoolClass.setCourse(course);schoolClass.setAdmissionPolicyId(2L);var result=CourseClassificationView.of(schoolClass,em);assertEquals("Tên lúc phân lớp",result.programName());assertTrue(result.audiences().isEmpty());}
 @Test void audienceIsInformationalCourseMetadata(){var course=new Course();var group=new AudienceGroup();group.setCode("SECONDARY");group.setName("THCS");course.getAudienceGroups().add(group);var view=CourseClassificationView.of(course,mock(EntityManager.class));assertEquals("SECONDARY",view.audiences().getFirst().code());assertFalse(view.complete());}
}
