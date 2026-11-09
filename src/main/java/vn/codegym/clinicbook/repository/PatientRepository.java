package vn.codegym.clinicbook.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.codegym.clinicbook.entity.Patient;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByUserUsername(String username);
}
