package com.faforever.neroxis.graph.domain;

import com.faforever.neroxis.graph.GraphContext;
import com.faforever.neroxis.mask.Mask;
import java.lang.reflect.Method;
import lombok.Getter;

@Getter
public strictfp class MaskInputVertex<T extends Mask<?, T>> extends MaskGraphVertex<Method> {
    private final String name;
    private T result;

    public MaskInputVertex(String name, Class<T> returnClass) throws NoSuchMethodException {
        super(MaskInputVertex.class.getDeclaredMethod("getResult"), returnClass);
        this.name = name;
    }

    @Override
    public String getExecutableName() {
        return name + " Input";
    }

    @Override
    public boolean isComputed() {
        return result != null;
    }

    @Override
    public boolean isDefined(GraphContext graphContext) {
        return true;
    }

    @Override
    protected void computeResults(GraphContext graphContext) {}

    @Override
    public MaskInputVertex<T> copy() {
        MaskInputVertex<T> newVertex;
        try {
            newVertex = new MaskInputVertex<T>(name, (Class<T>) resultClasses.get(SELF));
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

    public void setResult(T mask) {
        result = mask;
    }
}
