package com.example.academic;
import static org.junit.jupiter.api.Assertions.*;
import com.example.academic.enrollment.Enrollment;
import com.example.academic.student.StudentBillingController;
import com.example.academic.common.ApiException;
import java.util.*;
import org.junit.jupiter.api.Test;
class StudentBillingTest {
 Enrollment e(long id,Long root){var e=new Enrollment();e.setId(id);e.setBillingEnrollmentId(root);return e;}
 @Test void chainedTransferResolvesOneOwnedRoot(){var root=e(1L,null);var middle=e(2L,1L);var next=e(3L,2L);assertEquals(1L,StudentBillingController.root(next,Map.of(1L,root,2L,middle,3L,next)));}
 @Test void foreignMissingOrCircularRootFailsClosed(){var foreign=e(1L,2L);assertThrows(ApiException.class,()->StudentBillingController.root(foreign,Map.of(1L,foreign)));var circular=e(2L,1L);assertThrows(ApiException.class,()->StudentBillingController.root(foreign,Map.of(1L,foreign,2L,circular)));}
 @Test void selfReferenceIsCanonical(){var root=e(1L,1L);assertEquals(1L,StudentBillingController.root(root,Map.of(1L,root)));}
}
