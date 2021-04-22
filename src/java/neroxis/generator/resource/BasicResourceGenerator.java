package neroxis.generator.resource;

import neroxis.generator.terrain.TerrainGenerator;
import neroxis.map.BooleanMask;
import neroxis.map.MapParameters;
import neroxis.map.SCMap;
import neroxis.map.SymmetrySettings;
import neroxis.util.Pipeline;
import neroxis.util.Util;

public class BasicResourceGenerator extends ResourceGenerator {
    protected BooleanMask resourceMask;
    protected BooleanMask waterResourceMask;

    @Override
    public void initialize(SCMap map, long seed, MapParameters mapParameters, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, mapParameters, terrainGenerator);
        SymmetrySettings symmetrySettings = mapParameters.getSymmetrySettings();
        resourceMask = new BooleanMask(1, random.nextLong(), symmetrySettings, "resourceMask", true);
        waterResourceMask = new BooleanMask(1, random.nextLong(), symmetrySettings, "waterResourceMask", true);
    }

    @Override
    public void setupPipeline() {
        resourceMask.init(passableLand);
        waterResourceMask.init(passableLand).invert();

        resourceMask.minus(unbuildable).deflate(4);
        resourceMask.fillEdge(16, false).fillCenter(24, false);
        waterResourceMask.minus(unbuildable).deflate(8).fillEdge(16, false).fillCenter(24, false);
    }

    @Override
    public void placeResources() {
        Pipeline.await(resourceMask, waterResourceMask);
        Util.timedRun("neroxis.generator", "generateResources", () -> {
            mexPlacer.placeMexes(resourceMask.getFinalMask(), waterResourceMask.getFinalMask());
            hydroPlacer.placeHydros(resourceMask.getFinalMask().deflate(8));
        });
    }
}
