package com.faforever.neroxis.generator.resource;

import com.faforever.neroxis.util.DebugUtil;

public class OneHydroFourMexResourceGenerator extends BasicResourceGenerator
{
    @Override
    public void placeResources() {
        DebugUtil.timedRun("com.faforever.neroxis.map.generator", "generateResources", () -> {
            mexPlacer.placeMexes(getMexCount(), resourceMask.getFinalMask().subtract(mexDeadZone), waterResourceMask.getFinalMask(), true);
            hydroPlacer.placeHydros(generatorParameters.spawnCount(), resourceMask.getFinalMask().deflate(8), true);
        });
    }
}
