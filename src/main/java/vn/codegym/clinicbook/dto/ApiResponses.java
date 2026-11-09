package vn.codegym.clinicbook.dto;

import vn.codegym.clinicbook.entity.Appointment;
import vn.codegym.clinicbook.entity.enums.AppointmentStatus;
import vn.codegym.clinicbook.entity.Doctor;
import vn.codegym.clinicbook.entity.enums.SlotStatus;
import vn.codegym.clinicbook.entity.Specialty;
import vn.codegym.clinicbook.entity.TimeSlot;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public final class ApiResponses {
    private ApiResponses() {
    }

    public record AuthResponse(String token, String username, String role) {
    }

    public record SpecialtyResponse(Long id, String name, String description) {
        public static SpecialtyResponse from(Specialty s) {
            return new SpecialtyResponse(s.getId(), s.getName(), s.getDescription());
        }
    }

    public record DoctorResponse(Long id, String fullName, Long specialtyId, String specialtyName,
                                 int yearsExperience, String bio) {
        public static DoctorResponse from(Doctor d) {
            return new DoctorResponse(d.getId(), d.getFullName(), d.getSpecialty().getId(),
                    d.getSpecialty().getName(), d.getYearsExperience(), d.getBio());
        }
    }

    public record SlotResponse(Long id, Long doctorId, LocalDate slotDate, LocalTime startTime,
                               LocalTime endTime, SlotStatus status) {
        public static SlotResponse from(TimeSlot s) {
            return new SlotResponse(s.getId(), s.getDoctor().getId(), s.getSlotDate(),
                    s.getStartTime(), s.getEndTime(), s.getStatus());
        }
    }

    public record AppointmentResponse(Long id, Long patientId, String patientName, Long doctorId,
                                      String doctorName, Long slotId, LocalDate slotDate,
                                      LocalTime startTime, LocalTime endTime,
                                      AppointmentStatus status, String reason, LocalDateTime createdAt) {
        public static AppointmentResponse from(Appointment a) {
            TimeSlot s = a.getSlot();
            return new AppointmentResponse(a.getId(), a.getPatient().getId(),
                    a.getPatient().getUser().getFullName(), a.getDoctor().getId(),
                    a.getDoctor().getFullName(), s.getId(), s.getSlotDate(), s.getStartTime(),
                    s.getEndTime(), a.getStatus(), a.getReason(), a.getCreatedAt());
        }
    }
}
