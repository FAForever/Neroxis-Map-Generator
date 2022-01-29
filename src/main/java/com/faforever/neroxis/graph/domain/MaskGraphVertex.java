package com.faforever.neroxis.graph.domain;

import com.faforever.neroxis.mask.Mask;
import com.faforever.neroxis.ui.GraphParameter;
import com.faforever.neroxis.util.MaskReflectUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public abstract strictfp class MaskGraphVertex<T extends Executable> {
    public static final String SELF = "self";

    protected final Map<String, String> nonMaskParameters = new HashMap<>();
    protected final Map<String, MaskVertexResult> maskParameters = new HashMap<>();
    protected final Map<String, Mask<?, ?>> results = new LinkedHashMap<>();
    protected final Map<String, Mask<?, ?>> immutableResults = new LinkedHashMap<>();
    protected final Map<String, Class<? extends Mask<?, ?>>> resultClasses = new LinkedHashMap<>();
    @Getter
    protected T executable;
    @Getter
    protected Class<? extends Mask<?, ?>> executorClass;

    protected abstract void computeResults(GraphContext graphContext) throws InvocationTargetException, IllegalAccessException, InstantiationException;

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
        nonMaskParameters.clear();
        results.clear();
        if (executable != null) {
            Arrays.stream(executable.getAnnotationsByType(GraphParameter.class))
                    .filter(parameterAnnotation -> !parameterAnnotation.value().equals(""))
                    .forEach(parameterAnnotation -> setParameter(parameterAnnotation.name(), parameterAnnotation.value()));
        }
        results.put(SELF, null);
        resultClasses.put(SELF, executorClass);
    }

    public String getParameterExpression(Parameter parameter) {
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

        return parameterAnnotations.stream()
                .filter(parameterAnnotation -> !parameterAnnotation.value().isEmpty())
                .findFirst()
                .map(GraphParameter::value)
                .orElse(nonMaskParameters.get(parameter.getName()));
    }

    public Mask<?, ?> getResult(String resultName) {
        if (!isComputed()) {
            throw new IllegalStateException("Cannot get result, not yet computed");
        }
        if (!results.containsKey(resultName)) {
            throw new IllegalArgumentException("Result name not recognized");
        }
        return results.get(resultName);
    }

    public Mask<?, ?> getImmutableResult(String resultName) {
        if (!isComputed()) {
            throw new IllegalStateException("Cannot get result, not yet computed");
        }
        if (!immutableResults.containsKey(resultName)) {
            throw new IllegalArgumentException("Result name not recognized");
        }
        return immutableResults.get(resultName);
    }

    public List<String> getResultNames() {
        return new ArrayList<>(results.keySet());
    }

    public Class<? extends Mask<?, ?>> getResultClass(String resultName) {
        return resultClasses.get(resultName);
    }

    private List<GraphParameter> getGraphAnnotationsForParameter(Parameter parameter) {
        return Arrays.stream(executable.getAnnotationsByType(GraphParameter.class))
                .filter(annotation -> parameter.getName().equals(annotation.name()))
                .collect(Collectors.toList());
    }

    protected Object getParameterFinalValue(Parameter parameter, GraphContext graphContext) throws InvocationTargetException, IllegalAccessException, InstantiationException {
        if (executable == null) {
            throw new IllegalStateException("Executable is null");
        }

        if (Mask.class.isAssignableFrom(MaskReflectUtil.getActualParameterClass(executorClass, parameter))) {
            return maskParameters.get(parameter.getName()).getResult();
        }

        List<GraphParameter> parameterAnnotations = getGraphAnnotationsForParameter(parameter);

        String rawValue = parameterAnnotations.stream()
                .filter(parameterAnnotation -> !parameterAnnotation.value().isEmpty())
                .findFirst()
                .map(GraphParameter::value)
                .orElse(nonMaskParameters.get(parameter.getName()));

        if (rawValue == null && parameterAnnotations.stream()
                .noneMatch(annotation -> parameter.getName().equals(annotation.name()) && annotation.nullable())) {
            throw new IllegalArgumentException(String.format("Parameter is null: parameter=%s", parameter.getName()));
        }

        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        return graphContext.getValue(rawValue, MaskReflectUtil.getActualParameterClass(executorClass, parameter));
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

        if (Mask.class.isAssignableFrom(MaskReflectUtil.getActualParameterClass(executorClass, parameter)) && MaskVertexResult.class.isAssignableFrom(value.getClass())) {
            maskParameters.put(parameter.getName(), (MaskVertexResult) value);
            return;
        }

        nonMaskParameters.put(parameterName, (String) value);
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
        nonMaskParameters.remove(parameterName);
    }

    public boolean isNotDefined() {
        return !Arrays.stream(executable.getParameters()).allMatch(param -> nonMaskParameters.containsKey(param.getName())
                || maskParameters.containsKey(param.getName())
                || Arrays.stream(executable.getAnnotationsByType(GraphParameter.class)).filter(annotation -> param.getName().equals(annotation.name()))
                .anyMatch(annotation -> annotation.nullable() || !annotation.value().equals("")));
    }

    public boolean isComputed() {
        return results.values().stream().noneMatch(Objects::isNull);
    }

    public void clearParameterValues() {
        nonMaskParameters.clear();
    }

    public void prepareResults(GraphContext graphContext) throws InvocationTargetException, IllegalAccessException, InstantiationException {
        if (executable == null) {
            throw new IllegalStateException("Executable is null");
        }
        if (isNotDefined()) {
            throw new IllegalStateException("Cannot get result all parameters are not fully defined");
        }
        if (!isComputed()) {
            computeResults(graphContext);
            results.forEach((key, value) -> immutableResults.put(key, value.mock()));
        }
    }

    public void resetResult() {
        results.entrySet().forEach(entry -> entry.setValue(null));
        immutableResults.entrySet().forEach(entry -> entry.setValue(null));
    }
}
