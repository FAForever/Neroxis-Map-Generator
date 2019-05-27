package biomes;

import com.google.gson.Gson;
import lombok.Data;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Data
public strictfp class Biome {

	private final String data1;
	private final int data2;


	//TODO: remove me, JSON demo
	public static void main(String args[]) {
		Gson gson = new Gson();
		try {
			Biome grassBiome = gson.fromJson(new String(Files.readAllBytes(Paths.get(Biome.class.getClassLoader().getResource("grassBiome.json").toURI()))), Biome.class);

			System.out.println(grassBiome.getData1());
			System.out.println(grassBiome.getData2());
		} catch (IOException e) {
			e.printStackTrace();//TODO
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}
}
