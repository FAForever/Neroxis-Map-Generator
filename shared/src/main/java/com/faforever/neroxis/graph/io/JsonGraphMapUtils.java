package com.faforever.neroxis.graph.io;

import com.faforever.neroxis.graph.domain.MapMaskMethodVertex;
import com.faforever.neroxis.graph.domain.MaskConstructorVertex;
import com.faforever.neroxis.graph.domain.MaskGraphVertex;
import com.faforever.neroxis.graph.domain.MaskInputVertex;
import com.faforever.neroxis.graph.domain.MaskMethodEdge;
import com.faforever.neroxis.graph.domain.MaskMethodVertex;
import com.faforever.neroxis.graph.domain.MaskOutputVertex;
import com.faforever.neroxis.mask.MapMaskMethods;
import com.faforever.neroxis.mask.Mask;
import com.faforever.neroxis.util.MaskGraphReflectUtil;
import com.faforever.neroxis.util.serial.graph.JsonGraphEdge;
import com.faforever.neroxis.util.serial.graph.JsonGraphVertex;

import java.lang.reflect.Parameter;
import java.util.List;
import java.util.stream.Collectors;

public class JsonGraphMapUtils {
    public static JsonGraphVertex map(MaskGraphVertex<?> vertex) {
        List<Parameter> parameters = List.of(vertex.getExecutable().getParameters());
        return new JsonGraphVertex(vertex.getIdentifier(), vertex.getClass().getName(),
                                   vertex.getExecutorClass().getName(), parameters.stream()
                                                                                  .map(Parameter::getType)
                                                                                  .map(Class::getName)
                                                                                  .collect(Collectors.toList()),
                                   parameters.stream().map(vertex::getParameterExpression).collect(Collectors.toList()),
                                   vertex.getExecutableName());
    }

    public static JsonGraphEdge map(MaskMethodEdge edge) {
        return new JsonGraphEdge(edge.getResultName(), edge.getParameterName());
    }

    public static MaskGraphVertex<?> map(
            JsonGraphVertex jsonGraphVertex) throws ClassNotFoundException, NoSuchMethodException {
        Class<? extends MaskGraphVertex<?>> vertexClass = (Class<? extends MaskGraphVertex<?>>) MaskGraphReflectUtil.getClassFromString(
                jsonGraphVertex.getClazz());
        Class<? extends Mask<?, ?>> maskClass = (Class<? extends Mask<?, ?>>) MaskGraphReflectUtil.getClassFromString(
                jsonGraphVertex.getMaskClass());
        int parameterCount = jsonGraphVertex.getParameterClasses().size();
        Class<?>[] parameterTypes = new Class[parameterCount];
        String[] parameterValues = new String[parameterCount];
        for (int i = 0; i < parameterCount; ++i) {
            parameterTypes[i] = MaskGraphReflectUtil.getClassFromString(jsonGraphVertex.getParameterClasses().get(i));
            parameterValues[i] = jsonGraphVertex.getParameterValues().get(i);
        }

        MaskGraphVertex<?> vertex;
        if (MaskConstructorVertex.class.equals(vertexClass)) {
            vertex = new MaskConstructorVertex(maskClass.getConstructor(parameterTypes));
        } else if (MaskMethodVertex.class.equals(vertexClass)) {
            vertex = new MaskMethodVertex(maskClass.getMethod(jsonGraphVertex.getExecutable(), parameterTypes),
                                          maskClass);
        } else if (MapMaskMethodVertex.class.equals(vertexClass)) {
            vertex = new MapMaskMethodVertex(
                    MapMaskMethods.class.getMethod(jsonGraphVertex.getExecutable(), parameterTypes));
        } else if (MaskOutputVertex.class.equals(vertexClass)) {
            vertex = new MaskOutputVertex(jsonGraphVertex.getExecutable(), maskClass);
        } else if (MaskInputVertex.class.equals(vertexClass)) {
            vertex = new MaskInputVertex(jsonGraphVertex.getExecutable(), maskClass);
        } else {
            throw new IllegalArgumentException(String.format("Unrecognized vertex class: %s", vertexClass.getName()));
        }

        Parameter[] parameters = vertex.getExecutable().getParameters();
        for (int i = 0; i < parameterCount; ++i) {
            if (parameterValues[i] != null) {
                vertex.setParameter(parameters[i].getName(), parameterValues[i]);
            }
        }

        vertex.setIdentifier(jsonGraphVertex.getIdentifier());

        return vertex;
    }

    public static MaskMethodEdge map(JsonGraphEdge edge) {
        return new MaskMethodEdge(edge.getResultName(), edge.getParameterName());
    }
}
