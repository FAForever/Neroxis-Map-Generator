package neroxis.generator.mapstyles;

import neroxis.map.MapParameters;
import neroxis.map.SymmetryType;

import java.util.Random;

public strictfp abstract class PathedPlateauStyleGenerator extends DefaultStyleGenerator {

    public PathedPlateauStyleGenerator(MapParameters mapParameters, Random random) {
        super(mapParameters, random);
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
}
