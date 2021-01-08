package neroxis.util;

import java.util.Comparator;

public class MaskNameComparator implements Comparator<String> {

    public int compare(String maskName1, String maskName2) {
        int val = maskName1.compareTo(maskName2);
        if (val == 0) {
            val = maskName1.split(" ")[2].compareTo(maskName2.split(" ")[2]);
        }
        return val;
    }
}
