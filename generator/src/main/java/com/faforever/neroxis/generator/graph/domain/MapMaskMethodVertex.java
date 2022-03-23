package com.faforever.neroxis.generator.graph.domain;

import com.faforever.neroxis.mask.Mask;
import com.faforever.neroxis.util.MaskReflectUtil;
import lombok.Getter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

@Getter
public strictfp class MapMaskMethodVertex extends MaskGraphVertex<Method> {

    public MapMaskMethodVertex(Method executable) {
        super(executable, (Class<? extends Mask<?, ?>>) executable.getReturnType());

        if (!Mask.class.isAssignableFrom(executable.getReturnType())) {
            throw new IllegalArgumentException("Method does not return a subclass of Mask");
        }
    }

    @Override
    public String getExecutableName() {
        return executable.getName();
    }

    @Override
    public Class<? extends Mask<?, ?>> getReturnedClass() {
        return (Class<? extends Mask<?, ?>>) MaskReflectUtil.getActualTypeClass(executorClass, executable.getGenericReturnType());
    }

    protected void computeResults(GraphContext graphContext) throws InvocationTargetException, IllegalAccessException {
        Object[] args = Arrays.stream(executable.getParameters()).map(parameter -> {
            try {
                return getParameterFinalValue(parameter, graphContext);
            } catch (InvocationTargetException | IllegalAccessException | InstantiationException e) {
                throw new RuntimeException(e);
            }
        }).toArray();
        Mask<?, ?> result = (Mask<?, ?>) executable.invoke(null, args);
        results.put(SELF, result);
    }

    public String toString() {
        return executable.getName();
    }
}
