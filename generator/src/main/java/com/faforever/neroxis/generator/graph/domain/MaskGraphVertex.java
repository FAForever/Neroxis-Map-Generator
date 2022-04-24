package com.faforever.neroxis.generator.graph.domain;

import com.faforever.neroxis.annotations.GraphParameter;
import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.GraphComputationException;
import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.mask.MapMaskMethods;
import com.faforever.neroxis.mask.Mask;
import com.faforever.neroxis.util.MaskReflectUtil;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelParseException;

public abstract strictfp class MaskGraphVertex<T extends Executable> {

    public static final String SELF = "self";
    protected final Map<String, String> nonMaskParameters = new LinkedHashMap<>();
    protected final Map<String, MaskVertexResult> maskParameters = new LinkedHashMap<>();
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
        if (executable == null || (!executable.getDeclaringClass().isAssignableFrom(executorClass)
                                   && !executable.getDeclaringClass().equals(MapMaskMethods.class))) {
            throw new IllegalArgumentException("Executable not runnable by executor class");
        }
        this.executable = executable;
        this.executorClass = executorClass;
        results.put(SELF, null);
        resultClasses.put(SELF, executorClass);
        Arrays.stream(this.executable.getAnnotationsByType(GraphParameter.class))
              .filter(parameterAnnotation -> !parameterAnnotation.value().equals(""))
              .forEach(parameterAnnotation -> setParameter(parameterAnnotation.name(), parameterAnnotation.value()));
        Arrays.stream(executable.getParameters()).forEach(parameter -> {
            if (Mask.class.isAssignableFrom(
                    MaskReflectUtil.getActualTypeClass(executorClass, parameter.getParameterizedType()))) {
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
        if (Mask.class.isAssignableFrom(
                MaskReflectUtil.getActualTypeClass(executorClass, parameter.get().getParameterizedType()))
            && MaskVertexResult.class.isAssignableFrom(value.getClass())) {
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
        if (Mask.class.isAssignableFrom(
                MaskReflectUtil.getActualTypeClass(executorClass, parameter.getParameterizedType()))) {
            maskParameters.put(parameterName, null);
            return;
        }
        nonMaskParameters.put(parameterName, null);
    }

    public boolean isMaskParameterSet(String parameter) {
        return maskParameters.get(parameter) != null;
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

        if (Mask.class.isAssignableFrom(
                MaskReflectUtil.getActualTypeClass(executorClass, parameter.getParameterizedType()))) {
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

    private List<GraphParameter> getGraphAnnotationsForParameter(Parameter parameter) {
        return Arrays.stream(executable.getAnnotationsByType(GraphParameter.class))
                     .filter(annotation -> parameter.getName().equals(annotation.name()))
                     .collect(Collectors.toList());
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
                                                   .map(parameter -> MaskReflectUtil.getActualTypeClass(executorClass,
                                                                                                        parameter.getParameterizedType()))
                                                   .filter(clazz -> Mask.class.isAssignableFrom(clazz))
                                                   .findFirst()
                                                   .orElse(null);
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
        if (Mask.class.isAssignableFrom(
                MaskReflectUtil.getActualTypeClass(executorClass, parameter.getParameterizedType()))) {
            return maskParameters.get(parameter.getName()).getResult();
        }
        List<GraphParameter> parameterAnnotations = getGraphAnnotationsForParameter(parameter);

        String rawValue = parameterAnnotations.stream()
                                              .filter(parameterAnnotation -> !parameterAnnotation.value().isEmpty())
                                              .findFirst()
                                              .map(GraphParameter::value)
                                              .orElse(nonMaskParameters.get(parameter.getName()));
        if ((rawValue == null || rawValue.isBlank()) && parameterAnnotations.stream()
                                                                            .noneMatch(annotation -> parameter.getName()
                                                                                                              .equals(annotation.name())
                                                                                                     && annotation.nullable())) {
            throw new GraphComputationException(
                    String.format("Parameter is null: parameter=%s, identifier=%s, method=%s", parameter.getName(),
                                  identifier, getExecutableName()));
        }
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            return graphContext.getValue(rawValue, identifier, MaskReflectUtil.getActualTypeClass(executorClass,
                                                                                                  parameter.getParameterizedType()));
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

    public void prepareResults(
            GraphContext graphContext) throws InvocationTargetException, IllegalAccessException, InstantiationException {
        if (!isDefined()) {
            throw new IllegalStateException("Cannot get result all parameters are not fully defined");
        }
        if (!isComputed()) {
            computeResults(graphContext);
            results.forEach((key, value) -> immutableResults.put(key, value.mock()));
        }
    }

    public boolean isComputed() {
        return results.values().stream().noneMatch(Objects::isNull);
    }

    public boolean isDefined() {
        GraphContext graphContext = new GraphContext(0L, GeneratorParameters.builder()
                                                                            .terrainSymmetry(Symmetry.POINT2)
                                                                            .mapSize(512)
                                                                            .numTeams(2)
                                                                            .spawnCount(2)
                                                                            .build(),
                                                     ParameterConstraints.builder().build());
        return Arrays.stream(executable.getParameters())
                     .allMatch(parameter -> isParameterWellDefined(parameter, graphContext));
    }

    private boolean isParameterWellDefined(Parameter parameter, GraphContext graphContext) {
        if (executable == null) {
            return false;
        }
        if (Mask.class.isAssignableFrom(
                MaskReflectUtil.getActualTypeClass(executorClass, parameter.getParameterizedType()))) {
            return maskParameters.get(parameter.getName()) != null;
        }
        List<GraphParameter> parameterAnnotations = getGraphAnnotationsForParameter(parameter);
        String rawValue = parameterAnnotations.stream()
                                              .filter(parameterAnnotation -> !parameterAnnotation.value().isEmpty())
                                              .findFirst()
                                              .map(GraphParameter::value)
                                              .orElse(nonMaskParameters.get(parameter.getName()));
        boolean emptyParameter = rawValue == null || rawValue.isBlank();
        if ((emptyParameter) && !parameterNullable(parameter)) {
            return false;
        } else if (emptyParameter) {
            return true;
        }
        try {
            graphContext.getValue(rawValue, identifier,
                                  MaskReflectUtil.getActualTypeClass(executorClass, parameter.getParameterizedType()));
            return true;
        } catch (SpelParseException | SpelEvaluationException e) {
            return false;
        }
    }

    private boolean parameterNullable(Parameter parameter) {
        return getGraphAnnotationsForParameter(parameter).stream()
                                                         .anyMatch(annotation -> annotation.nullable()
                                                                                 || !annotation.value().equals(""));
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

    public abstract MaskGraphVertex<T> copy();
}
