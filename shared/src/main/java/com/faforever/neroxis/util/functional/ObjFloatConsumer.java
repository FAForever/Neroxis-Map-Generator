package com.faforever.neroxis.util.functional;

@FunctionalInterface
public interface ObjFloatConsumer<T> {

    void accept(T obj, float value);
}
