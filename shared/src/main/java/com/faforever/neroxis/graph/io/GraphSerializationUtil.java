package com.faforever.neroxis.graph.io;

import com.faforever.neroxis.graph.domain.MaskGraphVertex;
import com.faforever.neroxis.graph.domain.MaskMethodEdge;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.FileUtil;
import com.faforever.neroxis.util.serial.graph.JsonGraph;
import com.faforever.neroxis.util.serial.graph.JsonGraphEdge;
import com.faforever.neroxis.util.serial.graph.JsonGraphVertex;
import org.jgrapht.Graph;

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

public class GraphSerializationUtil {
    public static void importGraph(Graph<MaskGraphVertex<?>, MaskMethodEdge> graph, File file) throws IOException {
        if (file.getName().endsWith(".graph")) {
            GraphSerializationUtil.importGraphJson(graph, new FileInputStream(file));
        } else {
            throw new UnsupportedOperationException("Unsupported file type");
        }
    }

    public static void exportGraph(Graph<MaskGraphVertex<?>, MaskMethodEdge> graph, File file) throws IOException {
        if (file.getName().endsWith(".graph")) {
            GraphSerializationUtil.exportGraphJson(graph, new FileOutputStream(file));
        } else {
            throw new UnsupportedOperationException("Unsupported file type");
        }
    }

    private static void exportGraphJson(Graph<MaskGraphVertex<?>, MaskMethodEdge> graph, OutputStream outputStream) {
        DebugUtil.timedRun("Graph Export", () -> {
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

            try {
                FileUtil.serialize(outputStream, new JsonGraph(jsonVertexIdMap, jsonEdgeMap));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void importGraphJson(Graph<MaskGraphVertex<?>, MaskMethodEdge> graph, InputStream inputStream) {
        DebugUtil.timedRun("Graph Import", () -> {
            try {
                JsonGraph jsonGraph = FileUtil.deserialize(inputStream, JsonGraph.class);
                Collector<Map.Entry<Integer, JsonGraphVertex>, ?, Map<Integer, MaskGraphVertex<?>>> entryMapCollector = Collectors.toMap(
                        Map.Entry::getKey, entry -> {
                            try {
                                return JsonGraphMapUtils.map(entry.getValue());
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
                    graph.addEdge(source, target, JsonGraphMapUtils.map(jsonGraphEdge));
                }));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
