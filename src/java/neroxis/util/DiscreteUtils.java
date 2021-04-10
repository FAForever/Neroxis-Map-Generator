package neroxis.util;

public strictfp class DiscreteUtils {

    public static float discretePercentage(float percent, int numBins) {
        return binPercentage(percent, numBins) / (float) numBins;
    }

    public static int binPercentage(float percent, int numBins) {
        return StrictMath.max(StrictMath.min(StrictMath.round(percent * numBins), numBins), 0);
    }

    public static float normalizeBin(int bin, int numBins) {
        return (float) bin / numBins;
    }
}
