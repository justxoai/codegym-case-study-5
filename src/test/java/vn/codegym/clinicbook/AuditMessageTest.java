package vn.codegym.clinicbook;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.test.context.ActiveProfiles;
import vn.codegym.clinicbook.aop.AuditDetail;
import vn.codegym.clinicbook.aop.AuditMessages;
import vn.codegym.clinicbook.entity.AuditLog;

import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class AuditMessageTest {
    @Autowired AuditMessages messages;

    @BeforeEach
    void vietnamese() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("vi"));
    }

    @AfterEach
    void reset() {
        LocaleContextHolder.resetLocaleContext();
    }

    private static AuditLog log(String action, String user, String detail) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setUsername(user);
        log.setDetail(detail);
        return log;
    }

    private static final String DETAIL = "v1;id=42;patient=Tran Mai;doctor=BS. Le An;at=2026-11-05T09:30;status=CONFIRMED";

    @Test
    void detailIsParsedIntoFacts() {
        Map<String, String> facts = AuditDetail.parse(DETAIL);
        assertNotNull(facts);
        assertEquals("42", facts.get("id"));
        assertEquals("Tran Mai", facts.get("patient"));
        assertEquals("CONFIRMED", facts.get("status"));
    }

    @Test
    void oldRowsAreNotParsed() {
        assertNull(AuditDetail.parse("book[1, x] -> appointment #7 [PENDING]"));
        assertNull(AuditDetail.parse(null));
    }

    @Test
    void searchKeyMatchesOneAppointmentOnly() {
        assertTrue(DETAIL.contains(AuditDetail.keyOf(42L)));
        assertTrue(!DETAIL.contains(AuditDetail.keyOf(4L)));
    }

    @Test
    void statusChangeReadsAsASentenceInVietnamese() {
        String text = messages.describe(log("APPOINTMENT_STATUS_CHANGED", "doctor3", DETAIL));
        assertTrue(text.contains("doctor3"), text);
        assertTrue(text.contains("#42"), text);
        assertTrue(text.contains("Tran Mai"), text);
        assertTrue(text.contains("BS. Le An"), text);
        assertTrue(text.contains("09:30") && text.contains("05/11/2026"), text);
        assertTrue(text.contains("Đã xác nhận"), text);
    }

    @Test
    void sameRowReadsInEnglishWithDrInFrontOfTheDoctor() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        String text = messages.describe(log("APPOINTMENT_STATUS_CHANGED", "doctor3", DETAIL));
        assertTrue(text.contains("Dr. Le An"), text);
        assertTrue(!text.contains("BS."), text);
        assertTrue(text.contains("Confirmed"), text);
    }

    @Test
    void bookingNamesThePatientAsTheActor() {
        String detail = "v1;id=8;patient=Tran Mai;doctor=Dr. Le An;at=2026-11-05T09:30;status=PENDING";
        String text = messages.describe(log("APPOINTMENT_BOOKED", "patient1", detail));
        assertTrue(text.contains("Tran Mai") && text.contains("BS. Le An") && text.contains("#8"), text);
    }

    @Test
    void oldRowsKeepTheirStoredText() {
        String old = "book[1, x] -> appointment #7 [PENDING]";
        assertEquals(old, messages.describe(log("APPOINTMENT_BOOKED", "x", old)));
    }

    @Test
    void passwordChangeHasAFixedSentence() {
        String text = messages.describe(log("PASSWORD_CHANGED", "patient9", null));
        assertTrue(text.contains("patient9"), text);
    }

    @Test
    void actionCodesAreTranslatedAndUnknownOnesStayAsTheyAre() {
        assertEquals("Booking", withEnglish(() -> messages.action("APPOINTMENT_BOOKED")));
        assertEquals("SOMETHING_NEW", messages.action("SOMETHING_NEW"));
    }

    private static String withEnglish(java.util.function.Supplier<String> call) {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        return call.get();
    }
}
