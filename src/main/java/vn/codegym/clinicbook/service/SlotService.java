package vn.codegym.clinicbook.service;

import vn.codegym.clinicbook.dto.SlotRequest;
import vn.codegym.clinicbook.entity.TimeSlot;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface SlotService {
    static final LocalTime OPENING = LocalTime.of(6, 0);

    static final LocalTime CLOSING = LocalTime.of(22, 0);

    static final int MIN_MINUTES = 15;

    static final int MAX_MINUTES = 240;

    static final int MAX_DAYS_AHEAD = 90;

    TimeSlot findById(Long id);

    List<TimeSlot> findAvailable(Long doctorId);

    List<TimeSlot> findUpcoming(Long doctorId);

    List<TimeSlot> findBetween(Long doctorId, LocalDate from, LocalDate to);

    TimeSlot create(SlotRequest request);

    void delete(Long id);
}
