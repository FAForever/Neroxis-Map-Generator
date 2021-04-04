package neroxis.generator.resource;

import neroxis.generator.MapGenerator;
import neroxis.generator.terrain.TerrainGenerator;
import neroxis.map.ConcurrentBinaryMask;
import neroxis.map.MapParameters;
import neroxis.map.SCMap;
import neroxis.map.SymmetrySettings;
import neroxis.util.Pipeline;
import neroxis.util.Util;

public class DefaultResourceGenerator extends ResourceGenerator {
    protected ConcurrentBinaryMask resourceMask;
    protected ConcurrentBinaryMask waterResourceMask;

    @Override
    public void initialize(SCMap map, long seed, MapParameters mapParameters, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, mapParameters, terrainGenerator);
        SymmetrySettings symmetrySettings = mapParameters.getSymmetrySettings();
        resourceMask = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "resourceMask");
        waterResourceMask = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "waterResourceMask");
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
        long sTime = System.currentTimeMillis();
        mexPlacer.placeMexes(resourceMask.getFinalMask(), waterResourceMask.getFinalMask());
        hydroPlacer.placeHydros(resourceMask.getFinalMask().deflate(8));
        if (MapGenerator.DEBUG) {
            System.out.printf("Done: %4d ms, %s, generateResources\n",
                    System.currentTimeMillis() - sTime,
                    Util.getStackTraceLineInPackage("neroxis.generator"));
        }
    }
}
