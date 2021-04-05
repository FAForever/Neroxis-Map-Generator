package neroxis.generator.prop;

import neroxis.generator.UnitPlacer;
import neroxis.generator.terrain.TerrainGenerator;
import neroxis.map.*;
import neroxis.util.Pipeline;
import neroxis.util.Util;

import java.io.IOException;
import java.util.ArrayList;

public class NeutralCivPropGenerator extends BasicPropGenerator {

    protected ConcurrentBinaryMask civReclaimMask;
    protected BinaryMask noCivs;

    @Override
    public void initialize(SCMap map, long seed, MapParameters mapParameters, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, mapParameters, terrainGenerator);
        SymmetrySettings symmetrySettings = mapParameters.getSymmetrySettings();
        civReclaimMask = new ConcurrentBinaryMask(1, random.nextLong(), symmetrySettings, "civReclaimMask");

        noCivs = new BinaryMask(1, random.nextLong(), symmetrySettings);
    }

    @Override
    public void setupPipeline() {
        super.setupPipeline();
        setupCivilianPipeline();
    }

    protected void setupCivilianPipeline() {
        int mapSize = map.getSize();
        civReclaimMask.setSize(mapSize / 4);

        if (!map.isUnexplored()) {
            civReclaimMask.randomize(.005f).setSize(mapSize + 1);
            civReclaimMask.intersect(passableLand.copy().minus(unbuildable).deflate(24)).fillCenter(32, false).fillEdge(64, false);
        }
    }

    @Override
    protected void generatePropExclusionMasks() {
        super.generatePropExclusionMasks();
        noProps.combine(civReclaimMask.getFinalMask());
    }

    protected void generateUnitExclusionMasks() {
        noCivs.init(unbuildable.getFinalMask());
        noCivs.inflate(12);
        generateExclusionZones(noCivs, 96, 32, 32);
    }

    @Override
    public void placeUnits() {
        if (!mapParameters.isUnexplored()) {
            generateUnitExclusionMasks();
            Pipeline.await(civReclaimMask);
            Util.timedRun("neroxis.generator", "placeCivs", () -> {
                Army civilian = new Army("NEUTRAL_CIVILIAN", new ArrayList<>());
                Group civilianInitial = new Group("INITIAL", new ArrayList<>());
                civilian.addGroup(civilianInitial);
                map.addArmy(civilian);
                try {
                    unitPlacer.placeBases(civReclaimMask.getFinalMask().minus(noCivs), UnitPlacer.MEDIUM_RECLAIM, civilian, civilianInitial, 256f);
                } catch (IOException e) {
                    System.out.println("Could not generate bases due to lua parsing error");
                    e.printStackTrace();
                }
            });
        }
    }
}
