package com.faforever.neroxis.generator.prop;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.map.Army;
import com.faforever.neroxis.map.Group;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.placement.UnitPlacer;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.Pipeline;

import java.io.IOException;
import java.util.ArrayList;

public strictfp class EnemyCivPropGenerator extends BasicPropGenerator {

    protected BooleanMask baseMask;
    protected BooleanMask noBases;

    public EnemyCivPropGenerator() {
        parameterConstraints = ParameterConstraints.builder()
                .mountainDensity(0f, .75f)
                .plateauDensity(0f, .5f)
                .rampDensity(.5f, 1f)
                .build();
        weight = .5f;
    }

    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, generatorParameters, terrainGenerator);
        SymmetrySettings symmetrySettings = generatorParameters.getSymmetrySettings();
        baseMask = new BooleanMask(1, random.nextLong(), symmetrySettings, "baseMask", true);
        noBases = new BooleanMask(1, random.nextLong(), symmetrySettings);
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
            baseMask.multiply(passableLand.copy().subtract(unbuildable).deflate(24)).fillCenter(32, false).fillEdge(32, false);
        } else {
            baseMask.setSize(mapSize + 1);
        }
    }

    protected void generatePropExclusionMasks() {
        super.generatePropExclusionMasks();
        noProps.add(baseMask.getFinalMask());
    }

    protected void generateUnitExclusionMasks() {
        noBases.init(unbuildable.getFinalMask());
        noBases.inflate(12);
        generateExclusionZones(noBases, 128, 32, 32);
    }

    @Override
    public void placeUnits() {
        if (!generatorParameters.isUnexplored()) {
            generateUnitExclusionMasks();
            Pipeline.await(baseMask);
            DebugUtil.timedRun("com.faforever.neroxis.map.generator", "placeBases", () -> {
                Army army17 = new Army("ARMY_17", new ArrayList<>());
                Group army17Initial = new Group("INITIAL", new ArrayList<>());
                army17.addGroup(army17Initial);
                map.addArmy(army17);
                try {
                    unitPlacer.placeBases(baseMask.getFinalMask().subtract(noBases), UnitPlacer.MEDIUM_ENEMY, army17, army17Initial, 512f);
                } catch (IOException e) {
                    System.out.println("Could not generate bases due to lua parsing error");
                    e.printStackTrace();
                }
            });
        }
    }
}
