package com.faforever.neroxis.graph.domain;

import com.faforever.neroxis.mask.Mask;
import lombok.Getter;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

@Getter
public strictfp class MaskConstructorVertex extends MaskGraphVertex<Constructor<? extends Mask<?, ?>>> {

    public MaskConstructorVertex(Constructor<? extends Mask<?, ?>> executable) {
        super(executable, executable == null ? null : executable.getDeclaringClass());
    }

    @Override
    public String getExecutableName() {
        return executorClass.getSimpleName();
    }

    @Override
    public Class<? extends Mask<?, ?>> getReturnedClass() {
        return executable.getDeclaringClass();
    }

    @Override
    protected void computeResults(GraphContext graphContext) throws InvocationTargetException, IllegalAccessException, InstantiationException {
        Object[] args = Arrays.stream(executable.getParameters()).map(parameter -> {
            try {
                return getParameterFinalValue(parameter, graphContext);
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }).toArray();
        results.put(SELF, executable.newInstance(args));
    }

    public String toString() {
        return executorClass.getSimpleName();
    }
}
