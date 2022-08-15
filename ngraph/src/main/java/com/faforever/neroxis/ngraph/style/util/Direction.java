package com.faforever.neroxis.ngraph.style.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Direction {
    NORTH(0x1), SOUTH(0x2), EAST(0x4), WEST(0x8);
    private final int mask;
}
