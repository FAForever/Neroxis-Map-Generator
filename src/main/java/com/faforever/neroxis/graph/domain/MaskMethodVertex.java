package com.faforever.neroxis.graph.domain;

import com.faforever.neroxis.mask.Mask;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class MaskMethodVertex extends MaskGraphVertex {
    private Mask<?, ?> executor;

    public MaskMethodVertex(int index) {
        super(index);
    }

    public void setExecutable(Method method) {
        if (method != null) {
            if (!Mask.class.isAssignableFrom(method.getReturnType())) {
                throw new IllegalArgumentException("Method does not return a subclass of Mask");
            }
        }
        super.setExecutable(method);
    }

    public void setParameter(String parameterName, Object parameterValue) {
        if (executable == null) {
            throw new IllegalStateException("Executable is not yet set");
        }

        if (parameterName.equals("this") && executable.getDeclaringClass().isAssignableFrom(parameterValue.getClass())) {
            executor = (Mask<?, ?>) parameterValue;
        } else {
            super.setParameter(parameterName, parameterValue);
        }

        fullyDefined = fullyDefined && executor != null;
    }

    public void clearParameter(String parameterName) {
        if (parameterName.equals("this")) {
            executor = null;
        } else {
            super.clearParameter(parameterName);
        }
        fullyDefined = false;
    }

    public Mask<?, ?> call() throws InvocationTargetException, IllegalAccessException {
        Object[] args = Arrays.stream(executable.getParameters()).map(parameter -> getParameter(parameter.getName())).toArray();
        return (Mask<?, ?>) ((Method) executable).invoke(executor, args);
    }

    public String toString() {
        return executable == null ? String.valueOf(index) : executable.getName();
    }
}
