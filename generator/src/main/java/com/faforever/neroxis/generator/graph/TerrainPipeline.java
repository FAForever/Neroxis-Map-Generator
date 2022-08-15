package com.faforever.neroxis.generator.graph;

import com.faforever.neroxis.generator.GeneratorGraphContext;
import com.faforever.neroxis.graph.domain.MaskGraphVertex;
import com.faforever.neroxis.graph.domain.MaskMethodEdge;
import com.faforever.neroxis.graph.domain.MaskOutputVertex;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.mask.FloatMask;
import java.util.Map;
import lombok.Getter;
import org.jgrapht.graph.DirectedAcyclicGraph;

@Getter
public class TerrainPipeline extends GeneratorPipeline {
    public static final String HEIGHTMAP = "Heightmap";
    private final MaskOutputVertex<FloatMask> heightMapVertex;
    private FloatMask heightmap;
    private BooleanMask impassable;
    private BooleanMask unBuildable;
    private BooleanMask passable;
    private BooleanMask passableLand;
    private BooleanMask passableWater;
    private FloatMask slope;

    private TerrainPipeline(DirectedAcyclicGraph<MaskGraphVertex<?>, MaskMethodEdge> graph,
                            MaskOutputVertex<FloatMask> heightMapVertex) {
        super(graph);
        this.heightMapVertex = heightMapVertex;

        verifyContains(heightMapVertex);
    }

    public static TerrainPipeline fromGraphAndMap(DirectedAcyclicGraph<MaskGraphVertex<?>, MaskMethodEdge> graph,
                                                  Map<String, MaskGraphVertex<?>> vertexMap) {
        return new TerrainPipeline(graph, (MaskOutputVertex<FloatMask>) vertexMap.get(HEIGHTMAP));
    }

    public static TerrainPipeline createNew() {
        try {
            DirectedAcyclicGraph<MaskGraphVertex<?>, MaskMethodEdge> graph = new DirectedAcyclicGraph<>(
                    MaskMethodEdge.class);
            MaskOutputVertex<FloatMask> heightmapVertex = new MaskOutputVertex<>(HEIGHTMAP, FloatMask.class);
            graph.addVertex(heightmapVertex);
            return new TerrainPipeline(graph, heightmapVertex);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, MaskGraphVertex<?>> getEndpointMap() {
        return Map.of(HEIGHTMAP, heightMapVertex);
    }

    @Override
    protected void finalizePipeline(GeneratorGraphContext graphContext) {
        heightmap = heightMapVertex.getResult();
        BooleanMask actualLand = heightmap.copyAsBooleanMask(graphContext.getBiome().getWaterSettings().getElevation());

        slope = heightmap.copy().supcomGradient();
        impassable = slope.copyAsBooleanMask(.7f, "impassable").inflate(4);
        unBuildable = slope.copyAsBooleanMask(.05f, "unBuildable");

        passable = impassable.copy("passable").invert().fillEdge(8, false);
        passableLand = actualLand.copy("passableLand").multiply(passable);
        passableWater = actualLand.copy("passableWater").invert().multiply(passable).deflate(16);
    }

    @Override
    protected void initializePipeline(GeneratorGraphContext graphContext) {
        verifyDefined(graphContext, heightMapVertex);
    }
}
