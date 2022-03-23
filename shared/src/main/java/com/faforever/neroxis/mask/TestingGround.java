package com.faforever.neroxis.mask;

import com.faforever.neroxis.util.DebugUtil;

import java.awt.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public strictfp class TestingGround {

    public static void main(String[] args) throws Exception {
        DebugUtil.DEBUG = true;
        int ITER = 1000;
        int size = 1000;
        AtomicInteger b = new AtomicInteger();

        Consumer<Point> pointConsumer = point -> b.set(point.x + point.y);

        DebugUtil.timedRun("Point", () -> {
            for (int i = 0; i < ITER; ++i) {
                Point point = new Point();
                for (int x = 0; x < size; ++x) {
                    for (int y = 0; y < size; ++y) {
                        point.setLocation(x, y);
                        pointConsumer.accept(point);
                    }
                }
            }
        });

        Consumer<int[]> arrayConsumer = point -> b.set(point[0] + point[1]);

        DebugUtil.timedRun("Array", () -> {
            for (int i = 0; i < ITER; ++i) {
                int[] point = new int[2];
                for (int x = 0; x < size; ++x) {
                    for (int y = 0; y < size; ++y) {
                        point[0] = x;
                        point[1] = y;
                        arrayConsumer.accept(point);
                    }
                }
            }
        });
    }
}
