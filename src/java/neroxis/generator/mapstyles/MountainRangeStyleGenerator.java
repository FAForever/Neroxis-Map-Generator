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
        float normalizedMountainDensity = MapStyle.MOUNTAIN_RANGE.getStyleConstraints().getMountainDensityRange().normalize(mountainDensity);
        mountains.setSize(mapSize / 2);

        mountains.progressiveWalk((int) (normalizedMountainDensity * 2 / symmetrySettings.getTerrainSymmetry().getNumSymPoints()) + 6, mapSize / 4);
        mountains.inflate(2);

        mountains.setSize(mapSize + 1);
    }

}
