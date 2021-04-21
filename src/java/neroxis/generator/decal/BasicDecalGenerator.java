package neroxis.generator.decal;

import neroxis.generator.terrain.TerrainGenerator;
import neroxis.map.BinaryMask;
import neroxis.map.MapParameters;
import neroxis.map.SCMap;
import neroxis.map.SymmetrySettings;
import neroxis.util.Pipeline;
import neroxis.util.Util;

public class BasicDecalGenerator extends DecalGenerator {
    protected BinaryMask fieldDecal;
    protected BinaryMask slopeDecal;

    @Override
    public void initialize(SCMap map, long seed, MapParameters mapParameters, TerrainGenerator terrainGenerator) {
        super.initialize(map, seed, mapParameters, terrainGenerator);
        SymmetrySettings symmetrySettings = mapParameters.getSymmetrySettings();
        fieldDecal = new BinaryMask(1, random.nextLong(), symmetrySettings, "fieldDecal", true);
        slopeDecal = new BinaryMask(1, random.nextLong(), symmetrySettings, "slopeDecal", true);
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
            decalPlacer.placeDecals((BinaryMask) fieldDecal.getFinalMask(), mapParameters.getBiome().getDecalMaterials().getFieldNormals(), 32, 32, 32, 64);
            decalPlacer.placeDecals((BinaryMask) fieldDecal.getFinalMask(), mapParameters.getBiome().getDecalMaterials().getFieldAlbedos(), 64, 128, 24, 48);
            decalPlacer.placeDecals((BinaryMask) slopeDecal.getFinalMask(), mapParameters.getBiome().getDecalMaterials().getSlopeNormals(), 16, 32, 16, 32);
            decalPlacer.placeDecals((BinaryMask) slopeDecal.getFinalMask(), mapParameters.getBiome().getDecalMaterials().getSlopeAlbedos(), 64, 128, 32, 48);

        });
    }
}
