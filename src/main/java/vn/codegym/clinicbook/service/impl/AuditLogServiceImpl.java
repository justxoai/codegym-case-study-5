package vn.codegym.clinicbook.service.impl;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.codegym.clinicbook.dto.PageView;
import vn.codegym.clinicbook.entity.AuditLog;
import vn.codegym.clinicbook.repository.AuditLogRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import vn.codegym.clinicbook.service.AuditLogService;

@Service
public class AuditLogServiceImpl implements AuditLogService {
    private static final LocalDateTime MIN = LocalDateTime.of(1970, 1, 1, 0, 0);
    private static final LocalDateTime MAX = LocalDateTime.of(9999, 1, 1, 0, 0);

    private final AuditLogRepository auditLogRepository;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void record(String action, String username, String detail) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setUsername(username);
        log.setDetail(detail != null && detail.length() > 1000 ? detail.substring(0, 1000) : detail);
        log.setCreatedAt(LocalDateTime.now());
        auditLogRepository.save(log);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    @Override
    public List<AuditLog> findRecent() {
        return auditLogRepository.findTop100ByOrderByCreatedAtDesc();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    @Override
    public List<AuditLog> findLatest() {
        return auditLogRepository.findTop5ByOrderByCreatedAtDesc();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    @Override
    public Search search(LocalDate day, String action, String keyword, int page) {
        LocalDateTime from = day == null ? MIN : day.atStartOfDay();
        LocalDateTime to = day == null ? MAX : day.plusDays(1).atStartOfDay();
        String act = action == null ? "" : action.trim();
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();

        long matching = auditLogRepository.countMatching(from, to, act, kw);
        int current = PageView.clamp(page, matching, PAGE_SIZE);
        List<AuditLog> rows = auditLogRepository.search(from, to, act, kw, PageRequest.of(current - 1, PAGE_SIZE));

        Search result = new Search();
        result.setLogs(rows);
        result.setMatching(matching);
        result.setPage(PageView.ofSlice(rows, matching, current, PAGE_SIZE));
        return result;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    @Override
    public long total() {
        return auditLogRepository.count();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    @Override
    public List<String> actions() {
        return auditLogRepository.findDistinctActions();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    @Override
    public Summary summary() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        Summary s = new Summary();
        s.setTotal(auditLogRepository.count());
        s.setToday(auditLogRepository.countMatching(start, end, "", ""));
        s.setBookedToday(auditLogRepository.countMatching(start, end, "APPOINTMENT_BOOKED", ""));
        s.setCancelledToday(auditLogRepository.countMatching(start, end, "APPOINTMENT_CANCELLED", ""));
        s.setChangedToday(auditLogRepository.countMatching(start, end, "APPOINTMENT_STATUS_CHANGED", ""));
        return s;
    }
}
