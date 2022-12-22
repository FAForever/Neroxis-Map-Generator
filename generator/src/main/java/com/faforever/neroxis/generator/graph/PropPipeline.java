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
public class PropPipeline extends GeneratorPipeline {
    public static final String IMPASSABLE = "Impassable";
    public static final String UN_BUILDABLE = "UnBuildable";
    public static final String PASSABLE_LAND = "Passable Land";
    public static final String TREES = "Trees";
    public static final String ROCKS = "Rocks";
    public static final String BOULDERS = "Boulders";
    private final MaskInputVertex<BooleanMask> impassableVertex;
    private final MaskInputVertex<BooleanMask> unBuildableVertex;
    private final MaskInputVertex<BooleanMask> passableLandVertex;
    private final MaskOutputVertex<BooleanMask> treeVertex;
    private final MaskOutputVertex<BooleanMask> rockVertex;
    private final MaskOutputVertex<BooleanMask> boulderVertex;
    @Setter
    private TerrainPipeline terrainPipeline;
    private BooleanMask landResource;
    private BooleanMask waterResource;

    private PropPipeline(DirectedAcyclicGraph<MaskGraphVertex<?>, MaskMethodEdge> graph,
                         MaskInputVertex<BooleanMask> impassableVertex, MaskInputVertex<BooleanMask> unBuildableVertex,
                         MaskInputVertex<BooleanMask> passableLandVertex, MaskOutputVertex<BooleanMask> treeVertex,
                         MaskOutputVertex<BooleanMask> rockVertex, MaskOutputVertex<BooleanMask> boulderVertex) {
        super(graph);
        this.impassableVertex = impassableVertex;
        this.unBuildableVertex = unBuildableVertex;
        this.passableLandVertex = passableLandVertex;
        this.treeVertex = treeVertex;
        this.rockVertex = rockVertex;
        this.boulderVertex = boulderVertex;

        verifyContains(impassableVertex, unBuildableVertex, passableLandVertex, boulderVertex, treeVertex, rockVertex);
    }

    public static PropPipeline fromGraphAndMap(DirectedAcyclicGraph<MaskGraphVertex<?>, MaskMethodEdge> graph,
                                               Map<String, MaskGraphVertex<?>> vertexMap) {
        return new PropPipeline(graph, (MaskInputVertex<BooleanMask>) vertexMap.get(IMPASSABLE),
                                (MaskInputVertex<BooleanMask>) vertexMap.get(UN_BUILDABLE),
                                (MaskInputVertex<BooleanMask>) vertexMap.get(PASSABLE_LAND),
                                (MaskOutputVertex<BooleanMask>) vertexMap.get(TREES),
                                (MaskOutputVertex<BooleanMask>) vertexMap.get(ROCKS),
                                (MaskOutputVertex<BooleanMask>) vertexMap.get(BOULDERS));
    }

    public static PropPipeline createNew() {
        try {
            DirectedAcyclicGraph<MaskGraphVertex<?>, MaskMethodEdge> graph = new DirectedAcyclicGraph<>(
                    MaskMethodEdge.class);
            MaskInputVertex<BooleanMask> impassableVertex = new MaskInputVertex<>(IMPASSABLE, BooleanMask.class);
            MaskInputVertex<BooleanMask> unBuildableVertex = new MaskInputVertex<>(UN_BUILDABLE, BooleanMask.class);
            MaskInputVertex<BooleanMask> passableLandVertex = new MaskInputVertex<>(PASSABLE_LAND, BooleanMask.class);
            MaskOutputVertex<BooleanMask> treeVertex = new MaskOutputVertex<>(TREES, BooleanMask.class);
            MaskOutputVertex<BooleanMask> rockVertex = new MaskOutputVertex<>(ROCKS, BooleanMask.class);
            MaskOutputVertex<BooleanMask> boulderVertex = new MaskOutputVertex<>(BOULDERS, BooleanMask.class);
            graph.addVertex(impassableVertex);
            graph.addVertex(unBuildableVertex);
            graph.addVertex(passableLandVertex);
            graph.addVertex(treeVertex);
            graph.addVertex(rockVertex);
            graph.addVertex(boulderVertex);
            return new PropPipeline(graph, impassableVertex, unBuildableVertex, passableLandVertex, treeVertex,
                                    rockVertex, boulderVertex);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, MaskGraphVertex<?>> getEndpointMap() {
        return Map.of(IMPASSABLE, impassableVertex, UN_BUILDABLE, unBuildableVertex, PASSABLE_LAND, passableLandVertex,
                      TREES, treeVertex, ROCKS, rockVertex, BOULDERS, boulderVertex);
    }

    @Override
    protected void finalizePipeline(GeneratorGraphContext graphContext) {
        landResource = treeVertex.getResult();
        waterResource = boulderVertex.getResult();
    }

    @Override
    protected void initializePipeline(GeneratorGraphContext graphContext) {
        unBuildableVertex.setResult(terrainPipeline.getUnBuildable());
        passableLandVertex.setResult(terrainPipeline.getPassableLand());
        impassableVertex.setResult(terrainPipeline.getImpassable());

        verifyDefined(graphContext, impassableVertex, unBuildableVertex, passableLandVertex, treeVertex, boulderVertex,
                      rockVertex);
    }
}
