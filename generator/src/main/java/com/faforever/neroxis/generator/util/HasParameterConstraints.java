package com.faforever.neroxis.generator.util;

import com.faforever.neroxis.generator.ParameterConstraints;

public interface HasParameterConstraints {

    default ParameterConstraints parameterConstraints() {
        return ParameterConstraints.ANY;
    }

}
