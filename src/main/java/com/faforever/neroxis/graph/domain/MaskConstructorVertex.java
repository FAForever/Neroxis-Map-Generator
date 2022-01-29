package com.faforever.neroxis.graph.domain;

import com.faforever.neroxis.mask.Mask;
import lombok.Getter;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

@Getter
public strictfp class MaskConstructorVertex<T extends Mask<?, ?>> extends MaskGraphVertex<Constructor<T>> {

    @Override
    protected void computeResults(GraphContext graphContext) throws InvocationTargetException, IllegalAccessException, InstantiationException {
        Object[] args = Arrays.stream(executable.getParameters()).map(parameter -> {
            try {
                return getParameterFinalValue(parameter, graphContext);
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }).toArray();
        results.put(SELF, ((Constructor<? extends Mask<?, ?>>) executable).newInstance(args));
    }
}
