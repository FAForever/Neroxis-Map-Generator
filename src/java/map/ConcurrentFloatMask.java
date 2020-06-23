package map;

import generator.VisualDebugger;
import util.Pipeline;

import java.nio.file.Path;
import java.util.Arrays;

public strictfp class ConcurrentFloatMask implements ConcurrentMask {

	private FloatMask floatMask;
	private String name = "new float mask";

	public ConcurrentFloatMask(int size, long seed, String name) {
		this.floatMask = new FloatMask(size, seed);
		this.name = name;

		Pipeline.add(this, Arrays.asList(), res ->
				Arrays.asList()
		);
	}

	public ConcurrentFloatMask(ConcurrentFloatMask mask, long seed, String name) {
		this.name = name;

		if(name.equals("mocked")) {
			this.floatMask = new FloatMask(mask.getFloatMask(), seed);
		} else {
			Pipeline.add(this, Arrays.asList(mask), res -> {
				this.floatMask = new FloatMask(((ConcurrentFloatMask)res.get(0)).getFloatMask(), seed);
				return Arrays.asList(this.floatMask);
			});
		}
	}

	public ConcurrentFloatMask init(ConcurrentBinaryMask other, float low, float high) {
		return Pipeline.add(this, Arrays.asList(this, other), res -> {
				return this.floatMask.init(((ConcurrentBinaryMask) res.get(1)).getBinaryMask(), low, high);
			}
		);
	}

	public ConcurrentFloatMask add(ConcurrentFloatMask other) {
		return Pipeline.add(this, Arrays.asList(this, other), res ->
				this.floatMask.add(((ConcurrentFloatMask)res.get(1)).getFloatMask())
		);
	}

	public ConcurrentFloatMask maskToMoutains(float firstSlope, float slope, ConcurrentBinaryMask other) {
		return Pipeline.add(this, Arrays.asList(this, other), res ->
				this.floatMask.maskToMoutains(firstSlope, slope, ((ConcurrentBinaryMask)res.get(1)).getBinaryMask())
		);
	}

	public ConcurrentFloatMask maskToHeightmap(float slope, float underWaterSlope, int maxRepeat, ConcurrentBinaryMask other) {
		return Pipeline.add(this, Arrays.asList(this, other), res ->
				this.floatMask.maskToHeightmap(slope, underWaterSlope, maxRepeat, ((ConcurrentBinaryMask)res.get(1)).getBinaryMask())
		);
	}

	public ConcurrentFloatMask smooth(float radius) {
		return Pipeline.add(this, Arrays.asList(this), res ->
				this.floatMask.smooth(radius)
		);
	}

	public ConcurrentFloatMask smooth(float radius, ConcurrentBinaryMask limiter) {
		return Pipeline.add(this, Arrays.asList(this, limiter), res ->
				this.floatMask.smooth(radius, ((ConcurrentBinaryMask) res.get(1)).getBinaryMask())
		);
	}

	@Override
	public void writeToFile(Path path) {
		floatMask.writeToFile(path);
	}

	public FloatMask getFloatMask() {
		return floatMask;
	}

	@Override
	public ConcurrentFloatMask mockClone() {
		return new ConcurrentFloatMask(this, 0, "mocked");
	}

	@Override
	public String getName() {
		return name;
	}
	
	public void startVisualDebugger() {
		VisualDebugger.whitelistMask(this.floatMask);
	}
	
	public void startVisualDebugger(String maskName) {
		VisualDebugger.whitelistMask(this.floatMask, maskName);
	}
}
