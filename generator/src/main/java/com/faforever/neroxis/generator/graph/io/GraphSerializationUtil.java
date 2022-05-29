package com.faforever.neroxis.generator.graph.io;

import com.faforever.neroxis.generator.graph.domain.MapMaskMethodVertex;
import com.faforever.neroxis.generator.graph.domain.MaskConstructorVertex;
import com.faforever.neroxis.generator.graph.domain.MaskEndpointVertex;
import com.faforever.neroxis.generator.graph.domain.MaskGraphVertex;
import com.faforever.neroxis.generator.graph.domain.MaskMethodEdge;
import com.faforever.neroxis.generator.graph.domain.MaskMethodVertex;
import com.faforever.neroxis.mask.MapMaskMethods;
import com.faforever.neroxis.mask.Mask;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.FileUtil;
import com.faforever.neroxis.util.MaskGraphReflectUtil;
import com.faforever.neroxis.util.serial.graph.JsonGraph;
import com.faforever.neroxis.util.serial.graph.JsonGraphEdge;
import com.faforever.neroxis.util.serial.graph.JsonGraphVertex;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.jgrapht.Graph;

public class GraphSerializationUtil {
    public static void importGraph(Graph<MaskGraphVertex<?>, MaskMethodEdge> graph, File file) throws IOException {
        if (file.getName().endsWith(".json")) {
            GraphSerializationUtil.importGraphJson(graph, new FileInputStream(file));
        } else {
            throw new UnsupportedOperationException("Unsupported file type");
        }
    }

    public static void exportGraph(Graph<MaskGraphVertex<?>, MaskMethodEdge> graph, File file) throws IOException {
        if (file.getName().endsWith(".json")) {
            GraphSerializationUtil.exportGraphJson(graph, new FileOutputStream(file));
        } else {
            throw new UnsupportedOperationException("Unsupported file type");
        }
    }

    private static void exportGraphJson(Graph<MaskGraphVertex<?>, MaskMethodEdge> graph, OutputStream outputStream) {
        DebugUtil.timedRun("Json Export", () -> {
            Map<MaskGraphVertex<?>, Integer> vertexIdMap = new HashMap<>();
            Map<Integer, JsonGraphVertex> jsonVertexIdMap = new HashMap<>();
            int id = 0;
            for (MaskGraphVertex<?> vertex : graph.vertexSet()) {
                int index = id++;
                vertexIdMap.put(vertex, index);
                jsonVertexIdMap.put(index, map(vertex));
            }

            Map<JsonGraphEdge, JsonGraph.SourceTarget> jsonEdgeMap = new HashMap<>();
            for (MaskMethodEdge edge : graph.edgeSet()) {
                JsonGraph.SourceTarget sourceTarget = new JsonGraph.SourceTarget(vertexIdMap.get(edge.getSource()),
                                                                                 vertexIdMap.get(edge.getTarget()));
                jsonEdgeMap.put(map(edge), sourceTarget);
            }

            try {
                FileUtil.serialize(outputStream, new JsonGraph(jsonVertexIdMap, jsonEdgeMap));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void importGraphJson(Graph<MaskGraphVertex<?>, MaskMethodEdge> graph, InputStream inputStream) {
        DebugUtil.timedRun("Json Import", () -> {
            try {
                JsonGraph jsonGraph = FileUtil.deserialize(inputStream, JsonGraph.class);
                Collector<Map.Entry<Integer, JsonGraphVertex>, ?, Map<Integer, MaskGraphVertex<?>>> entryMapCollector = Collectors.toMap(
                        Map.Entry::getKey, entry -> {
                            try {
                                return map(entry.getValue());
                            } catch (ClassNotFoundException | NoSuchMethodException e) {
                                throw new RuntimeException(e);
                            }
                        });
                Map<Integer, MaskGraphVertex<?>> idVertexMap = jsonGraph.getVertices()
                                                                        .entrySet()
                                                                        .stream()
                                                                        .collect(entryMapCollector);
                idVertexMap.values().forEach(graph::addVertex);

                jsonGraph.getEdges().forEach(((jsonGraphEdge, sourceTarget) -> {
                    MaskGraphVertex<?> source = idVertexMap.get(sourceTarget.getSource());
                    MaskGraphVertex<?> target = idVertexMap.get(sourceTarget.getTarget());
                    graph.addEdge(source, target, map(jsonGraphEdge));
                }));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static JsonGraphVertex map(MaskGraphVertex<?> vertex) {
        List<Parameter> parameters = List.of(vertex.getExecutable().getParameters());
        return new JsonGraphVertex(vertex.getIdentifier(), vertex.getClass().getName(),
                                   vertex.getExecutorClass().getName(), parameters.stream()
                                                                                  .map(Parameter::getType)
                                                                                  .map(Class::getName)
                                                                                  .collect(Collectors.toList()),
                                   parameters.stream().map(vertex::getParameterExpression).collect(Collectors.toList()),
                                   vertex.getExecutableName());
    }

    private static JsonGraphEdge map(MaskMethodEdge edge) {
        return new JsonGraphEdge(edge.getResultName(), edge.getParameterName());
    }

    private static MaskGraphVertex<?> map(
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
        } else if (MaskEndpointVertex.class.equals(vertexClass)) {
            vertex = new MaskEndpointVertex(jsonGraphVertex.getExecutable(), maskClass);
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

    private static MaskMethodEdge map(JsonGraphEdge edge) {
        return new MaskMethodEdge(edge.getResultName(), edge.getParameterName());
    }
}
