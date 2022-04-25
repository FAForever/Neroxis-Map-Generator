package com.faforever.neroxis.generator.graph.domain;

import com.faforever.neroxis.generator.GeneratorParameters;
import com.faforever.neroxis.generator.ParameterConstraints;
import com.faforever.neroxis.map.SCMap;
import com.faforever.neroxis.map.SymmetrySettings;
import com.faforever.neroxis.map.placement.SpawnPlacer;
import com.faforever.neroxis.util.SymmetrySelector;
import java.util.Random;
import lombok.Getter;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

@Getter
public strictfp class GraphContext {

    private final Random random;
    private final SymmetrySettings symmetrySettings;
    private final ExpressionParser parser;
    private final EvaluationContext evalContext;
    private final SCMap map;
    private final GeneratorParameters generatorParameters;
    private final float landDensity;
    private final float plateauDensity;
    private final float mountainDensity;
    private final float rampDensity;
    private final float reclaimDensity;
    private final float mexDensity;
    private final float normalizedLandDensity;
    private final float normalizedPlateauDensity;
    private final float normalizedMountainDensity;
    private final float normalizedRampDensity;
    private final float normalizedReclaimDensity;
    private final float normalizedMexDensity;
    private final int mapSize;
    private final int numSymPoints;
    private String identifier;

    public GraphContext(long seed, GeneratorParameters generatorParameters, ParameterConstraints parameterConstraints) {
        random = new Random(seed);
        this.symmetrySettings = SymmetrySelector.getSymmetrySettingsFromTerrainSymmetry(random,
                                                                                        generatorParameters.getTerrainSymmetry(),
                                                                                        generatorParameters.getSpawnCount(),
                                                                                        generatorParameters.getNumTeams());
        this.generatorParameters = generatorParameters;
        numSymPoints = symmetrySettings.getSpawnSymmetry().getNumSymPoints();
        landDensity = generatorParameters.getLandDensity();
        plateauDensity = generatorParameters.getPlateauDensity();
        mountainDensity = generatorParameters.getMountainDensity();
        rampDensity = generatorParameters.getRampDensity();
        reclaimDensity = generatorParameters.getMexDensity();
        mexDensity = generatorParameters.getMexDensity();
        normalizedLandDensity = parameterConstraints.getLandDensityRange().normalize(landDensity);
        normalizedPlateauDensity = parameterConstraints.getPlateauDensityRange().normalize(plateauDensity);
        normalizedMountainDensity = parameterConstraints.getMountainDensityRange().normalize(mountainDensity);
        normalizedRampDensity = parameterConstraints.getRampDensityRange().normalize(rampDensity);
        normalizedMexDensity = parameterConstraints.getMexDensityRange().normalize(mexDensity);
        normalizedReclaimDensity = parameterConstraints.getReclaimDensityRange().normalize(reclaimDensity);
        map = new SCMap(generatorParameters.getMapSize(), generatorParameters.getBiome());
        mapSize = generatorParameters.getMapSize();
        parser = new SpelExpressionParser();
        evalContext = new StandardEvaluationContext(this);
    }

    public void placeSpawns() {
        float spawnSeparation;
        int teamSeparation;
        if (generatorParameters.getNumTeams() < 2) {
            spawnSeparation = (float) generatorParameters.getMapSize() / generatorParameters.getSpawnCount() * 1.5f;
            teamSeparation = 0;
        } else if (generatorParameters.getNumTeams() == 2) {
            spawnSeparation = random.nextInt(map.getSize() / 4 - map.getSize() / 16) + map.getSize() / 16f;
            teamSeparation = map.getSize() / generatorParameters.getNumTeams();
        } else {
            if (generatorParameters.getNumTeams() < 8) {
                spawnSeparation = random.nextInt(
                        map.getSize() / 2 / generatorParameters.getNumTeams() - map.getSize() / 16)
                                  + map.getSize() / 16f;
            } else {
                spawnSeparation = 0;
            }
            teamSeparation = map.getSize() / generatorParameters.getNumTeams();
        }
        new SpawnPlacer(map, random.nextLong()).placeSpawns(generatorParameters.getSpawnCount(), spawnSeparation,
                                                            teamSeparation, symmetrySettings);
    }

    public <T> T getValue(String expression, String identifier, Class<T> clazz) {
        this.identifier = identifier;
        return parser.parseExpression(expression).getValue(evalContext, clazz);
    }
}
