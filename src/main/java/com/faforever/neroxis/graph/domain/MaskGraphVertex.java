package com.faforever.neroxis.graph.domain;

import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.mask.Mask;
import com.faforever.neroxis.ui.GraphParameter;
import com.faforever.neroxis.util.MaskReflectUtil;
import com.faforever.neroxis.util.Pipeline;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public abstract class MaskGraphVertex<T extends Executable> {
    protected final Map<String, Object> parameterValues = new HashMap<>();
    @Getter
    private Mask<?, ?> result;
    @Getter
    private Mask<?, ?> immutableResult;
    @Getter
    protected boolean fullyDefined = false;
    @Getter
    protected T executable;
    @Getter
    protected Class<? extends Mask<?, ?>> executorClass;
    @Getter
    private Pipeline.Entry entry;

    public abstract Mask<?, ?> call(GraphContext graphContext) throws InvocationTargetException, IllegalAccessException, InstantiationException;

    public void setExecutorClass(Class<? extends Mask<?, ?>> executorClass) {
        if (Modifier.isAbstract(executorClass.getModifiers())) {
            throw new IllegalArgumentException(String.format("Given mask class is abstract: maskClass=%s", executorClass.getSimpleName()));
        }

        this.executorClass = executorClass;
    }

    public void setExecutable(T executable) {
        if (executorClass == null) {
            throw new IllegalArgumentException("maskClass is not set");
        }
        if (executable != null) {
            if (!executable.getDeclaringClass().isAssignableFrom(executorClass)) {
                throw new IllegalArgumentException(String.format("Executable class does not match the vertex maskClass: executableClass=%s, maskClass=%s", executable.getDeclaringClass().getSimpleName(), executorClass.getSimpleName()));
            }
        }
        this.executable = executable;
        parameterValues.clear();
        fullyDefined = false;
        if (executable != null) {
            Arrays.stream(executable.getAnnotationsByType(GraphParameter.class))
                    .filter(parameterAnnotation -> !parameterAnnotation.value().equals(""))
                    .forEach(parameterAnnotation -> setParameter(parameterAnnotation.name(), parameterAnnotation.value()));
        }
    }

    public Object getParameter(Parameter parameter) {
        if (parameter == null) {
            throw new IllegalArgumentException("Parameter is null");
        }
        if (executable == null) {
            throw new IllegalStateException("Executable is null");
        }
        if (!parameter.getDeclaringExecutable().equals(executable)) {
            throw new IllegalArgumentException("Parameter is not valid for executable");
        }

        List<GraphParameter> parameterAnnotations = getGraphAnnotationsForParameter(parameter);

        Optional<GraphParameter> supplierAnnotation = parameterAnnotations.stream()
                .filter(parameterAnnotation -> !parameterAnnotation.contextSupplier().equals(GraphContext.SupplierType.USER_SPECIFIED))
                .findFirst();

        if (supplierAnnotation.isPresent()) {
            return "";
        }

        return parameterValues.get(parameter.getName());
    }

    public abstract Class<? extends Mask<?, ?>> getResultClass();

    private List<GraphParameter> getGraphAnnotationsForParameter(Parameter parameter) {
        return Arrays.stream(executable.getAnnotationsByType(GraphParameter.class))
                .filter(annotation -> parameter.getName().equals(annotation.name()))
                .collect(Collectors.toList());
    }

    protected Object getParameterFinalValue(Parameter parameter, GraphContext graphContext) throws InvocationTargetException, IllegalAccessException, InstantiationException {
        if (executable == null) {
            throw new IllegalStateException("Executable is null");
        }

        List<GraphParameter> parameterAnnotations = getGraphAnnotationsForParameter(parameter);

        Optional<GraphParameter> supplierAnnotation = parameterAnnotations.stream()
                .filter(parameterAnnotation -> !parameterAnnotation.contextSupplier().equals(GraphContext.SupplierType.USER_SPECIFIED))
                .findFirst();

        if (supplierAnnotation.isPresent()) {
            return graphContext.getSuppliedValue(supplierAnnotation.get().contextSupplier());
        }

        Object rawValue = parameterValues.get(parameter.getName());

        if (rawValue == null && parameterAnnotations.stream()
                .noneMatch(annotation -> parameter.getName().equals(annotation.name()) && annotation.nullable())) {
            throw new IllegalArgumentException(String.format("Parameter is null: parameter=%s", parameter.getName()));
        }

        if (rawValue instanceof MaskGraphVertex) {
            return ((MaskGraphVertex<?>) rawValue).prepareResult(graphContext);
        }

        return rawValue;
    }

    public void setParameter(String parameterName, Object value) {
        if (executable == null) {
            throw new IllegalStateException("Executable is not set");
        }

        if (value == null) {
            clearParameter(parameterName);
            return;
        }

        Parameter parameter = Arrays.stream(executable.getParameters())
                .filter(param -> param.getName().equals(parameterName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Parameter name does not match any parameter: parameterName=%s, validParameters=[%s]",
                        parameterName,
                        Arrays.stream(executable.getParameters()).map(param -> String.format("%s", param.getName())).collect(Collectors.joining(","))))
                );

        Object parameterValue;

        if (value instanceof String) {
            parameterValue = stringToParameterType(parameter, (String) value);
            if (parameterValue == null) {
                clearParameter(parameterName);
                return;
            }
        } else {
            parameterValue = value;
        }

        if (isParameterCompatible(parameter, parameterValue)) {
            parameterValues.put(parameterName, parameterValue);
        } else {
            throw new IllegalArgumentException(
                    String.format("Parameter does not match the required constructor: parameterName=%s, parameterValueClass=%s, validParameters=[%s]",
                            parameterName,
                            parameterValue.getClass().getSimpleName(),
                            String.format("%s->%s", parameter.getName(), parameter.getType().getSimpleName())
                    )
            );
        }
        checkFullyDefined();
    }

    private Object stringToParameterType(Parameter parameter, String value) {
        Class<?> parameterClass = MaskReflectUtil.getActualParameterClass(executorClass, parameter);

        Object transformedValue;
        if (value.isBlank()) {
            return null;
        }

        if (parameterClass.equals(Integer.class) || parameterClass.equals(int.class)) {
            transformedValue = Integer.parseInt(value);
        } else if (parameterClass.equals(Float.class) || parameterClass.equals(float.class)) {
            transformedValue = Float.parseFloat(value);
        } else if (parameterClass.equals(Short.class) || parameterClass.equals(short.class)) {
            transformedValue = Float.parseFloat(value);
        } else if (parameterClass.equals(Boolean.class) || parameterClass.equals(boolean.class)) {
            transformedValue = Boolean.parseBoolean(value);
        } else if (parameterClass.equals(Long.class) || parameterClass.equals(long.class)) {
            transformedValue = Long.parseLong(value);
        } else if (parameterClass.equals(SymmetryType.class)) {
            transformedValue = SymmetryType.valueOf(value.toUpperCase());
        } else {
            transformedValue = value;
        }
        return transformedValue;
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
        checkFullyDefined();
    }

    protected void checkFullyDefined() {
        fullyDefined = Arrays.stream(executable.getParameters()).allMatch(param -> parameterValues.containsKey(param.getName())
                || Arrays.stream(executable.getAnnotationsByType(GraphParameter.class)).filter(annotation -> param.getName().equals(annotation.name()))
                .anyMatch(annotation -> annotation.nullable() || !annotation.value().equals("") || !annotation.contextSupplier().equals(GraphContext.SupplierType.USER_SPECIFIED)));
    }

    public void clearParameterValues() {
        parameterValues.clear();
    }

    public Mask<?, ?> prepareResult(GraphContext graphContext) throws InvocationTargetException, IllegalAccessException, InstantiationException {
        if (executable == null) {
            throw new IllegalStateException("Executable is null");
        }
        if (!fullyDefined) {
            throw new IllegalStateException("Cannot get result all parameters are not fully defined");
        }
        if (result == null) {
            result = call(graphContext);
            immutableResult = result.mock();
            entry = Pipeline.getMostRecentEntryForMask(result).orElse(null);
        }
        return immutableResult;
    }

    public void resetResult() {
        result = null;
        entry = null;
    }

    private boolean isParameterCompatible(Parameter parameter, Object value) {
        Class<?> parameterClass = MaskReflectUtil.getActualParameterClass(executorClass, parameter);
        Class<?> valueClass = value.getClass();

        if (parameterClass.isAssignableFrom(valueClass)) {
            return true;
        }

        if (parameterClass.isPrimitive()) {
            if (parameterClass.equals(boolean.class)) {
                return valueClass == Boolean.class;
            } else if (parameterClass.equals(float.class)) {
                return valueClass == Float.class;
            } else if (parameterClass.equals(int.class)) {
                return valueClass == Integer.class;
            } else if (parameterClass.equals(long.class)) {
                return valueClass == Long.class;
            }
        }

        return MaskGraphVertex.class.isAssignableFrom(valueClass) && ((MaskGraphVertex<?>) value).getResultClass().equals(parameterClass);
    }
}
