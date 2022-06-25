package com.faforever.neroxis.utilities;

import com.faforever.neroxis.util.DebugUtil;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public strictfp class TestingGround {
    public static void main(String[] args) throws Exception {
        DebugUtil.DEBUG = true;

        String input = "Fill the center of the mask using the team {@link Symmetry Symmetry} of the {@link SymmetrySettings SymmetrySettings}";
        Matcher matcher = Pattern.compile("\\{@link\\s(\\w*)\\s(\\w*)}").matcher(input);
        System.out.println(matcher.find());
        System.out.println(input.replaceAll("\\{@link\\s(\\w*)\\s(\\w*)}", "<i>$1</i>"));
    }
}
