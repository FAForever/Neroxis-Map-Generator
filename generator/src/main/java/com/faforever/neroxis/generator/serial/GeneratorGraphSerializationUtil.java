package com.faforever.neroxis.generator.serial;

import com.faforever.neroxis.generator.graph.GeneratorPipeline;
import com.faforever.neroxis.graph.domain.MaskGraphVertex;
import com.faforever.neroxis.graph.domain.MaskMethodEdge;
import com.faforever.neroxis.graph.domain.MaskVertexResult;
import com.faforever.neroxis.graph.io.JsonGraphMapUtils;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.FileUtil;
import com.faforever.neroxis.util.MaskGraphReflectUtil;
import com.faforever.neroxis.util.serial.graph.JsonGraph;
import com.faforever.neroxis.util.serial.graph.JsonGraphEdge;
import com.faforever.neroxis.util.serial.graph.JsonGraphVertex;
import org.jgrapht.Graph;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class GeneratorGraphSerializationUtil {
    public static GeneratorPipeline importPipeline(File file) throws IOException {
        if (file.getName().endsWith(".pipe")) {
            return GeneratorGraphSerializationUtil.importPipelineJson(new FileInputStream(file));
        } else {
            throw new UnsupportedOperationException("Unsupported file type");
        }
    }

    public static void exportPipeline(GeneratorPipeline pipeline, File file) throws IOException {
        if (file.getName().endsWith(".pipe")) {
            GeneratorGraphSerializationUtil.exportPipelineJson(pipeline, new FileOutputStream(file));
        } else {
            throw new UnsupportedOperationException("Unsupported file type");
        }
    }

    private static void exportPipelineJson(GeneratorPipeline pipeline, OutputStream outputStream) {
        DebugUtil.timedRun("Pipeline Export", () -> {
            Graph<MaskGraphVertex<?>, MaskMethodEdge> graph = pipeline.getGraph();
            Map<MaskGraphVertex<?>, Integer> vertexIdMap = new HashMap<>();
            Map<Integer, JsonGraphVertex> jsonVertexIdMap = new HashMap<>();
            int id = 0;
            for (MaskGraphVertex<?> vertex : graph.vertexSet()) {
                int index = id++;
                vertexIdMap.put(vertex, index);
                jsonVertexIdMap.put(index, JsonGraphMapUtils.map(vertex));
            }

            Map<JsonGraphEdge, JsonGraph.SourceTarget> jsonEdgeMap = new HashMap<>();
            for (MaskMethodEdge edge : graph.edgeSet()) {
                JsonGraph.SourceTarget sourceTarget = new JsonGraph.SourceTarget(vertexIdMap.get(edge.getSource()),
                                                                                 vertexIdMap.get(edge.getTarget()));
                jsonEdgeMap.put(JsonGraphMapUtils.map(edge), sourceTarget);
            }

            Map<String, Integer> endpointMap = pipeline.getEndpointMap()
                                                       .entrySet()
                                                       .stream()
                                                       .collect(Collectors.toMap(Map.Entry::getKey,
                                                                                 entry -> vertexIdMap.get(
                                                                                         entry.getValue())));

            try {
                FileUtil.serialize(outputStream, new JsonPipeline(jsonVertexIdMap, jsonEdgeMap,
                                                                  pipeline.getClass().getCanonicalName(), endpointMap));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static GeneratorPipeline importPipelineJson(InputStream inputStream) {
        return DebugUtil.timedRun("Pipeline Import", () -> {
            try {
                JsonPipeline jsonPipeline = FileUtil.deserialize(inputStream, JsonPipeline.class);
                DirectedAcyclicGraph<MaskGraphVertex<?>, MaskMethodEdge> graph = new DirectedAcyclicGraph<>(
                        MaskMethodEdge.class);
                Collector<Map.Entry<Integer, JsonGraphVertex>, ?, Map<Integer, MaskGraphVertex<?>>> entryMapCollector = Collectors.toMap(
                        Map.Entry::getKey, entry -> {
                            try {
                                return JsonGraphMapUtils.map(entry.getValue());
                            } catch (ClassNotFoundException | NoSuchMethodException e) {
                                throw new RuntimeException(e);
                            }
                        });
                Map<Integer, MaskGraphVertex<?>> idVertexMap = jsonPipeline.getVertices()
                                                                           .entrySet()
                                                                           .stream()
                                                                           .collect(entryMapCollector);
                idVertexMap.values().forEach(graph::addVertex);
                Map<String, MaskGraphVertex<?>> endpointMap = jsonPipeline.getEndpointVertexMap()
                                                                          .entrySet()
                                                                          .stream()
                                                                          .collect(Collectors.toMap(Map.Entry::getKey,
                                                                                                    entry -> idVertexMap.get(
                                                                                                            entry.getValue())));

                jsonPipeline.getEdges().forEach(((jsonGraphEdge, sourceTarget) -> {
                    MaskGraphVertex<?> source = idVertexMap.get(sourceTarget.getSource());
                    MaskGraphVertex<?> target = idVertexMap.get(sourceTarget.getTarget());
                    MaskMethodEdge edge = JsonGraphMapUtils.map(jsonGraphEdge);
                    graph.addEdge(source, target, edge);
                    target.setParameter(edge.getParameterName(),
                                        new MaskVertexResult(edge.getParameterName(), edge.getResultName(), source));
                }));

                Class<? extends GeneratorPipeline> pipelineClass = (Class<? extends GeneratorPipeline>) MaskGraphReflectUtil.getClassFromString(
                        jsonPipeline.getGeneratorClass());

                return GeneratorPipeline.fromGraphAndMap(graph, endpointMap, pipelineClass);
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
