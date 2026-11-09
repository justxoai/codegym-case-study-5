package vn.codegym.clinicbook.service;

import vn.codegym.clinicbook.dto.SpecialtyRequest;
import vn.codegym.clinicbook.entity.Specialty;
import java.util.List;

public interface SpecialtyService {
    List<Specialty> findAll();

    Specialty findById(Long id);

    Specialty create(SpecialtyRequest request);

    Specialty update(Long id, SpecialtyRequest request);

    void delete(Long id);
}
