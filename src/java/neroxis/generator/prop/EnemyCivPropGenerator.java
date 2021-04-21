package neroxis.generator.prop;

import neroxis.generator.placement.UnitPlacer;
import neroxis.generator.terrain.TerrainGenerator;
import neroxis.map.*;
import neroxis.util.Pipeline;
import neroxis.util.Util;

import java.io.IOException;
import java.util.ArrayList;

public class EnemyCivPropGenerator extends BasicPropGenerator {

    protected BinaryMask baseMask;
    protected BinaryMask noBases;

    @Override
    public void initialize(SCMap map, long seed, MapParameters mapParameters, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, mapParameters, terrainGenerator);
        SymmetrySettings symmetrySettings = mapParameters.getSymmetrySettings();
        baseMask = new BinaryMask(1, random.nextLong(), symmetrySettings, "baseMask", true);
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
        noProps.combine((BinaryMask) baseMask.getFinalMask());
    }

    protected void generateUnitExclusionMasks() {
        noBases.init((BinaryMask) unbuildable.getFinalMask());
        noBases.inflate(12);
        generateExclusionZones(noBases, 128, 32, 32);
    }

    @Override
    public void placeUnits() {
        if (!mapParameters.isUnexplored()) {
            generateUnitExclusionMasks();
            Pipeline.await(baseMask);
            Util.timedRun("neroxis.generator", "placeBases", () -> {
                Army army17 = new Army("ARMY_17", new ArrayList<>());
                Group army17Initial = new Group("INITIAL", new ArrayList<>());
                army17.addGroup(army17Initial);
                map.addArmy(army17);
                try {
                    unitPlacer.placeBases(((BinaryMask) baseMask.getFinalMask()).minus(noBases), UnitPlacer.MEDIUM_ENEMY, army17, army17Initial, 512f);
                } catch (IOException e) {
                    System.out.println("Could not generate bases due to lua parsing error");
                    e.printStackTrace();
                }
            });
        }
    }
}
