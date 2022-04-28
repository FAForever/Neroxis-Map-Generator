package com.faforever.neroxis.util.functional;

@FunctionalInterface
public interface ToBooleanFunction<T> {

    boolean apply(T value);
}
