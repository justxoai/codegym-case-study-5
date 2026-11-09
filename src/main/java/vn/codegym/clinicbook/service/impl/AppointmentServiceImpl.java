package vn.codegym.clinicbook.service.impl;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.codegym.clinicbook.aop.Audited;
import vn.codegym.clinicbook.entity.Appointment;
import vn.codegym.clinicbook.entity.enums.AppointmentStatus;
import vn.codegym.clinicbook.entity.Patient;
import vn.codegym.clinicbook.entity.enums.SlotStatus;
import vn.codegym.clinicbook.entity.TimeSlot;
import vn.codegym.clinicbook.exception.BusinessException;
import vn.codegym.clinicbook.exception.ResourceNotFoundException;
import vn.codegym.clinicbook.repository.AppointmentRepository;
import vn.codegym.clinicbook.repository.DoctorRepository;
import vn.codegym.clinicbook.repository.PatientRepository;
import vn.codegym.clinicbook.repository.TimeSlotRepository;
import vn.codegym.clinicbook.security.CurrentUser;
import java.time.LocalDateTime;
import java.util.List;
import vn.codegym.clinicbook.constant.ErrorMessage;

import vn.codegym.clinicbook.service.AppointmentService;

@Service
@Transactional(readOnly = true)
public class AppointmentServiceImpl implements AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final TimeSlotRepository slotRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final CurrentUser currentUser;

    public AppointmentServiceImpl(AppointmentRepository appointmentRepository,
                              TimeSlotRepository slotRepository,
                              PatientRepository patientRepository,
                              DoctorRepository doctorRepository,
                              CurrentUser currentUser) {
        this.appointmentRepository = appointmentRepository;
        this.slotRepository = slotRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.currentUser = currentUser;
    }

    @Audited("APPOINTMENT_BOOKED")
    @PreAuthorize("hasRole('PATIENT')")
    @Transactional
    @Override
    public Appointment book(Long slotId, String reason) {
        Patient patient = patientRepository.findByUserUsername(currentUser.username())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.PATIENT_NOT_FOUND));
        TimeSlot slot = slotRepository.findByIdForUpdate(slotId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.SLOT_NOT_FOUND));

        if (slot.getStatus() != SlotStatus.AVAILABLE) {
            throw new BusinessException(ErrorMessage.SLOT_UNAVAILABLE);
        }
        if (!slot.startAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorMessage.SLOT_IN_PAST);
        }

        slot.setStatus(SlotStatus.BOOKED);

        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(slot.getDoctor());
        appointment.setSlot(slot);
        appointment.setReason(reason);
        appointment.setStatus(AppointmentStatus.PENDING);
        return appointmentRepository.save(appointment);
    }

    @Audited("APPOINTMENT_CANCELLED")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR')")
    @Transactional
    @Override
    public Appointment cancel(Long id) {
        Appointment appointment = loadWithAccessCheck(id);
        return applyStatus(appointment, AppointmentStatus.CANCELLED);
    }

    @Audited("APPOINTMENT_STATUS_CHANGED")
    @PreAuthorize("hasRole('DOCTOR')")
    @Transactional
    @Override
    public Appointment updateStatus(Long id, AppointmentStatus newStatus) {
        Appointment appointment = loadWithAccessCheck(id);
        return applyStatus(appointment, newStatus);
    }

    @Override
    public Appointment findById(Long id) {
        return loadWithAccessCheck(id);
    }

    @Override
    public List<Appointment> findForCurrentUser() {
        String username = currentUser.username();
        if (currentUser.hasRole("ADMIN")) {
            return appointmentRepository.findAllByOrderByCreatedAtDesc();
        }
        if (currentUser.hasRole("DOCTOR")) {
            return appointmentRepository.findForDoctor(doctorIdOf(username));
        }
        Patient patient = patientRepository.findByUserUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.PATIENT_NOT_FOUND));
        return appointmentRepository.findByPatientIdOrderByCreatedAtDesc(patient.getId());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public List<Appointment> findAll() {
        return appointmentRepository.findAllByOrderByCreatedAtDesc();
    }

    private Long doctorIdOf(String username) {
        return doctorRepository.findByUserUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.DOCTOR_NOT_FOUND))
                .getId();
    }

    private Appointment loadWithAccessCheck(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.APPOINTMENT_NOT_FOUND));
        String username = currentUser.username();
        boolean allowed = currentUser.hasRole("ADMIN")
                || (currentUser.hasRole("PATIENT")
                && appointment.getPatient().getUser().getUsername().equals(username))
                || (currentUser.hasRole("DOCTOR")
                && appointment.getDoctor().getUser().getUsername().equals(username));
        if (!allowed) {
            throw new AccessDeniedException("Not your appointment");
        }
        return appointment;
    }

    private Appointment applyStatus(Appointment appointment, AppointmentStatus target) {
        AppointmentStatus current = appointment.getStatus();
        boolean valid = switch (current) {
            case PENDING -> target == AppointmentStatus.CONFIRMED || target == AppointmentStatus.CANCELLED;
            case CONFIRMED -> target == AppointmentStatus.COMPLETED || target == AppointmentStatus.CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
        if (!valid) {
            throw new BusinessException(ErrorMessage.APPOINTMENT_INVALID_TRANSITION);
        }
        if (target == AppointmentStatus.CANCELLED) {
            TimeSlot slot = slotRepository.findByIdForUpdate(appointment.getSlot().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.SLOT_NOT_FOUND));
            slot.setStatus(SlotStatus.AVAILABLE);
        }
        appointment.setStatus(target);
        return appointmentRepository.save(appointment);
    }
}
