package ch.ruppen.danceschool.shared.logging;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
class BusinessLoggingAspect {

    // Dedicated logger name — downstream filters/levels can target business events independently
    // of the aspect's package, and log aggregators see a clean `logger=business` field.
    private static final Logger log = LoggerFactory.getLogger("business");

    @AfterReturning(pointcut = "@annotation(op)", returning = "result")
    void logBusinessEvent(JoinPoint joinPoint, BusinessOperation op, Object result) {
        Map<String, Object> details = new LinkedHashMap<>();
        extractArgumentDetails(joinPoint, details);
        extractResultDetails(result, details);

        StringJoiner joiner = new StringJoiner(" ");
        joiner.add("event=" + op.event());
        details.forEach((key, value) ->
                joiner.add(value instanceof String
                        ? key + "=\"" + value + "\""
                        : key + "=" + value));

        log.info("{}", joiner);
    }

    private void extractArgumentDetails(JoinPoint joinPoint, Map<String, Object> details) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            String name = paramNames[i];
            if (arg == null) {
                continue;
            }
            if (arg instanceof Long) {
                details.put(name, arg);
            } else if (arg instanceof String) {
                details.put(name, arg);
            } else {
                extractAccessor(arg, "name", details);
                extractAccessor(arg, "title", details);
                extractAccessor(arg, "role", details);
                extractNestedId(arg, "school", "schoolId", details);
                extractNestedId(arg, "user", "userId", details);
            }
        }
    }

    private void extractResultDetails(Object result, Map<String, Object> details) {
        if (result == null) {
            return;
        }
        if (result instanceof Long id) {
            details.put("resultId", id);
            return;
        }
        extractAccessor(result, "id", details);
        extractAccessor(result, "enrollmentId", details);
        extractAccessor(result, "name", details);
        extractAccessor(result, "title", details);
        extractAccessor(result, "role", details);
        extractAccessor(result, "status", details);
        extractNestedId(result, "school", "schoolId", details);
        extractNestedId(result, "user", "userId", details);
    }

    private void extractAccessor(Object obj, String field, Map<String, Object> details) {
        if (details.containsKey(field)) {
            return;
        }
        Object value = invokeAccessor(obj, field);
        if (value != null) {
            details.put(field, value);
        }
    }

    private void extractNestedId(Object obj, String nestedField, String targetKey,
                                  Map<String, Object> details) {
        if (details.containsKey(targetKey)) {
            return;
        }
        Object nested = invokeAccessor(obj, nestedField);
        if (nested != null) {
            Object id = invokeAccessor(nested, "id");
            if (id != null) {
                details.put(targetKey, id);
            }
        }
    }

    private Object invokeAccessor(Object obj, String field) {
        // Try record-style accessor first (field()), then getter (getField())
        try {
            Method method = obj.getClass().getMethod(field);
            return method.invoke(obj);
        } catch (Exception ignored) {
        }
        try {
            String getter = "get" + Character.toUpperCase(field.charAt(0)) + field.substring(1);
            Method method = obj.getClass().getMethod(getter);
            return method.invoke(obj);
        } catch (Exception ignored) {
        }
        return null;
    }
}
