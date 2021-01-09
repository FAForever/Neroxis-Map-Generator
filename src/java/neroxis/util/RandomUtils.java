package neroxis.util;

import java.util.Random;

public class RandomUtils {

    public static boolean andRandomBoolean(Random random, int numToAnd) {
        for (int i = 0; i < numToAnd; i++) {
            if (!random.nextBoolean()) {
                return false;
            }
        }
        return true;
    }

    public static float sumRandomFloat(Random random, int numToSum) {
        float value = 0;
        for (int i = 0; i < numToSum; i++) {
            value += random.nextFloat();
        }
        return value;
    }

    public static float sumRandomInt(Random random, int numToSum) {
        float value = 0;
        for (int i = 0; i < numToSum; i++) {
            value += random.nextFloat();
        }
        return value;
    }

    public static float averageRandomFloat(Random random, int numToAvg) {
        return sumRandomFloat(random, numToAvg) / numToAvg;
    }

    public static float averageRandomInt(Random random, int numToAvg) {
        return sumRandomInt(random, numToAvg) / numToAvg;
    }

}
