package com.faforever.neroxis.generator.graph;

import com.faforever.neroxis.exporter.PreviewGenerator;
import com.faforever.neroxis.generator.GeneratorGraphContext;
import com.faforever.neroxis.graph.domain.MaskGraphVertex;
import com.faforever.neroxis.graph.domain.MaskInputVertex;
import com.faforever.neroxis.graph.domain.MaskMethodEdge;
import com.faforever.neroxis.graph.domain.MaskOutputVertex;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.mask.NormalMask;
import com.faforever.neroxis.mask.Vector4Mask;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.jgrapht.graph.DirectedAcyclicGraph;

@Getter
public class TexturePipeline extends GeneratorPipeline {
    public static final String HEIGHTMAP = "Heightmap";
    public static final String SLOPE = "Slope";
    public static final String SHADOW = "Shadow";
    public static final String SHADOW_MASK = "Shadow Mask";
    public static final String NORMAL = "Normal";
    public static final String TEXTURES_LOW = "Textures Low";
    public static final String TEXTURES_HIGH = "Textures High";
    private final MaskInputVertex<FloatMask> heightMapVertex;
    private final MaskInputVertex<FloatMask> slopeVertex;
    private final MaskOutputVertex<FloatMask> shadowVertex;
    private final MaskOutputVertex<BooleanMask> shadowMaskVertex;
    private final MaskOutputVertex<NormalMask> normalVertex;
    private final MaskOutputVertex<Vector4Mask> texturesLowVertex;
    private final MaskOutputVertex<Vector4Mask> texturesHighVertex;
    @Setter
    private TerrainPipeline terrainPipeline;
    private Vector4Mask texturesLowPreviewMask;
    private Vector4Mask texturesHighPreviewMask;
    private FloatMask heightmapPreview;
    private FloatMask reflectance;
    private FloatMask shadow;
    private BooleanMask shadowMask;
    private NormalMask normal;
    private Vector4Mask texturesLow;
    private Vector4Mask texturesHigh;

    private TexturePipeline(DirectedAcyclicGraph<MaskGraphVertex<?>, MaskMethodEdge> graph,
                            MaskInputVertex<FloatMask> heightMapVertex, MaskInputVertex<FloatMask> slopeVertex,
                            MaskOutputVertex<FloatMask> shadowVertex, MaskOutputVertex<BooleanMask> shadowMaskVertex,
                            MaskOutputVertex<NormalMask> normalVertex, MaskOutputVertex<Vector4Mask> texturesLowVertex,
                            MaskOutputVertex<Vector4Mask> texturesHighVertex) {
        super(graph);
        this.heightMapVertex = heightMapVertex;
        this.slopeVertex = slopeVertex;
        this.shadowVertex = shadowVertex;
        this.shadowMaskVertex = shadowMaskVertex;
        this.normalVertex = normalVertex;
        this.texturesLowVertex = texturesLowVertex;
        this.texturesHighVertex = texturesHighVertex;

        verifyContains(heightMapVertex, slopeVertex, shadowMaskVertex, shadowVertex, normalVertex, texturesLowVertex,
                       texturesHighVertex);
    }

    public static TexturePipeline fromGraphAndMap(DirectedAcyclicGraph<MaskGraphVertex<?>, MaskMethodEdge> graph,
                                                  Map<String, MaskGraphVertex<?>> vertexMap) {
        return new TexturePipeline(graph, (MaskInputVertex<FloatMask>) vertexMap.get(HEIGHTMAP),
                                   (MaskInputVertex<FloatMask>) vertexMap.get(SLOPE),
                                   (MaskOutputVertex<FloatMask>) vertexMap.get(SHADOW),
                                   (MaskOutputVertex<BooleanMask>) vertexMap.get(SHADOW_MASK),
                                   (MaskOutputVertex<NormalMask>) vertexMap.get(NORMAL),
                                   (MaskOutputVertex<Vector4Mask>) vertexMap.get(TEXTURES_LOW),
                                   (MaskOutputVertex<Vector4Mask>) vertexMap.get(TEXTURES_HIGH));
    }

    public static TexturePipeline createNew() {
        try {
            DirectedAcyclicGraph<MaskGraphVertex<?>, MaskMethodEdge> graph = new DirectedAcyclicGraph<>(
                    MaskMethodEdge.class);
            MaskInputVertex<FloatMask> heightmapVertex = new MaskInputVertex<>(HEIGHTMAP, FloatMask.class);
            MaskInputVertex<FloatMask> slopeVertex = new MaskInputVertex<>(SLOPE, FloatMask.class);
            MaskOutputVertex<FloatMask> shadowVertex = new MaskOutputVertex<>(SHADOW, FloatMask.class);
            MaskOutputVertex<BooleanMask> shadowMaskVertex = new MaskOutputVertex<>(SHADOW_MASK, BooleanMask.class);
            MaskOutputVertex<NormalMask> normalVertex = new MaskOutputVertex<>(NORMAL, NormalMask.class);
            MaskOutputVertex<Vector4Mask> texturesLowMask = new MaskOutputVertex<>(TEXTURES_LOW, Vector4Mask.class);
            MaskOutputVertex<Vector4Mask> texturesHighMask = new MaskOutputVertex<>(TEXTURES_HIGH, Vector4Mask.class);
            graph.addVertex(heightmapVertex);
            graph.addVertex(slopeVertex);
            graph.addVertex(shadowVertex);
            graph.addVertex(shadowMaskVertex);
            graph.addVertex(normalVertex);
            graph.addVertex(texturesLowMask);
            graph.addVertex(texturesHighMask);
            return new TexturePipeline(graph, heightmapVertex, slopeVertex, shadowVertex, shadowMaskVertex,
                                       normalVertex, texturesLowMask, texturesHighMask);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, MaskGraphVertex<?>> getEndpointMap() {
        return Map.of(HEIGHTMAP, heightMapVertex, SLOPE, slopeVertex, SHADOW, shadowVertex, SHADOW_MASK,
                      shadowMaskVertex, NORMAL, normalVertex, TEXTURES_LOW, texturesLowVertex, TEXTURES_HIGH,
                      texturesHighVertex);
    }

    @Override
    protected void finalizePipeline(GeneratorGraphContext graphContext) {
        FloatMask heightmap = heightMapVertex.getResult();
        heightmapPreview = heightmap.copy().resample(PreviewGenerator.PREVIEW_SIZE);
        shadow = shadowVertex.getResult();
        shadowMask = shadowMaskVertex.getResult();
        normal = normalVertex.getResult();
        texturesLow = texturesLowVertex.getResult();
        texturesHigh = texturesHighVertex.getResult();
        texturesLowPreviewMask = texturesLow.copy().resample(PreviewGenerator.PREVIEW_SIZE);
        texturesHighPreviewMask = texturesHigh.copy().resample(PreviewGenerator.PREVIEW_SIZE);
        reflectance = heightmap.copy()
                               .copyAsNormalMask(8f)
                               .resample(PreviewGenerator.PREVIEW_SIZE)
                               .copyAsDotProduct(graphContext.getGeneratorParameters()
                                                             .getBiome()
                                                             .getLightingSettings()
                                                             .getSunDirection())
                               .add(1f)
                               .divide(2f);
    }

    @Override
    protected void initializePipeline(GeneratorGraphContext graphContext) {
        heightMapVertex.setResult(terrainPipeline.getHeightmap());
        slopeVertex.setResult(terrainPipeline.getSlope());

        verifyDefined(graphContext, heightMapVertex, slopeVertex, shadowVertex, shadowMaskVertex, normalVertex,
                      texturesLowVertex, texturesHighVertex);
    }
}
