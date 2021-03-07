package neroxis.generator.mapstyles;

import neroxis.map.MapParameters;

import java.util.Random;

public strictfp class LittleMountainStyleGenerator extends PathedPlateauStyleGenerator {

    public LittleMountainStyleGenerator(MapParameters mapParameters, Random random) {
        super(mapParameters, random);
        mountainBrushSize = 24;
        mountainBrushDensity = .35f;
        mountainBrushIntensity = 8;
    }

    protected void mountainInit() {
        float normalizedMountainDensity = MapStyle.LITTLE_MOUNTAIN.getStyleConstraints().getMountainDensityRange().normalize(mountainDensity);
        mountains.setSize(mapSize / 4);

        mountains.randomWalk((int) (normalizedMountainDensity * 200 / symmetrySettings.getTerrainSymmetry().getNumSymPoints() + 20), mapSize / 32);

        mountains.setSize(mapSize + 1);
    }

}
