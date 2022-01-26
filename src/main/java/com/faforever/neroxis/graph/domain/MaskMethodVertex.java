package com.faforever.neroxis.graph.domain;

import com.faforever.neroxis.mask.Mask;
import com.faforever.neroxis.ui.GraphParameter;
import com.faforever.neroxis.util.MaskReflectUtil;
import lombok.Getter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

@Getter
public class MaskMethodVertex extends MaskGraphVertex<Method> {
    private MaskGraphVertex<?> executor;

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

        if (parameterValue != null
                && parameterName.equals("executor")
                && MaskGraphVertex.class.isAssignableFrom(parameterValue.getClass())
                && executable.getDeclaringClass().isAssignableFrom(((MaskGraphVertex<?>) parameterValue).getResultClass())) {
            executor = (MaskGraphVertex<?>) parameterValue;
            checkFullyDefined();
        } else {
            super.setParameter(parameterName, parameterValue);
        }
    }

    @Override
    protected void checkFullyDefined() {
        fullyDefined = executor != null && Arrays.stream(executable.getParameters()).allMatch(param -> parameterValues.containsKey(param.getName())
                || Arrays.stream(executable.getAnnotationsByType(GraphParameter.class)).filter(annotation -> param.getName().equals(annotation.name()))
                .anyMatch(annotation -> annotation.nullable() || !annotation.value().equals("") || !annotation.contextSupplier().equals(GraphContext.SupplierType.USER_SPECIFIED)));
    }

    public void clearParameter(String parameterName) {
        if (parameterName.equals("executor")) {
            executor = null;
            checkFullyDefined();
        } else {
            super.clearParameter(parameterName);
        }
    }

    public Mask<?, ?> call(GraphContext graphContext) throws InvocationTargetException, IllegalAccessException {
        Object[] args = Arrays.stream(executable.getParameters()).map(parameter -> {
            try {
                return getParameterFinalValue(parameter, graphContext);
            } catch (InvocationTargetException | IllegalAccessException | InstantiationException e) {
                throw new RuntimeException(e);
            }
        }).toArray();
        return (Mask<?, ?>) executable.invoke(executor.getResult(), args);
    }

    @Override
    public Class<? extends Mask<?, ?>> getResultClass() {
        return executable == null ? null : (Class<? extends Mask<?, ?>>) MaskReflectUtil.getActualTypeClass(executorClass, executable.getGenericReturnType());
    }

    public String toString() {
        return executable == null ? String.valueOf(hashCode()) : executable.getName();
    }
}
