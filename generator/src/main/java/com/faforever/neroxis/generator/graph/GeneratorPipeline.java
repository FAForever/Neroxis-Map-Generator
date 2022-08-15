package com.faforever.neroxis.generator.graph;

import com.faforever.neroxis.generator.GeneratorGraphContext;
import com.faforever.neroxis.graph.domain.MaskGraphVertex;
import com.faforever.neroxis.graph.domain.MaskMethodEdge;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jgrapht.graph.DirectedAcyclicGraph;

@RequiredArgsConstructor
@Getter
public abstract class GeneratorPipeline {
    protected final DirectedAcyclicGraph<MaskGraphVertex<?>, MaskMethodEdge> graph;

    public static List<Class<? extends GeneratorPipeline>> getPipelineTypes() {
        return List.of(GeneralPipeline.class, TerrainPipeline.class, TexturePipeline.class, PropPipeline.class,
                       ResourcePipeline.class, WreckPipeline.class, DecalPipeline.class, CivilianPipeline.class);
    }

    public static <T extends GeneratorPipeline> T fromGraphAndMap(
            DirectedAcyclicGraph<MaskGraphVertex<?>, MaskMethodEdge> graph, Map<String, MaskGraphVertex<?>> endpointMap,
            Class<T> pipelineClass) {
        try {
            return (T) pipelineClass.getDeclaredMethod("fromGraphAndMap", DirectedAcyclicGraph.class, Map.class)
                                    .invoke(null, graph, endpointMap);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalArgumentException("Unknown class or does not contain a createNew method");
        }
    }

    public static <T extends GeneratorPipeline> T createNew(Class<T> pipelineClass) {
        try {
            return (T) pipelineClass.getDeclaredMethod("createNew").invoke(null);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalArgumentException("Unknown class or does not contain a createNew method");
        }
    }

    public abstract Map<String, MaskGraphVertex<?>> getEndpointMap();

    protected abstract void finalizePipeline(GeneratorGraphContext graphContext);

    protected abstract void initializePipeline(GeneratorGraphContext graphContext);

    public void setupPipeline(GeneratorGraphContext graphContext) {
        initializePipeline(graphContext);
        graph.forEach(vertex -> {
            try {
                vertex.prepareResults(graphContext, true);
            } catch (InvocationTargetException | IllegalAccessException | InstantiationException e) {
                throw new RuntimeException(e);
            }
        });
        finalizePipeline(graphContext);
    }

    protected void verifyDefined(GeneratorGraphContext graphContext, MaskGraphVertex<?>... vertices) {
        List<MaskGraphVertex<?>> undefined = Arrays.stream(vertices)
                                                   .filter(vertex -> !vertex.isDefined(graphContext))
                                                   .collect(Collectors.toList());
        if (!undefined.isEmpty()) {
            throw new IllegalStateException("The following necessary vertices are undefined: " + undefined);
        }
    }

    protected void verifyContains(MaskGraphVertex<?>... vertices) {
        List<MaskGraphVertex<?>> notContained = Arrays.stream(vertices)
                                                      .filter(vertex -> !graph.containsVertex(vertex))
                                                      .collect(Collectors.toList());
        if (!notContained.isEmpty()) {
            throw new IllegalStateException("Graph does not contain the following vertices: " + notContained);
        }
    }
}
