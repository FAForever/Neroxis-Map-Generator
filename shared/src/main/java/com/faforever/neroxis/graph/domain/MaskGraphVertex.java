package com.faforever.neroxis.graph.domain;

import com.faforever.neroxis.annotations.GraphParameter;
import com.faforever.neroxis.graph.GraphContext;
import com.faforever.neroxis.mask.MapMaskMethods;
import com.faforever.neroxis.mask.Mask;
import com.faforever.neroxis.util.MaskGraphReflectUtil;
import lombok.Getter;
import lombok.Setter;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelParseException;

import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;

public abstract class MaskGraphVertex<T extends Executable> {
    public static final String SELF = "self";
    protected final Map<String, String> nonMaskParameters = new LinkedHashMap<>();
    protected final Map<String, MaskVertexResult> maskParameters = new LinkedHashMap<>();
    protected final Map<String, Mask<?, ?>> results = new LinkedHashMap<>();
    protected final Map<String, Mask<?, ?>> immutableResults = new LinkedHashMap<>();
    protected final Map<String, Class<? extends Mask<?, ?>>> resultClasses = new LinkedHashMap<>();
    protected final GraphParameter[] annotatedParameters;
    @Getter
    protected final T executable;
    @Getter
    protected final Class<? extends Mask<?, ?>> executorClass;
    @Getter
    @Setter
    protected String identifier;

    protected MaskGraphVertex(T executable, Class<? extends Mask<?, ?>> executorClass) {
        if (executable == null || (!executable.getDeclaringClass().isAssignableFrom(executorClass)
                                   && !executable.getDeclaringClass().equals(MapMaskMethods.class)
                                   && !executable.getDeclaringClass().equals(MaskOutputVertex.class)
                                   && !executable.getDeclaringClass().equals(MaskInputVertex.class))) {
            throw new IllegalArgumentException("Executable not runnable by executor class");
        }
        this.executable = executable;
        this.executorClass = executorClass;
        results.put(SELF, null);
        resultClasses.put(SELF, executorClass);
        annotatedParameters = MaskGraphReflectUtil.getGraphParameterAnnotations(executable);
        Arrays.stream(annotatedParameters)
              .filter(parameterAnnotation -> !parameterAnnotation.value().isBlank())
              .forEach(parameterAnnotation -> setParameter(parameterAnnotation.name(), parameterAnnotation.value()));
        Arrays.stream(executable.getParameters()).forEach(parameter -> {
            Class<?> parameterClass = getParameterClass(parameter);
            if (Mask.class.isAssignableFrom(parameterClass)) {
                maskParameters.put(parameter.getName(), null);
            } else {
                nonMaskParameters.put(parameter.getName(), null);
            }
        });
    }

    public void setParameter(String parameterName, Object value) {
        if (parameterName == null && value == null) {
            return;
        }
        if (value == null) {
            clearParameter(parameterName);
            return;
        }
        Optional<Parameter> parameter = Arrays.stream(executable.getParameters())
                                              .filter(param -> param.getName().equals(parameterName))
                                              .findFirst();
        if (parameter.isEmpty()) {
            return;
        }
        if (Mask.class.isAssignableFrom(getParameterClass(parameter.get())) && MaskVertexResult.class.isAssignableFrom(
                value.getClass())) {
            maskParameters.put(parameter.get().getName(), (MaskVertexResult) value);
            return;
        }
        nonMaskParameters.put(parameterName, (String) value);
    }

    public void clearParameter(String parameterName) {
        Parameter parameter = Arrays.stream(executable.getParameters())
                                    .filter(param -> param.getName().equals(parameterName))
                                    .findFirst()
                                    .orElseThrow(() -> new IllegalArgumentException(String.format(
                                            "Parameter name does not match any parameter: parameterName=%s, validParameters=[%s]",
                                            parameterName, Arrays.stream(executable.getParameters())
                                                                 .map(param -> String.format("%s", param.getName()))
                                                                 .collect(Collectors.joining(",")))));
        if (Mask.class.isAssignableFrom(getParameterClass(parameter))) {
            maskParameters.put(parameterName, null);
            return;
        }
        nonMaskParameters.put(parameterName, null);
    }

    public boolean isMaskParameterNull(String parameter) {
        return maskParameters.get(parameter) == null;
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

        if (Mask.class.isAssignableFrom(getParameterClass(parameter))) {
            return Optional.ofNullable(maskParameters.get(parameter.getName()))
                           .map(MaskVertexResult::getSourceVertex)
                           .map(MaskGraphVertex::getIdentifier)
                           .orElse(null);
        }

        return getGraphAnnotationForParameter(parameter).filter(
                                                                parameterAnnotation -> !parameterAnnotation.value().isBlank())
                                                        .map(GraphParameter::value)
                                                        .orElse(nonMaskParameters.get(parameter.getName()));
    }

    public Optional<GraphParameter> getGraphAnnotationForParameter(Parameter parameter) {
        return Arrays.stream(annotatedParameters)
                     .filter(annotation -> parameter.getName().equals(annotation.name()))
                     .findFirst();
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

    public Class<? extends Mask<?, ?>> getMaskParameterClass(String parameterName) {
        return (Class<? extends Mask<?, ?>>) Arrays.stream(executable.getParameters())
                                                   .filter(parameter -> parameter.getName().equals(parameterName))
                                                   .map(this::getParameterClass)
                                                   .filter(clazz -> Mask.class.isAssignableFrom(clazz))
                                                   .findFirst()
                                                   .orElse(null);
    }

    private Class<?> getParameterClass(Parameter parameter) {
        Class<?> parameterClass;
        if (Mask.class.isAssignableFrom(executable.getDeclaringClass())) {
            parameterClass = MaskGraphReflectUtil.getActualTypeClass(executorClass, parameter.getParameterizedType());
        } else {
            parameterClass = parameter.getType();
        }
        return parameterClass;
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

    protected Object getParameterFinalValue(Parameter parameter,
                                            GraphContext graphContext) throws GraphComputationException {
        if (executable == null) {
            throw new GraphComputationException("Executable is null");
        }
        if (Mask.class.isAssignableFrom(getParameterClass(parameter))) {
            return maskParameters.get(parameter.getName()).getResult();
        }

        Optional<GraphParameter> possibleParameterAnnotation = getGraphAnnotationForParameter(parameter);

        String rawValue = possibleParameterAnnotation.filter(
                                                             parameterAnnotation -> !parameterAnnotation.value().isBlank())
                                                     .map(GraphParameter::value)
                                                     .orElse(nonMaskParameters.get(parameter.getName()));

        if (rawValue == null || rawValue.isBlank()) {
            if (possibleParameterAnnotation.map(GraphParameter::nullable).orElse(false)) {
                return null;
            }

            throw new GraphComputationException(
                    String.format("Parameter is null: parameter=%s, identifier=%s, method=%s", parameter.getName(),
                                  identifier, getExecutableName()));
        }

        try {
            return graphContext.getValue(rawValue, identifier, getParameterClass(parameter));
        } catch (SpelParseException e) {
            throw new GraphComputationException(
                    String.format("Error parsing parameter: parameter=%s, identifier=%s, method=%s",
                                  parameter.getName(), identifier, getExecutableName()), e);
        }
    }

    public abstract String getExecutableName();

    public void clearParameterValues() {
        nonMaskParameters.clear();
    }

    public void prepareResults(GraphContext graphContext,
                               boolean failOnUndefined) throws InvocationTargetException, IllegalAccessException, InstantiationException {
        if (!isDefined(graphContext)) {
            if (!failOnUndefined) {
                return;
            }

            throw new IllegalStateException("Cannot get result all parameters are not fully defined");
        }
        if (!isComputed()) {
            computeResults(graphContext);
            results.forEach((key, value) -> immutableResults.put(key, value.immutableCopy()));
        }
    }

    public boolean isComputed() {
        return results.values().stream().noneMatch(Objects::isNull);
    }

    public boolean isDefined(GraphContext graphContext) {
        return Arrays.stream(executable.getParameters())
                     .allMatch(parameter -> isParameterWellDefined(parameter, graphContext));
    }

    private boolean isParameterWellDefined(Parameter parameter, GraphContext graphContext) {
        if (executable == null) {
            return false;
        }
        if (Mask.class.isAssignableFrom(getParameterClass(parameter))) {
            return maskParameters.get(parameter.getName()) != null;
        }
        Optional<GraphParameter> possibleParameterAnnotation = getGraphAnnotationForParameter(parameter);
        String rawValue = possibleParameterAnnotation.filter(
                                                             parameterAnnotation -> !parameterAnnotation.value().isEmpty())
                                                     .map(GraphParameter::value)
                                                     .orElse(nonMaskParameters.get(parameter.getName()));

        if (rawValue == null || rawValue.isBlank()) {
            return possibleParameterAnnotation.map(GraphParameter::nullable).orElse(false);
        }

        try {
            graphContext.getValue(rawValue, identifier, getParameterClass(parameter));
            return true;
        } catch (SpelParseException | SpelEvaluationException e) {
            return false;
        }
    }

    protected abstract void computeResults(
            GraphContext graphContext) throws InvocationTargetException, IllegalAccessException, InstantiationException;

    public List<String> getMaskParameters() {
        return List.copyOf(maskParameters.keySet());
    }

    public void resetResult() {
        results.entrySet().forEach(entry -> entry.setValue(null));
        immutableResults.entrySet().forEach(entry -> entry.setValue(null));
    }

    public String toString() {
        return (identifier == null ? "" : identifier) + " " + getExecutableName();
    }

    public abstract MaskGraphVertex<T> copy();
}
