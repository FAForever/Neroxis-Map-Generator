package neroxis.generator.mapstyles;

import neroxis.map.MapParameters;
import neroxis.map.SymmetryType;
import neroxis.util.Vector2f;

import java.util.Random;

public strictfp class LandBridgeStyleGenerator extends PathedStyleGenerator {

    public LandBridgeStyleGenerator(MapParameters mapParameters, Random random) {
        super(mapParameters, random);
        teamSeparation = mapSize / 2;
        spawnSeparation = mapSize / 8f;
    }

    protected void landInit() {
        float maxStepSize = mapSize / 128f;
        int numPaths = 32 / spawnCount;

        land.setSize(mapSize + 1);
        connectTeammates(land, 8, 2, maxStepSize);
        connectTeams(land, 0, 2, 1, maxStepSize);
        map.getSpawns().forEach(spawn -> {
            pathAroundPoint(land, new Vector2f(spawn.getPosition()), maxStepSize, numPaths, 4, 196, (float) (StrictMath.PI / 2f));
        });
        land.inflate(maxStepSize);
        land.setSize(mapSize / 8);
        land.grow(.5f, SymmetryType.SPAWN, 8);
        land.setSize(mapSize + 1);
        land.smooth(8);
    }

    protected void plateausInit() {
        float maxStepSize = mapSize / 128f;
        int maxMiddlePoints = 2;
        int numPaths = (int) (16 * plateauDensity) / symmetrySettings.getSpawnSymmetry().getNumSymPoints();
        int bound = mapSize / 4;
        plateaus.setSize(mapSize + 1);

        pathInEdgeBounds(plateaus, maxStepSize, numPaths, maxMiddlePoints, bound, (float) (StrictMath.PI / 2));
        plateaus.inflate(mapSize / 256f).setSize(mapSize / 4);
        plateaus.grow(.5f, SymmetryType.TERRAIN, 4).setSize(mapSize + 1);
        plateaus.smooth(12);
    }
}

