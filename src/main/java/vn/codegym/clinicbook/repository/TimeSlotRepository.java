package vn.codegym.clinicbook.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.codegym.clinicbook.entity.enums.SlotStatus;
import vn.codegym.clinicbook.entity.TimeSlot;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {
    List<TimeSlot> findByDoctorIdAndStatusAndSlotDateGreaterThanEqualOrderBySlotDateAscStartTimeAsc(
            Long doctorId, SlotStatus status, LocalDate from);

    List<TimeSlot> findByDoctorIdAndSlotDateGreaterThanEqualOrderBySlotDateAscStartTimeAsc(
            Long doctorId, LocalDate from);

    List<TimeSlot> findByDoctorIdAndSlotDate(Long doctorId, LocalDate slotDate);

    List<TimeSlot> findByDoctorIdAndSlotDateBetweenOrderBySlotDateAscStartTimeAsc(
            Long doctorId, LocalDate from, LocalDate to);

    List<TimeSlot> findBySlotDateBetween(LocalDate from, LocalDate to);

    void deleteByDoctorId(Long doctorId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from TimeSlot s where s.id = :id")
    Optional<TimeSlot> findByIdForUpdate(@Param("id") Long id);
}
