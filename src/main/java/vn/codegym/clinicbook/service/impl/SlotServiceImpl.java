package vn.codegym.clinicbook.service.impl;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.codegym.clinicbook.dto.SlotRequest;
import vn.codegym.clinicbook.entity.Doctor;
import vn.codegym.clinicbook.entity.enums.SlotStatus;
import vn.codegym.clinicbook.entity.TimeSlot;
import vn.codegym.clinicbook.exception.BusinessException;
import vn.codegym.clinicbook.exception.ResourceNotFoundException;
import vn.codegym.clinicbook.repository.TimeSlotRepository;
import vn.codegym.clinicbook.security.CurrentUser;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import vn.codegym.clinicbook.constant.ErrorMessage;

import vn.codegym.clinicbook.service.SlotService;
import vn.codegym.clinicbook.service.DoctorService;

@Service
@Transactional(readOnly = true)
public class SlotServiceImpl implements SlotService {
    private final TimeSlotRepository slotRepository;
    private final DoctorService doctorService;
    private final CurrentUser currentUser;

    public SlotServiceImpl(TimeSlotRepository slotRepository, DoctorService doctorService, CurrentUser currentUser) {
        this.slotRepository = slotRepository;
        this.doctorService = doctorService;
        this.currentUser = currentUser;
    }

    @Override
    public TimeSlot findById(Long id) {
        return slotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.SLOT_NOT_FOUND));
    }

    @Override
    public List<TimeSlot> findAvailable(Long doctorId) {
        LocalDateTime now = LocalDateTime.now();
        return slotRepository
                .findByDoctorIdAndStatusAndSlotDateGreaterThanEqualOrderBySlotDateAscStartTimeAsc(
                        doctorId, SlotStatus.AVAILABLE, now.toLocalDate())
                .stream()
                .filter(s -> s.startAt().isAfter(now))
                .toList();
    }

    @Override
    public List<TimeSlot> findUpcoming(Long doctorId) {
        return slotRepository.findByDoctorIdAndSlotDateGreaterThanEqualOrderBySlotDateAscStartTimeAsc(
                doctorId, LocalDate.now());
    }

    @Override
    public List<TimeSlot> findBetween(Long doctorId, LocalDate from, LocalDate to) {
        return slotRepository.findByDoctorIdAndSlotDateBetweenOrderBySlotDateAscStartTimeAsc(doctorId, from, to);
    }

    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR')")
    @Transactional
    @Override
    public TimeSlot create(SlotRequest request) {
        Doctor doctor = resolveDoctor(request.getDoctorId());

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BusinessException(ErrorMessage.SLOT_TIME_ORDER);
        }
        if (request.getStartTime().isBefore(OPENING) || request.getEndTime().isAfter(CLOSING)) {
            throw new BusinessException(ErrorMessage.SLOT_OUTSIDE_HOURS);
        }
        long minutes = Duration.between(request.getStartTime(), request.getEndTime()).toMinutes();
        if (minutes < MIN_MINUTES || minutes > MAX_MINUTES) {
            throw new BusinessException(ErrorMessage.SLOT_DURATION);
        }
        if (!request.getSlotDate().atTime(request.getStartTime()).isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorMessage.SLOT_IN_PAST);
        }
        if (request.getSlotDate().isAfter(LocalDate.now().plusDays(MAX_DAYS_AHEAD))) {
            throw new BusinessException(ErrorMessage.SLOT_TOO_FAR);
        }
        boolean overlaps = slotRepository.findByDoctorIdAndSlotDate(doctor.getId(), request.getSlotDate())
                .stream()
                .anyMatch(s -> request.getStartTime().isBefore(s.getEndTime())
                        && request.getEndTime().isAfter(s.getStartTime()));
        if (overlaps) {
            throw new BusinessException(ErrorMessage.SLOT_OVERLAP);
        }

        TimeSlot slot = new TimeSlot();
        slot.setDoctor(doctor);
        slot.setSlotDate(request.getSlotDate());
        slot.setStartTime(request.getStartTime());
        slot.setEndTime(request.getEndTime());
        slot.setStatus(SlotStatus.AVAILABLE);
        return slotRepository.save(slot);
    }

    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR')")
    @Transactional
    @Override
    public void delete(Long id) {
        TimeSlot slot = findById(id);
        if (currentUser.hasRole("DOCTOR")) {
            Doctor me = doctorService.findByUsername(currentUser.username());
            if (!me.getId().equals(slot.getDoctor().getId())) {
                throw new AccessDeniedException("Not your slot");
            }
        }
        if (slot.getStatus() == SlotStatus.BOOKED) {
            throw new BusinessException(ErrorMessage.SLOT_ALREADY_BOOKED);
        }
        slotRepository.delete(slot);
    }

    private Doctor resolveDoctor(Long requestedDoctorId) {
        if (currentUser.hasRole("DOCTOR")) {
            return doctorService.findByUsername(currentUser.username());
        }
        if (requestedDoctorId == null) {
            throw new BusinessException(ErrorMessage.SLOT_DOCTOR_REQUIRED);
        }
        return doctorService.findById(requestedDoctorId);
    }
}
