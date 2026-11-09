package vn.codegym.clinicbook.service;

import vn.codegym.clinicbook.dto.DoctorRequest;
import vn.codegym.clinicbook.entity.Doctor;
import java.util.List;

public interface DoctorService {
    List<Doctor> findAll(Long specialtyId, String keyword);

    Doctor findById(Long id);

    List<Doctor> findByIds(List<Long> ids);

    Doctor findByUsername(String username);

    Doctor create(DoctorRequest request);

    Doctor update(Long id, DoctorRequest request);

    void delete(Long id);
}
