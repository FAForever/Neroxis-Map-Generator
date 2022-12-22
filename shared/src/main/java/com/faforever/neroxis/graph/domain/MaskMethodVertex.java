package com.faforever.neroxis.graph.domain;

import com.faforever.neroxis.annotations.GraphMethod;
import com.faforever.neroxis.graph.GraphContext;
import com.faforever.neroxis.mask.Mask;
import com.faforever.neroxis.util.MaskGraphReflectUtil;
import lombok.Getter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

@Getter
public class MaskMethodVertex extends MaskGraphVertex<Method> {
    public static final String NEW_MASK = "new";
    public static final String EXECUTOR = "exec";
    private final boolean returnsSelf;
    private MaskVertexResult executor;

    public MaskMethodVertex(Method executable, Class<? extends Mask<?, ?>> executorClass) {
        super(executable, executorClass);
        if (!Mask.class.isAssignableFrom(executable.getReturnType())) {
            throw new IllegalArgumentException("Method does not return a subclass of Mask");
        }

        returnsSelf = MaskGraphReflectUtil.getOverriddenMethod(executable)
                                          .getAnnotation(GraphMethod.class)
                                          .returnsSelf();

        maskParameters.put(EXECUTOR, null);
        if (!returnsSelf) {
            results.put(NEW_MASK, null);
            resultClasses.put(NEW_MASK,
                              (Class<? extends Mask<?, ?>>) MaskGraphReflectUtil.getActualTypeClass(executorClass,
                                                                                                    executable.getGenericReturnType()));
        }
    }

    @Override
    public void setParameter(String parameterName, Object parameterValue) {
        if (executable == null) {
            throw new IllegalStateException("Executable is not yet set");
        }
        if (parameterValue != null && EXECUTOR.equals(parameterName) && MaskVertexResult.class.isAssignableFrom(
                parameterValue.getClass()) && executable.getDeclaringClass()
                                                        .isAssignableFrom(
                                                                ((MaskVertexResult) parameterValue).getResultClass())) {
            if (executor != null) {
                throw new IllegalStateException("executor already set");
            }
            executor = (MaskVertexResult) parameterValue;
        } else {
            super.setParameter(parameterName, parameterValue);
        }
    }

    @Override
    public void clearParameter(String parameterName) {
        if (EXECUTOR.equals(parameterName)) {
            executor = null;
        } else {
            super.clearParameter(parameterName);
        }
    }

    @Override
    public boolean isMaskParameterNull(String parameter) {
        if (EXECUTOR.equals(parameter)) {
            return executor == null;
        }
        return super.isMaskParameterNull(parameter);
    }

    @Override
    public Class<? extends Mask<?, ?>> getMaskParameterClass(String parameterName) {
        if (EXECUTOR.equals(parameterName)) {
            return executorClass;
        }
        return super.getMaskParameterClass(parameterName);
    }

    @Override
    public String getExecutableName() {
        return executable.getName();
    }

    @Override
    public boolean isDefined(GraphContext graphContext) {
        return executor != null && super.isDefined(graphContext);
    }

    @Override
    protected void computeResults(GraphContext graphContext) throws InvocationTargetException, IllegalAccessException {
        Object[] args = Arrays.stream(executable.getParameters())
                              .map(parameter -> getParameterFinalValue(parameter, graphContext))
                              .toArray();
        Mask<?, ?> result = (Mask<?, ?>) executable.invoke(executor.getResult(), args);
        if (!returnsSelf) {
            results.put(NEW_MASK, result);
            results.put(SELF, executor.getResult());
        } else {
            results.put(SELF, result);
        }
    }

    @Override
    public MaskMethodVertex copy() {
        MaskMethodVertex newVertex = new MaskMethodVertex(executable, executorClass);
        newVertex.setIdentifier(identifier);
        nonMaskParameters.forEach(newVertex::setParameter);
        return newVertex;
    }
}
