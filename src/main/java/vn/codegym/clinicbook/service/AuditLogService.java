package vn.codegym.clinicbook.service;

import lombok.Getter;
import lombok.Setter;
import vn.codegym.clinicbook.dto.PageView;
import vn.codegym.clinicbook.entity.AuditLog;
import java.time.LocalDate;
import java.util.List;

public interface AuditLogService {
    static final int PAGE_SIZE = PageView.DEFAULT_SIZE;

    void record(String action, String username, String detail);

    List<AuditLog> findRecent();

    List<AuditLog> findLatest();

    Search search(LocalDate day, String action, String keyword, int page);

    long total();

    List<String> actions();

    Summary summary();

    @Getter
    @Setter
    public static class Search {
        private List<AuditLog> logs = List.of();
        private long matching;
        private PageView<AuditLog> page;
    }

    @Getter
    @Setter
    public static class Summary {
        private long total;
        private long today;
        private long bookedToday;
        private long cancelledToday;
        private long changedToday;
    }
}
