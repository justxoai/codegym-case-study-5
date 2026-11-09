package vn.codegym.clinicbook.service.impl;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.codegym.clinicbook.entity.enums.AppointmentStatus;
import vn.codegym.clinicbook.entity.Doctor;
import vn.codegym.clinicbook.entity.enums.SlotStatus;
import vn.codegym.clinicbook.entity.Specialty;
import vn.codegym.clinicbook.entity.TimeSlot;
import vn.codegym.clinicbook.repository.AppointmentRepository;
import vn.codegym.clinicbook.repository.DoctorRepository;
import vn.codegym.clinicbook.repository.PatientRepository;
import vn.codegym.clinicbook.repository.SpecialtyRepository;
import vn.codegym.clinicbook.repository.TimeSlotRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import vn.codegym.clinicbook.service.AdminDashboardService;

@Service
@Transactional(readOnly = true)
public class AdminDashboardServiceImpl implements AdminDashboardService {
    private static final List<AppointmentStatus> OPEN =
            List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED);

    private final AppointmentRepository appointmentRepository;
    private final TimeSlotRepository slotRepository;
    private final DoctorRepository doctorRepository;
    private final SpecialtyRepository specialtyRepository;
    private final PatientRepository patientRepository;

    public AdminDashboardServiceImpl(AppointmentRepository appointmentRepository,
                                 TimeSlotRepository slotRepository,
                                 DoctorRepository doctorRepository,
                                 SpecialtyRepository specialtyRepository,
                                 PatientRepository patientRepository) {
        this.appointmentRepository = appointmentRepository;
        this.slotRepository = slotRepository;
        this.doctorRepository = doctorRepository;
        this.specialtyRepository = specialtyRepository;
        this.patientRepository = patientRepository;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public Overview overview() {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        LocalDate last = today.plusDays(WINDOW_DAYS - 1L);

        Overview o = new Overview();
        o.setToday(today);
        o.setWindowEnd(last);

        o.setTodayPending(appointmentRepository.countOnDate(today, AppointmentStatus.PENDING));
        o.setTodayConfirmed(appointmentRepository.countOnDate(today, AppointmentStatus.CONFIRMED));
        o.setTodayCompleted(appointmentRepository.countOnDate(today, AppointmentStatus.COMPLETED));
        o.setTodayTotal(o.getTodayPending() + o.getTodayConfirmed() + o.getTodayCompleted());

        List<Doctor> doctors = doctorRepository.findAll();
        List<Specialty> specialties = specialtyRepository.findAllByOrderByNameAsc();
        o.setDoctorCount(doctors.size());
        o.setSpecialtyCount(specialties.size());
        o.setPatientCount(patientRepository.count());
        o.setAppointmentCount(appointmentRepository.count());

        List<TimeSlot> window = slotRepository.findBySlotDateBetween(today, last).stream()
                .filter(s -> s.getStatus() == SlotStatus.BOOKED || s.startAt().isAfter(now))
                .toList();

        Map<Long, long[]> perSpecialty = new HashMap<>();
        Map<Long, long[]> perDoctor = new HashMap<>();
        long booked = 0;
        for (TimeSlot s : window) {
            boolean isBooked = s.getStatus() == SlotStatus.BOOKED;
            tally(perSpecialty, s.getDoctor().getSpecialty().getId(), isBooked);
            tally(perDoctor, s.getDoctor().getId(), isBooked);
            if (isBooked) {
                booked++;
            }
        }
        o.setSlotTotal(window.size());
        o.setSlotBooked(booked);
        o.setOccupancyPercent(AdminDashboardService.percent(booked, window.size()));

        Map<Long, Long> doctorsPerSpecialty = doctors.stream()
                .collect(Collectors.groupingBy(d -> d.getSpecialty().getId(), Collectors.counting()));
        List<SpecialtyLoad> loads = specialties.stream()
                .map(sp -> {
                    long[] v = perSpecialty.getOrDefault(sp.getId(), new long[2]);
                    return new SpecialtyLoad(sp, doctorsPerSpecialty.getOrDefault(sp.getId(), 0L),
                            v[0], v[1], AdminDashboardService.percent(v[1], v[0]));
                })
                .sorted(Comparator.comparingLong(SpecialtyLoad::getBooked).reversed()
                        .thenComparing(l -> l.getSpecialty().getName()))
                .toList();
        o.setSpecialties(loads);

        Map<Long, DoctorLoad> loadByDoctor = new HashMap<>();
        for (Doctor d : doctors) {
            long[] v = perDoctor.getOrDefault(d.getId(), new long[2]);
            loadByDoctor.put(d.getId(), new DoctorLoad(d, v[1], v[0]));
        }
        o.setDoctorLoads(loadByDoctor);
        o.setAvgExperience(doctors.stream().mapToInt(Doctor::getYearsExperience).average().orElse(0));
        o.setTopDoctors(loadByDoctor.values().stream()
                .filter(l -> l.getBooked() > 0)
                .sorted(Comparator.comparingLong(DoctorLoad::getBooked).reversed()
                        .thenComparing(l -> l.getDoctor().getFullName()))
                .limit(4)
                .toList());

        List<String> idle = loadByDoctor.values().stream()
                .filter(l -> l.getTotal() - l.getBooked() == 0)
                .map(l -> l.getDoctor().getFullName())
                .sorted()
                .toList();
        o.setIdleDoctorCount(idle.size());
        o.setDoctorsWithFreeSlots(doctors.size() - idle.size());
        String names = String.join(", ", idle.stream().limit(3).toList());
        o.setIdleSummary(idle.size() > 3 ? names + " +" + (idle.size() - 3) : names);

        o.setPendingTotal(appointmentRepository.countByStatus(AppointmentStatus.PENDING));
        o.setPendingSoon(appointmentRepository.countWithStatusBetween(
                AppointmentStatus.PENDING, today, today.plusDays(1)));
        o.setOverdue(appointmentRepository.countOpenBefore(OPEN, today));
        o.setAlertCount((o.getPendingTotal() > 0 ? 1 : 0)
                + (o.getOverdue() > 0 ? 1 : 0)
                + (o.getIdleDoctorCount() > 0 ? 1 : 0));

        o.setUpcoming(appointmentRepository.findUpcoming(OPEN, today, PageRequest.of(0, 6)));
        return o;
    }

    private static void tally(Map<Long, long[]> map, Long key, boolean booked) {
        long[] v = map.computeIfAbsent(key, k -> new long[2]);
        v[0]++;
        if (booked) {
            v[1]++;
        }
    }
}
