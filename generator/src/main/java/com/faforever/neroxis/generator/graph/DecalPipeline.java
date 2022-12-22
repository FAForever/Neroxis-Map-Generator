package com.faforever.neroxis.generator.graph;

import com.faforever.neroxis.generator.GeneratorGraphContext;
import com.faforever.neroxis.graph.domain.MaskGraphVertex;
import com.faforever.neroxis.graph.domain.MaskInputVertex;
import com.faforever.neroxis.graph.domain.MaskMethodEdge;
import com.faforever.neroxis.graph.domain.MaskOutputVertex;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.mask.FloatMask;
import lombok.Getter;
import lombok.Setter;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.util.Map;

@Getter
public class DecalPipeline extends GeneratorPipeline {
    public static final String SLOPE = "Slope";
    public static final String PASSABLE_LAND = "Passable Land";
    public static final String FIELD_DECALS = "Field Decals";
    public static final String SLOPE_DECALS = "Slope Decals";
    private final MaskInputVertex<FloatMask> slopeVertex;
    private final MaskInputVertex<BooleanMask> passableLandVertex;
    private final MaskOutputVertex<BooleanMask> fieldDecalVertex;
    private final MaskOutputVertex<BooleanMask> slopeDecalVertex;
    @Setter
    private TerrainPipeline terrainPipeline;
    private BooleanMask fieldDecal;
    private BooleanMask slopeDecal;

    private DecalPipeline(DirectedAcyclicGraph<MaskGraphVertex<?>, MaskMethodEdge> graph,
                          MaskInputVertex<FloatMask> slopeVertex, MaskInputVertex<BooleanMask> passableLandVertex,
                          MaskOutputVertex<BooleanMask> fieldDecalVertex,
                          MaskOutputVertex<BooleanMask> slopeDecalVertex) {
        super(graph);
        this.slopeVertex = slopeVertex;
        this.passableLandVertex = passableLandVertex;
        this.fieldDecalVertex = fieldDecalVertex;
        this.slopeDecalVertex = slopeDecalVertex;

        verifyContains(slopeVertex, passableLandVertex, fieldDecalVertex, slopeDecalVertex);
    }

    public static DecalPipeline fromGraphAndMap(DirectedAcyclicGraph<MaskGraphVertex<?>, MaskMethodEdge> graph,
                                                Map<String, MaskGraphVertex<?>> vertexMap) {
        return new DecalPipeline(graph, (MaskInputVertex<FloatMask>) vertexMap.get(SLOPE),
                                 (MaskInputVertex<BooleanMask>) vertexMap.get(PASSABLE_LAND),
                                 (MaskOutputVertex<BooleanMask>) vertexMap.get(FIELD_DECALS),
                                 (MaskOutputVertex<BooleanMask>) vertexMap.get(SLOPE_DECALS));
    }

    public static DecalPipeline createNew() {
        try {
            DirectedAcyclicGraph<MaskGraphVertex<?>, MaskMethodEdge> graph = new DirectedAcyclicGraph<>(
                    MaskMethodEdge.class);
            MaskInputVertex<FloatMask> slopeVertex = new MaskInputVertex<>(SLOPE, FloatMask.class);
            MaskInputVertex<BooleanMask> passableLandVertex = new MaskInputVertex<>(PASSABLE_LAND, BooleanMask.class);
            MaskOutputVertex<BooleanMask> fieldDecalVertex = new MaskOutputVertex<>(FIELD_DECALS, BooleanMask.class);
            MaskOutputVertex<BooleanMask> slopeDecalVertex = new MaskOutputVertex<>(SLOPE_DECALS, BooleanMask.class);
            graph.addVertex(slopeVertex);
            graph.addVertex(passableLandVertex);
            graph.addVertex(fieldDecalVertex);
            graph.addVertex(slopeDecalVertex);
            return new DecalPipeline(graph, slopeVertex, passableLandVertex, fieldDecalVertex, slopeDecalVertex);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, MaskGraphVertex<?>> getEndpointMap() {
        return Map.of(SLOPE, slopeVertex, PASSABLE_LAND, passableLandVertex, FIELD_DECALS, fieldDecalVertex,
                      SLOPE_DECALS, slopeDecalVertex);
    }

    @Override
    protected void finalizePipeline(GeneratorGraphContext graphContext) {
        fieldDecal = fieldDecalVertex.getResult();
        slopeDecal = slopeDecalVertex.getResult();
    }

    @Override
    protected void initializePipeline(GeneratorGraphContext graphContext) {
        passableLandVertex.setResult(terrainPipeline.getPassableLand());
        slopeVertex.setResult(terrainPipeline.getSlope());

        verifyDefined(graphContext, passableLandVertex, slopeVertex, fieldDecalVertex, slopeDecalVertex);
    }
}
