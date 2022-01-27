package com.faforever.neroxis.util;

import com.faforever.neroxis.mask.Mask;
import com.faforever.neroxis.ui.GraphMethod;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class MaskReflectUtil {
    private static final Map<Class<? extends Mask<?, ?>>, List<Method>> MASK_METHOD_MAP = new HashMap<>();
    private static final Map<Class<? extends Mask<?, ?>>, Map<String, Class<?>>> MASK_GENERIC_MAP = new HashMap<>();

    static {
        findAllMaskClassesUsingClassLoader().forEach(clazz -> {
            if (Mask.class.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers())) {
                Class<? extends Mask<?, ?>> maskClass = (Class<? extends Mask<?, ?>>) clazz;
                List<Method> maskMethods = Arrays.stream(maskClass.getMethods())
                        .filter(method -> method.isAnnotationPresent(GraphMethod.class))
                        .sorted(Comparator.comparing(Method::getName))
                        .collect(Collectors.toList());
                MASK_METHOD_MAP.put(maskClass, Collections.unmodifiableList(maskMethods));

                Map<String, Class<?>> genericMap = new HashMap<>();

                ParameterizedType parameterizedType = (ParameterizedType) maskClass.getGenericSuperclass();
                TypeVariable<?>[] typeVariables = ((Class<?>) parameterizedType.getRawType()).getTypeParameters();
                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                for (int i = 0; i < actualTypeArguments.length; ++i) {
                    genericMap.put(typeVariables[i].getName(), (Class<?>) actualTypeArguments[i]);
                }

                MASK_GENERIC_MAP.put(maskClass, Collections.unmodifiableMap(genericMap));
            }
        });
    }

    public static List<Method> getMaskMethods(Class<? extends Mask<?, ?>> maskClass) {
        return MASK_METHOD_MAP.get(maskClass);
    }

    public static List<Class<? extends Mask<?, ?>>> getMaskClasses() {
        return new ArrayList<>(MASK_METHOD_MAP.keySet());
    }

    private static Set<Class<?>> findAllMaskClassesUsingClassLoader() {
        InputStream stream = Objects.requireNonNull(ClassLoader.getSystemClassLoader()
                .getResourceAsStream("com.faforever.neroxis.mask".replaceAll("[.]", "/")));
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        return reader.lines()
                .filter(line -> line.endsWith(".class"))
                .map(MaskReflectUtil::getMaskClass)
                .collect(Collectors.toSet());
    }

    private static Class<?> getMaskClass(String className) {
        try {
            return Class.forName("com.faforever.neroxis.mask." + className.substring(0, className.lastIndexOf('.')));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not find class", e);
        }
    }

    public static Class<?> getActualParameterClass(Class<? extends Mask<?, ?>> maskClass, Parameter parameter) {
        if (parameter.getParameterizedType() instanceof TypeVariable) {
            return MASK_GENERIC_MAP.get(maskClass).get(((TypeVariable<?>) parameter.getParameterizedType()).getName());
        }

        return parameter.getType();
    }
}
