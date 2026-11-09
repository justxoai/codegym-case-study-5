package vn.codegym.clinicbook.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PerformanceAspect {
    private static final Logger log = LoggerFactory.getLogger(PerformanceAspect.class);

    @Around("execution(public * vn.codegym.clinicbook.service..*.*(..))")
    public Object measure(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.nanoTime();
        String name = joinPoint.getSignature().getDeclaringType().getSimpleName()
                + "." + joinPoint.getSignature().getName();
        try {
            return joinPoint.proceed();
        } finally {
            long ms = (System.nanoTime() - start) / 1_000_000;
            log.info("[PERF] {} finished in {} ms", name, ms);
        }
    }
}
