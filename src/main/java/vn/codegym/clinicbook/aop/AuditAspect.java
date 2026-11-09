package vn.codegym.clinicbook.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import vn.codegym.clinicbook.entity.Appointment;
import vn.codegym.clinicbook.security.CurrentUser;
import vn.codegym.clinicbook.service.AuditLogService;

import java.util.Arrays;

@Aspect
@Component
public class AuditAspect {
    private final AuditLogService auditLogService;
    private final CurrentUser currentUser;

    public AuditAspect(AuditLogService auditLogService, CurrentUser currentUser) {
        this.auditLogService = auditLogService;
        this.currentUser = currentUser;
    }

    @AfterReturning(pointcut = "@annotation(audited)", returning = "result")
    public void audit(JoinPoint joinPoint, Audited audited, Object result) {
        String detail = result instanceof Appointment appointment
                ? AuditDetail.of(appointment)
                : joinPoint.getSignature().getName() + Arrays.toString(joinPoint.getArgs());
        String username = currentUser.username();
        auditLogService.record(audited.value(), username == null ? "anonymous" : username, detail);
    }
}
