package neroxis.generator.mapstyles;

import neroxis.map.ConcurrentBinaryMask;
import neroxis.map.MapParameters;
import neroxis.map.SymmetryType;
import neroxis.util.Vector2f;

import java.util.Random;

public strictfp class SmallIslandsStyleGenerator extends PathedStyleGenerator {

    public SmallIslandsStyleGenerator(MapParameters mapParameters, Random random) {
        super(mapParameters, random);
        spawnSize = 64;
        teamSeparation = mapSize / 2;
    }

    protected void landInit() {
        float normalizedLandDensity = MapStyle.SMALL_ISLANDS.getLandDensityRange().normalize(landDensity);
        int maxMiddlePoints = 4;
        int numPaths = (int) (4 * normalizedLandDensity + 4) / symmetrySettings.getSpawnSymmetry().getNumSymPoints();
        int bound = ((int) (mapSize / 16 * (random.nextFloat() * .25f + normalizedLandDensity * .75f)) + mapSize / 16);
        float maxStepSize = mapSize / 128f;

        ConcurrentBinaryMask islands = new ConcurrentBinaryMask(mapSize / 4, random.nextLong(), symmetrySettings, "islands");

        land.setSize(mapSize + 1);
        map.getSpawns().forEach(spawn -> pathAroundPoint(land, new Vector2f(spawn.getPosition()), maxStepSize, numPaths, maxMiddlePoints, bound, (float) StrictMath.PI / 2));
        land.inflate(maxStepSize).setSize(mapSize / 4);

        islands.randomWalk((int) (normalizedLandDensity * 40 / symmetrySettings.getTerrainSymmetry().getNumSymPoints()) + 2, mapSize / 8);

        land.combine(islands);
        land.grow(.5f, SymmetryType.SPAWN, 8);

        land.setSize(mapSize + 1);
        land.smooth(16);
    }
}


