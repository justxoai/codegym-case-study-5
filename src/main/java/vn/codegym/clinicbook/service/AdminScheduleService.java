package vn.codegym.clinicbook.service;

import lombok.Getter;
import lombok.Setter;
import vn.codegym.clinicbook.dto.PageView;
import vn.codegym.clinicbook.entity.Appointment;
import vn.codegym.clinicbook.entity.enums.AppointmentStatus;
import vn.codegym.clinicbook.entity.Doctor;
import vn.codegym.clinicbook.entity.TimeSlot;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public interface AdminScheduleService {
    static final int PAGE_SIZE = PageView.DEFAULT_SIZE;

    long appointmentCount();

    DaySchedule day(LocalDate day, Long specialtyId);

    History history(AppointmentStatus status, Long specialtyId, String keyword, int page);

    @Getter
    @Setter
    public static class DaySchedule {
        private LocalDate day;
        private List<Doctor> doctors = List.of();
        private List<Row> rows = List.of();
        private long total;
        private long pending;
        private long confirmed;
        private long completed;
        private long cancelled;
        private long slotTotal;
        private long slotBooked;
        private int occupancyPercent;
    }

    @Getter
    public static class Row {
        private final LocalTime time;
        private final List<Cell> cells;

        public Row(LocalTime time, List<Cell> cells) {
            this.time = time;
            this.cells = cells;
        }
    }

    @Getter
    public static class Cell {
        private final TimeSlot slot;
        private final Appointment appointment;

        public Cell(TimeSlot slot, Appointment appointment) {
            this.slot = slot;
            this.appointment = appointment;
        }
    }

    @Getter
    @Setter
    public static class History {
        private List<Appointment> rows = List.of();
        private long matching;
        private PageView<Appointment> page;
        private Map<String, Long> counts = Map.of();
    }
}
