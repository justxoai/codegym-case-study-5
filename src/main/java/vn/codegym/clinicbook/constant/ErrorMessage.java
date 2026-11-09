package vn.codegym.clinicbook.constant;

public final class ErrorMessage {
    private ErrorMessage() {
    }

    public static final String SLOT_NOT_FOUND = "error.slot.notFound";
    public static final String DOCTOR_NOT_FOUND = "error.doctor.notFound";
    public static final String USERNAME_TAKEN = "error.username.taken";
    public static final String USER_NOT_FOUND = "error.user.notFound";
    public static final String SPECIALTY_DUPLICATE = "error.specialty.duplicate";
    public static final String SLOT_UNAVAILABLE = "error.slot.unavailable";
    public static final String SLOT_IN_PAST = "error.slot.past";
    public static final String PATIENT_NOT_FOUND = "error.patient.notFound";
    public static final String PASSWORD_SAME_AS_OLD = "error.password.same";
    public static final String SPECIALTY_NOT_FOUND = "error.specialty.notFound";
    public static final String SPECIALTY_IN_USE = "error.specialty.inUse";
    public static final String SLOT_TIME_ORDER = "error.slot.timeOrder";
    public static final String SLOT_OVERLAP = "error.slot.overlap";
    public static final String SLOT_OUTSIDE_HOURS = "error.slot.hours";
    public static final String SLOT_TOO_FAR = "error.slot.farFuture";
    public static final String SLOT_DURATION = "error.slot.duration";
    public static final String SLOT_DOCTOR_REQUIRED = "error.slot.doctorRequired";
    public static final String SLOT_ALREADY_BOOKED = "error.slot.booked";
    public static final String WRONG_PASSWORD = "error.password.wrong";
    public static final String PASSWORD_MISMATCH = "error.password.mismatch";
    public static final String LOGIN_INVALID = "error.login.invalid";
    public static final String DOCTOR_HAS_APPOINTMENTS = "error.doctor.hasAppointments";
    public static final String DOCTOR_ACCOUNT_REQUIRED = "error.doctor.account.required";
    public static final String ARTICLE_NOT_FOUND = "error.article.notFound";
    public static final String APPOINTMENT_NOT_FOUND = "error.appointment.notFound";
    public static final String APPOINTMENT_INVALID_TRANSITION = "error.appointment.invalidTransition";
}
