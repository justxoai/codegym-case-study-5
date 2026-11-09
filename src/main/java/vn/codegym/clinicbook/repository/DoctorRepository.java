package vn.codegym.clinicbook.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.codegym.clinicbook.entity.Doctor;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    List<Doctor> findByFullNameContainingIgnoreCaseOrderByFullName(String keyword);

    List<Doctor> findBySpecialtyIdAndFullNameContainingIgnoreCaseOrderByFullName(Long specialtyId, String keyword);

    Optional<Doctor> findByUserUsername(String username);

    boolean existsBySpecialtyId(Long specialtyId);
}
