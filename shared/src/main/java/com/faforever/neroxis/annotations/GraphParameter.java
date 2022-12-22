package com.faforever.neroxis.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Repeatable(GraphParameter.GraphParameters.class)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface GraphParameter {
    String name();

    String value() default "";

    String description() default "";

    boolean nullable() default false;

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
    @interface GraphParameters {
        GraphParameter[] value();
    }
}
