package com.faforever.neroxis.generator.graph.domain;

import com.faforever.neroxis.mask.Mask;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import lombok.Getter;

@Getter
public strictfp class MaskEndpointVertex<T extends Mask<T, ?>> extends MaskGraphVertex<Method> {
    private final String name;

    public MaskEndpointVertex(String name, Class<T> returnClass) throws NoSuchMethodException {
        super(MaskEndpointVertex.class.getDeclaredMethod("setResult", returnClass), returnClass);
        this.name = name;

        if (!Mask.class.isAssignableFrom(executable.getReturnType())) {
            throw new IllegalArgumentException("Method does not return a subclass of Mask");
        }
    }

    @Override
    public String getExecutableName() {
        return name;
    }

    @Override
    protected void computeResults(GraphContext graphContext) throws InvocationTargetException, IllegalAccessException {
        Object[] args = Arrays.stream(executable.getParameters())
                              .map(parameter -> getParameterFinalValue(parameter, graphContext))
                              .toArray();
        executable.invoke(this, args);
    }

    @Override
    public MaskEndpointVertex<T> copy() {
        MaskEndpointVertex<T> newVertex;
        try {
            newVertex = new MaskEndpointVertex<T>(name, (Class<T>) resultClasses.get(SELF));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        newVertex.setIdentifier(identifier);
        nonMaskParameters.forEach(newVertex::setParameter);
        return newVertex;
    }

    public String toString() {
        return identifier == null ? "" : identifier;
    }

    public T getResult() {
        return (T) getResult(SELF);
    }

    protected void setResult(T mask) {
        results.put(SELF, mask);
    }
}
