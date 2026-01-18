package com.faforever.neroxis.utilities;

import com.faforever.neroxis.util.ImageUtil;

public class TestingGround {
    public static void main(String[] args) throws Exception {
        String path = "images/generatedMapIcon.png";
        Class<?> clazz = ImageUtil.class;
        System.out.println(clazz.getResource("/" + path));
        System.out.println(clazz.getResource(path));
        System.out.println(clazz.getModule().getResourceAsStream(path));
        System.out.println(clazz.getModule().getResourceAsStream("/" + path));
        System.out.println(ClassLoader.getSystemResource("/" + path));
        System.out.println(ClassLoader.getSystemResource(path));
    }
}
