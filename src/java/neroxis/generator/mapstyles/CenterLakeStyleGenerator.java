package neroxis.generator.mapstyles;

import neroxis.map.ConcurrentBinaryMask;
import neroxis.map.MapParameters;
import neroxis.map.SymmetryType;

import java.util.Random;

public strictfp class CenterLakeStyleGenerator extends PathedStyleGenerator {

    public CenterLakeStyleGenerator(MapParameters mapParameters, Random random) {
        super(mapParameters, random);
        mountainBrushSize = 32;
        mountainBrushDensity = .05f;
        mountainBrushIntensity = 10;
        spawnSize = 48;
    }

    protected void landInit() {
        float normalizedLandDensity = MapStyle.CENTER_LAKE.getStyleConstraints().getLandDensityRange().normalize(landDensity);
        float maxStepSize = mapSize / 128f;
        int maxMiddlePoints = 8;
        int numWalkers = (int) (8 * (1 - normalizedLandDensity) + 8) / symmetrySettings.getSpawnSymmetry().getNumSymPoints();
        int bound = (int) (mapSize / 64 * (24 * (random.nextFloat() * .25f + normalizedLandDensity * .75f))) + mapSize / 8;
        land.setSize(mapSize + 1);
        land.invert();
        ConcurrentBinaryMask noLand = new ConcurrentBinaryMask(mapSize + 1, random.nextLong(), symmetrySettings, "noLand");

        pathInCenterBounds(noLand, maxStepSize, numWalkers, maxMiddlePoints, bound, (float) (StrictMath.PI / 2));
        noLand.inflate(1).setSize(mapSize / 4);
        noLand.grow(.5f, SymmetryType.TERRAIN, 10).setSize(mapSize + 1);
        noLand.smooth(mapSize / 64, .5f);
        land.minus(noLand);
    }
}


