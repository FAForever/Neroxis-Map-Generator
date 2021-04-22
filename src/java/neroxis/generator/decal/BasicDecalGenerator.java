package neroxis.generator.decal;

import neroxis.generator.terrain.TerrainGenerator;
import neroxis.map.BooleanMask;
import neroxis.map.MapParameters;
import neroxis.map.SCMap;
import neroxis.map.SymmetrySettings;
import neroxis.util.Pipeline;
import neroxis.util.Util;

public class BasicDecalGenerator extends DecalGenerator {
    protected BooleanMask fieldDecal;
    protected BooleanMask slopeDecal;

    @Override
    public void initialize(SCMap map, long seed, MapParameters mapParameters, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, mapParameters, terrainGenerator);
        SymmetrySettings symmetrySettings = mapParameters.getSymmetrySettings();
        fieldDecal = new BooleanMask(1, random.nextLong(), symmetrySettings, "fieldDecal", true);
        slopeDecal = new BooleanMask(1, random.nextLong(), symmetrySettings, "slopeDecal", true);
    }

    @Override
    public void setupPipeline() {
        fieldDecal.init(passableLand);
        slopeDecal.init(slope, .25f);
        fieldDecal.minus(slopeDecal.copy().inflate(16));
    }

    @Override
    public void placeDecals() {
        Pipeline.await(fieldDecal, slopeDecal);
        Util.timedRun("neroxis.generator", "placeDecals", () -> {
            decalPlacer.placeDecals(fieldDecal.getFinalMask(), mapParameters.getBiome().getDecalMaterials().getFieldNormals(), 32, 32, 32, 64);
            decalPlacer.placeDecals(fieldDecal.getFinalMask(), mapParameters.getBiome().getDecalMaterials().getFieldAlbedos(), 64, 128, 24, 48);
            decalPlacer.placeDecals(slopeDecal.getFinalMask(), mapParameters.getBiome().getDecalMaterials().getSlopeNormals(), 16, 32, 16, 32);
            decalPlacer.placeDecals(slopeDecal.getFinalMask(), mapParameters.getBiome().getDecalMaterials().getSlopeAlbedos(), 64, 128, 32, 48);

        });
    }
}
