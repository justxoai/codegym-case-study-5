package vn.codegym.clinicbook.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.List;

public final class RecentDoctorsCookie {
    public static final String NAME = "recentDoctors";
    public static final String CONSENT = "clinicbook-consent";
    public static final String CONSENT_ALL = "all";
    private static final int MAX = 4;
    private static final int MAX_AGE_SECONDS = 60 * 60 * 24 * 14;

    private RecentDoctorsCookie() {
    }

    public static List<Long> parse(String value) {
        List<Long> ids = new ArrayList<>();
        if (value == null || value.isBlank()) {
            return ids;
        }
        for (String part : value.split("-")) {
            try {
                Long id = Long.valueOf(part.trim());
                if (!ids.contains(id) && ids.size() < MAX) {
                    ids.add(id);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return ids;
    }

    public static boolean allowed(String consent) {
        return CONSENT_ALL.equals(consent);
    }

    public static void forget(HttpServletResponse response) {
        Cookie cookie = new Cookie(NAME, "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
    }

    public static void remember(String currentValue, Long doctorId, HttpServletResponse response) {
        List<Long> ids = parse(currentValue);
        ids.remove(doctorId);
        ids.add(0, doctorId);
        if (ids.size() > MAX) {
            ids = new ArrayList<>(ids.subList(0, MAX));
        }
        StringBuilder sb = new StringBuilder();
        for (Long id : ids) {
            if (sb.length() > 0) {
                sb.append('-');
            }
            sb.append(id);
        }
        Cookie cookie = new Cookie(NAME, sb.toString());
        cookie.setPath("/");
        cookie.setMaxAge(MAX_AGE_SECONDS);
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
    }
}
