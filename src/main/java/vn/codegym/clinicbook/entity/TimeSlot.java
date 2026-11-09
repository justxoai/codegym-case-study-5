package vn.codegym.clinicbook.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import vn.codegym.clinicbook.entity.enums.SlotStatus;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "time_slots",
        uniqueConstraints = @UniqueConstraint(columnNames = {"doctor_id", "slot_date", "start_time"}))
public class TimeSlot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(name = "slot_date", nullable = false)
    private LocalDate slotDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SlotStatus status = SlotStatus.AVAILABLE;

    @Version
    private Long version;

    public LocalDateTime startAt() {
        return slotDate.atTime(startTime);
    }
}
