package com.faforever.neroxis.util.functional;

@FunctionalInterface
public interface ToFloatFunction<T> {
    float apply(T value);
}
