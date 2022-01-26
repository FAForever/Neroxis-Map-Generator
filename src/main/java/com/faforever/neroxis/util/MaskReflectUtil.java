package com.faforever.neroxis.util;

import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.mask.IntegerMask;
import com.faforever.neroxis.mask.Mask;
import com.faforever.neroxis.mask.NormalMask;
import com.faforever.neroxis.mask.Vector2Mask;
import com.faforever.neroxis.mask.Vector3Mask;
import com.faforever.neroxis.mask.Vector4Mask;
import com.faforever.neroxis.ui.GraphMethod;

import java.lang.reflect.Constructor;
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
import java.util.Set;
import java.util.stream.Collectors;

public class MaskReflectUtil {
    private static final Map<Class<? extends Mask<?, ?>>, List<Constructor<?>>> MASK_CONSTRUCTOR_MAP = new HashMap<>();
    private static final Map<Class<? extends Mask<?, ?>>, List<Method>> MASK_METHOD_MAP = new HashMap<>();
    private static final Map<Class<? extends Mask<?, ?>>, Map<String, Class<?>>> MASK_GENERIC_MAP = new HashMap<>();

    static {
        getAllMaskClasses().forEach(clazz -> {
            if (Mask.class.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers())) {
                List<Method> maskMethods = Arrays.stream(clazz.getMethods())
                        .filter(method -> method.isAnnotationPresent(GraphMethod.class))
                        .filter(method -> !method.isBridge())
                        .sorted(Comparator.comparing(Method::getName))
                        .collect(Collectors.toList());
                MASK_METHOD_MAP.put(clazz, Collections.unmodifiableList(maskMethods));

                List<Constructor<?>> maskConstructors = Arrays.stream(clazz.getConstructors())
                        .filter(constructor -> constructor.isAnnotationPresent(GraphMethod.class))
                        .sorted(Comparator.comparing(Constructor::getName))
                        .collect(Collectors.toList());
                MASK_CONSTRUCTOR_MAP.put(clazz, Collections.unmodifiableList(maskConstructors));

                Map<String, Class<?>> genericMap = new HashMap<>();

                ParameterizedType parameterizedType = (ParameterizedType) clazz.getGenericSuperclass();
                TypeVariable<?>[] typeVariables = ((Class<?>) parameterizedType.getRawType()).getTypeParameters();
                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                for (int i = 0; i < actualTypeArguments.length; ++i) {
                    genericMap.put(typeVariables[i].getName(), (Class<?>) actualTypeArguments[i]);
                }

                MASK_GENERIC_MAP.put(clazz, Collections.unmodifiableMap(genericMap));
            }
        });
    }

    public static List<Method> getMaskMethods(Class<? extends Mask<?, ?>> maskClass) {
        return MASK_METHOD_MAP.get(maskClass);
    }

    public static List<Constructor<?>> getMaskConstructors(Class<? extends Mask<?, ?>> maskClass) {
        return MASK_CONSTRUCTOR_MAP.get(maskClass);
    }

    public static List<Class<? extends Mask<?, ?>>> getMaskClasses() {
        return new ArrayList<>(MASK_METHOD_MAP.keySet());
    }

    private static Set<Class<? extends Mask<?, ?>>> getAllMaskClasses() {
        return Set.of(BooleanMask.class, IntegerMask.class, FloatMask.class, Vector2Mask.class, Vector3Mask.class, Vector4Mask.class, NormalMask.class);
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

    public static Class<?> getActualTypeClass(Class<? extends Mask<?, ?>> maskClass, Type type) {
        if (type instanceof TypeVariable) {
            return MASK_GENERIC_MAP.get(maskClass).get(((TypeVariable<?>) type).getName());
        }

        return (Class<?>) type;
    }
}
