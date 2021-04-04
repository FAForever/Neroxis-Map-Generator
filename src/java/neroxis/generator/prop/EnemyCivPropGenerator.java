package neroxis.generator.prop;

import neroxis.generator.MapGenerator;
import neroxis.generator.UnitPlacer;
import neroxis.generator.terrain.TerrainGenerator;
import neroxis.map.*;
import neroxis.util.Pipeline;
import neroxis.util.Util;

import java.io.IOException;
import java.util.ArrayList;

public class EnemyCivPropGenerator extends DefaultPropGenerator {

    protected ConcurrentBinaryMask baseMask;
    protected BinaryMask noBases;

    @Override
    public void initialize(SCMap map, long seed, MapParameters mapParameters, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, mapParameters, terrainGenerator);
        SymmetrySettings symmetrySettings = mapParameters.getSymmetrySettings();
        baseMask = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "baseMask");
        noBases = new BinaryMask(1, random.nextLong(), symmetrySettings);
    }

    @Override
    public void setupPipeline() {
        super.setupPipeline();
        setupCivilianPipeline();
    }

    protected void setupCivilianPipeline() {
        int mapSize = map.getSize();
        baseMask.setSize(mapSize / 4);

        if (!map.isUnexplored()) {
            baseMask.randomize(.005f).setSize(mapSize + 1);
            baseMask.intersect(passableLand.copy().minus(unbuildable).deflate(24)).fillCenter(32, false).fillEdge(32, false);
        }
    }

    protected void generatePropExclusionMasks() {
        super.generatePropExclusionMasks();
        noProps.combine(baseMask.getFinalMask());
    }

    protected void generateUnitExclusionMasks() {
        noBases.init(unbuildable.getFinalMask());
        noBases.inflate(12);
        generateExclusionZones(noBases, 128, 32, 32);
    }

    @Override
    public void placeUnits() {
        if (!mapParameters.isUnexplored()) {
            generateUnitExclusionMasks();
            Pipeline.await(baseMask);
            long sTime = System.currentTimeMillis();
            Army army17 = new Army("ARMY_17", new ArrayList<>());
            Group army17Initial = new Group("INITIAL", new ArrayList<>());
            Group army17Wreckage = new Group("WRECKAGE", new ArrayList<>());
            army17.addGroup(army17Initial);
            army17.addGroup(army17Wreckage);
            map.addArmy(army17);
            try {
                unitPlacer.placeBases(baseMask.getFinalMask().minus(noBases), UnitPlacer.MEDIUM_ENEMY, army17, army17Initial, 512f);
            } catch (IOException e) {
                System.out.println("Could not generate bases due to lua parsing error");
                e.printStackTrace();
            }
            if (MapGenerator.DEBUG) {
                System.out.printf("Done: %4d ms, %s, placeBases\n",
                        System.currentTimeMillis() - sTime,
                        Util.getStackTraceLineInPackage("neroxis.generator"));
            }
        }
    }
}
