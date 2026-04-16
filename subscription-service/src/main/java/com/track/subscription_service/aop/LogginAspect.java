package com.track.subscription_service.aop;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

public class LogginAspect {

    private final HttpServletRequest request;

    public LogginAspect(HttpServletRequest request) {
        this.request = request;
    }

    @Pointcut("execution(* com.track.subscription_service..controller..*(..))")
    public void controllerMethods(){}

    @Before("controllerMethods()")
    public void logBefore(JoinPoint joinPoint){
        String method = request.getMethod();
        String uri = request.getRequestURI();
        System.out.println("Before " + method + " " + uri);
    }

    @AfterReturning(pointcut = "controllerMethods()",returning = "results")
    public void logAfter(Object result){
        String method = request.getMethod();
        String uri = request.getRequestURI();

        System.out.println("After " + method + " " + uri);
    }

    @AfterThrowing(pointcut = "controllerMethods()",throwing = "ex")
    public void logError(Exception ex){
        String method = request.getMethod();
        String uri = request.getRequestURI();
        System.out.println("Throwing " + method + " " + uri + " Error: " + ex.getMessage());
    }
}
