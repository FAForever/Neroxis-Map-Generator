package generator;

import map.BinaryMask;
import map.SCMap;
import util.Vector2f;
import util.Vector3f;

import java.util.Random;

public strictfp class MarkerGenerator {
	private final SCMap map;
	private final Random random;

	public MarkerGenerator(SCMap map, long seed) {
		this.map = map;
		random = new Random(seed);
	}

	private void placeOnHeightmap(float x, float z, Vector3f v) {
		v.x = x;
		v.z = z;
		v.y = map.getHeightmap().getRaster().getPixel((int) v.x, (int) v.z, new int[]{0})[0] * (SCMap.HEIGHTMAP_SCALE);
	}
	
	private Vector3f placeOnHeightmap(float x, float z) {
		Vector3f v = new Vector3f(x, 0, z);
		v.y = map.getHeightmap().getRaster().getPixel((int) v.x, (int) v.z, new int[]{0})[0] * (SCMap.HEIGHTMAP_SCALE);
		return v;
	}

	private void randomizeVectorPair(Vector3f v1, Vector3f v2) {
		placeOnHeightmap((int)(random.nextFloat() * map.getSize()), (int)(random.nextFloat() * map.getSize()), v1);
		placeOnHeightmap(map.getSize() - v1.x, map.getSize() - v1.z, v2);
	}	
	
	public void generateSpawns(BinaryMask spawnable, float separation) {
		BinaryMask spawnableCopy = new BinaryMask(spawnable, random.nextLong());
		Vector2f location = spawnableCopy.getRandomPosition();
		for (int i = 0; i < map.getSpawns().length; i += 2) {
			spawnableCopy.fillCircle(location.x, location.y, separation, false);
			spawnableCopy.fillCircle(map.getSize() - location.x, map.getSize() - location.y, separation, false);
			map.getSpawns()[i] = placeOnHeightmap(location.x, location.y);
			map.getSpawns()[i + 1] = placeOnHeightmap(map.getSize() - location.x, map.getSize() - location.y);
			location = spawnableCopy.getRandomPosition();
		}
	}

	public void generateMexs(BinaryMask spawnable) {
		int baseMexCount = map.getSpawns().length * 4;

		for (int i = 0; i < map.getSpawns().length; i++) {
			map.getMexs()[i * 4] = new Vector3f(0, 0, 0);
			map.getMexs()[i * 4 + 1] = new Vector3f(0, 0, 0);
			map.getMexs()[i * 4 + 2] = new Vector3f(0, 0, 0);
			map.getMexs()[i * 4 + 3] = new Vector3f(0, 0, 0);
			placeOnHeightmap(map.getSpawns()[i].x + 10, map.getSpawns()[i].z, map.getMexs()[i * 4]);
			placeOnHeightmap(map.getSpawns()[i].x - 10, map.getSpawns()[i].z, map.getMexs()[i * 4 + 1]);
			placeOnHeightmap(map.getSpawns()[i].x, map.getSpawns()[i].z + 10, map.getMexs()[i * 4 + 2]);
			placeOnHeightmap(map.getSpawns()[i].x, map.getSpawns()[i].z - 10, map.getMexs()[i * 4 + 3]);
		}

		for (int i = baseMexCount; i < map.getMexs().length; i+= 2) {
			map.getMexs()[i] = new Vector3f(0, 0, 0);
			map.getMexs()[i + 1] = new Vector3f(0, 0, 0);
			randomizeVectorPair(map.getMexs()[i], map.getMexs()[i + 1]);
		}

		for (int i = baseMexCount; i < map.getMexs().length; i+= 2) {
			while (!isMexValid(i, 16, 16, spawnable)) {
				randomizeVectorPair(map.getMexs()[i], map.getMexs()[i + 1]);
			}
		}
	}

	public void generateHydros(BinaryMask spawnable) {
		int baseHydroCount = map.getSpawns().length;

		for (int i = 0; i < map.getSpawns().length; i++) {
			int dx = map.getSpawns()[i].x < map.getSize() / 2 ? -4 : +4;
			int dz = map.getSpawns()[i].z < map.getSize() / 2 ? -14 : +14;
			map.getHydros()[i] = new Vector3f(0, 0, 0);
			placeOnHeightmap(map.getSpawns()[i].x + dx, map.getSpawns()[i].z + dz, map.getHydros()[i]);
		}

		for (int i = baseHydroCount; i < map.getHydros().length - 1; i += 2) {
			map.getHydros()[i] = new Vector3f(0, 0, 0);
			map.getHydros()[i + 1] = new Vector3f(0, 0, 0);
			randomizeVectorPair(map.getHydros()[i], map.getHydros()[i + 1]);
		}

		for (int i = baseHydroCount; i < map.getHydros().length; i += 2) {
			while (!isHydroValid(i, 64, 16, 32, spawnable)) {
				randomizeVectorPair(map.getHydros()[i], map.getHydros()[i + 1]);
			}
		}
	}

	private boolean isMexValid(int index, float distance, float edgeSpacing, BinaryMask spawnable) {
		boolean valid = true;
		float dx;
		float dz;
		if (map.getMexs()[index].x < edgeSpacing)
			valid = false;
		if (map.getMexs()[index].z < edgeSpacing)
			valid = false;
		if (map.getMexs()[index].x > map.getSize() - edgeSpacing)
			valid = false;
		if (map.getMexs()[index].z > map.getSize() - edgeSpacing)
			valid = false;

		for (int i = 0; i < map.getMexs().length; i++) {
			if (i != index) {
				dx = map.getMexs()[i].x - map.getMexs()[index].x;
				dz = map.getMexs()[i].z - map.getMexs()[index].z;
				if (Math.sqrt(dx * dx + dz * dz) < distance)
					valid = false;
			}
		}
		if (!spawnable.get((int) map.getMexs()[index].x, (int) map.getMexs()[index].z))
			valid = false;
		return valid;
	}

	private boolean isHydroValid(int index, float hydroDistance, float mexDistance, float edgeSpacing, BinaryMask spawnable) {
		boolean valid = true;
		float dx;
		float dz;
		if (map.getHydros()[index].x < edgeSpacing)
			valid = false;
		if (map.getHydros()[index].z < edgeSpacing)
			valid = false;
		if (map.getHydros()[index].x > map.getSize() - edgeSpacing)
			valid = false;
		if (map.getHydros()[index].z > map.getSize() - edgeSpacing)
			valid = false;

		for (int i = 0; i < map.getHydros().length; i++) {
			if (i != index) {
				dx = map.getHydros()[i].x - map.getHydros()[index].x;
				dz = map.getHydros()[i].z - map.getHydros()[index].z;
				if (Math.sqrt(dx * dx + dz * dz) < hydroDistance)
					valid = false;
			}
		}
		for (int i = 0; i < map.getMexs().length; i++) {
			if (i != index) {
				dx = map.getMexs()[i].x - map.getHydros()[index].x;
				dz = map.getMexs()[i].z - map.getHydros()[index].z;
				if (Math.sqrt(dx * dx + dz * dz) < mexDistance)
					valid = false;
			}
		}
		if (!spawnable.get((int) map.getHydros()[index].x, (int) map.getHydros()[index].z))
			valid = false;
		return valid;
	}

}
