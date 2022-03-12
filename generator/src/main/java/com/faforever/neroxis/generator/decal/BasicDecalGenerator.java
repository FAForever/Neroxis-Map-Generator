package com.faforever.neroxis.generator.decal;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.terrain.TerrainGenerator;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.Pipeline;

public strictfp class BasicDecalGenerator extends DecalGenerator {
    protected BooleanMask fieldDecal;
    protected BooleanMask slopeDecal;

    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, generatorParameters, terrainGenerator);
        SymmetrySettings symmetrySettings = generatorParameters.getSymmetrySettings();
        fieldDecal = new BooleanMask(1, random.nextLong(), symmetrySettings, "fieldDecal", true);
        slopeDecal = new BooleanMask(1, random.nextLong(), symmetrySettings, "slopeDecal", true);
    }

    @Override
    public void setupPipeline() {
        fieldDecal.init(passableLand);
        slopeDecal.init(slope, .25f);
        fieldDecal.subtract(slopeDecal.copy().inflate(16));
    }

    @Override
    public void placeDecals() {
        Pipeline.await(fieldDecal, slopeDecal);
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "placeDecals", () -> {
            decalPlacer.placeDecals(fieldDecal.getFinalMask(), generatorParameters.getBiome().getDecalMaterials().getFieldNormals(), 32, 32, 24, 32);
            decalPlacer.placeDecals(fieldDecal.getFinalMask(), generatorParameters.getBiome().getDecalMaterials().getFieldAlbedos(), 64, 128, 24, 32);
            decalPlacer.placeDecals(slopeDecal.getFinalMask(), generatorParameters.getBiome().getDecalMaterials().getSlopeNormals(), 16, 32, 16, 32);
            decalPlacer.placeDecals(slopeDecal.getFinalMask(), generatorParameters.getBiome().getDecalMaterials().getSlopeAlbedos(), 64, 128, 32, 48);

        });
    }
}
