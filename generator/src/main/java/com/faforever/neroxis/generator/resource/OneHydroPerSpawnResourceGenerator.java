package com.faforever.neroxis.generator.resource;

import com.faforever.neroxis.util.DebugUtil;

public class OneHydroPerSpawnResourceGenerator extends BasicResourceGenerator {
    @Override
    public void placeResources() {
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "generateResources", () -> {
            hydroPlacer.placeOneHydroPerPlayer(resourceMask.getFinalMask());
        });
    }
}
