package neroxis.generator.mapstyles;

import neroxis.map.MapParameters;
import neroxis.map.SymmetryType;

import java.util.Random;

public class MountainRangeStyleGenerator extends DefaultStyleGenerator {

    public MountainRangeStyleGenerator(MapParameters mapParameters, Random random) {
        super(mapParameters, random);
        mountainBrushSize = mapSize / 16;
        mountainBrushDensity = 2f;
        mountainBrushIntensity = 2;
    }

    protected void plateausInit() {
        float maxStepSize = mapSize / 128f;
        int maxMiddlePoints = 16;
        int numPaths = (int) (12 * plateauDensity) / symmetrySettings.getSpawnSymmetry().getNumSymPoints();
        int bound = 0;
        plateaus.setSize(mapSize + 1);

        pathInCenterBounds(plateaus, maxStepSize, numPaths, maxMiddlePoints, bound, (float) (StrictMath.PI / 2));
        plateaus.inflate(mapSize / 256f).setSize(mapSize / 4);
        plateaus.grow(.5f, SymmetryType.TERRAIN, 4).setSize(mapSize + 1);
        plateaus.smooth(12);
    }

    protected void mountainInit() {
        mountains.setSize(mapSize / 4);

        mountains.progressiveWalk((int) (mountainDensity * 25 / symmetrySettings.getTerrainSymmetry().getNumSymPoints()), mapSize / 4);

        mountains.setSize(mapSize + 1);
    }

}
