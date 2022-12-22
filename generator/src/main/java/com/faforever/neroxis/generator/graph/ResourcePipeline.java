package com.faforever.neroxis.generator.graph;

import com.faforever.neroxis.generator.GeneratorGraphContext;
import com.faforever.neroxis.graph.domain.MaskGraphVertex;
import com.faforever.neroxis.graph.domain.MaskInputVertex;
import com.faforever.neroxis.graph.domain.MaskMethodEdge;
import com.faforever.neroxis.graph.domain.MaskOutputVertex;
import com.faforever.neroxis.mask.BooleanMask;
import lombok.Getter;
import lombok.Setter;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.util.Map;

@Getter
public class ResourcePipeline extends GeneratorPipeline {
    public static final String IMPASSABLE = "Impassable";
    public static final String UN_BUILDABLE = "UnBuildable";
    public static final String PASSABLE_LAND = "Passable Land";
    public static final String LAND_RESOURCE = "Land Resource";
    public static final String WATER_RESOURCE = "Water Resource";
    private final MaskInputVertex<BooleanMask> unBuildableVertex;
    private final MaskInputVertex<BooleanMask> passableLandVertex;
    private final MaskOutputVertex<BooleanMask> landResourceVertex;
    private final MaskOutputVertex<BooleanMask> waterResourceVertex;
    @Setter
    private TerrainPipeline terrainPipeline;
    private BooleanMask landResource;
    private BooleanMask waterResource;

    private ResourcePipeline(DirectedAcyclicGraph<MaskGraphVertex<?>, MaskMethodEdge> graph,
                             MaskInputVertex<BooleanMask> unBuildableVertex,
                             MaskInputVertex<BooleanMask> passableLandVertex,
                             MaskOutputVertex<BooleanMask> landResourceVertex,
                             MaskOutputVertex<BooleanMask> waterResourceVertex) {
        super(graph);
        this.unBuildableVertex = unBuildableVertex;
        this.passableLandVertex = passableLandVertex;
        this.landResourceVertex = landResourceVertex;
        this.waterResourceVertex = waterResourceVertex;

        verifyContains(unBuildableVertex, passableLandVertex, waterResourceVertex, landResourceVertex);
    }

    public static ResourcePipeline fromGraphAndMap(DirectedAcyclicGraph<MaskGraphVertex<?>, MaskMethodEdge> graph,
                                                   Map<String, MaskGraphVertex<?>> vertexMap) {
        return new ResourcePipeline(graph, (MaskInputVertex<BooleanMask>) vertexMap.get(UN_BUILDABLE),
                                    (MaskInputVertex<BooleanMask>) vertexMap.get(PASSABLE_LAND),
                                    (MaskOutputVertex<BooleanMask>) vertexMap.get(LAND_RESOURCE),
                                    (MaskOutputVertex<BooleanMask>) vertexMap.get(WATER_RESOURCE));
    }

    public static ResourcePipeline createNew() {
        try {
            DirectedAcyclicGraph<MaskGraphVertex<?>, MaskMethodEdge> graph = new DirectedAcyclicGraph<>(
                    MaskMethodEdge.class);
            MaskInputVertex<BooleanMask> unBuildableVertex = new MaskInputVertex<>(UN_BUILDABLE, BooleanMask.class);
            MaskInputVertex<BooleanMask> passableLandVertex = new MaskInputVertex<>(PASSABLE_LAND, BooleanMask.class);
            MaskOutputVertex<BooleanMask> landResourceVertex = new MaskOutputVertex<>(LAND_RESOURCE, BooleanMask.class);
            MaskOutputVertex<BooleanMask> waterResourceVertex = new MaskOutputVertex<>(WATER_RESOURCE,
                                                                                       BooleanMask.class);
            graph.addVertex(unBuildableVertex);
            graph.addVertex(passableLandVertex);
            graph.addVertex(landResourceVertex);
            graph.addVertex(waterResourceVertex);
            return new ResourcePipeline(graph, unBuildableVertex, passableLandVertex, landResourceVertex,
                                        waterResourceVertex);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, MaskGraphVertex<?>> getEndpointMap() {
        return Map.of(UN_BUILDABLE, unBuildableVertex, PASSABLE_LAND, passableLandVertex, LAND_RESOURCE,
                      landResourceVertex, WATER_RESOURCE, waterResourceVertex);
    }

    @Override
    protected void finalizePipeline(GeneratorGraphContext graphContext) {
        landResource = landResourceVertex.getResult();
        waterResource = waterResourceVertex.getResult();
    }

    @Override
    protected void initializePipeline(GeneratorGraphContext graphContext) {
        unBuildableVertex.setResult(terrainPipeline.getUnBuildable());
        passableLandVertex.setResult(terrainPipeline.getPassableLand());

        verifyDefined(graphContext, unBuildableVertex, passableLandVertex, landResourceVertex, waterResourceVertex);
    }
}
