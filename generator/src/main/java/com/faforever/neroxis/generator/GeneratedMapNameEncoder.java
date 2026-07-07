package com.faforever.neroxis.generator;

import com.faforever.neroxis.cli.VersionProvider;
import com.faforever.neroxis.map.Symmetry;
import com.faforever.neroxis.util.MathUtil;
import org.apache.commons.codec.CodecPolicy;
import org.apache.commons.codec.binary.Base32;

import java.nio.ByteBuffer;
import java.util.Locale;

public class GeneratedMapNameEncoder {
    public static final int NUM_BINS = 127;
    private static final Base32 ENCODER = Base32.builder()
                                                .setLineLength(0)
                                                .setPadding((byte) '=')
                                                .setDecodingPolicy(CodecPolicy.LENIENT)
                                                .get();
    private static final String VERSION = new VersionProvider().getVersion()[0];

    private static String encodeFromBytes(byte[] bytes) {
        return ENCODER.encodeAsString(bytes).replace("=", "").toLowerCase(Locale.ROOT);
    }

    private static byte[] decodeToBytes(String encoded) {
        return ENCODER.decode(encoded);
    }

    public static GeneratorParameters decode(String mapName) {
        String[] nameArgs = verifyMapName(mapName);
        long seed = ByteBuffer.wrap(GeneratedMapNameEncoder.decodeToBytes(nameArgs[4])).getLong();
        int spawnCount = 6;
        int mapSize = 512;
        int numTeams = 2;
        GeneratorParameters.Mode mode = new GeneratorParameters.Casual(null, null);

        if (nameArgs.length >= 6) {
            String optionString = nameArgs[5];
            byte[] optionBytes = GeneratedMapNameEncoder.decodeToBytes(optionString);
            // The lobby server uses map names with specifically created strings to control the
            // map generation, so we can't assume that the basic options are always present.
            if (optionBytes.length > 0) {
                spawnCount = optionBytes[0];
            }
            if (optionBytes.length > 1) {
                mapSize = optionBytes[1] * 64;
            }

            if (optionBytes.length > 2) {
                numTeams = optionBytes[2];
            }

            if (optionBytes.length == 4 && nameArgs.length >= 7) {
                long generationTime = ByteBuffer.wrap(GeneratedMapNameEncoder.decodeToBytes(nameArgs[6])).getLong();
                Visibility visibility = Visibility.values()[optionBytes[3]];
                mode = new GeneratorParameters.Competitive(generationTime, visibility);
            } else if (optionBytes.length > 3) {
                byte terrainSymmetryOption = optionBytes[3];
                Symmetry terrainSymmetry = terrainSymmetryOption < 0 ? null : Symmetry.values()[terrainSymmetryOption];
                MapStyle mapStyle = null;
                if (optionBytes.length == 5) {
                    mapStyle = MapStyle.Predefined.values()[optionBytes[4]];
                } else if (optionBytes.length == 10) {
                    TextureStyle textureStyle = TextureStyle.values()[optionBytes[4]];
                    TerrainStyle terrainStyle = TerrainStyle.values()[optionBytes[5]];
                    ResourceStyle resourceStyle = ResourceStyle.values()[optionBytes[6]];
                    PropStyle propStyle = PropStyle.values()[optionBytes[7]];
                    float reclaimDensity = MathUtil.normalizeBin(optionBytes[8], NUM_BINS);
                    float resourceDensity = MathUtil.normalizeBin(optionBytes[9], NUM_BINS);
                    mapStyle = new MapStyle.Custom(terrainStyle, textureStyle, propStyle, resourceStyle, reclaimDensity,
                                                   resourceDensity);
                }
                mode = new GeneratorParameters.Casual(terrainSymmetry, mapStyle);
            }
        }

        return new GeneratorParameters(seed, spawnCount, mapSize, numTeams, mode);
    }

    private static String[] verifyMapName(String mapName) {
        if (!mapName.startsWith("neroxis_map_generator")) {
            throw new MapNameException(String.format("Map name `%s` is not a generated map", mapName));
        }

        String[] nameArgs = mapName.split("_");
        if (nameArgs.length < 4) {
            throw new MapNameException(String.format("Map name `%s` does not specify a version", mapName));
        }

        String version = nameArgs[3];

        if (!VERSION.equals(version)) {
            throw new MapNameException(
                    String.format("Version for `%s` does not match this generator version", mapName));
        }

        if (nameArgs.length < 5) {
            throw new MapNameException(String.format("Map name `%s` does not specify a seed", mapName));
        }
        return nameArgs;
    }

    public static String encode(GeneratorParameters generatorParameters) {
        ByteBuffer seedBuffer = ByteBuffer.allocate(8);
        seedBuffer.putLong(generatorParameters.seed());
        String seedString = GeneratedMapNameEncoder.encodeFromBytes(seedBuffer.array());
        byte[] optionArray;
        byte spawnOption = (byte) generatorParameters.spawnCount();
        byte mapSizeOption = (byte) (generatorParameters.mapSize() / 64);
        byte numTeamsOption = (byte) generatorParameters.numTeams();

        String optionString = switch (generatorParameters.mode()) {
            case GeneratorParameters.Competitive(long generationTime, Visibility visibility) -> {
                optionArray = new byte[]{spawnOption, mapSizeOption, numTeamsOption, (byte) visibility.ordinal()};
                String timeString = GeneratedMapNameEncoder.encodeFromBytes(
                        ByteBuffer.allocate(8).putLong(generationTime).array());
                yield GeneratedMapNameEncoder.encodeFromBytes(optionArray) + "_" + timeString;
            }
            case GeneratorParameters.Casual(Symmetry terrainSymmetry, MapStyle mapStyle) -> {
                byte terrainSymmetryOption = terrainSymmetry == null ? -1 : (byte) terrainSymmetry.ordinal();
                switch (mapStyle) {
                    case MapStyle.Predefined predefinedMapStyle -> {
                        optionArray = new byte[]{spawnOption, mapSizeOption, numTeamsOption, terrainSymmetryOption, (byte) predefinedMapStyle.ordinal()};
                        yield GeneratedMapNameEncoder.encodeFromBytes(optionArray);
                    }
                    case MapStyle.Custom customMapStyle -> {
                        byte textureStyleOption = (byte) customMapStyle.textureStyle().ordinal();
                        byte terrainStyleOption = (byte) customMapStyle.terrainStyle().ordinal();
                        byte resourceStyleOption = (byte) customMapStyle.resourceStyle().ordinal();
                        byte propStyleOption = (byte) customMapStyle.propStyle().ordinal();
                        byte reclaimDensityOption = (byte) MathUtil.binPercentage(customMapStyle.reclaimDensity(),
                                                                                  NUM_BINS);
                        byte resourceDensityOption = (byte) MathUtil.binPercentage(customMapStyle.resourceDensity(),
                                                                                   NUM_BINS);
                        optionArray = new byte[]{spawnOption, mapSizeOption, numTeamsOption, terrainSymmetryOption, textureStyleOption, terrainStyleOption, resourceStyleOption, propStyleOption, reclaimDensityOption, resourceDensityOption};
                        yield GeneratedMapNameEncoder.encodeFromBytes(optionArray);
                    }
                    case null -> {
                        optionArray = terrainSymmetryOption <
                                      0 ? new byte[]{spawnOption, mapSizeOption, numTeamsOption} : new byte[]{spawnOption, mapSizeOption, numTeamsOption, terrainSymmetryOption};
                        yield GeneratedMapNameEncoder.encodeFromBytes(optionArray);
                    }
                }
            }
        };

        return "neroxis_map_generator_%s_%s_%s".formatted(VERSION, seedString, optionString).toLowerCase(Locale.ROOT);
    }
}
