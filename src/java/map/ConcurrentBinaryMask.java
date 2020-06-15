package map;

import util.Pipeline;

import java.nio.file.Path;
import java.util.Arrays;

import generator.VisualDebugger;

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

		if(name.equals("mocked") || VisualDebugger.ENABLED) {
			this.binaryMask = new BinaryMask(mask.getBinaryMask(), seed);
		} else {
			Pipeline.add(this, Arrays.asList(mask), res -> {
				this.binaryMask = new BinaryMask(((ConcurrentBinaryMask)res.get(0)).getBinaryMask(), seed);
				return Arrays.asList(this.binaryMask);
			});
		}
	}

	public ConcurrentBinaryMask randomize(float density) {
		if (VisualDebugger.ENABLED) {
			binaryMask.randomize(density);
			return this;
		}
		return Pipeline.add(this, Arrays.asList(this), res ->
				this.binaryMask.randomize(density)
		);
	}

	public ConcurrentBinaryMask invert() {
		if (VisualDebugger.ENABLED) {
			binaryMask.invert();
			return this;
		}
		return Pipeline.add(this, Arrays.asList(this), res ->
				this.binaryMask.invert()
		);
	}

	public ConcurrentBinaryMask enlarge(int size) {
		if (VisualDebugger.ENABLED) {
			binaryMask.enlarge(size);
			return this;
		}
		return Pipeline.add(this, Arrays.asList(this), res ->
				this.binaryMask.enlarge(size)
		);
	}

	public ConcurrentBinaryMask shrink(int size) {
		if (VisualDebugger.ENABLED) {
			binaryMask.shrink(size);
			return this;
		}
		return Pipeline.add(this, Arrays.asList(this), res ->
				this.binaryMask.shrink(size)
		);
	}

	public ConcurrentBinaryMask inflate(float radius) {
		if (VisualDebugger.ENABLED) {
			binaryMask.inflate(radius);
			return this;
		}
		return Pipeline.add(this, Arrays.asList(this), res ->
				this.binaryMask.inflate(radius)
		);
	}

	public ConcurrentBinaryMask deflate(float radius) {
		if (VisualDebugger.ENABLED) {
			binaryMask.deflate(radius);
			return this;
		}
		return Pipeline.add(this, Arrays.asList(this), res ->
				this.binaryMask.deflate(radius)
		);
	}

	public ConcurrentBinaryMask cutCorners() {
		if (VisualDebugger.ENABLED) {
			binaryMask.cutCorners();
			return this;
		}
		return Pipeline.add(this, Arrays.asList(this), res ->
				this.binaryMask.cutCorners()
		);
	}

	public ConcurrentBinaryMask acid(float strength) {
		if (VisualDebugger.ENABLED) {
			binaryMask.acid(strength);
			return this;
		}
		return Pipeline.add(this, Arrays.asList(this), res ->
				this.binaryMask.acid(strength)
		);
	}

	public ConcurrentBinaryMask outline() {
		if (VisualDebugger.ENABLED) {
			binaryMask.outline();
			return this;
		}
		return Pipeline.add(this, Arrays.asList(this), res ->
				this.binaryMask.outline()
		);
	}

	public ConcurrentBinaryMask smooth(float radius) {
		if (VisualDebugger.ENABLED) {
			binaryMask.smooth(radius);
			return this;
		}
		return Pipeline.add(this, Arrays.asList(this), res ->
				this.binaryMask.smooth(radius)
		);
	}

	public ConcurrentBinaryMask combine(ConcurrentBinaryMask other) {
		if (VisualDebugger.ENABLED) {
			binaryMask.combine(other.getBinaryMask());
			return this;
		}
		return Pipeline.add(this, Arrays.asList(this, other), res ->
				this.binaryMask.combine(((ConcurrentBinaryMask)res.get(1)).getBinaryMask())
		);
	}

	public ConcurrentBinaryMask intersect(ConcurrentBinaryMask other) {
		if (VisualDebugger.ENABLED) {
			binaryMask.intersect(other.getBinaryMask());
			return this;
		}
		return Pipeline.add(this, Arrays.asList(this, other), res ->
				this.binaryMask.intersect(((ConcurrentBinaryMask)res.get(1)).getBinaryMask())
		);
	}

	public ConcurrentBinaryMask minus(ConcurrentBinaryMask other) {
		if (VisualDebugger.ENABLED) {
			binaryMask.minus(other.getBinaryMask());
			return this;
		}
		return Pipeline.add(this, Arrays.asList(this, other), res ->
				this.binaryMask.minus(((ConcurrentBinaryMask)res.get(1)).getBinaryMask())
		);
	}

	public ConcurrentBinaryMask fillCircle(float x, float y, float radius, boolean value) {
		if (VisualDebugger.ENABLED) {
			binaryMask.fillCircle(x, y, radius, value);
			return this;
		}
		return Pipeline.add(this, Arrays.asList(this), res ->
				this.binaryMask.fillCircle(x, y, radius, value)
		);
	}

	public ConcurrentBinaryMask trimEdge(int rimWidth) {
		if (VisualDebugger.ENABLED) {
			binaryMask.trimEdge(rimWidth);
			return this;
		}
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
