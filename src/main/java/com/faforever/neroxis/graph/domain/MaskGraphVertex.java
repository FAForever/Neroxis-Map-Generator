package com.faforever.neroxis.graph.domain;

import com.faforever.neroxis.mask.Mask;
import com.faforever.neroxis.util.MaskReflectUtil;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@RequiredArgsConstructor
public abstract class MaskGraphVertex {
    @EqualsAndHashCode.Include
    protected final int index;
    private final Map<String, Object> parameterValues = new HashMap<>();
    private Mask<?, ?> result;
    protected boolean fullyDefined = false;
    @Getter
    protected Executable executable;
    @Getter
    protected Class<? extends Mask<?, ?>> maskClass;

    public abstract Mask<?, ?> call() throws InvocationTargetException, IllegalAccessException, InstantiationException;

    public void setMaskClass(Class<? extends Mask<?, ?>> maskClass) {
        if (Modifier.isAbstract(maskClass.getModifiers())) {
            throw new IllegalArgumentException(String.format("Given mask class is abstract: maskClass=%s", maskClass.getSimpleName()));
        }

        this.maskClass = maskClass;
    }

    public void setExecutable(Executable executable) {
        if (maskClass == null) {
            throw new IllegalArgumentException("maskClass is not set");
        }
        if (executable != null) {
            if (!executable.getDeclaringClass().isAssignableFrom(maskClass)) {
                throw new IllegalArgumentException(String.format("Executable class does not match the vertex maskClass: executableClass=%s, maskClass=%s", executable.getDeclaringClass().getSimpleName(), maskClass.getSimpleName()));
            }
        }
        this.executable = executable;
        parameterValues.clear();
        fullyDefined = false;
    }

    public Object getParameter(String parameterName) {
        if (executable == null) {
            return null;
        }
        if (Arrays.stream(executable.getParameters()).noneMatch(parameter -> parameter.getName().equals(parameterName))) {
            throw new IllegalArgumentException(
                    String.format("Parameter is not valid: parameter=%s, validParameters=[%s]",
                            parameterName,
                            Arrays.stream(executable.getParameters()).map(Parameter::getName).collect(Collectors.joining(","))
                    )
            );
        }
        return parameterValues.get(parameterName);
    }

    public void setParameter(String parameterName, Object parameterValue) {
        if (executable == null) {
            return;
        }
        if (Arrays.stream(executable.getParameters()).anyMatch(parameter -> parameter.getName().equals(parameterName) && MaskReflectUtil.getActualParameterClass(maskClass, parameter).equals(parameterValue.getClass()))) {
            parameterValues.put(parameterName, parameterValue);
        } else {
            throw new IllegalArgumentException(
                    String.format("Parameter does not match the required constructor: parameterName=%s, parameterValueClass=%s, validParameters=[%s]",
                            parameterName,
                            parameterValue.getClass().getSimpleName(),
                            Arrays.stream(executable.getParameters()).map(parameter -> String.format("%s->%s", parameter.getName(), parameter.getType().getSimpleName())).collect(Collectors.joining(","))
                    )
            );
        }
        fullyDefined = Arrays.stream(executable.getParameters()).allMatch(parameter -> parameterValues.containsKey(parameter.getName()));
    }

    public void clearParameter(String parameterName) {
        if (executable == null) {
            return;
        }
        if (Arrays.stream(executable.getParameters()).noneMatch(parameter -> parameter.getName().equals(parameterName))) {
            throw new IllegalArgumentException(
                    String.format("Parameter is not valid: parameter=%s, validParameters=[%s]",
                            parameterName,
                            Arrays.stream(executable.getParameters()).map(Parameter::getName).collect(Collectors.joining(","))
                    )
            );
        }
        parameterValues.remove(parameterName);
        fullyDefined = false;
    }

    public void clearParameterValues() {
        parameterValues.clear();
    }

    public Mask<?, ?> getResult() throws InvocationTargetException, IllegalAccessException, InstantiationException {
        if (executable == null) {
            throw new IllegalStateException("Executable is null");
        }
        if (!fullyDefined) {
            throw new IllegalStateException("Cannot get result all parameters are not fully defined");
        }
        if (result == null) {
            result = call();
        }
        return result;
    }

}
