package com.faforever.neroxis.util.functional;

@FunctionalInterface
public interface ObjFloatConsumer<T> {

    void accept(T point, float value);
}
