package com.faforever.neroxis.generator;

import com.faforever.neroxis.generator.prop.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.Supplier;

@Getter
@AllArgsConstructor
public enum PropGeneratorSupplier {
    BASIC(BasicPropGenerator.class, BasicPropGenerator::new),
    ENEMY_CIV(EnemyCivPropGenerator.class, EnemyCivPropGenerator::new),
    HIGH_RECLAIM(HighReclaimPropGenerator.class, HighReclaimPropGenerator::new),
    LARGE_BATTLE(LargeBattlePropGenerator.class, LargeBattlePropGenerator::new),
    NAVY_WRECKS(NavyWrecksPropGenerator.class, NavyWrecksPropGenerator::new),
    NEUTRAL_CIV(NeutralCivPropGenerator.class, NeutralCivPropGenerator::new),
    ROCK_FIELD(RockFieldPropGenerator.class, RockFieldPropGenerator::new),
    SMALL_BATTLE(SmallBattlePropGenerator.class, SmallBattlePropGenerator::new);

    private final Class<? extends com.faforever.neroxis.generator.prop.PropGenerator> generatorClass;
    private final Supplier<com.faforever.neroxis.generator.prop.PropGenerator> generatorSupplier;
}
