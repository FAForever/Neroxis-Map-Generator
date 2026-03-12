package com.faforever.neroxis.generator.resource;

import com.faforever.neroxis.util.DebugUtil;

public class OneMexPerSpawnResourceGenerator extends BasicResourceGenerator {
    @Override
    public void placeResources() {
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "generateResources", () -> {
            mexPlacer.placeOneMexPerPlayer(resourceMask.getFinalMask());
        });
    }
}
