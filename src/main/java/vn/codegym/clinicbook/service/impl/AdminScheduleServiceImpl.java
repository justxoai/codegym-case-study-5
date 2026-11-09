package vn.codegym.clinicbook.service.impl;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.codegym.clinicbook.dto.PageView;
import vn.codegym.clinicbook.entity.Appointment;
import vn.codegym.clinicbook.entity.enums.AppointmentStatus;
import vn.codegym.clinicbook.entity.Doctor;
import vn.codegym.clinicbook.entity.enums.SlotStatus;
import vn.codegym.clinicbook.entity.TimeSlot;
import vn.codegym.clinicbook.repository.AppointmentRepository;
import vn.codegym.clinicbook.repository.TimeSlotRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import vn.codegym.clinicbook.service.AdminScheduleService;

@Service
@Transactional(readOnly = true)
public class AdminScheduleServiceImpl implements AdminScheduleService {
    private static final List<String> STATUS_KEYS = List.of("ALL", "PENDING", "CONFIRMED", "COMPLETED", "CANCELLED");

    private final AppointmentRepository appointmentRepository;
    private final TimeSlotRepository slotRepository;

    public AdminScheduleServiceImpl(AppointmentRepository appointmentRepository, TimeSlotRepository slotRepository) {
        this.appointmentRepository = appointmentRepository;
        this.slotRepository = slotRepository;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public long appointmentCount() {
        return appointmentRepository.count();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public DaySchedule day(LocalDate day, Long specialtyId) {
        List<TimeSlot> slots = slotRepository.findBySlotDateBetween(day, day).stream()
                .filter(s -> specialtyId == null || s.getDoctor().getSpecialty().getId().equals(specialtyId))
                .toList();
        List<Appointment> appointments = appointmentRepository.findOnDate(day).stream()
                .filter(a -> specialtyId == null || a.getDoctor().getSpecialty().getId().equals(specialtyId))
                .toList();

        Map<Long, Appointment> bySlot = new HashMap<>();
        for (Appointment a : appointments) {
            if (a.getStatus() != AppointmentStatus.CANCELLED) {
                bySlot.put(a.getSlot().getId(), a);
            }
        }

        List<Doctor> doctors = slots.stream()
                .map(TimeSlot::getDoctor)
                .collect(Collectors.toMap(Doctor::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new))
                .values().stream()
                .sorted(Comparator.comparing(Doctor::getFullName))
                .toList();
        List<LocalTime> times = slots.stream().map(TimeSlot::getStartTime).distinct().sorted().toList();
        Map<String, TimeSlot> cellSlot = new HashMap<>();
        for (TimeSlot s : slots) {
            cellSlot.put(s.getDoctor().getId() + "|" + s.getStartTime(), s);
        }

        List<Row> rows = times.stream().map(t -> {
            List<Cell> cells = doctors.stream().map(d -> {
                TimeSlot slot = cellSlot.get(d.getId() + "|" + t);
                return new Cell(slot, slot == null ? null : bySlot.get(slot.getId()));
            }).toList();
            return new Row(t, cells);
        }).toList();

        DaySchedule result = new DaySchedule();
        result.setDay(day);
        result.setDoctors(doctors);
        result.setRows(rows);
        result.setPending(count(appointments, AppointmentStatus.PENDING));
        result.setConfirmed(count(appointments, AppointmentStatus.CONFIRMED));
        result.setCompleted(count(appointments, AppointmentStatus.COMPLETED));
        result.setCancelled(count(appointments, AppointmentStatus.CANCELLED));
        result.setTotal(result.getPending() + result.getConfirmed() + result.getCompleted());
        result.setSlotTotal(slots.size());
        result.setSlotBooked(slots.stream().filter(s -> s.getStatus() == SlotStatus.BOOKED).count());
        result.setOccupancyPercent(result.getSlotTotal() == 0
                ? 0 : (int) Math.round(result.getSlotBooked() * 100.0 / result.getSlotTotal()));
        return result;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public History history(AppointmentStatus status, Long specialtyId, String keyword, int page) {
        String kw = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        List<Appointment> base = appointmentRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(a -> specialtyId == null || a.getDoctor().getSpecialty().getId().equals(specialtyId))
                .filter(a -> kw.isEmpty() || matches(a, kw))
                .toList();

        Map<String, Long> counts = new HashMap<>();
        for (String key : STATUS_KEYS) {
            counts.put(key, "ALL".equals(key)
                    ? (long) base.size()
                    : count(base, AppointmentStatus.valueOf(key)));
        }

        List<Appointment> filtered = base.stream()
                .filter(a -> status == null || a.getStatus() == status)
                .toList();

        History result = new History();
        result.setCounts(counts);
        result.setMatching(filtered.size());
        PageView<Appointment> view = PageView.of(filtered, page, PAGE_SIZE);
        result.setPage(view);
        result.setRows(view.getContent());
        return result;
    }

    private static boolean matches(Appointment a, String kw) {
        return String.valueOf(a.getId()).equals(kw.replace("#", ""))
                || a.getPatient().getUser().getFullName().toLowerCase(Locale.ROOT).contains(kw)
                || a.getDoctor().getFullName().toLowerCase(Locale.ROOT).contains(kw)
                || (a.getReason() != null && a.getReason().toLowerCase(Locale.ROOT).contains(kw));
    }

    private static long count(List<Appointment> list, AppointmentStatus status) {
        return list.stream().filter(a -> a.getStatus() == status).count();
    }
}
