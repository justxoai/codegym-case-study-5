package vn.codegym.clinicbook.service.impl;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.codegym.clinicbook.dto.SpecialtyRequest;
import vn.codegym.clinicbook.entity.Specialty;
import vn.codegym.clinicbook.exception.BusinessException;
import vn.codegym.clinicbook.exception.ResourceNotFoundException;
import vn.codegym.clinicbook.repository.DoctorRepository;
import vn.codegym.clinicbook.repository.SpecialtyRepository;
import java.util.List;
import vn.codegym.clinicbook.constant.ErrorMessage;

import vn.codegym.clinicbook.service.SpecialtyService;

@Service
@Transactional(readOnly = true)
public class SpecialtyServiceImpl implements SpecialtyService {
    private final SpecialtyRepository specialtyRepository;
    private final DoctorRepository doctorRepository;

    public SpecialtyServiceImpl(SpecialtyRepository specialtyRepository, DoctorRepository doctorRepository) {
        this.specialtyRepository = specialtyRepository;
        this.doctorRepository = doctorRepository;
    }

    @Override
    public List<Specialty> findAll() {
        return specialtyRepository.findAllByOrderByNameAsc();
    }

    @Override
    public Specialty findById(Long id) {
        return specialtyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.SPECIALTY_NOT_FOUND));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Override
    public Specialty create(SpecialtyRequest request) {
        String name = request.getName().trim();
        if (specialtyRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessException(ErrorMessage.SPECIALTY_DUPLICATE);
        }
        Specialty specialty = new Specialty();
        specialty.setName(name);
        specialty.setNameEn(blankToNull(request.getNameEn()));
        specialty.setDescription(request.getDescription());
        return specialtyRepository.save(specialty);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Override
    public Specialty update(Long id, SpecialtyRequest request) {
        Specialty specialty = findById(id);
        String name = request.getName().trim();
        if (specialtyRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new BusinessException(ErrorMessage.SPECIALTY_DUPLICATE);
        }
        specialty.setName(name);
        specialty.setNameEn(blankToNull(request.getNameEn()));
        specialty.setDescription(request.getDescription());
        return specialtyRepository.save(specialty);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Override
    public void delete(Long id) {
        Specialty specialty = findById(id);
        if (doctorRepository.existsBySpecialtyId(id)) {
            throw new BusinessException(ErrorMessage.SPECIALTY_IN_USE);
        }
        specialtyRepository.delete(specialty);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
