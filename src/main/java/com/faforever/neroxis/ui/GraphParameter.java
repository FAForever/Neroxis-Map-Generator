package com.faforever.neroxis.ui;

import com.faforever.neroxis.graph.domain.GraphContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Repeatable(GraphParameters.class)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface GraphParameter {
    String name();

    String value() default "";

    GraphContext.SupplierType contextSupplier() default GraphContext.SupplierType.USER_SPECIFIED;

    boolean nullable() default false;
}
