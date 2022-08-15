package com.faforever.neroxis.graph.domain;

import com.faforever.neroxis.graph.GraphContext;
import com.faforever.neroxis.mask.Mask;
import lombok.Getter;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

@Getter
public strictfp class MaskConstructorVertex extends MaskGraphVertex<Constructor<? extends Mask<?, ?>>> {
    public MaskConstructorVertex(Constructor<? extends Mask<?, ?>> executable) {
        super(executable, executable.getDeclaringClass());
    }

    @Override
    public String getExecutableName() {
        return "new";
    }

    @Override
    protected void computeResults(
            GraphContext graphContext) throws InvocationTargetException, IllegalAccessException, InstantiationException {
        Object[] args = Arrays.stream(executable.getParameters())
                              .map(parameter -> getParameterFinalValue(parameter, graphContext))
                              .toArray();
        results.put(SELF, executable.newInstance(args));
    }

    @Override
    public MaskConstructorVertex copy() {
        MaskConstructorVertex newVertex = new MaskConstructorVertex(executable);
        newVertex.setIdentifier(identifier);
        nonMaskParameters.forEach(newVertex::setParameter);
        return newVertex;
    }
}
