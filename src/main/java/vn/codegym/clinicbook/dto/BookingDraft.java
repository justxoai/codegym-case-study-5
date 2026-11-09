package vn.codegym.clinicbook.dto;

import java.io.Serializable;

public class BookingDraft implements Serializable {
    public static final String SESSION_KEY = "bookingDraft";

    private final Long doctorId;
    private final Long slotId;
    private final String reason;

    public BookingDraft(Long doctorId, Long slotId, String reason) {
        this.doctorId = doctorId;
        this.slotId = slotId;
        this.reason = reason;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public Long getSlotId() {
        return slotId;
    }

    public String getReason() {
        return reason;
    }
}
