package neroxis.util;

import lombok.Value;

import java.util.Random;

@Value
public strictfp class Range {
    float min;
    float max;
    float range;

    public Range(float min, float max) {
        if (max < min) {
            throw new IllegalArgumentException(String.format("Max %f greater than Min %f", max, min));
        }
        this.min = min;
        this.max = max;
        this.range = max - min;
    }

    public static Range of(float min, float max) {
        return new Range(min, max);
    }

    public boolean contains(float value) {
        return value >= min && value <= max;
    }

    public float normalize(float value) {
        return StrictMath.max(StrictMath.min((value - min) / range, 1f), 0f);
    }

    public float getRandomFloat(Random random) {
        return random.nextFloat() * range + min;
    }
}
