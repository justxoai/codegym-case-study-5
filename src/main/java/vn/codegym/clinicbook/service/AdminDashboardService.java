package vn.codegym.clinicbook.service;

import lombok.Getter;
import lombok.Setter;
import vn.codegym.clinicbook.entity.Appointment;
import vn.codegym.clinicbook.entity.Doctor;
import vn.codegym.clinicbook.entity.Specialty;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AdminDashboardService {
    static final int WINDOW_DAYS = 7;

    static final int PEAK_PERCENT = 85;

    Overview overview();

    static int percent(long part, long whole) {
        return whole == 0 ? 0 : (int) Math.round(part * 100.0 / whole);
    }

    @Getter
    @Setter
    public static class Overview {
        private LocalDate today;
        private LocalDate windowEnd;

        private long todayTotal;
        private long todayPending;
        private long todayConfirmed;
        private long todayCompleted;

        private long doctorCount;
        private double avgExperience;
        private Map<Long, DoctorLoad> doctorLoads = Map.of();
        private long doctorsWithFreeSlots;
        private long specialtyCount;
        private long patientCount;
        private long appointmentCount;

        private long slotTotal;
        private long slotBooked;
        private int occupancyPercent;

        private List<SpecialtyLoad> specialties = List.of();
        private List<DoctorLoad> topDoctors = List.of();
        private List<Appointment> upcoming = List.of();

        private long pendingTotal;
        private long pendingSoon;
        private long overdue;
        private long idleDoctorCount;
        private String idleSummary = "";
        private int alertCount;
    }

    @Getter
    public static class SpecialtyLoad {
        private final Specialty specialty;
        private final long doctors;
        private final long total;
        private final long booked;
        private final int percent;

        public SpecialtyLoad(Specialty specialty, long doctors, long total, long booked, int percent) {
            this.specialty = specialty;
            this.doctors = doctors;
            this.total = total;
            this.booked = booked;
            this.percent = percent;
        }

        public String getLevel() {
            if (total == 0) {
                return "none";
            }
            return percent >= PEAK_PERCENT ? "peak" : "normal";
        }
    }

    @Getter
    public static class DoctorLoad {
        private final Doctor doctor;
        private final long booked;
        private final long total;

        public DoctorLoad(Doctor doctor, long booked, long total) {
            this.doctor = doctor;
            this.booked = booked;
            this.total = total;
        }

        public int getPercent() {
            return AdminDashboardService.percent(booked, total);
        }
    }
}
