package com.faforever.neroxis.generator;

import com.faforever.neroxis.generator.style.StyleGenerator;
import com.faforever.neroxis.map.SCMap;

record GenerationResults(
        SCMap map,
        StyleGenerator styleGenerator
) {}
