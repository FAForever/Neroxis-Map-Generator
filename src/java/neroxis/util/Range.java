package neroxis.util;

import lombok.Value;

import java.util.Random;

@Value
public class Range {
    float min;
    float max;

    public static Range of(float min, float max) {
        return new Range(min, max);
    }

    public boolean contains(float value) {
        return value >= min && value <= max;
    }

    public boolean contains(int value) {
        return value >= min && value <= max;
    }

    public float getRandomFloat(Random random) {
        return random.nextFloat() * (max - min) + min;
    }

    public int getRandomInteger(Random random) {
        if (max - min > 0) {
            return (int) (random.nextInt((int) (max - min)) + min);
        } else {
            return 0;
        }
    }
}
