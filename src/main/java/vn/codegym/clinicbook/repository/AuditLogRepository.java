package vn.codegym.clinicbook.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.codegym.clinicbook.entity.AuditLog;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findTop100ByOrderByCreatedAtDesc();

    List<AuditLog> findTop5ByOrderByCreatedAtDesc();

    @Query("select l from AuditLog l where l.createdAt >= :from and l.createdAt < :to "
            + "and (:action = '' or l.action = :action) "
            + "and (:kw = '' or lower(l.username) like concat('%', :kw, '%') "
            + "or lower(l.detail) like concat('%', :kw, '%')) "
            + "order by l.createdAt desc, l.id desc")
    List<AuditLog> search(@Param("from") LocalDateTime from,
                          @Param("to") LocalDateTime to,
                          @Param("action") String action,
                          @Param("kw") String keyword,
                          Pageable pageable);

    @Query("select count(l) from AuditLog l where l.createdAt >= :from and l.createdAt < :to "
            + "and (:action = '' or l.action = :action) "
            + "and (:kw = '' or lower(l.username) like concat('%', :kw, '%') "
            + "or lower(l.detail) like concat('%', :kw, '%'))")
    long countMatching(@Param("from") LocalDateTime from,
                       @Param("to") LocalDateTime to,
                       @Param("action") String action,
                       @Param("kw") String keyword);

    @Query("select distinct l.action from AuditLog l order by l.action")
    List<String> findDistinctActions();
}
