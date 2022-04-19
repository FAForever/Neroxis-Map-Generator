package com.faforever.neroxis.generator.graph.domain;

import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.Mask;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import lombok.Getter;

@Getter
public strictfp class MaskConstructorVertex extends MaskGraphVertex<Constructor<? extends Mask<?, ?>>> {
    public static MaskConstructorVertex ofClass(Class<? extends Mask<?, ?>> clazz) throws NoSuchMethodException {
        return new MaskConstructorVertex(clazz.getDeclaredConstructor(int.class, Long.class, SymmetrySettings.class, String.class, boolean.class));
    }

    public MaskConstructorVertex(Constructor<? extends Mask<?, ?>> executable) {
        super(executable, executable.getDeclaringClass());
    }

    @Override
    public String getExecutableName() {
        return "new";
    }

    @Override
    protected void computeResults(GraphContext graphContext) throws InvocationTargetException, IllegalAccessException, InstantiationException {
        Object[] args = Arrays.stream(executable.getParameters()).map(parameter -> getParameterFinalValue(parameter, graphContext)).toArray();
        results.put(SELF, executable.newInstance(args));
    }

    public String toString() {
        return identifier == null ? "" : identifier;
    }

    public MaskConstructorVertex copy() {
        MaskConstructorVertex newVertex = new MaskConstructorVertex(executable);
        newVertex.setIdentifier(identifier);
        nonMaskParameters.forEach(newVertex::setParameter);
        return newVertex;
    }
}
