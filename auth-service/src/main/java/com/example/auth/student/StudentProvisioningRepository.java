package com.example.auth.student;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface StudentProvisioningRepository extends JpaRepository<StudentProvisioning,String> {
 Optional<StudentProvisioning> findByStudentId(Long id);
 Optional<StudentProvisioning> findByAccountId(Long id);
}
