package neroxis.generator.mapstyles;

import neroxis.map.MapParameters;
import neroxis.map.SymmetryType;

import java.util.Random;

public strictfp class DropPlateauStyleGenerator extends PathedStyleGenerator {

    public DropPlateauStyleGenerator(MapParameters mapParameters, Random random) {
        super(mapParameters, random);
        plateauHeight = 12f;
        plateauBrushIntensity = 16f;
        spawnSize = 32;
    }

    protected void plateausInit() {
        float normalizedPlateauDensity = MapStyle.LITTLE_MOUNTAIN.getStyleConstraints().getPlateauDensityRange().normalize(mountainDensity);
        spawnPlateauMask.clear();
        plateaus.setSize(mapSize / 4);

        plateaus.randomWalk((int) (normalizedPlateauDensity * 10 / symmetrySettings.getTerrainSymmetry().getNumSymPoints() + 2), mapSize * 4);
        plateaus.grow(.5f, SymmetryType.SPAWN, 4);

        plateaus.setSize(mapSize + 1);
        plateaus.minus(connections.copy().inflate(plateauBrushSize / 2f).smooth(12, .125f));
    }

    protected void initRamps() {
        ramps.setSize(mapSize + 1);
    }

    protected void setupCivilianPipeline() {
        baseMask.setSize(mapSize + 1);
        allBaseMask.setSize(mapSize + 1);

        if (!unexplored) {
            baseMask.setSize(mapSize + 1);
            civReclaimMask.init(plateaus.copy().deflate(16));
            civReclaimMask.fillCenter(32, false).fillEdge(32, false);
        } else {
            baseMask.setSize(mapSize + 1);
            civReclaimMask.setSize(mapSize + 1);
        }
        allBaseMask.combine(baseMask.copy().inflate(24)).combine(civReclaimMask.copy().inflate(24));
    }
}


