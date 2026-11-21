package com.example.auditing.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditedOperation {
    String action();
    String targetType();
    // Index of the argument containing a stable business identifier.
    int targetIdArgument() default 0;
}
