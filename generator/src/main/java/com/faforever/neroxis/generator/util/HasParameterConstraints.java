package com.faforever.neroxis.generator.util;

import com.faforever.neroxis.generator.ParameterConstraints;

public interface HasParameterConstraints {

    default ParameterConstraints getParameterConstraints() {
        return ParameterConstraints.builder().build();
    }

}
