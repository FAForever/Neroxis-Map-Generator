package map;

import util.Pipeline;

import java.util.Arrays;

public class ConcurrentFloatMask implements ConcurrentMask {

	private FloatMask floatMask;
	private String name = "new float mask";

	{
		Pipeline.add(this, Arrays.asList(), res ->
				Arrays.asList()
		);
	}

	public ConcurrentFloatMask(int size, long seed, String name) {
		this.floatMask = new FloatMask(size, seed);
		this.name = name;
	}

	public ConcurrentFloatMask(ConcurrentFloatMask mask, long seed, String name) {
		this.floatMask = new FloatMask(mask.getFloatMask(), seed);
		this.name = name;
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

	public ConcurrentFloatMask max(ConcurrentBinaryMask other) {
		return Pipeline.add(this, Arrays.asList(this, other), res ->
				this.floatMask.max(((ConcurrentFloatMask)res.get(1)).getFloatMask())
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




	public FloatMask getFloatMask() {
		return floatMask;
	}

	@Override
	public ConcurrentFloatMask mockClone() {
		return new ConcurrentFloatMask(this, 0, "mock");
	}

	@Override
	public String getName() {
		return name;
	}
}
