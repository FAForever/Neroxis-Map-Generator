package com.faforever.neroxis.generator.graph;

import com.faforever.neroxis.generator.GeneratorGraphContext;
import com.faforever.neroxis.graph.domain.MaskGraphVertex;
import com.faforever.neroxis.graph.domain.MaskMethodEdge;
import lombok.Getter;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.util.Map;

@Getter
public class GeneralPipeline extends GeneratorPipeline {
    private GeneralPipeline(DirectedAcyclicGraph<MaskGraphVertex<?>, MaskMethodEdge> graph) {
        super(graph);
    }

    public static GeneralPipeline createNew() {
        DirectedAcyclicGraph<MaskGraphVertex<?>, MaskMethodEdge> graph = new DirectedAcyclicGraph<>(
                MaskMethodEdge.class);
        return new GeneralPipeline(graph);
    }

    public static GeneralPipeline fromGraphAndMap(DirectedAcyclicGraph<MaskGraphVertex<?>, MaskMethodEdge> graph,
                                                  Map<String, MaskGraphVertex<?>> vertexMap) {
        return new GeneralPipeline(graph);
    }

    @Override
    public Map<String, MaskGraphVertex<?>> getEndpointMap() {
        return Map.of();
    }

    @Override
    protected void finalizePipeline(GeneratorGraphContext graphContext) {}

    @Override
    protected void initializePipeline(GeneratorGraphContext graphContext) {}
}
