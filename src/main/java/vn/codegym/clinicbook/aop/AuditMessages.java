package vn.codegym.clinicbook.aop;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import vn.codegym.clinicbook.entity.AuditLog;
import vn.codegym.clinicbook.util.DoctorNames;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;

@Component("auditMessages")
public class AuditMessages {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final MessageSource messages;

    public AuditMessages(MessageSource messages) {
        this.messages = messages;
    }

    public String describe(AuditLog log) {
        Locale locale = LocaleContextHolder.getLocale();
        Map<String, String> facts = AuditDetail.parse(log.getDetail());
        String key = "audit.msg." + log.getAction();
        if (facts == null) {
            boolean appointmentRow = log.getAction() != null && log.getAction().startsWith("APPOINTMENT_");
            if (!appointmentRow) {
                String fixed = messages.getMessage(key, new Object[]{log.getUsername()}, null, locale);
                if (fixed != null) {
                    return fixed;
                }
            }
            return log.getDetail() == null ? log.getAction() : log.getDetail();
        }

        String time = "";
        String day = "";
        try {
            LocalDateTime at = LocalDateTime.parse(facts.getOrDefault("at", ""));
            time = at.format(TIME);
            day = at.format(DAY);
        } catch (DateTimeParseException ignored) {
        }
        String status = facts.getOrDefault("status", "");
        String statusLabel = messages.getMessage("status." + status, null, status, locale);

        Object[] args = {
                log.getUsername(),
                "#" + facts.getOrDefault("id", "?"),
                facts.getOrDefault("patient", ""),
                DoctorNames.display(facts.getOrDefault("doctor", ""), locale),
                time,
                day,
                statusLabel
        };
        return messages.getMessage(key, args, log.getAction(), locale);
    }

    public String action(String code) {
        return messages.getMessage("audit.action." + code, null, code, LocaleContextHolder.getLocale());
    }
}
