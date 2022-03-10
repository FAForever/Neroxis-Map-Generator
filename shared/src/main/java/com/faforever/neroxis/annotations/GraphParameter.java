package com.faforever.neroxis.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Repeatable(GraphParameters.class)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public strictfp @interface GraphParameter {
    String name();

    String value() default "";

    String description() default "";

    boolean nullable() default false;
}
