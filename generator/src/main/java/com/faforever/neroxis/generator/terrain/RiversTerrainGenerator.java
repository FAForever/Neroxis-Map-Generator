package com.faforever.neroxis.generator.terrain;

import com.faforever.neroxis.brushes.Brushes;
import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.util.vector.Vector2;
import com.faforever.neroxis.util.vector.Vector3;

public class RiversTerrainGenerator extends BasicTerrainGenerator {

    protected BooleanMask riverExclusionMask;
    protected BooleanMask riverMountains;
    protected BooleanMask riverMask;

    @Override
    public void initialize(SCMap map, long seed, GeneratorParameters generatorParameters,
                           SymmetrySettings symmetrySettings) {
        super.initialize(map, seed, generatorParameters, symmetrySettings);
        plateauHeight = 9f;
        plateauBrushSize = 96;
        plateauBrushIntensity = 8f;
        plateauBrushDensity = 0.3f;

        plateauDensity = 0.83f;

        rampDensity = random.nextFloat() * 0.2f + 0.8f;

        mountainBrushSize = 24;
        mountainBrushDensity = 8f;
        mountainBrushIntensity = 0.4f;
    }

    @Override
    protected void landSetup() {
        int mapSize = map.getSize();

        land.setSize(mapSize);

        int riversScale = mapSize / 64;
        FloatMask rivers = new FloatMask(mapSize, getRandom().nextLong(), land.getSymmetrySettings(), "rivers", true);
        rivers.addPerlinNoise(StrictMath.min(96 + riversScale, mapSize), 1);
        riverMask = rivers.copyAsBooleanMask(0.5f, 0.65f);

        FloatMask riverExclusion = new FloatMask(mapSize, getRandom().nextLong(), land.getSymmetrySettings(), "riversExclusion", true);
        riverExclusion.addPerlinNoise(StrictMath.min(256, mapSize), 1);
        float exclusionThickness = 0.06f * ((float)mapSize / 256f);
        riverExclusionMask = riverExclusion.copyAsBooleanMask(0.5f, 0.5f + exclusionThickness);
        riverMask.subtract(riverExclusionMask);
        riverExclusionMask.setSize(mapSize+1);

        riverMask.invert();
        riverMask.blur(10);

        riverMask.add(connections.copy().dilute(1, 10).setSize(riverMask.getSize()));

        riverMask.erode(0.3f, 10);

        String[] SPAWN_MASK_BRUSHES = {
                "mountain4.png",
                "mountain7.png",
                "mountain8.png",
                "mountain9.png"
        };
        map.getSpawns().forEach(spawn -> {
            if (spawn.getTeamID() == 0) {
                Vector3 location = spawn.getPosition();
                String brush = SPAWN_MASK_BRUSHES[StrictMath.abs(random.nextInt()) % SPAWN_MASK_BRUSHES.length];
                riverMask.addBrush(new Vector2(location.x(), location.z()), brush, 1f, 256f, 150);
            }
        });

        land.add(riverMask);

        land.setSize(mapSize+1);
    }

    @Override
    protected void plateausSetup() {
        int mapSize = map.getSize();
        spawnPlateauMask.clear();
        plateaus.setSize(mapSize);

        FloatMask plats = new FloatMask(mapSize, getRandom().nextLong(), plateaus.getSymmetrySettings(), "mountainplateaus", true);
        plats.addPerlinNoise(32, 1f);
        BooleanMask platMountains = plats.copyAsBooleanMask(plateauDensity);

        BooleanMask plateauExclusion = new BooleanMask(mapSize, random.nextLong(), getSymmetrySettings(), "plateauExclusion", true);
        String[] SPAWN_MASK_BRUSHES = {
                "mountain4.png",
                "mountain7.png",
                "mountain8.png",
                "mountain9.png"
        };
        map.getSpawns().forEach(spawn -> {
            if (spawn.getTeamID() == 0) {
                Vector3 location = spawn.getPosition();
                String brush = SPAWN_MASK_BRUSHES[StrictMath.abs(random.nextInt()) % SPAWN_MASK_BRUSHES.length];
                plateauExclusion.addBrush(new Vector2(location.x(), location.z()), brush, 1f, 256f, 150);
            }
        });
        platMountains.subtract(plateauExclusion);
        platMountains.setSize(mapSize);
        plateaus.add(platMountains);

        plateaus.setSize(mapSize + 1);
        plateaus.subtract(riverMask.copy().invert().blur(10).setSize(plateaus.getSize()));
    }

    @Override
    protected void setupPlateauHeightmapPipeline() {
        String brush = Brushes.GENERATOR_BRUSHES.get(random.nextInt(Brushes.GENERATOR_BRUSHES.size()));

        heightmapPlateaus.setSize(map.getSize() + 1);
        heightmapPlateaus.useBrushWithinAreaWithDensity(plateaus, brush, plateauBrushSize, plateauBrushDensity,
                                                        plateauBrushIntensity, false).clampMax(plateauHeight);

        BooleanMask inverseRiverMask = riverMask.copy().invert().setSize(heightmapPlateaus.getSize()).blur(6);
        heightmapPlateaus
                .subtract(inverseRiverMask, heightmapPlateaus)
                .subtract(inverseRiverMask, 0.45f)
                .blur(1);


        BooleanMask paintedPlateaus = heightmapPlateaus.copyAsBooleanMask(plateauHeight - 3);

        land.add(paintedPlateaus);
        plateaus.init(paintedPlateaus);
        plateaus
                .subtract(spawnLandMask)
                .subtract(riverMask.copy().invert().setSize(plateaus.getSize()))
                .add(spawnPlateauMask);

        heightmapPlateaus
                .add(plateaus, 2f)
                .clampMax(plateauHeight)
                .blur(1, plateaus);


        BooleanMask plateauBase = heightmapPlateaus.copyAsBooleanMask(1f);

        heightmapPlateaus
                .blur(4, plateauBase.copy()
                                    .inflate(96)
                                    .subtract(plateauBase.copy().inflate(4)));
    }

    @Override
    protected void initRamps() {
        ramps.setSize(map.getSize() + 1);

        ramps = plateaus.copy();
        ramps.outline();

        FloatMask rampExclusion = new FloatMask(ramps.getSize(), random.nextLong(), this.symmetrySettings, "rampExclusion", true);
        rampExclusion.addPerlinNoise(64, 1);
        BooleanMask rampExclusionMask = rampExclusion.copyAsBooleanMask(0.4f, 0.8f);

        ramps.subtract(rampExclusionMask);
    }

    @Override
    protected void blurRamps() {
        BooleanMask inflatedRamps = ramps.copy();
        heightmap.blur(48, inflatedRamps)
                 .blur(32, inflatedRamps.inflate(5))
                 .blur(4, inflatedRamps.copy().outline().inflate(4))
                 .blur(8, inflatedRamps.copy().outline().inflate(8))
                 .blur(4, inflatedRamps.copy().outline().inflate(16))
                 .clampMin(0f)
                 .clampMax(255f);
    }

    @Override
    protected void mountainSetup() {
        int mapSize = map.getSize();

        mountains.setSize(mapSize / 2);


        riverMountains = riverMask.copy()
                                  .erode(1f, 5)
                                  .outline()
                                  .inflate(2);

        FloatMask riverMountainExclusion = new FloatMask(mapSize, random.nextLong(), this.symmetrySettings, "riverMountainExclusion", true);
        riverMountainExclusion.addPerlinNoise(128, 1);
        BooleanMask riverMountainExclusionMask = riverMountainExclusion.copyAsBooleanMask(0.3f, 0.8f);

        riverMountains.subtract(riverMountainExclusionMask);
        riverMountains.setSize(mountains.getSize());
        mountains.add(riverMountains);
        mountains.setSize(mapSize + 1);
    }

    @Override
    protected void spawnTerrainSetup() {
        int mapSize = map.getSize();
        spawnPlateauMask.setSize(mapSize / 4);
        spawnPlateauMask.erode(.5f, 4).dilute(.5f, 8);
        spawnPlateauMask.erode(.5f).setSize(mapSize + 1);
        spawnPlateauMask.blur(4);

        spawnLandMask.setSize(mapSize / 4);
        spawnLandMask.erode(.25f, mapSize / 128).dilute(.5f, 4);
        spawnLandMask.erode(.5f).setSize(mapSize + 1);
        spawnLandMask.blur(4);

        plateaus.subtract(spawnLandMask).add(spawnPlateauMask);
        land.add(spawnLandMask).add(spawnPlateauMask);

        mountains.subtract(spawnLandMask.copy().inflate(mountainBrushSize / 4f));

        plateaus.multiply(land).subtract(spawnLandMask).add(spawnPlateauMask);
        land.add(plateaus).add(spawnLandMask).add(spawnPlateauMask);
    }

    @Override
    protected void spawnMaskSetup() {
        map.getSpawns().forEach(spawn -> {
            Vector3 location = spawn.getPosition();
            spawnLandMask.fillCircle(location, 10, true);
        });
    }

}
