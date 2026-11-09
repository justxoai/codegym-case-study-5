package vn.codegym.clinicbook.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.codegym.clinicbook.entity.Appointment;
import vn.codegym.clinicbook.entity.enums.AppointmentStatus;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    @Query("select a from Appointment a where a.doctor.id = :doctorId "
            + "order by a.slot.slotDate asc, a.slot.startTime asc")
    List<Appointment> findForDoctor(@Param("doctorId") Long doctorId);

    List<Appointment> findAllByOrderByCreatedAtDesc();

    boolean existsByDoctorId(Long doctorId);

    long countByStatus(AppointmentStatus status);

    @Query("select count(a) from Appointment a where a.slot.slotDate = :date and a.status = :status")
    long countOnDate(@Param("date") LocalDate date, @Param("status") AppointmentStatus status);

    @Query("select count(a) from Appointment a where a.status = :status "
            + "and a.slot.slotDate between :from and :to")
    long countWithStatusBetween(@Param("status") AppointmentStatus status,
                                @Param("from") LocalDate from,
                                @Param("to") LocalDate to);

    @Query("select count(a) from Appointment a where a.status in :statuses and a.slot.slotDate < :before")
    long countOpenBefore(@Param("statuses") Collection<AppointmentStatus> statuses,
                         @Param("before") LocalDate before);

    @Query("select a from Appointment a where a.status in :statuses and a.slot.slotDate >= :from "
            + "order by a.slot.slotDate asc, a.slot.startTime asc")
    List<Appointment> findUpcoming(@Param("statuses") Collection<AppointmentStatus> statuses,
                                   @Param("from") LocalDate from,
                                   Pageable pageable);

    @Query("select a from Appointment a where a.slot.slotDate = :date")
    List<Appointment> findOnDate(@Param("date") LocalDate date);
}
