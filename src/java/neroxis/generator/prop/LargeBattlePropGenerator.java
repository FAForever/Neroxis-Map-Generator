package neroxis.generator.prop;

import neroxis.generator.ParameterConstraints;
import neroxis.generator.placement.UnitPlacer;
import neroxis.generator.terrain.TerrainGenerator;
import neroxis.map.*;
import neroxis.util.Pipeline;
import neroxis.util.Util;

import java.util.ArrayList;

public class LargeBattlePropGenerator extends ReducedNaturalPropGenerator {

    protected BooleanMask landWreckMask;
    protected BooleanMask noWrecks;

    public LargeBattlePropGenerator() {
        parameterConstraints = ParameterConstraints.builder()
                .landDensity(.5f, 1f)
                .reclaimDensity(.75f, 1f)
                .build();
        weight = 2;
    }

    @Override
    public void initialize(SCMap map, long seed, MapParameters mapParameters, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, mapParameters, terrainGenerator);
        SymmetrySettings symmetrySettings = mapParameters.getSymmetrySettings();
        landWreckMask = new BooleanMask(1, random.nextLong(), symmetrySettings, "landWreckMask", true);
        noWrecks = new BooleanMask(1, random.nextLong(), symmetrySettings);
    }

    @Override
    public void setupPipeline() {
        super.setupPipeline();
        setupWreckPipeline();
    }

    protected void setupWreckPipeline() {
        int mapSize = map.getSize();

        landWreckMask.setSize(mapSize + 1);
        landWreckMask.fillCenter(196, true);
        map.getSpawns().forEach(spawn -> landWreckMask.fillCircle(spawn.getPosition(), 128, false));
        landWreckMask.intersect(passableLand).space(96, 128);
        landWreckMask.inflate(40f).setSize(mapSize / 4);
        landWreckMask.erode(.5f, SymmetryType.SPAWN, 10).setSize(mapSize + 1);
        landWreckMask.intersect(passableLand).fillEdge(96, false);
    }

    protected void generateUnitExclusionMasks() {
        noWrecks.init(passableLand.getFinalMask().invert());
        generateExclusionZones(noWrecks, 128, 4, 32);
    }

    @Override
    public void placeUnits() {
        if (!mapParameters.isUnexplored()) {
            generateUnitExclusionMasks();
            Pipeline.await(landWreckMask);
            Util.timedRun("neroxis.generator", "placeProps", () -> {
                Army army17 = new Army("ARMY_17", new ArrayList<>());
                Group army17Wreckage = new Group("WRECKAGE", new ArrayList<>());
                army17.addGroup(army17Wreckage);
                map.addArmy(army17);
                BooleanMask placementMask = landWreckMask.getFinalMask().minus(noWrecks);
                unitPlacer.placeUnits(placementMask, UnitPlacer.T1_Land, army17, army17Wreckage, 2f, 8f);
                unitPlacer.placeUnits(placementMask, UnitPlacer.T2_Land, army17, army17Wreckage, 8f, 12f);
                unitPlacer.placeUnits(placementMask, UnitPlacer.T3_Land, army17, army17Wreckage, 30f, 40f);
            });
        }
    }
}
