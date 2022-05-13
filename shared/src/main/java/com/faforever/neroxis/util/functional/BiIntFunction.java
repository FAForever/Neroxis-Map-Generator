package com.faforever.neroxis.util.functional;

@FunctionalInterface
public interface BiIntFunction<T> {
    T apply(int i, int j);
}
