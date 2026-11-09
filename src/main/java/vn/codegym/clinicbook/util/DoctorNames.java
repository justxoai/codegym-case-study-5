package vn.codegym.clinicbook.util;

import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;
import java.util.regex.Pattern;

public final class DoctorNames {
    private static final Pattern TITLE = Pattern.compile("^(?:bs|dr)(?:\\.\\s*|\\s+)", Pattern.CASE_INSENSITIVE);

    private DoctorNames() {
    }

    public static String strip(String fullName) {
        if (fullName == null) {
            return "";
        }
        return TITLE.matcher(fullName.trim()).replaceFirst("").trim();
    }

    public static String display(String fullName, Locale locale) {
        String name = strip(fullName);
        boolean english = locale != null && "en".equalsIgnoreCase(locale.getLanguage());
        return (english ? "Dr. " : "BS. ") + name;
    }

    public static String display(String fullName) {
        return display(fullName, LocaleContextHolder.getLocale());
    }
}
