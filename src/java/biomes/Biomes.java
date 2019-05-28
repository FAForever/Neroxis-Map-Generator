package biomes;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import lombok.Data;
import map.Material;
import map.TerrainMaterials;
import util.serialized.MaterialSet;
import util.serialized.WaterSettings;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static map.SCMap.*;

@Data
public strictfp class Biomes {

	public static List<Biome> list = new ArrayList<>();

	static{

		Path biomePath = null;
		try {
			biomePath = Paths.get(Biome.class.getClassLoader().getResource("custom_biomes").toURI());
		}
		catch (Exception e){
			e.printStackTrace();
			System.exit(1);
		}

		Gson gson = new Gson();
		String content = "";
		try {
			DirectoryStream<Path> directoryStream = Files.newDirectoryStream(biomePath);
			for (final Path dirEntry : directoryStream) {

				// ├ Biome
				// ├-- materials.json <required>
				// └-- WaterSettings.scmwtr <optional>

				// Materials
				content = new String(Files.readAllBytes(dirEntry.resolve("materials.json")));
				MaterialSet newMatSet = gson.fromJson(content, MaterialSet.class);
				Material[] materials = new Material[newMatSet.materials.size()];

				for(int i = 0; i < materials.length; i++){
					MaterialSet.Material biomeMaterial = newMatSet.materials.get(i);
					materials[i] = new Material(
						biomeMaterial.texture.environment,
						biomeMaterial.normal.environment,
						biomeMaterial.texture.name,
						biomeMaterial.normal.name,
						biomeMaterial.texture.scale,
						biomeMaterial.normal.scale
					);
				}

				TerrainMaterials terrainMaterials = new TerrainMaterials(
					materials,
					new Material(
						newMatSet.macroTexture.environment,
						newMatSet.macroTexture.name,
						newMatSet.macroTexture.scale
					)
				);

				// Water
				WaterSettings waterSettings;
				Path waterPath = dirEntry.resolve("WaterSettings.scmwtr");
				if (Files.exists(waterPath)){
					content = new String(Files.readAllBytes(waterPath));
					waterSettings = gson.fromJson(content, WaterSettings.class);

					// We always set elevation and other settings back to the original value
					// because the map generator does not expect to have a varying water height
					waterSettings.Elevation = WATER_HEIGHT;
					waterSettings.ElevationDeep = WATER_DEEP_HEIGHT;
					waterSettings.ElevationAbyss = WATER_ABYSS_HEIGHT;
				}
				else{
					waterSettings = new WaterSettings();
				}
				list.add(new Biome(terrainMaterials, waterSettings));
			}
		}
		catch (IOException e){
			e.printStackTrace();
			System.out.println("An error occured while loading biomes. Please check that all biomes JSONs are correct.");
			System.exit(1);
		}
		catch (JsonParseException e) {
			e.printStackTrace();
			System.out.println("An error occured while loading the following biome:");
			System.out.println(content);
			System.exit(1);
		}
	}
}
