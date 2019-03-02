package generator;

import export.SCMapExporter;
import export.SaveExporter;
import export.ScenarioExporter;
import export.ScriptExporter;
import map.*;
import util.Pipeline;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.concurrent.ExecutionException;

public strictfp class MapGenerator {
	
	public static final String VERSION = "0.1.0";

	public static void main(String[] args) throws ExecutionException, InterruptedException {
		try {
			String folderPath = args[0];
			long seed = Long.parseLong(args[1]);
			String version = args[2];
			String mapName = args.length >= 4 ? args[3] : "NeroxisGen_" + VERSION + "_" + seed;
			
			if(version.equals(VERSION)) {
				MapGenerator generator = new MapGenerator();
				System.out.println("Generating map " + mapName);
				SCMap map = generator.generate(seed);
				System.out.println("Saving map to " + Paths.get(folderPath).toAbsolutePath());
				generator.save(folderPath, mapName, map, seed);
				System.out.println("Done");
				
			} else {
				System.out.println("This generator only supports version " + VERSION);
				
			}
		} catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
			System.out.println("Usage: generator [targetFolder] [seed] [expectedVersion] (mapName)");
			
		}
	}

	public void save(String folderName, String mapName, SCMap map, long seed) {
		try {
			Path folderPath = Paths.get(folderName);
			Files.deleteIfExists(folderPath.resolve(mapName));
			Files.createDirectory(folderPath.resolve(mapName));
			SCMapExporter.exportSCMAP(folderPath, mapName, map);
			SaveExporter.exportSave(folderPath, mapName, map);
			ScenarioExporter.exportScenario(folderPath, mapName, map);
			ScriptExporter.exportScript(folderPath, mapName, map);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public SCMap generate(long seed) throws ExecutionException, InterruptedException {
		long startTime = System.currentTimeMillis();
		final Random random = new Random(seed);
		final SCMap map = new SCMap(512, 6, 64, 10);

		final ConcurrentBinaryMask land = new ConcurrentBinaryMask(16, random.nextLong(), "land");
		final ConcurrentBinaryMask mountains = new ConcurrentBinaryMask(32, random.nextLong(), "mountains");
		final ConcurrentBinaryMask plateaus = new ConcurrentBinaryMask(32, random.nextLong(), "plateaus");
		final ConcurrentBinaryMask ramps = new ConcurrentBinaryMask(128, random.nextLong(), "ramps");

		land.randomize(0.2f).inflate(1).cutCorners().enlarge(32).acid(0.5f).enlarge(128).smooth(4).acid(0.5f);
		mountains.randomize(0.05f).inflate(1).cutCorners().acid(0.5f).enlarge(128).smooth(4).acid(0.5f);
		plateaus.randomize(0.1f).inflate(1).cutCorners().acid(0.5f).enlarge(128).smooth(4).acid(0.5f);
		ramps.randomize(0.1f);

		plateaus.intersect(land).minus(mountains);
		ramps.intersect(plateaus).outline().minus(plateaus).intersect(land).minus(mountains).inflate(2);
		land.combine(mountains);

		land.enlarge(513).smooth(6);
		mountains.enlarge(513).inflate(1).smooth(6);
		plateaus.enlarge(513).inflate(1).smooth(6);

		ramps.enlarge(513).smooth(6);

		final ConcurrentFloatMask heightmapBase = new ConcurrentFloatMask(513, random.nextLong(), "heightmapBase");
		final ConcurrentFloatMask heightmapLand = new ConcurrentFloatMask(513, random.nextLong(), "heightmapLand");
		final ConcurrentFloatMask heightmapMountains = new ConcurrentFloatMask(513, random.nextLong(), "heightmapMountains");
		final ConcurrentFloatMask heightmapPlateaus = new ConcurrentFloatMask(513, random.nextLong(), "heightmapPlateaus");

		heightmapBase.init(land, 25f, 25f);
		heightmapLand.maskToHeightmap(0.025f, 0.25f, 95, land).smooth(2);

		heightmapMountains.maskToMoutains(2f, 0.5f, mountains);
		plateaus.combine(mountains);
		heightmapPlateaus.init(plateaus, 0, 3f).smooth(5f, ramps);
		heightmapMountains.add(heightmapPlateaus).smooth(1);

		final ConcurrentBinaryMask grass = new ConcurrentBinaryMask(land, random.nextLong(), "grass");
		final ConcurrentFloatMask grassTexture = new ConcurrentFloatMask(256, random.nextLong(), "grassTexture");
		final ConcurrentBinaryMask rock = new ConcurrentBinaryMask(mountains, random.nextLong(), "rock");
		final ConcurrentFloatMask rockTexture = new ConcurrentFloatMask(256, random.nextLong(), "rockTexture");

		heightmapBase.add(heightmapLand);
		heightmapBase.add(heightmapMountains);

		grass.deflate(6f).combine(plateaus).shrink(256).inflate(1);

		grassTexture.init(grass, 0, 0.999f).smooth(2);

		ConcurrentBinaryMask plateaus2 = new ConcurrentBinaryMask(plateaus, random.nextLong(), "plateaus2");
		plateaus.outline().inflate(2).minus(ramps);
		plateaus2.deflate(1).outline().inflate(2).minus(ramps);
		rock.inflate(3).combine(plateaus).combine(plateaus2).shrink(256);
		rockTexture.init(rock, 0, 0.999f).smooth(1);


		grass.minus(rock);

		Pipeline.start();

		Pipeline.await(heightmapBase);
		map.setHeightmap(heightmapBase.getFloatMask());
		map.getHeightmap().getRaster().setPixel(0, 0, new int[] { 0 });

		Pipeline.stop();
		System.out.printf("Terrain generation done: %d ms\n", System.currentTimeMillis() - startTime);

		MarkerGenerator markerGenerator = new MarkerGenerator(map, random.nextLong());
		BinaryMask spawnsMask = new BinaryMask(grass.getBinaryMask(), random.nextLong());
		spawnsMask.enlarge(513).minus(ramps.getBinaryMask()).deflate(16).trimEdge(20).fillCircle(256, 256, 128, false);
		markerGenerator.generateSpawns(spawnsMask, 64);
		BinaryMask resourceMask = new BinaryMask(grass.getBinaryMask().minus(rock.getBinaryMask()), random.nextLong());
		resourceMask.enlarge(513).minus(ramps.getBinaryMask()).deflate(5);
		markerGenerator.generateMexs(resourceMask);
		markerGenerator.generateHydros(resourceMask);

		BinaryMask noProps = new BinaryMask(rock.getBinaryMask(), random.nextLong());
		noProps.combine(ramps.getBinaryMask());
		for (int i = 0; i < map.getSpawns().length; i++) {
			noProps.fillCircle(map.getSpawns()[i].x, map.getSpawns()[i].z, 30, true);
		}
		for (int i = 0; i < map.getMexs().length; i++) {
			noProps.fillCircle(map.getMexs()[i].x, map.getMexs()[i].z, 5, true);
		}
		for (int i = 0; i < map.getHydros().length; i++) {
			noProps.fillCircle(map.getHydros()[i].x, map.getHydros()[i].z, 7, true);
		}

		PropGenerator propGenerator = new PropGenerator(map, random.nextLong());
		BinaryMask treeMask = new BinaryMask(32, random.nextLong());
		treeMask.randomize(0.2f).inflate(1).cutCorners().acid(0.5f).enlarge(128).smooth(4).acid(0.5f);
		BinaryMask fieldStoneMask = new BinaryMask(treeMask, random.nextLong());
		treeMask.enlarge(256).intersect(grass.getBinaryMask());
		fieldStoneMask.invert().enlarge(256).intersect(grass.getBinaryMask());
		treeMask.enlarge(513).deflate(5).fillCircle(256, 256, 96, false).minus(noProps).trimEdge(3);
		fieldStoneMask.enlarge(513).deflate(5).fillCircle(256, 256, 96, true).minus(noProps).trimEdge(10);

		propGenerator.generateProps(treeMask, propGenerator.TREE_GROUPS, 3f);
		propGenerator.generateProps(treeMask, propGenerator.ROCKS, 10f);
		propGenerator.generateProps(fieldStoneMask, propGenerator.FIELD_STONES, 30f);

		BinaryMask lightGrass = new BinaryMask(grass.getBinaryMask(), random.nextLong());
		lightGrass.randomize(0.5f);
		lightGrass.minus(rock.getBinaryMask()).intersect(grass.getBinaryMask()).minus(treeMask.shrink(256));
		FloatMask lightGrassTexture = new FloatMask(256, random.nextLong());
		lightGrassTexture.init(lightGrass, 0, 0.999f).smooth(2);

		map.setTextureMaskLow(grassTexture.getFloatMask(), lightGrassTexture, rockTexture.getFloatMask(), new FloatMask(513, 0));

		land.getBinaryMask().shrink(256);

		Graphics g = map.getPreview().getGraphics();
		for (int x = 0; x < 256; x++) {
			for (int y = 0; y < 256; y++) {
				int red = 0;
				int green = 0;
				int blue = 127;
				if (land.getBinaryMask().get(x, y)) {
					red = 191;
					green = 191;
					blue = 0;
				}
				if (grass.getBinaryMask().get(x, y)) {
					red = 0;
					green = 127;
					blue = 0;
				}
				if (lightGrass.get(x, y)) {
					red = 0;
					green = 191;
					blue = 0;
				}
				if (rock.getBinaryMask().get(x, y)) {
					red = 96;
					green = 96;
					blue = 96;
				}
				g.setColor(new Color(red, green, blue));
				g.fillRect(x, y, 1, 1);
			}
		}

		return map;
	}
}
