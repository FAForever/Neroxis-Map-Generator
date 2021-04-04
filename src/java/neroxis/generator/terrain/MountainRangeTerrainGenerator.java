package neroxis.generator.terrain;

import neroxis.generator.ParameterConstraints;
import neroxis.map.MapParameters;
import neroxis.map.SCMap;
import neroxis.map.SymmetrySettings;

public strictfp class MountainRangeTerrainGenerator extends PathedPlateauTerrainGenerator {

    public MountainRangeTerrainGenerator() {
        parameterConstraints = ParameterConstraints.builder()
                .landDensity(.75f, 1f)
                .mountainDensity(.5f, 1)
                .plateauDensity(0, .5f)
                .mexDensity(.375f, 1)
                .mapSizes(256, 512)
                .build();
    }

    public void initialize(SCMap map, long seed, MapParameters mapParameters) {
        super.initialize(map, seed, mapParameters);
        mountainBrushSize = map.getSize() / 16;
        mountainBrushDensity = 2f;
        mountainBrushIntensity = 2;
    }

    protected void mountainSetup() {
        SymmetrySettings symmetrySettings = mapParameters.getSymmetrySettings();
        int mapSize = map.getSize();
        float normalizedMountainDensity = parameterConstraints.getMountainDensityRange().normalize(mapParameters.getMountainDensity());
        mountains.setSize(mapSize / 2);

        mountains.progressiveWalk((int) (normalizedMountainDensity * 2 / symmetrySettings.getTerrainSymmetry().getNumSymPoints()) + 6, mapSize / 4);
        mountains.inflate(2);

        mountains.setSize(mapSize + 1);
    }

}
