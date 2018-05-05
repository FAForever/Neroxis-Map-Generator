package generator;

import java.awt.*;
import java.io.*;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import export.*;
import map.*;

public strictfp class MapGenerator {
	
	public static final String VERSION = "0.1.0";

	public static void main(String[] args) throws ExecutionException, InterruptedException {
		try {
			String folderPath = args[0];
			long seed = Long.parseLong(args[1]);
			String version = args[2];	
			
			if(version.equals(VERSION)) {
				MapGenerator generator = new MapGenerator();
				System.out.println("Generating map NeroxisGen_" + VERSION + "_" + seed);
				SCMap map = generator.generate(seed);
				System.out.println("Saving map to " + Paths.get(folderPath).toAbsolutePath());
				generator.save(folderPath, "NeroxisGen_" + VERSION, map, seed);
				System.out.println("Done");
				
			} else {
				System.out.println("This generator only supports version " + VERSION);
				
			}
		} catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
			System.out.println("Usage: generator [saveFileName] [seed] [expectedVersion]");
			
		}
	}

	public void save(String folderPath, String prefix, SCMap map, long seed) {
		try {
			File file = new File(folderPath + prefix + "_" + seed);
			file.mkdirs();
			SCMapExporter.exportSCMAP(folderPath, prefix + "_" + seed, map);
			SaveExporter.exportSave(folderPath, prefix + "_" + seed, map);
			ScenarioExporter.exportScenario(folderPath, prefix + "_" + seed, map);
			ScriptExporter.exportScript(folderPath, prefix + "_" + seed, map);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public SCMap generate(long seed) {
		final Random random = new Random(seed);
		final SCMap map = new SCMap(512, 6, 64, 10);

		final BinaryMask land = new BinaryMask(16, random.nextLong());
		final BinaryMask mountains = new BinaryMask(32, random.nextLong());
		final BinaryMask plateaus = new BinaryMask(32, random.nextLong());
		final BinaryMask ramps = new BinaryMask(128, random.nextLong());

		CompletableFuture<Void> landFuture = CompletableFuture.runAsync(() -> {
			land.randomize(0.2f).inflate(1).cutCorners().enlarge(32).acid(0.5f).enlarge(128).smooth(4).acid(0.5f);
		});
		CompletableFuture<Void> mountainsFuture = CompletableFuture.runAsync(() -> {
			mountains.randomize(0.05f).inflate(1).cutCorners().acid(0.5f).enlarge(128).smooth(4).acid(0.5f);
		});
		CompletableFuture<Void> plateausFuture = CompletableFuture.runAsync(() -> {
			plateaus.randomize(0.1f).inflate(1).cutCorners().acid(0.5f).enlarge(128).smooth(4).acid(0.5f);
		});
		CompletableFuture<Void> rampsFuture = CompletableFuture.runAsync(() -> {
			ramps.randomize(0.1f);
		});
		Arrays.asList(landFuture, mountainsFuture, plateausFuture, rampsFuture).forEach(CompletableFuture::join);

		plateaus.intersect(land).minus(mountains);
		ramps.intersect(plateaus).outline().minus(plateaus).intersect(land).minus(mountains).inflate(2);
		land.combine(mountains);

		landFuture = CompletableFuture.runAsync(() -> {
			land.enlarge(513).smooth(6);
		});
		mountainsFuture = CompletableFuture.runAsync(() -> {
			mountains.enlarge(513).inflate(1).smooth(6);
		});
		plateausFuture = CompletableFuture.runAsync(() -> {
			plateaus.enlarge(513).inflate(1).smooth(6);
		});
		rampsFuture = CompletableFuture.runAsync(() -> {
			ramps.enlarge(513).smooth(6);
		});

		final FloatMask heightmapBase = new FloatMask(513, random.nextLong());
		final FloatMask heightmapLand = new FloatMask(513, random.nextLong());
		final FloatMask heightmapMountains = new FloatMask(513, random.nextLong());
		final FloatMask heightmapPlateaus = new FloatMask(513, random.nextLong());

		Arrays.asList(landFuture, mountainsFuture, plateausFuture, rampsFuture).forEach(CompletableFuture::join);

		landFuture = CompletableFuture.runAsync(() -> {
			heightmapBase.init(land, 25f, 25f);
			heightmapLand.maskToHeightmap(0.025f, 0.25f, 95, land).smooth(2);
		});

		CompletableFuture<Void> mountainsPlateausRampsFuture = CompletableFuture.runAsync(() -> {
			heightmapMountains.maskToMoutains(2f, 0.5f, mountains);
			plateaus.combine(mountains);
			heightmapPlateaus.init(plateaus, 0, 3f).smooth(5f, ramps);
			heightmapMountains.add(heightmapPlateaus).smooth(1);
		});

		Arrays.asList(landFuture, mountainsPlateausRampsFuture).forEach(CompletableFuture::join);

		final BinaryMask grass = new BinaryMask(land, random.nextLong());
		final FloatMask grassTexture = new FloatMask(256, random.nextLong());
		final BinaryMask rock = new BinaryMask(mountains, random.nextLong());
		final FloatMask rockTexture = new FloatMask(256, random.nextLong());

		CompletableFuture<Void> heightmapFuture = CompletableFuture.runAsync(() -> {
			heightmapBase.add(heightmapLand);
			heightmapBase.add(heightmapMountains);
			map.setHeightmap(heightmapBase);
			map.getHeightmap().getRaster().setPixel(0, 0, new int[] { 0 });
		});

		grass.deflate(6f).combine(plateaus).shrink(256).inflate(1);

		CompletableFuture<Void> grassFuture = CompletableFuture.runAsync(() -> {
			grassTexture.init(grass, 0, 0.999f).smooth(2);
		});

		CompletableFuture<Void> rockFuture = CompletableFuture.runAsync(() -> {
			BinaryMask plateaus2 = new BinaryMask(plateaus, random.nextLong());
			plateaus.outline().inflate(2).minus(ramps);
			plateaus2.deflate(1).outline().inflate(2).minus(ramps);
			rock.inflate(3).combine(plateaus).combine(plateaus2).shrink(256);
			rockTexture.init(rock, 0, 0.999f).smooth(1);
		});

		Arrays.asList(grassFuture, rockFuture, heightmapFuture).forEach(CompletableFuture::join);

		grass.minus(rock);

		MarkerGenerator markerGenerator = new MarkerGenerator(map, random.nextLong());
		BinaryMask spawnsMask = new BinaryMask(grass, random.nextLong());
		spawnsMask.enlarge(513).minus(ramps).deflate(16).trimEdge(20).fillCircle(256, 256, 128, false);
		markerGenerator.generateSpawns(spawnsMask, 64);
		BinaryMask resourceMask = new BinaryMask(grass.minus(rock), random.nextLong());
		resourceMask.enlarge(513).minus(ramps).deflate(5);
		markerGenerator.generateMexs(resourceMask);
		markerGenerator.generateHydros(resourceMask);

		BinaryMask noProps = new BinaryMask(rock, random.nextLong());
		noProps.combine(ramps);
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
		treeMask.enlarge(256).intersect(grass);
		fieldStoneMask.invert().enlarge(256).intersect(grass);
		treeMask.enlarge(513).deflate(5).fillCircle(256, 256, 96, false).minus(noProps).trimEdge(3);
		fieldStoneMask.enlarge(513).deflate(5).fillCircle(256, 256, 96, true).minus(noProps).trimEdge(10);

		propGenerator.generateProps(treeMask, propGenerator.TREE_GROUPS, 3f);
		propGenerator.generateProps(treeMask, propGenerator.ROCKS, 10f);
		propGenerator.generateProps(fieldStoneMask, propGenerator.FIELD_STONES, 30f);

		BinaryMask lightGrass = new BinaryMask(grass, random.nextLong());
		lightGrass.randomize(0.5f);
		lightGrass.minus(rock).intersect(grass).minus(treeMask.shrink(256));
		FloatMask lightGrassTexture = new FloatMask(256, random.nextLong());
		lightGrassTexture.init(lightGrass, 0, 0.999f).smooth(2);

		map.setTextureMaskLow(grassTexture, lightGrassTexture, rockTexture, new FloatMask(513, 0));

		land.shrink(256);

		Graphics g = map.getPreview().getGraphics();
		for (int x = 0; x < 256; x++) {
			for (int y = 0; y < 256; y++) {
				int red = 0;
				int green = 0;
				int blue = 127;
				if (land.get(x, y)) {
					red = 191;
					green = 191;
					blue = 0;
				}
				if (grass.get(x, y)) {
					red = 0;
					green = 127;
					blue = 0;
				}
				if (lightGrass.get(x, y)) {
					red = 0;
					green = 191;
					blue = 0;
				}
				if (rock.get(x, y)) {
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
