package com.faforever.neroxis.graph.domain;

import com.faforever.neroxis.graph.GraphContext;
import com.faforever.neroxis.mask.Mask;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import lombok.Getter;

@Getter
public strictfp class MaskOutputVertex<T extends Mask<?, T>> extends MaskGraphVertex<Method> {
    private final String name;
    private T result;

    public MaskOutputVertex(String name, Class<T> returnClass) throws NoSuchMethodException {
        super(MaskOutputVertex.class.getDeclaredMethod("setResult", Mask.class), returnClass);
        this.name = name;
        results.clear();
        resultClasses.clear();
    }

    @Override
    public String getExecutableName() {
        return name + " Output";
    }

    @Override
    public boolean isComputed() {
        return result != null;
    }

    @Override
    protected void computeResults(GraphContext graphContext) throws InvocationTargetException, IllegalAccessException {
        Object[] args = Arrays.stream(executable.getParameters())
                              .map(parameter -> getParameterFinalValue(parameter, graphContext))
                              .toArray();
        executable.invoke(this, args);
    }

    @Override
    public MaskOutputVertex<T> copy() {
        MaskOutputVertex<T> newVertex;
        try {
            newVertex = new MaskOutputVertex<T>(name, (Class<T>) resultClasses.get(SELF));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        newVertex.setIdentifier(identifier);
        nonMaskParameters.forEach(newVertex::setParameter);
        return newVertex;
    }

    public T getResult() {
        return result.copy();
    }

    protected void setResult(T exec) {
        result = exec;
    }
}
