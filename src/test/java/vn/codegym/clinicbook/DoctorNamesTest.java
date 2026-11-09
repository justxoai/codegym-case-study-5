package vn.codegym.clinicbook;

import org.junit.jupiter.api.Test;
import vn.codegym.clinicbook.util.DoctorNames;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DoctorNamesTest {
    private static final Locale VI = Locale.forLanguageTag("vi");

    @Test
    void stripRemovesAnyLeadingTitle() {
        assertEquals("Nguyen Van An", DoctorNames.strip("BS. Nguyen Van An"));
        assertEquals("Nguyen Van An", DoctorNames.strip("bs.Nguyen Van An"));
        assertEquals("Nguyen Van An", DoctorNames.strip("Dr. Nguyen Van An"));
        assertEquals("Nguyen Van An", DoctorNames.strip("  dr Nguyen Van An "));
        assertEquals("Nguyen Van An", DoctorNames.strip("Nguyen Van An"));
        assertEquals("", DoctorNames.strip(null));
    }

    @Test
    void stripKeepsNamesThatOnlyStartWithTheseLetters() {
        assertEquals("Drake Tran", DoctorNames.strip("Drake Tran"));
        assertEquals("Bsoi Le", DoctorNames.strip("Bsoi Le"));
    }

    @Test
    void displayFollowsTheLanguage() {
        assertEquals("BS. Nguyen Van An", DoctorNames.display("Dr. Nguyen Van An", VI));
        assertEquals("Dr. Nguyen Van An", DoctorNames.display("BS. Nguyen Van An", Locale.ENGLISH));
        assertEquals("BS. Nguyen Van An", DoctorNames.display("Nguyen Van An", VI));
        assertEquals("Dr. Nguyen Van An", DoctorNames.display("Nguyen Van An", Locale.US));
    }

    @Test
    void anyOtherLanguageFallsBackToVietnamese() {
        assertEquals("BS. An", DoctorNames.display("An", Locale.FRENCH));
        assertEquals("BS. An", DoctorNames.display("An", null));
    }
}
