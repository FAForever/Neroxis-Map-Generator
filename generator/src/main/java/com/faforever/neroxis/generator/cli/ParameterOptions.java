package com.faforever.neroxis.generator.cli;

import com.faforever.neroxis.biomes.Biome;
import com.faforever.neroxis.biomes.Biomes;
import com.faforever.neroxis.cli.CLIUtils;
import com.faforever.neroxis.generator.MapGenerator;
import com.faforever.neroxis.map.Symmetry;
import lombok.Getter;
import picocli.CommandLine;

import static picocli.CommandLine.Option;
import static picocli.CommandLine.Spec;

@Getter
@SuppressWarnings("unused")
public strictfp class ParameterOptions {
    @Spec
    CommandLine.Model.CommandSpec spec;

    private Float landDensity;
    private Float plateauDensity;
    private Float mountainDensity;
    private Float rampDensity;
    private Float reclaimDensity;
    private Float mexDensity;
    private Symmetry terrainSymmetry;
    private Biome biome;

    @Option(names = "--land-density", description = "Land density for the generated map. Min: 0 Max: 1")
    public void setLandDensity(float density) {
        this.landDensity = CLIUtils.convertDensity(density, MapGenerator.NUM_BINS, spec);
    }

    @Option(names = "--plateau-density", description = "Plateau density for the generated map. Min: 0 Max: 1")
    public void setPlateauDensity(Float density) {
        this.plateauDensity = CLIUtils.convertDensity(density, MapGenerator.NUM_BINS, spec);
    }

    @Option(names = "--mountain-density", description = "Mountain density for the generated map. Min: 0 Max: 1")
    public void setMountainDensity(Float density) {
        this.mountainDensity = CLIUtils.convertDensity(density, MapGenerator.NUM_BINS, spec);
    }

    @Option(names = "--ramp-density", description = "Ramp density for the generated map. Min: 0 Max: 1")
    public void setRampDensity(Float density) {
        this.rampDensity = CLIUtils.convertDensity(density, MapGenerator.NUM_BINS, spec);
    }

    @Option(names = "--reclaim-density", description = "Reclaim density for the generated map. Min: 0 Max: 1")
    public void setReclaimDensity(Float density) {
        this.reclaimDensity = CLIUtils.convertDensity(density, MapGenerator.NUM_BINS, spec);
    }

    @Option(names = "--mex-density", description = "Mex density for the generated map. Min: 0 Max: 1")
    public void setMexDensity(Float density) {
        this.mexDensity = CLIUtils.convertDensity(density, MapGenerator.NUM_BINS, spec);
    }

    @Option(names = "--terrain-symmetry", description = "Base terrain symmetry for the map. Values: ${COMPLETION-CANDIDATES}")
    public void setTerrainSymmetry(Symmetry terrainSymmetry) {
        this.terrainSymmetry = terrainSymmetry;
    }

    @Option(names = "--biome", description = "Texture biome for the generated map")
    public void setBiome(String biome) {
        this.biome = Biomes.loadBiome(biome);
    }
}
