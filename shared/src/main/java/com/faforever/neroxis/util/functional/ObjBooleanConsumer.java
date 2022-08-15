package com.faforever.neroxis.util.functional;

@FunctionalInterface
public interface ObjBooleanConsumer<T> {
    void accept(T obj, boolean value);
}
