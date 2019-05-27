package biomes;

import com.google.gson.Gson;
import lombok.Data;
import map.Material;
import map.TerrainMaterials;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Data
public strictfp class Biomes {

	public static List<TerrainMaterials> terrainMaterials = new ArrayList<>();

	public static void load(){

		Path biomePath = null;
		try {
			biomePath = Paths.get(Biome.class.getClassLoader().getResource("custom_biomes").toURI());
		}
		catch (Exception e){
			e.printStackTrace();
		}

		Gson gson = new Gson();
		try {
			DirectoryStream<Path> directoryStream = Files.newDirectoryStream(biomePath);
			for (final Path fileEntry : directoryStream) {

				String content = new String(Files.readAllBytes(fileEntry));
				Biome newBiome = gson.fromJson(content, Biome.class);
				Material[] materials = new Material[newBiome.materials.size()];

				for(int i = 0; i < materials.length; i++){
					BiomeMaterial biomeMaterial = newBiome.materials.get(i);
					materials[i] = new Material
						(
							biomeMaterial.texture.environment,
							biomeMaterial.normal.environment,
							biomeMaterial.texture.name,
							biomeMaterial.normal.name,
							biomeMaterial.texture.scale,
							biomeMaterial.normal.scale
						);
				}

				terrainMaterials.add(
					new TerrainMaterials(
						materials,
						new Material(
								newBiome.macroTexture.environment,
								newBiome.macroTexture.name,
								newBiome.macroTexture.scale
						)
					)
				);
			}
		}
		catch (IOException e){
			e.printStackTrace();
		}
	}
}
