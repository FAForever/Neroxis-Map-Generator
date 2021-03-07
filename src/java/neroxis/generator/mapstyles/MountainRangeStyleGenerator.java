package neroxis.generator.mapstyles;

import neroxis.map.MapParameters;

import java.util.Random;

public strictfp class MountainRangeStyleGenerator extends PathedPlateauStyleGenerator {

    public MountainRangeStyleGenerator(MapParameters mapParameters, Random random) {
        super(mapParameters, random);
        mountainBrushSize = mapSize / 16;
        mountainBrushDensity = 2f;
        mountainBrushIntensity = 2;
    }

    protected void mountainInit() {
        float normalizedMountainDensity = MapStyle.LITTLE_MOUNTAIN.getStyleConstraints().getMountainDensityRange().normalize(mountainDensity);
        mountains.setSize(mapSize / 4);

        mountains.progressiveWalk((int) (normalizedMountainDensity * 25 / symmetrySettings.getTerrainSymmetry().getNumSymPoints()) + 4, mapSize / 4);

        mountains.setSize(mapSize + 1);
    }

}
