package com.faforever.neroxis.generator.prop;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.Visibility;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.map.Army;
import com.faforever.neroxis.map.Group;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.placement.UnitPlacer;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.util.DebugUtil;

import java.io.IOException;
import java.util.random.RandomGenerator;

public class NeutralCivPropGenerator extends BasicPropGenerator {
    protected BooleanMask civReclaimMask;

    @Override
    public void initialize(SCMap map, RandomGenerator.SplittableGenerator random,
                           GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings, TerrainGenerator terrainGenerator) {
        super.initialize(map, random, generatorParameters, symmetrySettings, terrainGenerator);
        civReclaimMask = new BooleanMask(1, random.split(), symmetrySettings, "civReclaimMask");
    }

    @Override
    public void placeUnits() {
        if ((generatorParameters.visibility() != Visibility.UNEXPLORED)) {
            BooleanMask noCivs = generateUnitExclusionMasks();
            DebugUtil.timedRun("com.faforever.neroxis.map.generator", "placeCivs", () -> {
                Army civilian = new Army("NEUTRAL_CIVILIAN");
                Group civilianInitial = new Group("INITIAL");
                civilian.addGroup(civilianInitial);
                map.addArmy(civilian);
                try {
                    unitPlacer.placeBases(civReclaimMask.getFinalMask().subtract(noCivs), UnitPlacer.MEDIUM_RECLAIM,
                                          civilian, civilianInitial, 256f);
                } catch (IOException e) {
                    System.out.println("Could not generate bases due to lua parsing error");
                    e.printStackTrace();
                }
            });
        }
    }

    @Override
    protected void generatePropExclusionMasks() {
        super.generatePropExclusionMasks();
        noProps.add(civReclaimMask.getFinalMask());
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
            civReclaimMask.multiply(passableLand.copy().subtract(unbuildable).deflate(24))
                          .fillCenter(32, false)
                          .fillEdge(64, false);
        } else {
            civReclaimMask.setSize(mapSize + 1);
        }
    }

    protected BooleanMask generateUnitExclusionMasks() {
        BooleanMask noCivs = new BooleanMask(1, random.split(), symmetrySettings, "noCivs");
        noCivs.init(unbuildable.getFinalMask());
        noCivs.inflate(12);
        generateExclusionZones(noCivs, 96, 32, 32);
        return noCivs;
    }
}
