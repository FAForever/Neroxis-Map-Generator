package generator;

import biomes.Biome;
import biomes.Biomes;
import export.SCMapExporter;
import export.SaveExporter;
import export.ScenarioExporter;
import export.ScriptExporter;
import lombok.SneakyThrows;
import map.*;
import util.ArgumentParser;
import util.FileUtils;
import util.Pipeline;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ExecutionException;

public strictfp class MapGenerator {

	public static final boolean DEBUG = false;

	public static final String VERSION = "1.0.0";

	private static final Random SEED_GEN = new Random();

	//read from cli args
	private static String FOLDER_PATH = ".";
	private static String MAP_NAME = "debugMap";
	private static long SEED = SEED_GEN.nextLong();
	private static Random RANDOM = new Random(SEED);
	private static Optional<Biome> BIOME = Optional.empty();

	//read from key value arguments or map name
	private static int SPAWN_COUNT;
	private static float LAND_DENSITY;
	private static float PLATEAU_DENSITY;
	private static float MOUNTAIN_DENSITY;
	private static float RAMP_DENSITY;

	public static void main(String[] args) throws ExecutionException, InterruptedException, IOException {

		Locale.setDefault(Locale.US);
		if (DEBUG) {
			Path debugDir = Paths.get(".", "debug");
			FileUtils.deleteRecursiveIfExists(debugDir);
			Files.createDirectory(debugDir);
		}

		interpretArguments(args);

		MapGenerator generator = new MapGenerator();
		System.out.println("Generating map " + MAP_NAME.replace('/','^'));
		SCMap map = generator.generate(RANDOM.nextLong());
		System.out.println("Saving map to " + Paths.get(FOLDER_PATH).toAbsolutePath() + "\\" + MAP_NAME.replace('/','^'));
		System.out.println("Seed: "+SEED);
		System.out.println("Land Density: "+LAND_DENSITY);
		System.out.println("Plateau Density: "+PLATEAU_DENSITY);
		System.out.println("Mountain Density: "+MOUNTAIN_DENSITY);
		System.out.println("Ramp Density: "+RAMP_DENSITY);
		generator.save(FOLDER_PATH, MAP_NAME.replace('/','^'), map);
		System.out.println("Done");

		generator.generateDebugOutput();
	}

	public void save(String folderName, String mapName, SCMap map) {
		try {
			Path folderPath = Paths.get(folderName);

			FileUtils.deleteRecursiveIfExists(folderPath.resolve(mapName));

			Files.createDirectory(folderPath.resolve(mapName));
			SCMapExporter.exportSCMAP(folderPath, mapName, map);
			SaveExporter.exportSave(folderPath, mapName, map);
			ScenarioExporter.exportScenario(folderPath, mapName, map);
			ScriptExporter.exportScript(folderPath, mapName, map);

		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Error while saving the map.");
		}
	}

	public SCMap generate(long seed) throws ExecutionException, InterruptedException {
		long startTime = System.currentTimeMillis();
		final Random random = new Random(seed);
		final int mexCount = SPAWN_COUNT*8 + 4/(SPAWN_COUNT/2)*SPAWN_COUNT + random.nextInt(40/SPAWN_COUNT)*SPAWN_COUNT;
		final int hydroCount = SPAWN_COUNT + random.nextInt(SPAWN_COUNT/2)*2;
		final SCMap map = new SCMap(512, SPAWN_COUNT, mexCount, hydroCount);

		final ConcurrentBinaryMask land = new ConcurrentBinaryMask(16, random.nextLong(), "land");
		final ConcurrentBinaryMask mountains = new ConcurrentBinaryMask(32, random.nextLong(), "mountains");
		final ConcurrentBinaryMask plateaus = new ConcurrentBinaryMask(32, random.nextLong(), "plateaus");
		final ConcurrentBinaryMask ramps = new ConcurrentBinaryMask(128, random.nextLong(), "ramps");

		land.randomize(LAND_DENSITY).inflate(1).cutCorners().enlarge(32).acid(0.5f).enlarge(128).smooth(4).acid(0.5f);
		mountains.randomize(MOUNTAIN_DENSITY).inflate(1).cutCorners().acid(0.5f).enlarge(128).smooth(4).acid(0.5f);
		plateaus.randomize(PLATEAU_DENSITY).inflate(1).cutCorners().acid(0.5f).enlarge(128).smooth(4).acid(0.5f);
		ramps.randomize(RAMP_DENSITY);

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

		ConcurrentBinaryMask cliffs = new ConcurrentBinaryMask(plateaus.getBinaryMask().getSize(), seed, "cliffs")
				.combine(plateaus)
				.combine(plateaus2)
                .shrink(256);


		grass.minus(rock);

		Pipeline.start();

		Pipeline.await(heightmapBase);
		map.setHeightmap(heightmapBase.getFloatMask());
		map.getHeightmap().getRaster().setPixel(0, 0, new int[] { 0 });

		Pipeline.stop();
		System.out.printf("Terrain generation done: %d ms\n", System.currentTimeMillis() - startTime);

		MarkerGenerator markerGenerator = new MarkerGenerator(map, random.nextLong());

		BinaryMask spawnsMask = new BinaryMask(grass.getBinaryMask(), random.nextLong());
		BinaryMask resourceMask = new BinaryMask(grass.getBinaryMask().minus(rock.getBinaryMask()), random.nextLong());

		spawnsMask.enlarge(513).minus(ramps.getBinaryMask()).deflate(18).trimEdge(20).fillCircle(256, 256, 128, false);
		resourceMask.enlarge(513).minus(ramps.getBinaryMask()).deflate(5).trimEdge(20);

		markerGenerator.generateSpawns(spawnsMask, 64);
		markerGenerator.generateMexes(resourceMask);
		markerGenerator.generateHydros(resourceMask);

		BinaryMask noProps = new BinaryMask(rock.getBinaryMask(), random.nextLong());
		noProps.combine(ramps.getBinaryMask());
		for (int i = 0; i < map.getSpawns().length; i++) {
			noProps.fillCircle(map.getSpawns()[i].x, map.getSpawns()[i].z, 30, true);
		}
		for (int i = 0; i < map.getMexes().length; i++) {
			noProps.fillCircle(map.getMexes()[i].x, map.getMexes()[i].z, 5, true);
		}
		for (int i = 0; i < map.getHydros().length; i++) {
			noProps.fillCircle(map.getHydros()[i].x, map.getHydros()[i].z, 7, true);
		}

		BinaryMask noWrecks = new BinaryMask(rock.getBinaryMask(), random.nextLong());
		noWrecks.combine(ramps.getBinaryMask());
		for (int i = 0; i < map.getSpawns().length; i++) {
			noWrecks.fillCircle(map.getSpawns()[i].x, map.getSpawns()[i].z, 96, true);
		}
		for (int i = 0; i < map.getMexes().length; i++) {
			noWrecks.fillCircle(map.getMexes()[i].x, map.getMexes()[i].z, 10, true);
		}
		for (int i = 0; i < map.getHydros().length; i++) {
			noWrecks.fillCircle(map.getHydros()[i].x, map.getHydros()[i].z, 15, true);
		}

		WreckGenerator wreckGenerator = new WreckGenerator(map, random.nextLong());
		UnitGenerator unitGenerator = new UnitGenerator(map, random.nextLong());

		BinaryMask t1LandWreckMask = new BinaryMask(64, random.nextLong());
		BinaryMask t2LandWreckMask = new BinaryMask(64, random.nextLong());
		BinaryMask t3LandWreckMask = new BinaryMask(64, random.nextLong());
		BinaryMask t2NavyWreckMask = new BinaryMask(64, random.nextLong());
		BinaryMask navyFactoryWreckMask = new BinaryMask(64, random.nextLong());
		BinaryMask navyLandCopy = new BinaryMask(land.getBinaryMask(), random.nextLong());
		BinaryMask allWreckMask = new BinaryMask(513, random.nextLong());

		t1LandWreckMask.randomize(0.01f).intersect(grass.getBinaryMask()).minus(noWrecks).deflate(1).trimEdge(20);
		t2LandWreckMask.randomize(0.005f).intersect(grass.getBinaryMask()).minus(noWrecks).minus(t1LandWreckMask).trimEdge(20);
		t3LandWreckMask.randomize(0.001f).intersect(grass.getBinaryMask()).minus(noWrecks).minus(t1LandWreckMask).minus(t2LandWreckMask).trimEdge(64);
		t2NavyWreckMask.randomize(0.01f).intersect(navyLandCopy.outline()).minus(noWrecks).trimEdge(20);
		navyFactoryWreckMask.randomize(0.005f).minus(land.getBinaryMask()).minus(noWrecks).deflate(3).trimEdge(20);
		allWreckMask.combine(t1LandWreckMask).combine(t2LandWreckMask).combine(t3LandWreckMask).combine(t2NavyWreckMask).inflate(2);
		noProps.combine(allWreckMask);

		wreckGenerator.generateWrecks(t1LandWreckMask, WreckGenerator.T1_Land, 2f);
		wreckGenerator.generateWrecks(t2LandWreckMask, WreckGenerator.T2_Land, 15f);
		wreckGenerator.generateWrecks(t3LandWreckMask, WreckGenerator.T3_Land, 30f);
		wreckGenerator.generateWrecks(t2NavyWreckMask, WreckGenerator.T2_Navy, 60f);
		wreckGenerator.generateWrecks(navyFactoryWreckMask, WreckGenerator.Navy_Factory, 120f);

		PropGenerator propGenerator = new PropGenerator(map, random.nextLong());

		BinaryMask treeMask = new BinaryMask(32, random.nextLong());
		BinaryMask cliffRockMask = new BinaryMask(land.getBinaryMask(), random.nextLong());
		BinaryMask cliffLandCopy = new BinaryMask(land.getBinaryMask(), random.nextLong());
    	BinaryMask fieldStoneMask = new BinaryMask(treeMask, random.nextLong());
		BinaryMask rockFieldMask = new BinaryMask(128, random.nextLong());
    
		cliffRockMask.randomize(.2f).intersect(rock.getBinaryMask()).minus(plateaus.getBinaryMask()).minus(mountains.getBinaryMask()).intersect(cliffLandCopy);
		fieldStoneMask.invert().enlarge(256).intersect(grass.getBinaryMask());
		fieldStoneMask.enlarge(513).deflate(5).minus(noProps).trimEdge(10);
		treeMask.randomize(0.1f).inflate(1).cutCorners().acid(0.5f).enlarge(128).smooth(4).acid(0.5f);
		treeMask.enlarge(256).intersect(grass.getBinaryMask());
		treeMask.enlarge(513).deflate(5).minus(noProps).trimEdge(3).fillCircle(256,256,96,false);
		rockFieldMask.randomize(.001f).trimEdge(48).inflate(3).acid(.5f).intersect(land.getBinaryMask()).minus(mountains.getBinaryMask()).minus(noProps);


		propGenerator.generateProps(treeMask, propGenerator.TREE_GROUPS, 4.5f);
		propGenerator.generateProps(cliffRockMask, propGenerator.ROCKS, 3f);
		propGenerator.generateProps(fieldStoneMask, propGenerator.FIELD_STONES, 60f);
		propGenerator.generateProps(rockFieldMask, propGenerator.ROCKS, 2f);

		BinaryMask lightGrass = new BinaryMask(grass.getBinaryMask(), random.nextLong());

		lightGrass.randomize(0.5f);
		lightGrass.minus(rock.getBinaryMask()).intersect(grass.getBinaryMask()).minus(treeMask.shrink(256));

		FloatMask lightGrassTexture = new FloatMask(256, random.nextLong());

		lightGrassTexture.init(lightGrass, 0, 0.999f).smooth(2);

		map.setTextureMaskLow(grassTexture.getFloatMask(), lightGrassTexture, rockTexture.getFloatMask(), new FloatMask(513, 0));

		Biome biomeSet = BIOME.orElseGet(() -> Biomes.getRandomBiome(random));

		System.out.printf("Using biome %s\n", biomeSet.getName());
		map.biome.setTerrainMaterials(biomeSet.getTerrainMaterials());
		map.biome.setWaterSettings(biomeSet.getWaterSettings());
		map.biome.setLightingSettings(biomeSet.getLightingSettings());

		land.getBinaryMask().shrink(256);

		Preview.generate(
			map.getPreview(),
			map,
			rock.getBinaryMask()
		);


		return map;
	}

	private static void interpretArguments(String[] args) {
		if (args.length == 0 || args[0].startsWith("--")) {
			interpretArguments(ArgumentParser.parse(args));
		}
		else if (args.length == 2){
			FOLDER_PATH = args[0];
			MAP_NAME = args[1];
			parseMapName();
		}
		else {
			try {
				FOLDER_PATH = args[0];
				try {
					SEED = Long.parseLong(args[1]);
					RANDOM = new Random(SEED);
				} catch (NumberFormatException nfe) {
					System.out.println("Seed not numeric using default seed or mapname");
				}
				if (!VERSION.equals(args[2])) {
					System.out.println("This generator only supports version " + VERSION);
					System.exit(-1);
				}
				if (args.length>=4) {
					MAP_NAME = args[3];
					parseMapName();
				}
				else {
					randomizeOptions();
					generateMapName();
				}
			} catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
				System.out.println("Usage: generator [targetFolder] [seed] [expectedVersion] (mapName)");
			}
		}
	}

	private static void interpretArguments(Map<String, String> arguments) {
		if (arguments.containsKey("help")) {
			System.out.println("map-gen usage:\n" +
					"--help                               produce help message\n" +
					"--folder-path arg                    mandatory, set the target folder for the generated map\n" +
					"--seed arg                           optional, set the seed for the generated map\n" +
					"--map-name arg                       optional, set the map name for the generated map\n" +
					"--spawn-count arg                    optional, set the spawn count for the generated map\n" +
					"--land-density arg                   optional, set the land density for the generated map\n" +
					"--plateau-density arg                optional, set the plateau density for the generated map (max .2)\n" +
					"--mountain-density arg               optional, set the mountain density for the generated map (max .1)\n" +
					"--ramp-density arg                   optional, set the ramp density for the generated map\n (max .2)");
			System.exit(0);
		}

		if (!Arrays.asList("folder-path").stream().allMatch(arguments::containsKey) && !DEBUG) {
			System.out.println("Missing necessary argument.");
			System.exit(-1);
		}

		FOLDER_PATH = arguments.get("folder-path");

		if (arguments.containsKey("map-name")) {
			MAP_NAME = arguments.get("map-name");
			parseMapName();
			return;
		}

		if (arguments.containsKey("seed")) {
			SEED = Long.parseLong(arguments.get("seed"));
			RANDOM = new Random(SEED);
		}
		randomizeOptions();

		if (arguments.containsKey("spawn-count")) {
			SPAWN_COUNT = Integer.parseInt(arguments.get("spawn-count"));
		}

		if (arguments.containsKey("land-density")) {
			LAND_DENSITY = Float.parseFloat(arguments.get("land-density"));
			LAND_DENSITY = (float) StrictMath.round(LAND_DENSITY*127)/127;
		}

		if (arguments.containsKey("plateau-density")) {
			PLATEAU_DENSITY = StrictMath.min(Float.parseFloat(arguments.get("plateau-density")),0.2f);
			PLATEAU_DENSITY = (float) StrictMath.round(LAND_DENSITY*127)/127;
		}

		if (arguments.containsKey("mountain-density")) {
			MOUNTAIN_DENSITY = StrictMath.min(Float.parseFloat(arguments.get("mountain-density")),0.1f);
			MOUNTAIN_DENSITY = (float) StrictMath.round(LAND_DENSITY*127)/127;
		}

		if (arguments.containsKey("ramp-density")) {
			RAMP_DENSITY = StrictMath.min(Float.parseFloat(arguments.get("ramp-density")),0.2f);
			RAMP_DENSITY = (float) StrictMath.round(LAND_DENSITY*127)/127;
		}

		generateMapName();
	}

	private static void parseMapName() {
		MAP_NAME = MAP_NAME.replace('^','/');
		if (!MAP_NAME.startsWith("neroxis_map_generator")){
			throw new IllegalArgumentException("Map name is not a generated map");
		}
		String[] args = MAP_NAME.split("_");
		if (args.length<4){
			throw new RuntimeException("Version not specified");
		}
		String version = args[3];
		if (!VERSION.equals(version)){
			throw new RuntimeException("Unsupported generator version: " + version);
		}
		if (args.length>=5) {
			String seedString = args[4];
			try {
				SEED = Long.parseLong(seedString);
			} catch (NumberFormatException nfe) {
				byte[] seedBytes = Base64.getDecoder().decode(seedString);
				ByteBuffer seedWrapper = ByteBuffer.wrap(seedBytes);
				SEED = seedWrapper.getLong();
			}
			RANDOM = new Random(SEED);
			randomizeOptions();
		}
		if (args.length>=6) {
			String optionString = args[5];
			byte[] optionBytes = Base64.getDecoder().decode(optionString);
			parseOptions(optionBytes);
		}
	}

	private static void randomizeOptions(){
		SPAWN_COUNT = 6;
		LAND_DENSITY = StrictMath.min((RANDOM.nextInt(127)+13.0f)/127,1);
		PLATEAU_DENSITY = (float) RANDOM.nextInt(127)/127*0.2f;
		MOUNTAIN_DENSITY = (float) RANDOM.nextInt(127)/127*0.075f;
		RAMP_DENSITY = (float) RANDOM.nextInt(127)/127*0.2f;
	}

	private static void parseOptions(byte[] optionBytes){
		if (optionBytes.length>0){
			if (optionBytes[0]<=16){
				SPAWN_COUNT = optionBytes[0];
			}
		}
		if (optionBytes.length>1){
			LAND_DENSITY = (float) optionBytes[1]/127+13.0f/127;
		}
		if (optionBytes.length>2){
			PLATEAU_DENSITY = (float) optionBytes[2]/127*0.2f;
		}
		if (optionBytes.length>3){
			MOUNTAIN_DENSITY = (float) optionBytes[3]/127*0.075f;
		}
		if (optionBytes.length>4){
			RAMP_DENSITY = (float) optionBytes[4]/127*0.2f;
		}
	}

	private static void generateMapName(){
		String mapNameFormat = "neroxis_map_generator_%s_%s_%s";
		ByteBuffer seedBuffer = ByteBuffer.allocate(8);
		seedBuffer.putLong(SEED);
		String seedString = Base64.getEncoder().encodeToString(seedBuffer.array());
		byte[] optionArray = {(byte) SPAWN_COUNT,
				(byte) (LAND_DENSITY*127),
				(byte) (PLATEAU_DENSITY/0.2f*127),
				(byte) (MOUNTAIN_DENSITY/0.1f*127),
				(byte) (RAMP_DENSITY/0.2f*127)};
		String optionString = Base64.getEncoder().encodeToString(optionArray);
		MAP_NAME = String.format( mapNameFormat, VERSION, seedString, optionString);
	}


	@SneakyThrows({IOException.class, NoSuchAlgorithmException.class})
	private void generateDebugOutput() {
		if(!DEBUG) {
			return;
		}

		FileWriter writer = new FileWriter(Paths.get(".", "debug", "summary.txt").toFile());
		Path masksDir = Paths.get(".", "debug");

		for(int i = 0;i < Pipeline.getPipelineSize();i++) {
			Path maskFile = masksDir.resolve(i + ".mask");
			writer.write(String.format( "%d:\t%s\n", i, hashFiles(maskFile)));
		}

		String mapHash = hashFiles(SCMapExporter.file.toPath(), SaveExporter.file.toPath());
		System.out.println("Map hash: " + mapHash);
		writer.write(String.format("Map hash:\t%s", mapHash));
		writer.flush();
		writer.close();
	}

	private String hashFiles(Path... files) throws NoSuchAlgorithmException, IOException {
		StringBuilder sb = new StringBuilder();
		Arrays.stream(files).map(this::hashFile).forEach(sb::append);
		byte[] hash = MessageDigest.getInstance("SHA-256").digest(sb.toString().getBytes());
		return toHex(hash);
	}

	@SneakyThrows({IOException.class, NoSuchAlgorithmException.class})
	private String hashFile(Path file) {
		byte[] hash = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file));
		return toHex(hash);
	}

	private String toHex(byte[] data) {
		StringBuilder sb = new StringBuilder();
		for(int i = 0;i < data.length;i++) {
			sb.append(String.format("%02x", data[i]));
		}
		return sb.toString();
	}
}
