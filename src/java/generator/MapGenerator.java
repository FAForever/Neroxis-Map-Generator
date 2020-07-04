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
import java.util.stream.Stream;

public strictfp class MapGenerator {

	public static final boolean DEBUG = false;
	public static final String VERSION = "1.0.1";
	private static final Random SEED_GEN = new Random();

	//read from cli args
	private static String folderPath = ".";
	private static String mapName = "debugMap";
	private static long seed = SEED_GEN.nextLong();
	private static Random random = new Random(seed);

	//read from key value arguments or map name
	private static int spawnCount;
	private static float landDensity;
	private static float plateauDensity;
	private static float mountainDensity;
	private static float rampDensity;
	private static float reclaimDensity;
	private static int mexCount;

	private SCMap map;

	//masks used in generation
	private ConcurrentBinaryMask land;
	private ConcurrentBinaryMask mountains;
	private ConcurrentBinaryMask plateaus;
	private ConcurrentBinaryMask ramps;
	private ConcurrentFloatMask heightmapBase;
	private ConcurrentBinaryMask grass;
	private ConcurrentFloatMask grassTexture;
	private ConcurrentBinaryMask rock;
	private ConcurrentFloatMask rockTexture;
	private ConcurrentBinaryMask allWreckMask;
	private ConcurrentBinaryMask spawnsMask;
	private ConcurrentBinaryMask resourceMask;
	private ConcurrentFloatMask lightGrassTexture;
	private ConcurrentBinaryMask t1LandWreckMask;
	private ConcurrentBinaryMask t2LandWreckMask;
	private ConcurrentBinaryMask t3LandWreckMask;
	private ConcurrentBinaryMask t2NavyWreckMask;
	private ConcurrentBinaryMask navyFactoryWreckMask;
	private ConcurrentBinaryMask treeMask;
	private ConcurrentBinaryMask cliffRockMask;
	private ConcurrentBinaryMask fieldStoneMask;
	private ConcurrentBinaryMask rockFieldMask;
	private BinaryMask noProps;
	private BinaryMask noWrecks;

	public static void main(String[] args) throws IOException {

		Locale.setDefault(Locale.US);
		if (DEBUG) {
			Path debugDir = Paths.get(".", "debug");
			FileUtils.deleteRecursiveIfExists(debugDir);
			Files.createDirectory(debugDir);
		}

		interpretArguments(args);

		MapGenerator generator = new MapGenerator();
		System.out.println("Generating map " + mapName.replace('/','^'));
		SCMap map = generator.generate();
		System.out.println("Saving map to " + Paths.get(folderPath).toAbsolutePath() + "\\" + mapName.replace('/','^'));
		System.out.println("Seed: "+ seed);
		System.out.println("Land Density: "+ landDensity);
		System.out.println("Plateau Density: "+ plateauDensity);
		System.out.println("Mountain Density: "+ mountainDensity);
		System.out.println("Ramp Density: "+ rampDensity);
		System.out.println("Reclaim Density: "+ reclaimDensity);
		System.out.println("Mex Count: "+ mexCount);
		generator.save(folderPath, mapName.replace('/','^'), map);
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

	public SCMap generate() {
		long startTime = System.currentTimeMillis();

		final int hydroCount = spawnCount + random.nextInt(spawnCount /2)*2;
		map = new SCMap(512, spawnCount, mexCount * spawnCount, hydroCount);

		MarkerGenerator markerGenerator = new MarkerGenerator(map, random.nextLong());
		WreckGenerator wreckGenerator = new WreckGenerator(map, random.nextLong());
		PropGenerator propGenerator = new PropGenerator(map, random.nextLong());

		setupTerrainPipeline();
		setupHeightmapPipeline();
		setupTexturePipeline();
		setupMarkerPipeline();
		setupWreckPipeline();
		setupPropPipeline();

		Pipeline.start();

		Pipeline.await(heightmapBase);
		map.setHeightmap(heightmapBase.getFloatMask());
		map.getHeightmap().getRaster().setPixel(0, 0, new int[] { 0 });

		Pipeline.await(spawnsMask);
		markerGenerator.generateSpawns(spawnsMask.getBinaryMask(), 64);
		Pipeline.await(resourceMask);
		markerGenerator.generateMexes(resourceMask.getBinaryMask());
		markerGenerator.generateHydros(resourceMask.getBinaryMask());

		Pipeline.stop();

		generateExclusionMasks();

		map.setTextureMaskLow(grassTexture.getFloatMask(), lightGrassTexture.getFloatMask(), rockTexture.getFloatMask(), new FloatMask(513, 0));

		wreckGenerator.generateWrecks(t1LandWreckMask.getBinaryMask().minus(noWrecks), WreckGenerator.T1_Land, 2f);
		wreckGenerator.generateWrecks(t2LandWreckMask.getBinaryMask().minus(noWrecks), WreckGenerator.T2_Land, 15f);
		wreckGenerator.generateWrecks(t3LandWreckMask.getBinaryMask().minus(noWrecks), WreckGenerator.T3_Land, 30f);
		wreckGenerator.generateWrecks(t2NavyWreckMask.getBinaryMask().minus(noWrecks), WreckGenerator.T2_Navy, 60f);
		wreckGenerator.generateWrecks(navyFactoryWreckMask.getBinaryMask().minus(noWrecks), WreckGenerator.Navy_Factory, 120f);

		propGenerator.generateProps(treeMask.getBinaryMask().minus(noProps), propGenerator.TREE_GROUPS, 3f);
		propGenerator.generateProps(cliffRockMask.getBinaryMask().minus(noProps), propGenerator.ROCKS, 1.5f);
		propGenerator.generateProps(fieldStoneMask.getBinaryMask().minus(noProps), propGenerator.FIELD_STONES, 60f);
		propGenerator.generateProps(rockFieldMask.getBinaryMask().minus(noProps), propGenerator.ROCKS, 2f);

		Biome biomeSet = Biomes.getRandomBiome(random);

		System.out.printf("Using biome %s\n", biomeSet.getName());
		map.biome.setTerrainMaterials(biomeSet.getTerrainMaterials());
		map.biome.setWaterSettings(biomeSet.getWaterSettings());
		map.biome.setLightingSettings(biomeSet.getLightingSettings());

		Preview.generate(
			map.getPreview(),
			map,
			rock.getBinaryMask()
		);

		System.out.printf("Map generation done: %d ms\n", System.currentTimeMillis() - startTime);

		return map;
	}

	private void setupTerrainPipeline(){
		land = new ConcurrentBinaryMask(16, random.nextLong(), "land");
		mountains = new ConcurrentBinaryMask(32, random.nextLong(), "mountains");
		plateaus = new ConcurrentBinaryMask(32, random.nextLong(), "plateaus");
		ramps = new ConcurrentBinaryMask(128, random.nextLong(), "ramps");

		land.randomize(landDensity).inflate(1).cutCorners().enlarge(32).acid(0.5f).enlarge(128).smooth(4).acid(0.5f);
		mountains.randomize(mountainDensity).inflate(1).cutCorners().acid(0.5f).enlarge(128).smooth(4).acid(0.5f);
		plateaus.randomize(plateauDensity).inflate(1).cutCorners().acid(0.5f).enlarge(128).smooth(4).acid(0.5f);
		ramps.randomize(rampDensity);

		plateaus.intersect(land).minus(mountains);
		ramps.intersect(plateaus).outline().minus(plateaus).intersect(land).minus(mountains).inflate(2);
		land.combine(mountains);

		land.enlarge(513).smooth(6);
		mountains.enlarge(513).inflate(1).smooth(6);
		plateaus.enlarge(513).inflate(1).smooth(6);
		ramps.enlarge(513).smooth(6);
	}

	private void setupHeightmapPipeline(){
		heightmapBase = new ConcurrentFloatMask(513, random.nextLong(), "heightmapBase");
		ConcurrentFloatMask heightmapLand = new ConcurrentFloatMask(513, random.nextLong(), "heightmapLand");
		ConcurrentFloatMask heightmapMountains = new ConcurrentFloatMask(513, random.nextLong(), "heightmapMountains");
		ConcurrentFloatMask heightmapPlateaus = new ConcurrentFloatMask(513, random.nextLong(), "heightmapPlateaus");

		plateaus.combine(mountains);
		heightmapBase.init(land, 25f, 25f);
		heightmapPlateaus.init(plateaus, 0, 3f).smooth(5f, ramps);
		heightmapLand.maskToHeightmap(0.025f, 0.25f, 95, land).smooth(2);
		heightmapMountains.maskToMoutains(2f, 0.5f, mountains);
		heightmapMountains.add(heightmapPlateaus).smooth(1);

		heightmapBase.add(heightmapLand);
		heightmapBase.add(heightmapMountains);
	}

	private void setupTexturePipeline(){
		grass = new ConcurrentBinaryMask(513, random.nextLong(), "grass");
		rock = new ConcurrentBinaryMask(513, random.nextLong(), "rock");
		ConcurrentBinaryMask lightGrass = new ConcurrentBinaryMask(513, random.nextLong(), "lightGrass");
		ConcurrentBinaryMask plateauCopy = new ConcurrentBinaryMask(513, random.nextLong(), "plateauCopy");
		rockTexture = new ConcurrentFloatMask(256, random.nextLong(), "rockTexture");
		grassTexture = new ConcurrentFloatMask(256, random.nextLong(), "grassTexture");
		lightGrassTexture = new ConcurrentFloatMask(256, random.nextLong(), "lightGrassTexture");

		plateauCopy.combine(plateaus).outline().inflate(3).minus(ramps);
		rock.combine(mountains).combine(plateauCopy).shrink(256);
		grass.combine(land).deflate(6f).combine(plateaus).shrink(256).inflate(1).minus(rock);
		lightGrass.randomize(0.5f).shrink(256);

		rockTexture.init(rock, 0, 0.999f).smooth(1);
		grassTexture.init(grass, 0, 0.999f).smooth(2);
		lightGrassTexture.init(lightGrass, 0, 0.999f).smooth(2);
	}

	private void setupMarkerPipeline(){
		spawnsMask = new ConcurrentBinaryMask(513, random.nextLong(), "spawns");
		resourceMask = new ConcurrentBinaryMask(513, random.nextLong(), "resource");

		spawnsMask.combine(land).deflate(6f).combine(plateaus).minus(rock).minus(ramps).deflate(18).trimEdge(20).fillCircle(256, 256, 128, false);
		resourceMask.combine(land).deflate(6f).combine(plateaus).minus(rock).minus(ramps).deflate(5).trimEdge(20);
	}

	private void setupWreckPipeline(){
		t1LandWreckMask = new ConcurrentBinaryMask(64, random.nextLong(), "t1LandWreck");
		t2LandWreckMask = new ConcurrentBinaryMask(64, random.nextLong(), "t2LandWreck");
		t3LandWreckMask = new ConcurrentBinaryMask(64, random.nextLong(), "t3LandWreck");
		t2NavyWreckMask = new ConcurrentBinaryMask(64, random.nextLong(), "t2NavyWreck");
		navyFactoryWreckMask = new ConcurrentBinaryMask(64, random.nextLong(), "navyFactoryWreck");
		allWreckMask = new ConcurrentBinaryMask(513, random.nextLong(),"allWreck");
		ConcurrentBinaryMask landCopy = new ConcurrentBinaryMask(land, random.nextLong(), "landCopy");

		t1LandWreckMask.randomize(reclaimDensity *.015f).intersect(land).deflate(1).trimEdge(20);
		t2LandWreckMask.randomize(reclaimDensity *0.01f).intersect(land).minus(t1LandWreckMask).trimEdge(20);
		t3LandWreckMask.randomize(reclaimDensity *0.002f).intersect(land).minus(t1LandWreckMask).minus(t2LandWreckMask).trimEdge(64);
		t2NavyWreckMask.randomize(reclaimDensity *0.015f).intersect(landCopy.outline()).trimEdge(20);
		navyFactoryWreckMask.randomize(reclaimDensity *0.0075f).minus(land).deflate(3).trimEdge(20);
		allWreckMask.combine(t1LandWreckMask).combine(t2LandWreckMask).combine(t3LandWreckMask).combine(t2NavyWreckMask).inflate(2);
	}

	private void setupPropPipeline(){
		treeMask = new ConcurrentBinaryMask(32, random.nextLong(),"tree");
		cliffRockMask = new ConcurrentBinaryMask(32, random.nextLong(), "cliffRock");
		fieldStoneMask = new ConcurrentBinaryMask(128, random.nextLong(), "fieldStone");
		rockFieldMask = new ConcurrentBinaryMask(128, random.nextLong(), "rockField");

		cliffRockMask.randomize(.15f).intersect(rock).minus(plateaus).minus(mountains).intersect(land);
		fieldStoneMask.randomize(reclaimDensity *.005f).enlarge(256).intersect(grass);
		fieldStoneMask.enlarge(513).trimEdge(10);
		treeMask.randomize(0.1f).inflate(1).cutCorners().acid(0.5f).enlarge(128).smooth(4).acid(0.5f);
		treeMask.enlarge(256).intersect(grass);
		treeMask.enlarge(513).deflate(5).trimEdge(3).fillCircle(256,256,96,false);
		rockFieldMask.randomize(reclaimDensity *.005f).trimEdge(48).inflate(3).acid(.5f).intersect(land).minus(mountains);
	}

	private void generateExclusionMasks(){
		noProps = new BinaryMask(256, random.nextLong());
		noProps.combine(rock.getBinaryMask()).combine(ramps.getBinaryMask());
		for (int i = 0; i < map.getSpawns().length; i++) {
			noProps.fillCircle(map.getSpawns()[i].x, map.getSpawns()[i].z, 30, true);
		}
		for (int i = 0; i < map.getMexes().length; i++) {
			noProps.fillCircle(map.getMexes()[i].x, map.getMexes()[i].z, 5, true);
		}
		for (int i = 0; i < map.getHydros().length; i++) {
			noProps.fillCircle(map.getHydros()[i].x, map.getHydros()[i].z, 7, true);
		}
		noProps.combine(allWreckMask.getBinaryMask());

		noWrecks = new BinaryMask(256, random.nextLong());
		noWrecks.combine(rock.getBinaryMask()).combine(ramps.getBinaryMask());
		for (int i = 0; i < map.getSpawns().length; i++) {
			noWrecks.fillCircle(map.getSpawns()[i].x, map.getSpawns()[i].z, 96, true);
		}
		for (int i = 0; i < map.getMexes().length; i++) {
			noWrecks.fillCircle(map.getMexes()[i].x, map.getMexes()[i].z, 10, true);
		}
		for (int i = 0; i < map.getHydros().length; i++) {
			noWrecks.fillCircle(map.getHydros()[i].x, map.getHydros()[i].z, 15, true);
		}
	}

	private static void interpretArguments(String[] args) {
		if (args.length == 0 || args[0].startsWith("--")) {
			interpretArguments(ArgumentParser.parse(args));
		}
		else if (args.length == 2){
			folderPath = args[0];
			mapName = args[1];
			parseMapName();
		}
		else {
			try {
				folderPath = args[0];
				try {
					seed = Long.parseLong(args[1]);
					random = new Random(seed);
				} catch (NumberFormatException nfe) {
					System.out.println("Seed not numeric using default seed or mapname");
				}
				if (!VERSION.equals(args[2])) {
					System.out.println("This generator only supports version " + VERSION);
					System.exit(-1);
				}
				if (args.length>=4) {
					mapName = args[3];
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
					"--ramp-density arg                   optional, set the ramp density for the generated map (max .2)\n +" +
					"--reclaim-density arg                optional, set the reclaim density for the generated map\n +" +
					"--mex-count arg                      optional, set the mex count per player for the generated map\n +");
			System.exit(0);
		}

		if (!Stream.of("folder-path").allMatch(arguments::containsKey) && !DEBUG) {
			System.out.println("Missing necessary argument.");
			System.exit(-1);
		}

		folderPath = arguments.get("folder-path");

		if (arguments.containsKey("map-name")) {
			mapName = arguments.get("map-name");
			parseMapName();
			return;
		}

		if (arguments.containsKey("seed")) {
			seed = Long.parseLong(arguments.get("seed"));
			random = new Random(seed);
		}
		randomizeOptions();

		if (arguments.containsKey("spawn-count")) {
			spawnCount = Integer.parseInt(arguments.get("spawn-count"));
		}

		if (arguments.containsKey("land-density")) {
			landDensity = Float.parseFloat(arguments.get("land-density"));
			landDensity = (float) StrictMath.round(landDensity *127)/127;
		}

		if (arguments.containsKey("plateau-density")) {
			plateauDensity = StrictMath.min(Float.parseFloat(arguments.get("plateau-density")),0.2f);
			plateauDensity = (float) StrictMath.round(plateauDensity *127)/127;
		}

		if (arguments.containsKey("mountain-density")) {
			mountainDensity = StrictMath.min(Float.parseFloat(arguments.get("mountain-density")),0.1f);
			mountainDensity = (float) StrictMath.round(mountainDensity *127)/127;
		}

		if (arguments.containsKey("ramp-density")) {
			rampDensity = StrictMath.min(Float.parseFloat(arguments.get("ramp-density")),0.2f);
			rampDensity = (float) StrictMath.round(rampDensity *127)/127;
		}

		if (arguments.containsKey("reclaim-density")) {
			reclaimDensity = Float.parseFloat(arguments.get("reclaim-density"));
			reclaimDensity = (float) StrictMath.round(reclaimDensity *127)/127;
		}

		if (arguments.containsKey("mex-count")) {
			mexCount = Integer.parseInt(arguments.get("mex-count"));
		}

		generateMapName();
	}

	private static void parseMapName() {
		mapName = mapName.replace('^','/');
		if (!mapName.startsWith("neroxis_map_generator")){
			throw new IllegalArgumentException("Map name is not a generated map");
		}
		String[] args = mapName.split("_");
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
				seed = Long.parseLong(seedString);
			} catch (NumberFormatException nfe) {
				byte[] seedBytes = Base64.getDecoder().decode(seedString);
				ByteBuffer seedWrapper = ByteBuffer.wrap(seedBytes);
				seed = seedWrapper.getLong();
			}
			random = new Random(seed);
			randomizeOptions();
		}
		if (args.length>=6) {
			String optionString = args[5];
			byte[] optionBytes = Base64.getDecoder().decode(optionString);
			parseOptions(optionBytes);
		}
	}

	private static void randomizeOptions(){
		spawnCount = 6;
		landDensity = StrictMath.min((random.nextInt(127)+13.0f)/127,1);
		plateauDensity = (float) random.nextInt(127)/127*0.2f;
		mountainDensity = (float) random.nextInt(127)/127*0.075f;
		rampDensity = (float) random.nextInt(127)/127*0.2f;
		reclaimDensity = (float) random.nextInt(127)/127;
		mexCount = 8 + 4/(spawnCount /2) + random.nextInt(40/ spawnCount);
	}

	private static void parseOptions(byte[] optionBytes){
		if (optionBytes.length>0){
			if (optionBytes[0]<=16){
				spawnCount = optionBytes[0];
			}
		}
		if (optionBytes.length>1){
			landDensity = (float) optionBytes[1]/127+13.0f/127;
		}
		if (optionBytes.length>2){
			plateauDensity = (float) optionBytes[2]/127*0.2f;
		}
		if (optionBytes.length>3){
			mountainDensity = (float) optionBytes[3]/127*0.075f;
		}
		if (optionBytes.length>4){
			rampDensity = (float) optionBytes[4]/127*0.2f;
		}
		if (optionBytes.length>5){
			reclaimDensity = (float) optionBytes[5]/127;
		}
		if (optionBytes.length>6){
			mexCount = optionBytes[6];
		}
	}

	private static void generateMapName(){
		String mapNameFormat = "neroxis_map_generator_%s_%s_%s";
		ByteBuffer seedBuffer = ByteBuffer.allocate(8);
		seedBuffer.putLong(seed);
		String seedString = Base64.getEncoder().encodeToString(seedBuffer.array());
		byte[] optionArray = {(byte) spawnCount,
				(byte) (landDensity *127),
				(byte) (plateauDensity /0.2f*127),
				(byte) (mountainDensity /0.1f*127),
				(byte) (rampDensity /0.2f*127),
				(byte) (reclaimDensity *127),
				(byte) (mexCount)};
		String optionString = Base64.getEncoder().encodeToString(optionArray);
		mapName = String.format( mapNameFormat, VERSION, seedString, optionString);
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

	private String hashFiles(Path... files) throws NoSuchAlgorithmException {
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
		for (byte datum : data) {
			sb.append(String.format("%02x", datum));
		}
		return sb.toString();
	}
}
