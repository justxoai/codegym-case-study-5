package vn.codegym.clinicbook.aop;

import vn.codegym.clinicbook.entity.Appointment;

import java.util.HashMap;
import java.util.Map;

public final class AuditDetail {
    public static final String VERSION = "v1";

    private AuditDetail() {
    }

    public static String of(Appointment a) {
        return String.join(";",
                VERSION,
                "id=" + a.getId(),
                "patient=" + clean(a.getPatient().getUser().getFullName()),
                "doctor=" + clean(a.getDoctor().getFullName()),
                "at=" + a.getSlot().startAt(),
                "status=" + a.getStatus());
    }

    public static String keyOf(Long appointmentId) {
        return VERSION + ";id=" + appointmentId + ";";
    }

    public static Map<String, String> parse(String detail) {
        if (detail == null || !detail.startsWith(VERSION + ";")) {
            return null;
        }
        Map<String, String> result = new HashMap<>();
        for (String part : detail.substring(VERSION.length() + 1).split(";")) {
            int eq = part.indexOf('=');
            if (eq > 0) {
                result.put(part.substring(0, eq), part.substring(eq + 1));
            }
        }
        return result;
    }

    private static String clean(String value) {
        return value == null ? "" : value.replace(';', ',').replace('=', '-');
    }
}
