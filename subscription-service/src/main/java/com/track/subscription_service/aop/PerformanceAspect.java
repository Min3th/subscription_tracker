package com.track.subscription_service.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PerformanceAspect {

    private static final Logger log = LoggerFactory.getLogger(PerformanceAspect.class);

    private static final long SLOW_METHOD_THRESHOLD = 300;

    @Pointcut("execution(* com.track.subscription_service..service..*(..))") // any package that contains service at any level
    public void serviceLayer(){}

    @Around("serviceLayer()")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {

        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long duration = System.currentTimeMillis() - start;
        String method = joinPoint.getSignature().toShortString();

        if (duration > SLOW_METHOD_THRESHOLD) {
            log.warn("Slow method : {} took {} ms",method,duration);
        }
        else {
            log.info("{} took {} ms",method,duration);
        }

        return result;
    }

}
