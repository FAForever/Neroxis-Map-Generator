package com.faforever.neroxis.generator.graph;

import com.faforever.neroxis.generator.GeneratorGraphContext;
import com.faforever.neroxis.graph.domain.MaskGraphVertex;
import com.faforever.neroxis.graph.domain.MaskInputVertex;
import com.faforever.neroxis.graph.domain.MaskMethodEdge;
import com.faforever.neroxis.graph.domain.MaskOutputVertex;
import com.faforever.neroxis.mask.BooleanMask;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.jgrapht.graph.DirectedAcyclicGraph;

@Getter
public class WreckPipeline extends GeneratorPipeline {
    public static final String IMPASSABLE = "Impassable";
    public static final String UN_BUILDABLE = "UnBuildable";
    public static final String PASSABLE_LAND = "Passable Land";
    public static final String SHORE_WRECK = "Shore Wreck";
    public static final String WATER_WRECK = "Water Wreck";
    public static final String LAND_WRECK = "Land Wreck";
    private final MaskInputVertex<BooleanMask> impassableVertex;
    private final MaskInputVertex<BooleanMask> unBuildableVertex;
    private final MaskInputVertex<BooleanMask> passableLandVertex;
    private final MaskOutputVertex<BooleanMask> shoreWreckVertex;
    private final MaskOutputVertex<BooleanMask> waterWreckVertex;
    private final MaskOutputVertex<BooleanMask> landWreckVertex;
    @Setter
    private TerrainPipeline terrainPipeline;
    private BooleanMask shoreWreck;
    private BooleanMask landWreck;
    private BooleanMask waterWreck;

    private WreckPipeline(DirectedAcyclicGraph<MaskGraphVertex<?>, MaskMethodEdge> graph,
                          MaskInputVertex<BooleanMask> impassableVertex, MaskInputVertex<BooleanMask> unBuildableVertex,
                          MaskInputVertex<BooleanMask> passableLandVertex,
                          MaskOutputVertex<BooleanMask> shoreWreckVertex,
                          MaskOutputVertex<BooleanMask> waterWreckVertex,
                          MaskOutputVertex<BooleanMask> landWreckVertex) {
        super(graph);
        this.impassableVertex = impassableVertex;
        this.unBuildableVertex = unBuildableVertex;
        this.passableLandVertex = passableLandVertex;
        this.shoreWreckVertex = shoreWreckVertex;
        this.waterWreckVertex = waterWreckVertex;
        this.landWreckVertex = landWreckVertex;

        verifyContains(impassableVertex, unBuildableVertex, passableLandVertex, landWreckVertex, shoreWreckVertex,
                       waterWreckVertex);
    }

    public static WreckPipeline fromGraphAndMap(DirectedAcyclicGraph<MaskGraphVertex<?>, MaskMethodEdge> graph,
                                                Map<String, MaskGraphVertex<?>> vertexMap) {
        return new WreckPipeline(graph, (MaskInputVertex<BooleanMask>) vertexMap.get(IMPASSABLE),
                                 (MaskInputVertex<BooleanMask>) vertexMap.get(UN_BUILDABLE),
                                 (MaskInputVertex<BooleanMask>) vertexMap.get(PASSABLE_LAND),
                                 (MaskOutputVertex<BooleanMask>) vertexMap.get(SHORE_WRECK),
                                 (MaskOutputVertex<BooleanMask>) vertexMap.get(WATER_WRECK),
                                 (MaskOutputVertex<BooleanMask>) vertexMap.get(LAND_WRECK));
    }

    public static WreckPipeline createNew() {
        try {
            DirectedAcyclicGraph<MaskGraphVertex<?>, MaskMethodEdge> graph = new DirectedAcyclicGraph<>(
                    MaskMethodEdge.class);
            MaskInputVertex<BooleanMask> impassableVertex = new MaskInputVertex<>(IMPASSABLE, BooleanMask.class);
            MaskInputVertex<BooleanMask> unBuildableVertex = new MaskInputVertex<>(UN_BUILDABLE, BooleanMask.class);
            MaskInputVertex<BooleanMask> passableLandVertex = new MaskInputVertex<>(PASSABLE_LAND, BooleanMask.class);
            MaskOutputVertex<BooleanMask> shoreWreckVertex = new MaskOutputVertex<>(SHORE_WRECK, BooleanMask.class);
            MaskOutputVertex<BooleanMask> waterWreckVertex = new MaskOutputVertex<>(WATER_WRECK, BooleanMask.class);
            MaskOutputVertex<BooleanMask> landWreckVertex = new MaskOutputVertex<>(LAND_WRECK, BooleanMask.class);
            graph.addVertex(impassableVertex);
            graph.addVertex(unBuildableVertex);
            graph.addVertex(passableLandVertex);
            graph.addVertex(shoreWreckVertex);
            graph.addVertex(waterWreckVertex);
            graph.addVertex(landWreckVertex);
            return new WreckPipeline(graph, impassableVertex, unBuildableVertex, passableLandVertex, shoreWreckVertex,
                                     waterWreckVertex, landWreckVertex);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, MaskGraphVertex<?>> getEndpointMap() {
        return Map.of(IMPASSABLE, impassableVertex, UN_BUILDABLE, unBuildableVertex, PASSABLE_LAND, passableLandVertex,
                      SHORE_WRECK, shoreWreckVertex, WATER_WRECK, waterWreckVertex, LAND_WRECK, landWreckVertex);
    }

    @Override
    protected void finalizePipeline(GeneratorGraphContext graphContext) {
        shoreWreck = shoreWreckVertex.getResult();
        landWreck = landWreckVertex.getResult();
        waterWreck = waterWreckVertex.getResult();
    }

    @Override
    protected void initializePipeline(GeneratorGraphContext graphContext) {
        unBuildableVertex.setResult(terrainPipeline.getUnBuildable());
        passableLandVertex.setResult(terrainPipeline.getPassableLand());
        impassableVertex.setResult(terrainPipeline.getImpassable());

        verifyDefined(graphContext, impassableVertex, unBuildableVertex, passableLandVertex, shoreWreckVertex,
                      waterWreckVertex, landWreckVertex);
    }
}
