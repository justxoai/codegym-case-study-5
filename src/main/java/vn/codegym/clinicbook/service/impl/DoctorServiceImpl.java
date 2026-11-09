package vn.codegym.clinicbook.service.impl;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.codegym.clinicbook.dto.DoctorRequest;
import vn.codegym.clinicbook.entity.Doctor;
import vn.codegym.clinicbook.entity.enums.Role;
import vn.codegym.clinicbook.entity.User;
import vn.codegym.clinicbook.exception.BusinessException;
import vn.codegym.clinicbook.exception.ResourceNotFoundException;
import vn.codegym.clinicbook.repository.AppointmentRepository;
import vn.codegym.clinicbook.repository.DoctorRepository;
import vn.codegym.clinicbook.repository.TimeSlotRepository;
import vn.codegym.clinicbook.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import vn.codegym.clinicbook.constant.ErrorMessage;

import vn.codegym.clinicbook.service.DoctorService;
import vn.codegym.clinicbook.service.SpecialtyService;

@Service
@Transactional(readOnly = true)
public class DoctorServiceImpl implements DoctorService {
    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final AppointmentRepository appointmentRepository;
    private final SpecialtyService specialtyService;
    private final PasswordEncoder passwordEncoder;

    public DoctorServiceImpl(DoctorRepository doctorRepository,
                         UserRepository userRepository,
                         TimeSlotRepository timeSlotRepository,
                         AppointmentRepository appointmentRepository,
                         SpecialtyService specialtyService,
                         PasswordEncoder passwordEncoder) {
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.appointmentRepository = appointmentRepository;
        this.specialtyService = specialtyService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<Doctor> findAll(Long specialtyId, String keyword) {
        String kw = keyword == null ? "" : keyword.trim();
        if (specialtyId == null) {
            return doctorRepository.findByFullNameContainingIgnoreCaseOrderByFullName(kw);
        }
        return doctorRepository.findBySpecialtyIdAndFullNameContainingIgnoreCaseOrderByFullName(specialtyId, kw);
    }

    @Override
    public Doctor findById(Long id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.DOCTOR_NOT_FOUND));
    }

    @Override
    public List<Doctor> findByIds(List<Long> ids) {
        List<Doctor> result = new ArrayList<>();
        for (Long id : ids) {
            doctorRepository.findById(id).ifPresent(result::add);
        }
        return result;
    }

    @Override
    public Doctor findByUsername(String username) {
        return doctorRepository.findByUserUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.DOCTOR_NOT_FOUND));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Override
    public Doctor create(DoctorRequest request) {
        String username = request.getUsername() == null ? "" : request.getUsername().trim();
        String password = request.getPassword() == null ? "" : request.getPassword();
        if (username.length() < 4 || password.length() < 6) {
            throw new BusinessException(ErrorMessage.DOCTOR_ACCOUNT_REQUIRED);
        }
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException(ErrorMessage.USERNAME_TAKEN);
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(request.getFullName().trim());
        user.setRole(Role.DOCTOR);
        user = userRepository.save(user);

        Doctor doctor = new Doctor();
        doctor.setUser(user);
        apply(doctor, request);
        return doctorRepository.save(doctor);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Override
    public Doctor update(Long id, DoctorRequest request) {
        Doctor doctor = findById(id);
        apply(doctor, request);
        doctor.getUser().setFullName(doctor.getFullName());
        return doctorRepository.save(doctor);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Override
    public void delete(Long id) {
        Doctor doctor = findById(id);
        if (appointmentRepository.existsByDoctorId(id)) {
            throw new BusinessException(ErrorMessage.DOCTOR_HAS_APPOINTMENTS);
        }
        User user = doctor.getUser();
        timeSlotRepository.deleteByDoctorId(id);
        doctorRepository.delete(doctor);
        userRepository.delete(user);
    }

    private void apply(Doctor doctor, DoctorRequest request) {
        doctor.setFullName(request.getFullName().trim());
        doctor.setYearsExperience(request.getYearsExperience());
        doctor.setBio(request.getBio());
        doctor.setSpecialty(specialtyService.findById(request.getSpecialtyId()));
    }
}
