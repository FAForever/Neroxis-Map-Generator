package com.faforever.neroxis.graph;

public interface GraphContext {
    <T> T getValue(String expression, String identifier, Class<T> clazz);
}
