package com.faforever.neroxis.generator.decal;

import com.faforever.neroxis.util.DebugUtil;
import com.faforever.neroxis.util.Pipeline;

public class BasicDecalGenerator extends DecalGenerator {
    @Override
    public void placeDecals() {
        Pipeline.await(fieldDecal, slopeDecal);
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "placeDecals", () -> {
            decalPlacer.placeDecals(fieldDecal.getFinalMask(),
                                    map.getBiome().decalMaterials().fieldNormals(), 32, 32, 24, 32);
            decalPlacer.placeDecals(fieldDecal.getFinalMask(),
                                    map.getBiome().decalMaterials().fieldAlbedos(), 64, 128, 24, 32);
            decalPlacer.placeDecals(slopeDecal.getFinalMask(),
                                    map.getBiome().decalMaterials().slopeNormals(), 16, 32, 16, 32);
            decalPlacer.placeDecals(slopeDecal.getFinalMask(),
                                    map.getBiome().decalMaterials().slopeAlbedos(), 64, 128, 32, 48);
        });
    }

    @Override
    public void setupPipeline() {
        fieldDecal.init(passableLand);
        slopeDecal.init(slope, .25f);
        fieldDecal.subtract(slopeDecal.copy().inflate(16));
    }
}
