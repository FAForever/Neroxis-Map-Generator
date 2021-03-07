package neroxis.util;

public strictfp class ParseUtils {

    public static float discretePercentage(float percent, int numBins) {
        return StrictMath.max(StrictMath.min(StrictMath.round(percent * numBins) / (float) numBins, 1), 0);
    }

    public static int binPercentage(float percent, int numBins) {
        return StrictMath.max(StrictMath.min(StrictMath.round(percent * numBins), numBins), 0);
    }

    public static float normalizeBin(int bin, int numBins) {
        return (float) bin / numBins;
    }
}
