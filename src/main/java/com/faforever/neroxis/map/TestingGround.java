package com.faforever.neroxis.map;

import com.faforever.neroxis.util.Util;

public strictfp class TestingGround {

    public static void main(String[] args) throws Exception {
        Util.DEBUG = true;
        Util.VISUALIZE = true;

        Boolean[][] test1 = new Boolean[2048][2048];
        boolean[][] test2 = new boolean[2048][2048];

        for (int x = 0; x < 2048; ++x) {
            for (int y = 0; y < 2048; ++y) {
                test1[x][y] = false;
                test2[x][y] = false;
            }
        }
    }
}
