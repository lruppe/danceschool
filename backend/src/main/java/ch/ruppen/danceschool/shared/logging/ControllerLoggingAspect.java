package ch.ruppen.danceschool.shared.logging;

import java.util.Arrays;
import java.util.Collection;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Aspect
@Component
@Slf4j
class ControllerLoggingAspect {

    @Around("execution(public * ch.ruppen.danceschool..*..*Controller.*(..))")
    Object logController(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        log.debug("Arguments to {}.{}: {}", className, methodName, Arrays.toString(joinPoint.getArgs()));

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            Object result = joinPoint.proceed();
            stopWatch.stop();
            String resultSummary = summarize(result);
            log.info("Completed {}.{} in {} ms [{}]", className, methodName,
                    stopWatch.getTotalTimeMillis(), resultSummary);
            log.debug("Return value: {}", result);
            return result;
        } catch (Throwable ex) {
            stopWatch.stop();
            log.error("Failed {}.{} after {} ms: {}", className, methodName,
                    stopWatch.getTotalTimeMillis(), ex.getMessage());
            throw ex;
        }
    }

    private String summarize(Object result) {
        if (result == null) {
            return "void";
        }
        if (result instanceof Collection<?> c) {
            return "collection size=" + c.size();
        }
        return result.getClass().getSimpleName();
    }
}
