package com.faforever.neroxis.util.functional;

@FunctionalInterface
public interface BiIntObjConsumer<T> {
    void accept(int i, int j, T obj);
}
