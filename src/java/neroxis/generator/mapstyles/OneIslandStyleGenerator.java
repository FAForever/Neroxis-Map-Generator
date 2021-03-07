package neroxis.generator.mapstyles;

import neroxis.map.MapParameters;
import neroxis.map.SymmetryType;

import java.util.Random;

public strictfp class OneIslandStyleGenerator extends PathedStyleGenerator {

    public OneIslandStyleGenerator(MapParameters mapParameters, Random random) {
        super(mapParameters, random);
        mountainBrushSize = 32;
        mountainBrushDensity = .1f;
        mountainBrushIntensity = 10;
        spawnSize = 48;
    }

    protected void landInit() {
        float normalizedLandDensity = MapStyle.CENTER_LAKE.getStyleConstraints().getLandDensityRange().normalize(landDensity);
        int minMiddlePoints = 2;
        int maxMiddlePoints = 4;
        int numTeamConnections = (int) (4 * normalizedLandDensity + 4) / symmetrySettings.getSpawnSymmetry().getNumSymPoints();
        int numTeammateConnections = (int) (2 * normalizedLandDensity + 2) / symmetrySettings.getSpawnSymmetry().getNumSymPoints();
        int numWalkers = (int) (8 * normalizedLandDensity + 8) / symmetrySettings.getSpawnSymmetry().getNumSymPoints();
        int bound = (int) (mapSize / 64 * (16 * (random.nextFloat() * .25f + (1 - normalizedLandDensity) * .75f))) + mapSize / 8;
        float maxStepSize = mapSize / 128f;
        land.setSize(mapSize + 1);

        pathInCenterBounds(land, maxStepSize, numWalkers, maxMiddlePoints, bound, (float) (StrictMath.PI / 2));
        land.combine(connections.copy().fillEdge((int) (mapSize / 8 * (1 - normalizedLandDensity) + mapSize / 8), false)
                .inflate(mapSize / 64f).smooth(12, .125f));
        connectTeams(land, minMiddlePoints, maxMiddlePoints, numTeamConnections, maxStepSize);
        connectTeammates(land, maxMiddlePoints, numTeammateConnections, maxStepSize);
        land.inflate(mapSize / 128f).setSize(mapSize / 8);
        land.grow(.5f, SymmetryType.SPAWN, 8).erode(.5f, SymmetryType.SPAWN, 6);
        if (mapSize > 512) {
            land.erode(.5f, SymmetryType.SPAWN, 4);
        }
        land.setSize(mapSize + 1);
        land.smooth(mapSize / 64, .75f);
    }
}


