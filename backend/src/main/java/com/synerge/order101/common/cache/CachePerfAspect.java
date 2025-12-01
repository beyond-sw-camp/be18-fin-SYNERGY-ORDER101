package com.synerge.order101.common.cache;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class CachePerfAspect {

    @Around("@annotation(cachedPerf)")
    public Object measure(ProceedingJoinPoint pjp, CachedPerf cachedPerf) throws Throwable {

        String name = cachedPerf.value().isEmpty()
                ? pjp.getSignature().toShortString()
                : cachedPerf.value();

        long start = System.currentTimeMillis();
        Object result = pjp.proceed();
        long end = System.currentTimeMillis();

        log.info("[CACHE-PERF] {} executed in {} ms", name, (end - start));
        return result;
    }
}