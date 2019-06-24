package util;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public strictfp class Gradient {
    public Map<Float, Color> colors = new HashMap<>();

    public void addColor(float period, Color color){
        colors.put(period, color);
        colors
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue(), (e1, e2) -> e2));
    }

    public Color evaluate(float period){
        if (period > 1f || period < 0f){
            throw new RuntimeException("Period must be comprised between 0 and 1");
        }

        float previousKey = 0f;
        float nextKey = 1f;
        for (float key : colors.keySet()){
            if (key > period){
                nextKey = key;
                break;
            }
            previousKey = key;
        }

        float step = (period - previousKey)/(nextKey - previousKey);
        Color colorA = colors.get(previousKey);
        Color colorB = colors.get(nextKey);

        return lerp(colorA, colorB, step);

    }

    private float lerp(float a, float b, float step){
        return a + step * (b - a);
    }

    private Color lerp(Color a, Color b, float step){
        return new Color(
                lerp(a.getRed(), b.getRed(), step),
                lerp(a.getGreen(), b.getGreen(), step),
                lerp(a.getBlue(), b.getBlue(), step)
        );
    }
}
