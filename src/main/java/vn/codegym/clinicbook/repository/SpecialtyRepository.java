package vn.codegym.clinicbook.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.codegym.clinicbook.entity.Specialty;

import java.util.List;

public interface SpecialtyRepository extends JpaRepository<Specialty, Long> {
    List<Specialty> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
