package neroxis.generator.mapstyles;

import neroxis.map.MapParameters;

import java.util.Random;

public class LittleMountainStyleGenerator extends DefaultStyleGenerator {

    public LittleMountainStyleGenerator(MapParameters mapParameters, Random random) {
        super(mapParameters, random);
        mountainBrushSize = 24;
        mountainBrushDensity = .35f;
        mountainBrushIntensity = 8;
    }

    protected void mountainInit() {
        mountains.setSize(mapSize / 4);

        mountains.randomWalk((int) (mountainDensity * 200 / symmetrySettings.getTerrainSymmetry().getNumSymPoints()), mapSize / 32);

        mountains.setSize(mapSize + 1);
    }

}
