package com.faforever.neroxis.graph.domain;

import com.faforever.neroxis.mask.MapMaskMethods;
import com.faforever.neroxis.mask.Mask;
import com.faforever.neroxis.ui.GraphParameter;
import com.faforever.neroxis.util.MaskReflectUtil;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract strictfp class MaskGraphVertex<T extends Executable> {
    public static final String SELF = "self";

    protected final Map<String, String> nonMaskParameters = new HashMap<>();
    protected final Map<String, MaskVertexResult> maskParameters = new HashMap<>();
    protected final Map<String, Mask<?, ?>> results = new LinkedHashMap<>();
    protected final Map<String, Mask<?, ?>> immutableResults = new LinkedHashMap<>();
    protected final Map<String, Class<? extends Mask<?, ?>>> resultClasses = new LinkedHashMap<>();
    @Getter
    protected final T executable;
    @Getter
    protected final Class<? extends Mask<?, ?>> executorClass;
    @Getter
    @Setter
    protected String identifier;

    protected MaskGraphVertex(T executable, Class<? extends Mask<?, ?>> executorClass) {
        if (executable != null && (!executable.getDeclaringClass().isAssignableFrom(executorClass) && !executable.getDeclaringClass().equals(MapMaskMethods.class))) {
            throw new IllegalArgumentException("Executable not runnable by executor class");
        }

        this.executable = executable;
        this.executorClass = executorClass;
        results.put(SELF, null);
        resultClasses.put(SELF, executorClass);
        if (executable != null) {
            Arrays.stream(this.executable.getAnnotationsByType(GraphParameter.class))
                    .filter(parameterAnnotation -> !parameterAnnotation.value().equals(""))
                    .forEach(parameterAnnotation -> setParameter(parameterAnnotation.name(), parameterAnnotation.value()));
        }
    }

    public abstract String getExecutableName();

    public abstract Class<? extends Mask<?, ?>> getReturnedClass();

    protected abstract void computeResults(GraphContext graphContext) throws InvocationTargetException, IllegalAccessException, InstantiationException;

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

        if (Mask.class.isAssignableFrom(MaskReflectUtil.getActualTypeClass(executorClass, parameter.getParameterizedType()))) {
            return Optional.ofNullable(maskParameters.get(parameter.getName()))
                    .map(MaskVertexResult::getSourceVertex)
                    .map(MaskGraphVertex::getIdentifier)
                    .orElse(null);
        }

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

        if (Mask.class.isAssignableFrom(MaskReflectUtil.getActualTypeClass(executorClass, parameter.getParameterizedType()))) {
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

        return graphContext.getValue(rawValue, MaskReflectUtil.getActualTypeClass(executorClass, parameter.getParameterizedType()));
    }

    public void setParameter(String parameterName, Object value) {
        if (executable == null) {
            throw new IllegalStateException("Executable is not set");
        }

        if (parameterName == null && value == null) {
            return;
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

        if (Mask.class.isAssignableFrom(MaskReflectUtil.getActualTypeClass(executorClass, parameter.getParameterizedType())) && MaskVertexResult.class.isAssignableFrom(value.getClass())) {
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
