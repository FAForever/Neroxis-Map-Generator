package com.faforever.neroxis.generator.prop;

import com.faforever.neroxis.generator.Visibility;
import com.faforever.neroxis.map.Army;
import com.faforever.neroxis.map.Group;
import com.faforever.neroxis.map.SymmetryType;
import com.faforever.neroxis.map.placement.UnitPlacer;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.Pipeline;

import java.io.IOException;

public class NeutralCivPropGenerator extends BasicPropGenerator {
    protected BooleanMask civReclaimMask;
    protected BooleanMask noCivs;

    @Override
    protected void afterInitialize() {
        super.afterInitialize();
        civReclaimMask = new BooleanMask(1, random.nextLong(), symmetrySettings, "civReclaimMask", true);

        noCivs = new BooleanMask(1, random.nextLong(), symmetrySettings);
    }

    @Override
    public void placeUnits() {
        if ((generatorParameters.visibility() != Visibility.UNEXPLORED)) {
            generateUnitExclusionMasks();
            Pipeline.await(civReclaimMask);
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
                          .fillCenter(32, false, SymmetryType.TEAM)
                          .fillEdge(64, false);
        } else {
            civReclaimMask.setSize(mapSize + 1);
        }
    }

    protected void generateUnitExclusionMasks() {
        noCivs.init(unbuildable.getFinalMask());
        noCivs.inflate(12);
        generateExclusionZones(noCivs, 96, 32, 32);
    }
}
