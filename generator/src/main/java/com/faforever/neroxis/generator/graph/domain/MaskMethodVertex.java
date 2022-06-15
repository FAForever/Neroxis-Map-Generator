package com.faforever.neroxis.generator.graph.domain;

import com.faforever.neroxis.annotations.GraphMethod;
import com.faforever.neroxis.annotations.GraphParameter;
import com.faforever.neroxis.mask.Mask;
import com.faforever.neroxis.util.MaskReflectUtil;
import lombok.Getter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

@Getter
public strictfp class MaskMethodVertex extends MaskGraphVertex<Method> {
    public static final String NEW_MASK = "newMask";
    public static final String EXECUTOR = "executor";

    private MaskVertexResult executor;

    public MaskMethodVertex(Method executable, Class<? extends Mask<?, ?>> executorClass) {
        super(executable, executorClass);

        if (!Mask.class.isAssignableFrom(executable.getReturnType())) {
            throw new IllegalArgumentException("Method does not return a subclass of Mask");
        }

        if (!executable.getAnnotation(GraphMethod.class).returnsSelf()) {
            results.put(NEW_MASK, null);
            resultClasses.put(NEW_MASK, (Class<? extends Mask<?, ?>>) MaskReflectUtil.getActualTypeClass(executorClass, executable.getGenericReturnType()));
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

    public void setParameter(String parameterName, Object parameterValue) {
        if (executable == null) {
            throw new IllegalStateException("Executable is not yet set");
        }

        if (parameterValue != null
                && EXECUTOR.equals(parameterName)
                && MaskVertexResult.class.isAssignableFrom(parameterValue.getClass())
                && executable.getDeclaringClass().isAssignableFrom(((MaskVertexResult) parameterValue).getResultClass())) {

            if (executor != null) {
                throw new IllegalStateException("executor already set");
            }

            executor = (MaskVertexResult) parameterValue;
        } else {
            super.setParameter(parameterName, parameterValue);
        }
    }

    @Override
    public boolean isNotDefined() {
        return executor == null
                || !Arrays.stream(executable.getParameters()).allMatch(param -> nonMaskParameters.containsKey(param.getName())
                || maskParameters.containsKey(param.getName())
                || Arrays.stream(executable.getAnnotationsByType(GraphParameter.class)).filter(annotation -> param.getName().equals(annotation.name()))
                .anyMatch(annotation -> annotation.nullable() || !annotation.value().equals("")));
    }

    public void clearParameter(String parameterName) {
        if (EXECUTOR.equals(parameterName)) {
            executor = null;
        } else {
            super.clearParameter(parameterName);
        }
    }

    protected void computeResults(GraphContext graphContext) throws InvocationTargetException, IllegalAccessException {
        Object[] args = Arrays.stream(executable.getParameters()).map(parameter -> {
            try {
                return getParameterFinalValue(parameter, graphContext);
            } catch (InvocationTargetException | IllegalAccessException | InstantiationException e) {
                throw new RuntimeException(e);
            }
        }).toArray();
        Mask<?, ?> result = (Mask<?, ?>) executable.invoke(executor.getResult(), args);
        if (!executable.getAnnotation(GraphMethod.class).returnsSelf()) {
            results.put(NEW_MASK, result);
            results.put(SELF, executor.getResult());
        } else {
            results.put(SELF, result);
        }
    }

    public String toString() {
        return executable.getName();
    }
}