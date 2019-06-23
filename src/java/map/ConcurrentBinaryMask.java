package map;

import util.Pipeline;

import java.nio.file.Path;
import java.util.Arrays;

public strictfp class ConcurrentBinaryMask implements ConcurrentMask {

	private BinaryMask binaryMask;
	private String name = "new binary mask";

	public ConcurrentBinaryMask(int size, long seed, String name) {
		this.binaryMask = new BinaryMask(size, seed);
		this.name = name;

		Pipeline.add(this, Arrays.asList(), res ->
				Arrays.asList()
		);
	}

	public ConcurrentBinaryMask(ConcurrentBinaryMask mask, long seed, String name) {
		this.name = name;

		if(name.equals("mocked")) {
			this.binaryMask = new BinaryMask(mask.getBinaryMask(), seed);
		} else {
			Pipeline.add(this, Arrays.asList(mask), res -> {
				this.binaryMask = new BinaryMask(mask.getBinaryMask(), seed);
				return Arrays.asList(this.binaryMask);
			});
		}
	}

	public ConcurrentBinaryMask randomize(float density) {
		return Pipeline.add(this, Arrays.asList(this), res ->
				this.binaryMask.randomize(density)
		);
	}

	public ConcurrentBinaryMask invert() {
		return Pipeline.add(this, Arrays.asList(this), res ->
				this.binaryMask.invert()
		);
	}

	public ConcurrentBinaryMask enlarge(int size) {
		return Pipeline.add(this, Arrays.asList(this), res ->
				this.binaryMask.enlarge(size)
		);
	}

	public ConcurrentBinaryMask shrink(int size) {
		return Pipeline.add(this, Arrays.asList(this), res ->
				this.binaryMask.shrink(size)
		);
	}

	public ConcurrentBinaryMask inflate(float radius) {
		return Pipeline.add(this, Arrays.asList(this), res ->
				this.binaryMask.inflate(radius)
		);
	}

	public ConcurrentBinaryMask deflate(float radius) {
		return Pipeline.add(this, Arrays.asList(this), res ->
				this.binaryMask.deflate(radius)
		);
	}

	public ConcurrentBinaryMask cutCorners() {
		return Pipeline.add(this, Arrays.asList(this), res ->
				this.binaryMask.cutCorners()
		);
	}

	public ConcurrentBinaryMask acid(float strength) {
		return Pipeline.add(this, Arrays.asList(this), res ->
				this.binaryMask.acid(strength)
		);
	}

	public ConcurrentBinaryMask outline() {
		return Pipeline.add(this, Arrays.asList(this), res ->
				this.binaryMask.outline()
		);
	}

	public ConcurrentBinaryMask smooth(float radius) {
		return Pipeline.add(this, Arrays.asList(this), res ->
				this.binaryMask.smooth(radius)
		);
	}

	public ConcurrentBinaryMask combine(ConcurrentBinaryMask other) {
		return Pipeline.add(this, Arrays.asList(this, other), res ->
				this.binaryMask.combine(((ConcurrentBinaryMask)res.get(1)).getBinaryMask())
		);
	}

	public ConcurrentBinaryMask intersect(ConcurrentBinaryMask other) {
		return Pipeline.add(this, Arrays.asList(this, other), res ->
				this.binaryMask.intersect(((ConcurrentBinaryMask)res.get(1)).getBinaryMask())
		);
	}

	public ConcurrentBinaryMask minus(ConcurrentBinaryMask other) {
		return Pipeline.add(this, Arrays.asList(this, other), res ->
				this.binaryMask.minus(((ConcurrentBinaryMask)res.get(1)).getBinaryMask())
		);
	}

	public ConcurrentBinaryMask fillCircle(float x, float y, float radius, boolean value) {
		return Pipeline.add(this, Arrays.asList(this), res ->
				this.binaryMask.fillCircle(x, y, radius, value)
		);
	}

	public ConcurrentBinaryMask trimEdge(int rimWidth) {
		return Pipeline.add(this, Arrays.asList(this), res ->
				this.binaryMask.trimEdge(rimWidth)
		);
	}


	@Override
	public void writeToFile(Path path) {
		binaryMask.writeToFile(path);
	}

	public BinaryMask getBinaryMask() {
		return binaryMask;
	}

	@Override
	public ConcurrentBinaryMask mockClone() {
		return new ConcurrentBinaryMask(this, 0, "mocked");
	}

	@Override
	public String getName() {
		return name;
	}
}
