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
public class CivilianPipeline extends GeneratorPipeline {
    public static final String IMPASSABLE = "Impassable";
    public static final String UN_BUILDABLE = "UnBuildable";
    public static final String PASSABLE_LAND = "Passable Land";
    public static final String ENEMY_BASES = "Enemy Bases";
    public static final String NEUTRAL_BASES = "Neutral Bases";
    private final MaskInputVertex<BooleanMask> impassableVertex;
    private final MaskInputVertex<BooleanMask> unBuildableVertex;
    private final MaskInputVertex<BooleanMask> passableLandVertex;
    private final MaskOutputVertex<BooleanMask> enemyVertex;
    private final MaskOutputVertex<BooleanMask> neutralVertex;
    @Setter
    private TerrainPipeline terrainPipeline;
    private BooleanMask enemy;
    private BooleanMask neutral;

    private CivilianPipeline(DirectedAcyclicGraph<MaskGraphVertex<?>, MaskMethodEdge> graph,
                             MaskInputVertex<BooleanMask> impassableVertex,
                             MaskInputVertex<BooleanMask> unBuildableVertex,
                             MaskInputVertex<BooleanMask> passableLandVertex, MaskOutputVertex<BooleanMask> enemyVertex,
                             MaskOutputVertex<BooleanMask> neutralVertex) {
        super(graph);
        this.impassableVertex = impassableVertex;
        this.unBuildableVertex = unBuildableVertex;
        this.passableLandVertex = passableLandVertex;
        this.enemyVertex = enemyVertex;
        this.neutralVertex = neutralVertex;

        verifyContains(impassableVertex, unBuildableVertex, passableLandVertex, enemyVertex, neutralVertex);
    }

    public static CivilianPipeline createNew() {
        try {
            DirectedAcyclicGraph<MaskGraphVertex<?>, MaskMethodEdge> graph = new DirectedAcyclicGraph<>(
                    MaskMethodEdge.class);
            MaskInputVertex<BooleanMask> impassableVertex = new MaskInputVertex<>(IMPASSABLE, BooleanMask.class);
            MaskInputVertex<BooleanMask> unBuildableVertex = new MaskInputVertex<>(UN_BUILDABLE, BooleanMask.class);
            MaskInputVertex<BooleanMask> passableLandVertex = new MaskInputVertex<>(PASSABLE_LAND, BooleanMask.class);
            MaskOutputVertex<BooleanMask> enemyVertex = new MaskOutputVertex<>(ENEMY_BASES, BooleanMask.class);
            MaskOutputVertex<BooleanMask> neutralVertex = new MaskOutputVertex<>(NEUTRAL_BASES, BooleanMask.class);
            graph.addVertex(impassableVertex);
            graph.addVertex(unBuildableVertex);
            graph.addVertex(passableLandVertex);
            graph.addVertex(enemyVertex);
            graph.addVertex(neutralVertex);
            return new CivilianPipeline(graph, impassableVertex, unBuildableVertex, passableLandVertex, enemyVertex,
                                        neutralVertex);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static CivilianPipeline fromGraphAndMap(DirectedAcyclicGraph<MaskGraphVertex<?>, MaskMethodEdge> graph,
                                                   Map<String, MaskGraphVertex<?>> vertexMap) {
        return new CivilianPipeline(graph, (MaskInputVertex<BooleanMask>) vertexMap.get(IMPASSABLE),
                                    (MaskInputVertex<BooleanMask>) vertexMap.get(UN_BUILDABLE),
                                    (MaskInputVertex<BooleanMask>) vertexMap.get(PASSABLE_LAND),
                                    (MaskOutputVertex<BooleanMask>) vertexMap.get(ENEMY_BASES),
                                    (MaskOutputVertex<BooleanMask>) vertexMap.get(NEUTRAL_BASES));
    }

    @Override
    public Map<String, MaskGraphVertex<?>> getEndpointMap() {
        return Map.of(IMPASSABLE, impassableVertex, UN_BUILDABLE, unBuildableVertex, PASSABLE_LAND, passableLandVertex,
                      ENEMY_BASES, enemyVertex, NEUTRAL_BASES, neutralVertex);
    }

    @Override
    protected void finalizePipeline(GeneratorGraphContext graphContext) {
        enemy = enemyVertex.getResult();
        neutral = neutralVertex.getResult();
    }

    @Override
    protected void initializePipeline(GeneratorGraphContext graphContext) {
        unBuildableVertex.setResult(terrainPipeline.getUnBuildable());
        passableLandVertex.setResult(terrainPipeline.getPassableLand());
        impassableVertex.setResult(terrainPipeline.getImpassable());

        verifyDefined(graphContext, unBuildableVertex, passableLandVertex, impassableVertex, enemyVertex,
                      neutralVertex);
    }
}
