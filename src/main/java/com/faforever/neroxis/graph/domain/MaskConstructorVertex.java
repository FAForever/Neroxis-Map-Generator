package com.faforever.neroxis.graph.domain;

import com.faforever.neroxis.mask.Mask;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class MaskConstructorVertex extends MaskGraphVertex {

    public MaskConstructorVertex(int index) {
        super(index);
    }

    public Mask<?, ?> call() throws InvocationTargetException, IllegalAccessException, InstantiationException {
        Object[] args = Arrays.stream(executable.getParameters()).map(parameter -> getParameter(parameter.getName())).toArray();
        return ((Constructor<? extends Mask<?, ?>>) executable).newInstance(args);
    }
}
