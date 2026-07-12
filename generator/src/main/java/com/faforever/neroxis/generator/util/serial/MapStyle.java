package com.faforever.neroxis.generator.util.serial;

import com.faforever.neroxis.biomes.BiomeName;
import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.generator.WeightedOption;
import com.faforever.neroxis.generator.WeightedOptionsWithFallback;
import com.faforever.neroxis.generator.util.HasParameterConstraints;
import com.faforever.neroxis.util.MathUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;

import static com.faforever.neroxis.generator.util.serial.GeneratedMapNameEncoder.NUM_BINS;

public sealed interface MapStyle {

    String name();

    default WeightedOptionsWithFallback<TerrainStyle> getTerrainStyleOptions() {
        return WeightedOptionsWithFallback.of(TerrainStyle.BASIC_LAST);
    }

    default WeightedOptionsWithFallback<com.faforever.neroxis.biomes.BiomeName> getBiomeNameOptions() {
        return WeightedOptionsWithFallback.of(com.faforever.neroxis.biomes.BiomeName.BRIMSTONE,
                                              WeightedOption.of(com.faforever.neroxis.biomes.BiomeName.BRIMSTONE, 1f),
                                              WeightedOption.of(com.faforever.neroxis.biomes.BiomeName.DESERT, 1f),
                                              WeightedOption.of(com.faforever.neroxis.biomes.BiomeName.EARLYAUTUMN, 1f),
                                              WeightedOption.of(com.faforever.neroxis.biomes.BiomeName.FRITHEN, 1f),
                                              WeightedOption.of(com.faforever.neroxis.biomes.BiomeName.MARS, 1f),
                                              WeightedOption.of(com.faforever.neroxis.biomes.BiomeName.PRAYER, 1f),
                                              WeightedOption.of(com.faforever.neroxis.biomes.BiomeName.STONES, 1f),
                                              WeightedOption.of(com.faforever.neroxis.biomes.BiomeName.SUNSET, 1f),
                                              WeightedOption.of(com.faforever.neroxis.biomes.BiomeName.WINDINGRIVER,
                                                                1f),
                                              WeightedOption.of(com.faforever.neroxis.biomes.BiomeName.WONDER, 1f),
                                              WeightedOption.of(com.faforever.neroxis.biomes.BiomeName.CRYSTALLINE,
                                                                1f));
    }

    default WeightedOptionsWithFallback<ResourceStyle> getResourceStyleOptions() {
        return WeightedOptionsWithFallback.of(ResourceStyle.BASIC);
    }

    default WeightedOptionsWithFallback<PropStyle> getPropStyleOptions() {
        return WeightedOptionsWithFallback.of(PropStyle.BASIC);
    }

    @AllArgsConstructor
    enum Predefined implements MapStyle, HasParameterConstraints {
        BASIC(ParameterConstraints.ANY) {
            @Override
            public WeightedOptionsWithFallback<PropStyle> getPropStyleOptions() {
                return WeightedOptionsWithFallback.of(PropStyle.BASIC,
                                                      WeightedOption.of(PropStyle.BASIC, 1f),
                                                      WeightedOption.of(PropStyle.BOULDER_FIELD, .1f),
                                                      WeightedOption.of(PropStyle.ENEMY_CIV, .5f),
                                                      WeightedOption.of(PropStyle.HIGH_RECLAIM, .25f),
                                                      WeightedOption.of(PropStyle.LARGE_BATTLE, .5f),
                                                      WeightedOption.of(PropStyle.NAVY_WRECKS, 2f),
                                                      WeightedOption.of(PropStyle.NEUTRAL_CIV, 1f),
                                                      WeightedOption.of(PropStyle.ROCK_FIELD, 1f),
                                                      WeightedOption.of(PropStyle.SMALL_BATTLE, 1f));
            }
        },
        BIG_ISLANDS(ParameterConstraints.builder().mapSizes(768, 1024).build()) {
            @Override
            public WeightedOptionsWithFallback<TerrainStyle> getTerrainStyleOptions() {
                return WeightedOptionsWithFallback.of(TerrainStyle.BIG_ISLANDS);
            }

            @Override
            public WeightedOptionsWithFallback<PropStyle> getPropStyleOptions() {
                return WeightedOptionsWithFallback.of(PropStyle.BASIC,
                                                      WeightedOption.of(PropStyle.BASIC, 1f),
                                                      WeightedOption.of(PropStyle.BOULDER_FIELD, .1f),
                                                      WeightedOption.of(PropStyle.ENEMY_CIV, .5f),
                                                      WeightedOption.of(PropStyle.NAVY_WRECKS, 2),
                                                      WeightedOption.of(PropStyle.NEUTRAL_CIV, 1f),
                                                      WeightedOption.of(PropStyle.ROCK_FIELD, 1f),
                                                      WeightedOption.of(PropStyle.SMALL_BATTLE, 1f));
            }
        },
        CENTER_LAKE(ParameterConstraints.builder().mapSizes(384, 1024).build()) {
            @Override
            public WeightedOptionsWithFallback<TerrainStyle> getTerrainStyleOptions() {
                return WeightedOptionsWithFallback.of(TerrainStyle.CENTER_LAKE_LAST);
            }

            @Override
            public WeightedOptionsWithFallback<PropStyle> getPropStyleOptions() {
                return WeightedOptionsWithFallback.of(PropStyle.BASIC,
                                                      WeightedOption.of(PropStyle.BASIC, 1f),
                                                      WeightedOption.of(PropStyle.BOULDER_FIELD, .1f),
                                                      WeightedOption.of(PropStyle.ENEMY_CIV, .5f),
                                                      WeightedOption.of(PropStyle.NAVY_WRECKS, 2),
                                                      WeightedOption.of(PropStyle.NEUTRAL_CIV, 1f),
                                                      WeightedOption.of(PropStyle.ROCK_FIELD, 1f),
                                                      WeightedOption.of(PropStyle.SMALL_BATTLE, 1f));
            }
        },
        DROP_PLATEAU(ParameterConstraints.ANY) {
            @Override
            public WeightedOptionsWithFallback<TerrainStyle> getTerrainStyleOptions() {
                return WeightedOptionsWithFallback.of(TerrainStyle.DROP_PLATEAU_LAST);
            }

            @Override
            public WeightedOptionsWithFallback<PropStyle> getPropStyleOptions() {
                return WeightedOptionsWithFallback.of(PropStyle.BASIC,
                                                      WeightedOption.of(PropStyle.BASIC, 1f),
                                                      WeightedOption.of(PropStyle.BOULDER_FIELD, .1f),
                                                      WeightedOption.of(PropStyle.ENEMY_CIV, .5f),
                                                      WeightedOption.of(PropStyle.HIGH_RECLAIM, .5f),
                                                      WeightedOption.of(PropStyle.LARGE_BATTLE, .5f),
                                                      WeightedOption.of(PropStyle.NEUTRAL_CIV, 1f),
                                                      WeightedOption.of(PropStyle.ROCK_FIELD, 1f),
                                                      WeightedOption.of(PropStyle.SMALL_BATTLE, 1f));
            }
        },
        FLOODED(ParameterConstraints.builder().mapSizes(384, 1024).build()) {
            @Override
            public WeightedOptionsWithFallback<TerrainStyle> getTerrainStyleOptions() {
                return WeightedOptionsWithFallback.of(TerrainStyle.FLOODED);
            }

            @Override
            public WeightedOptionsWithFallback<ResourceStyle> getResourceStyleOptions() {
                return WeightedOptionsWithFallback.of(ResourceStyle.WATER_MEX);
            }

            @Override
            public WeightedOptionsWithFallback<PropStyle> getPropStyleOptions() {
                return WeightedOptionsWithFallback.of(PropStyle.BASIC,
                                                      WeightedOption.of(PropStyle.BASIC, 1f),
                                                      WeightedOption.of(PropStyle.NAVY_WRECKS, 2f));
            }
        },
        HIGH_RECLAIM(ParameterConstraints.ANY) {
            @Override
            public WeightedOptionsWithFallback<TerrainStyle> getTerrainStyleOptions() {
                return WeightedOptionsWithFallback.of(TerrainStyle.BASIC_LAST,
                                                      WeightedOption.of(TerrainStyle.DROP_PLATEAU_LAST, 1f),
                                                      WeightedOption.of(TerrainStyle.MOUNTAIN_RANGE_LAST, 1f),
                                                      WeightedOption.of(TerrainStyle.LITTLE_MOUNTAIN_LAST, 1f),
                                                      WeightedOption.of(TerrainStyle.VALLEY_LAST, 1f));
            }

            @Override
            public WeightedOptionsWithFallback<PropStyle> getPropStyleOptions() {
                return WeightedOptionsWithFallback.of(PropStyle.HIGH_RECLAIM,
                                                      WeightedOption.of(PropStyle.HIGH_RECLAIM, 1f),
                                                      WeightedOption.of(PropStyle.BOULDER_FIELD, 1f));
            }

            @Override
            public WeightedOptionsWithFallback<com.faforever.neroxis.biomes.BiomeName> getBiomeNameOptions() {
                return WeightedOptionsWithFallback.of(com.faforever.neroxis.biomes.BiomeName.DESERT,
                                                      WeightedOption.of(com.faforever.neroxis.biomes.BiomeName.DESERT,
                                                                        1f),
                                                      WeightedOption.of(com.faforever.neroxis.biomes.BiomeName.FRITHEN,
                                                                        1f),
                                                      WeightedOption.of(
                                                              com.faforever.neroxis.biomes.BiomeName.MOONLIGHT, 1f),
                                                      WeightedOption.of(com.faforever.neroxis.biomes.BiomeName.SUNSET,
                                                                        1f),
                                                      WeightedOption.of(com.faforever.neroxis.biomes.BiomeName.WONDER,
                                                                        1f));
            }
        },
        LAND_BRIDGE(ParameterConstraints.builder().mapSizes(768, 1024).numTeams(2, 4).build()) {
            @Override
            public WeightedOptionsWithFallback<TerrainStyle> getTerrainStyleOptions() {
                return WeightedOptionsWithFallback.of(TerrainStyle.LAND_BRIDGE);
            }

            @Override
            public WeightedOptionsWithFallback<PropStyle> getPropStyleOptions() {
                return WeightedOptionsWithFallback.of(PropStyle.LARGE_BATTLE,
                                                      WeightedOption.of(PropStyle.BOULDER_FIELD, .1f),
                                                      WeightedOption.of(PropStyle.LARGE_BATTLE, .5f),
                                                      WeightedOption.of(PropStyle.NAVY_WRECKS, 2f),
                                                      WeightedOption.of(PropStyle.NEUTRAL_CIV, 1f),
                                                      WeightedOption.of(PropStyle.ROCK_FIELD, 1f),
                                                      WeightedOption.of(PropStyle.SMALL_BATTLE, 1f));
            }
        },
        LITTLE_MOUNTAIN(ParameterConstraints.ANY) {
            @Override
            public WeightedOptionsWithFallback<TerrainStyle> getTerrainStyleOptions() {
                return WeightedOptionsWithFallback.of(TerrainStyle.LITTLE_MOUNTAIN_LAST);
            }

            @Override
            public WeightedOptionsWithFallback<PropStyle> getPropStyleOptions() {
                return WeightedOptionsWithFallback.of(PropStyle.BASIC,
                                                      WeightedOption.of(PropStyle.BASIC, 1f),
                                                      WeightedOption.of(PropStyle.BOULDER_FIELD, .1f),
                                                      WeightedOption.of(PropStyle.ENEMY_CIV, .5f),
                                                      WeightedOption.of(PropStyle.HIGH_RECLAIM, .5f),
                                                      WeightedOption.of(PropStyle.LARGE_BATTLE, .5f),
                                                      WeightedOption.of(PropStyle.NEUTRAL_CIV, 1f),
                                                      WeightedOption.of(PropStyle.ROCK_FIELD, 1f),
                                                      WeightedOption.of(PropStyle.SMALL_BATTLE, 1f));
            }
        },
        LOW_MEX(ParameterConstraints.builder().mapSizes(256, 640).spawnCount(0, 4).numTeams(2, 2).build()) {
            @Override
            public WeightedOptionsWithFallback<ResourceStyle> getResourceStyleOptions() {
                return WeightedOptionsWithFallback.of(ResourceStyle.LOW_MEX);
            }

            @Override
            public WeightedOptionsWithFallback<TerrainStyle> getTerrainStyleOptions() {
                return WeightedOptionsWithFallback.of(TerrainStyle.BASIC_LAST,
                                                      WeightedOption.of(TerrainStyle.BASIC_LAST, 1f),
                                                      WeightedOption.of(TerrainStyle.ONE_ISLAND, 1f),
                                                      WeightedOption.of(TerrainStyle.LITTLE_MOUNTAIN_LAST, 1f),
                                                      WeightedOption.of(TerrainStyle.VALLEY_LAST, 1f));
            }

            @Override
            public WeightedOptionsWithFallback<PropStyle> getPropStyleOptions() {
                return WeightedOptionsWithFallback.of(PropStyle.BASIC,
                                                      WeightedOption.of(PropStyle.BASIC, 1f),
                                                      WeightedOption.of(PropStyle.BOULDER_FIELD, .1f),
                                                      WeightedOption.of(PropStyle.ENEMY_CIV, .5f),
                                                      WeightedOption.of(PropStyle.HIGH_RECLAIM, .25f),
                                                      WeightedOption.of(PropStyle.LARGE_BATTLE, .5f),
                                                      WeightedOption.of(PropStyle.NAVY_WRECKS, 2f),
                                                      WeightedOption.of(PropStyle.NEUTRAL_CIV, 1f),
                                                      WeightedOption.of(PropStyle.ROCK_FIELD, 1f),
                                                      WeightedOption.of(PropStyle.SMALL_BATTLE, 1f));
            }
        },
        MOUNTAIN_RANGE(ParameterConstraints.builder().mapSizes(256, 640).build()) {
            @Override
            public WeightedOptionsWithFallback<TerrainStyle> getTerrainStyleOptions() {
                return WeightedOptionsWithFallback.of(TerrainStyle.MOUNTAIN_RANGE_LAST);
            }

            @Override
            public WeightedOptionsWithFallback<PropStyle> getPropStyleOptions() {
                return WeightedOptionsWithFallback.of(PropStyle.BASIC,
                                                      WeightedOption.of(PropStyle.BASIC, 1f),
                                                      WeightedOption.of(PropStyle.BOULDER_FIELD, .1f),
                                                      WeightedOption.of(PropStyle.ENEMY_CIV, .5f),
                                                      WeightedOption.of(PropStyle.HIGH_RECLAIM, .25f),
                                                      WeightedOption.of(PropStyle.LARGE_BATTLE, .5f),
                                                      WeightedOption.of(PropStyle.NEUTRAL_CIV, 1f),
                                                      WeightedOption.of(PropStyle.ROCK_FIELD, 1f),
                                                      WeightedOption.of(PropStyle.SMALL_BATTLE, 1f));
            }
        },
        MULTILEVEL(ParameterConstraints.ANY) {
            @Override
            public WeightedOptionsWithFallback<TerrainStyle> getTerrainStyleOptions() {
                return WeightedOptionsWithFallback.of(TerrainStyle.MULTILEVEL_LAST);
            }

            @Override
            public WeightedOptionsWithFallback<PropStyle> getPropStyleOptions() {
                return WeightedOptionsWithFallback.of(PropStyle.BASIC,
                                                      WeightedOption.of(PropStyle.BASIC, 1f),
                                                      WeightedOption.of(PropStyle.BOULDER_FIELD, 1f),
                                                      WeightedOption.of(PropStyle.ENEMY_CIV, .5f),
                                                      WeightedOption.of(PropStyle.HIGH_RECLAIM, .25f),
                                                      WeightedOption.of(PropStyle.LARGE_BATTLE, .5f),
                                                      WeightedOption.of(PropStyle.SMALL_BATTLE, .5f),
                                                      WeightedOption.of(PropStyle.NEUTRAL_CIV, 1f),
                                                      WeightedOption.of(PropStyle.ROCK_FIELD, 1f));
            }
        },
        ONE_ISLAND(ParameterConstraints.builder().mapSizes(384, 1024).build()) {
            @Override
            public WeightedOptionsWithFallback<TerrainStyle> getTerrainStyleOptions() {
                return WeightedOptionsWithFallback.of(TerrainStyle.ONE_ISLAND);
            }

            @Override
            public WeightedOptionsWithFallback<PropStyle> getPropStyleOptions() {
                return WeightedOptionsWithFallback.of(PropStyle.BASIC,
                                                      WeightedOption.of(PropStyle.BASIC, 1f),
                                                      WeightedOption.of(PropStyle.BOULDER_FIELD, .1f),
                                                      WeightedOption.of(PropStyle.NAVY_WRECKS, 2f),
                                                      WeightedOption.of(PropStyle.NEUTRAL_CIV, 1f),
                                                      WeightedOption.of(PropStyle.ROCK_FIELD, 1f),
                                                      WeightedOption.of(PropStyle.SMALL_BATTLE, 1f));
            }
        },
        SMALL_ISLANDS(ParameterConstraints.builder().mapSizes(768, 1024).build()) {
            @Override
            public WeightedOptionsWithFallback<TerrainStyle> getTerrainStyleOptions() {
                return WeightedOptionsWithFallback.of(TerrainStyle.SMALL_ISLANDS);
            }

            @Override
            public WeightedOptionsWithFallback<PropStyle> getPropStyleOptions() {
                return WeightedOptionsWithFallback.of(PropStyle.BASIC, WeightedOption.of(PropStyle.BASIC, 1f),
                                                      WeightedOption.of(PropStyle.BOULDER_FIELD, .1f),
                                                      WeightedOption.of(PropStyle.NAVY_WRECKS, 2f),
                                                      WeightedOption.of(PropStyle.ROCK_FIELD, 1f),
                                                      WeightedOption.of(PropStyle.SMALL_BATTLE, 1f));
            }
        },
        VALLEY(ParameterConstraints.builder().mapSizes(384, 1024).build()) {
            @Override
            public WeightedOptionsWithFallback<TerrainStyle> getTerrainStyleOptions() {
                return WeightedOptionsWithFallback.of(TerrainStyle.VALLEY_LAST);
            }

            @Override
            public WeightedOptionsWithFallback<PropStyle> getPropStyleOptions() {
                return WeightedOptionsWithFallback.of(PropStyle.BASIC,
                                                      WeightedOption.of(PropStyle.BASIC, 1f),
                                                      WeightedOption.of(PropStyle.BOULDER_FIELD, .1f),
                                                      WeightedOption.of(PropStyle.ENEMY_CIV, .5f),
                                                      WeightedOption.of(PropStyle.HIGH_RECLAIM, .5f),
                                                      WeightedOption.of(PropStyle.LARGE_BATTLE, .5f),
                                                      WeightedOption.of(PropStyle.NEUTRAL_CIV, 1f),
                                                      WeightedOption.of(PropStyle.ROCK_FIELD, 1f),
                                                      WeightedOption.of(PropStyle.SMALL_BATTLE, 1f));
            }
        },
        RIVERS(ParameterConstraints.ANY) {
            @Override
            public WeightedOptionsWithFallback<TerrainStyle> getTerrainStyleOptions() {
                return WeightedOptionsWithFallback.of(TerrainStyle.RIVERS);
            }

            @Override
            public WeightedOptionsWithFallback<PropStyle> getPropStyleOptions() {
                return WeightedOptionsWithFallback.of(PropStyle.BASIC,
                                                      WeightedOption.of(PropStyle.BASIC, 1f),
                                                      WeightedOption.of(PropStyle.BOULDER_FIELD, .1f),
                                                      WeightedOption.of(PropStyle.ENEMY_CIV, .5f),
                                                      WeightedOption.of(PropStyle.HIGH_RECLAIM, .25f),
                                                      WeightedOption.of(PropStyle.LARGE_BATTLE, .5f),
                                                      WeightedOption.of(PropStyle.NAVY_WRECKS, 2f),
                                                      WeightedOption.of(PropStyle.NEUTRAL_CIV, 1f),
                                                      WeightedOption.of(PropStyle.ROCK_FIELD, 1f),
                                                      WeightedOption.of(PropStyle.SMALL_BATTLE, 1f));
            }
        },
        RIVERS_AND_OCEANS(ParameterConstraints.ANY) {
            @Override
            public WeightedOptionsWithFallback<ResourceStyle> getResourceStyleOptions() {
                return WeightedOptionsWithFallback.of(ResourceStyle.HI_MEX_LAND_LOW_MEX_WATER);
            }

            @Override
            public WeightedOptionsWithFallback<TerrainStyle> getTerrainStyleOptions() {
                return WeightedOptionsWithFallback.of(TerrainStyle.RIVERS_AND_OCEANS);
            }

            @Override
            public WeightedOptionsWithFallback<PropStyle> getPropStyleOptions() {
                return WeightedOptionsWithFallback.of(PropStyle.BASIC,
                                                      WeightedOption.of(PropStyle.BASIC, 1f),
                                                      WeightedOption.of(PropStyle.BOULDER_FIELD, .1f),
                                                      WeightedOption.of(PropStyle.ENEMY_CIV, .5f),
                                                      WeightedOption.of(PropStyle.HIGH_RECLAIM, .25f),
                                                      WeightedOption.of(PropStyle.LARGE_BATTLE, .5f),
                                                      WeightedOption.of(PropStyle.NAVY_WRECKS, 2f),
                                                      WeightedOption.of(PropStyle.NEUTRAL_CIV, 1f),
                                                      WeightedOption.of(PropStyle.ROCK_FIELD, 1f),
                                                      WeightedOption.of(PropStyle.SMALL_BATTLE, 1f));
            }
        },
        FRACTAL_LAND(ParameterConstraints.ANY) {
            @Override
            public WeightedOptionsWithFallback<TerrainStyle> getTerrainStyleOptions() {
                return WeightedOptionsWithFallback.of(TerrainStyle.FRACTAL_LAND);
            }

            @Override
            public WeightedOptionsWithFallback<PropStyle> getPropStyleOptions() {
                return WeightedOptionsWithFallback.of(PropStyle.BASIC,
                                                      WeightedOption.of(PropStyle.BASIC, 1f),
                                                      WeightedOption.of(PropStyle.BOULDER_FIELD, .5f),
                                                      WeightedOption.of(PropStyle.ENEMY_CIV, .5f),
                                                      WeightedOption.of(PropStyle.HIGH_RECLAIM, .25f),
                                                      WeightedOption.of(PropStyle.LARGE_BATTLE, .5f),
                                                      WeightedOption.of(PropStyle.NEUTRAL_CIV, 1f),
                                                      WeightedOption.of(PropStyle.ROCK_FIELD, 1f),
                                                      WeightedOption.of(PropStyle.SMALL_BATTLE, 1f));
            }
        },
        FRACTAL_PLATEAU(ParameterConstraints.ANY) {
            @Override
            public WeightedOptionsWithFallback<TerrainStyle> getTerrainStyleOptions() {
                return WeightedOptionsWithFallback.of(TerrainStyle.FRACTAL_PLATEAU);
            }

            @Override
            public WeightedOptionsWithFallback<PropStyle> getPropStyleOptions() {
                return WeightedOptionsWithFallback.of(PropStyle.BASIC,
                                                      WeightedOption.of(PropStyle.BASIC, 1f),
                                                      WeightedOption.of(PropStyle.BOULDER_FIELD, .5f),
                                                      WeightedOption.of(PropStyle.ENEMY_CIV, .5f),
                                                      WeightedOption.of(PropStyle.HIGH_RECLAIM, .5f),
                                                      WeightedOption.of(PropStyle.LARGE_BATTLE, .5f),
                                                      WeightedOption.of(PropStyle.NEUTRAL_CIV, 1f),
                                                      WeightedOption.of(PropStyle.ROCK_FIELD, 1f),
                                                      WeightedOption.of(PropStyle.SMALL_BATTLE, 1f));
            }
        },
        FRACTAL_NAVY(ParameterConstraints.ANY) {
            @Override
            public WeightedOptionsWithFallback<ResourceStyle> getResourceStyleOptions() {
                return WeightedOptionsWithFallback.of(ResourceStyle.HI_MEX_LAND_LOW_MEX_WATER);
            }

            @Override
            public WeightedOptionsWithFallback<TerrainStyle> getTerrainStyleOptions() {
                return WeightedOptionsWithFallback.of(TerrainStyle.FRACTAL_NAVY);
            }

            @Override
            public WeightedOptionsWithFallback<PropStyle> getPropStyleOptions() {
                return WeightedOptionsWithFallback.of(PropStyle.BASIC,
                                                      WeightedOption.of(PropStyle.BASIC, 1f),
                                                      WeightedOption.of(PropStyle.NAVY_WRECKS, 3f),
                                                      WeightedOption.of(PropStyle.BOULDER_FIELD, 2f),
                                                      WeightedOption.of(PropStyle.HIGH_RECLAIM, 1f),
                                                      WeightedOption.of(PropStyle.LARGE_BATTLE, .5f),
                                                      WeightedOption.of(PropStyle.ROCK_FIELD, 0.5f),
                                                      WeightedOption.of(PropStyle.SMALL_BATTLE, 0.5f));
            }
        },
        SETONISH(ParameterConstraints.builder().mapSizes(512, 1024).numTeams(2, 2).build()) {
            @Override
            public WeightedOptionsWithFallback<TerrainStyle> getTerrainStyleOptions() {
                return WeightedOptionsWithFallback.of(TerrainStyle.SETONS);
            }

            @Override
            public WeightedOptionsWithFallback<PropStyle> getPropStyleOptions() {
                return WeightedOptionsWithFallback.of(PropStyle.BASIC,
                                                      WeightedOption.of(PropStyle.BASIC, 1f),
                                                      WeightedOption.of(PropStyle.LARGE_BATTLE, .5f),
                                                      WeightedOption.of(PropStyle.BOULDER_FIELD, 1f),
                                                      WeightedOption.of(PropStyle.SMALL_BATTLE, 2f));
            }
        },
        FORREST_SOMETHING(ParameterConstraints.ANY) {
            @Override
            public WeightedOptionsWithFallback<PropStyle> getPropStyleOptions() {
                return WeightedOptionsWithFallback.of(PropStyle.FORREST_SOMETHING);
            }

            @Override
            public WeightedOptionsWithFallback<ResourceStyle> getResourceStyleOptions() {
                return WeightedOptionsWithFallback.of(ResourceStyle.ONE_HYDRO_NO_MEX);
            }

            @Override
            public WeightedOptionsWithFallback<TerrainStyle> getTerrainStyleOptions() {
                return WeightedOptionsWithFallback.of(TerrainStyle.BASIC_LAST,
                                                      WeightedOption.of(TerrainStyle.BASIC_LAST, 1f),
                                                      WeightedOption.of(TerrainStyle.BIG_ISLANDS, 1f),
                                                      WeightedOption.of(TerrainStyle.CENTER_LAKE_LAST, 1f),
                                                      WeightedOption.of(TerrainStyle.DROP_PLATEAU_LAST, 1f),
                                                      WeightedOption.of(TerrainStyle.FRACTAL_LAND, 1f),
                                                      WeightedOption.of(TerrainStyle.FRACTAL_PLATEAU, 1f),
                                                      WeightedOption.of(TerrainStyle.LAND_BRIDGE, 1f),
                                                      WeightedOption.of(TerrainStyle.LITTLE_MOUNTAIN_LAST, 1f),
                                                      WeightedOption.of(TerrainStyle.MOUNTAIN_RANGE_LAST, 1f),
                                                      WeightedOption.of(TerrainStyle.MULTILEVEL_LAST, 1f),
                                                      WeightedOption.of(TerrainStyle.ONE_ISLAND, 1f),
                                                      WeightedOption.of(TerrainStyle.RIVERS, 1f),
                                                      WeightedOption.of(TerrainStyle.VALLEY_LAST, 1f)
                );
            }
        };

        private final ParameterConstraints parameterConstraints;

        @JsonIgnore
        @Override
        public ParameterConstraints parameterConstraints() {
            return parameterConstraints;
        }
    }

    record Custom(
            TerrainStyle terrainStyle,
            BiomeName biomeName,
            PropStyle propStyle,
            ResourceStyle resourceStyle,
            float reclaimDensity,
            float resourceDensity
    ) implements MapStyle {

        public Custom {
            reclaimDensity = MathUtil.discretePercentage(reclaimDensity, NUM_BINS);
            resourceDensity = MathUtil.discretePercentage(resourceDensity, NUM_BINS);
        }

        @Override
        public String name() {
            return "Custom";
        }
    }
}
