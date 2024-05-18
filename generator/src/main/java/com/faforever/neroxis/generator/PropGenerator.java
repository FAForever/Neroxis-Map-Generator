package com.faforever.neroxis.generator;

import com.faforever.neroxis.generator.prop.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.Supplier;

@Getter
@AllArgsConstructor
public enum PropGenerator {
    BASIC(BasicPropGenerator.class, BasicPropGenerator::new, 1),
    ENEMY_CIV(EnemyCivPropGenerator.class, EnemyCivPropGenerator::new, 1),
    HIGH_RECLAIM(HighReclaimPropGenerator.class, HighReclaimPropGenerator::new, 1),
    LARGE_BATTLE(LargeBattlePropGenerator.class, LargeBattlePropGenerator::new, 1),
    NAVY_WRECKS(NavyWrecksPropGenerator.class, NavyWrecksPropGenerator::new, 1),
    NEUTRAL_CIV(NeutralCivPropGenerator.class, NeutralCivPropGenerator::new, 1),
    ROCK_FIELD(RockFieldPropGenerator.class, RockFieldPropGenerator::new, 1),
    SMALL_BATTLE(SmallBattlePropGenerator.class, SmallBattlePropGenerator::new, 1);

    private final Class<? extends com.faforever.neroxis.generator.prop.PropGenerator> generatorClass;
    private final Supplier<com.faforever.neroxis.generator.prop.PropGenerator> generatorSupplier;
    private final float weight;
}
