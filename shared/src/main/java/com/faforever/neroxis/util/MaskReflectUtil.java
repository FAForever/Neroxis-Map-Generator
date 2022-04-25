package com.faforever.neroxis.util;

import com.faforever.neroxis.annotations.GraphMethod;
import com.faforever.neroxis.mask.BooleanMask;
import com.faforever.neroxis.mask.FloatMask;
import com.faforever.neroxis.mask.IntegerMask;
import com.faforever.neroxis.mask.MapMaskMethods;
import com.faforever.neroxis.mask.Mask;
import com.faforever.neroxis.mask.NormalMask;
import com.faforever.neroxis.mask.Vector2Mask;
import com.faforever.neroxis.mask.Vector3Mask;
import com.faforever.neroxis.mask.Vector4Mask;
import com.github.therapi.runtimejavadoc.MethodJavadoc;
import com.github.therapi.runtimejavadoc.RuntimeJavadoc;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MaskReflectUtil {

    private static final Map<Class<? extends Mask<?, ?>>, Constructor<? extends Mask<?, ?>>> MASK_CONSTRUCTOR_MAP = new LinkedHashMap<>();
    private static final Map<Class<? extends Mask<?, ?>>, List<Method>> MASK_METHOD_MAP = new LinkedHashMap<>();
    private static final Map<Class<? extends Mask<?, ?>>, Map<String, Class<?>>> MASK_GENERIC_MAP = new LinkedHashMap<>();

    static {
        getAllMaskClasses().forEach(clazz -> {
            if (Mask.class.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers())) {
                List<Method> maskMethods = Arrays.stream(clazz.getMethods()).map(MaskReflectUtil::getBaseMethod)
                                                 .filter(method -> method.isAnnotationPresent(GraphMethod.class))
                                                 .filter(method -> !method.isBridge())
                                                 .sorted(Comparator.comparing(Method::getName))
                                                 .collect(Collectors.toList());
                MASK_METHOD_MAP.put(clazz, maskMethods);

                Arrays.stream(clazz.getConstructors())
                      .filter(constructor -> constructor.isAnnotationPresent(GraphMethod.class))
                      .findFirst()
                      .ifPresent(constructor -> MASK_CONSTRUCTOR_MAP.put(clazz,
                                                                         (Constructor<? extends Mask<?, ?>>) constructor));

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

        Arrays.stream(MapMaskMethods.class.getMethods())
              .filter(method -> method.isAnnotationPresent(GraphMethod.class))
              .sorted(Comparator.comparing(Method::getName))
              .forEach(method -> {
                  Class<? extends Mask<?, ?>> executorClass = (Class<? extends Mask<?, ?>>) method.getReturnType();

                  MASK_METHOD_MAP.get(executorClass).add(method);
              });
    }

    private static Method getBaseMethod(Method method) {
        Class<?> clazz = method.getDeclaringClass();
        while (!clazz.equals(Object.class)) {
            clazz = clazz.getSuperclass();
            if (clazz == null) {
                break;
            }

            try {
                method = clazz.getMethod(method.getName(), method.getParameterTypes());
            } catch (NoSuchMethodException ignored) {
            }
        }

        return method;
    }

    private static boolean isGraphMethod(Method method) {
        Class<?> clazz = method.getDeclaringClass();
        while (!clazz.equals(Object.class)) {
            if (method.isAnnotationPresent(GraphMethod.class)) {
                return true;
            }

            clazz = clazz.getSuperclass();
            if (clazz == null) {
                return false;
            }

            try {
                method = clazz.getMethod(method.getName(), method.getParameterTypes());
            } catch (NoSuchMethodException ignored) {
            }
        }

        return false;
    }

    public static MethodJavadoc getJavadoc(Executable executable) {
        if (executable instanceof Method) {
            return getJavadoc((Method) executable);
        } else {
            return RuntimeJavadoc.getJavadoc((Constructor<?>) executable);
        }
    }

    private static MethodJavadoc getJavadoc(Method method) {
        Class<?> clazz = method.getDeclaringClass();
        while (!clazz.equals(Object.class)) {
            MethodJavadoc methodJavadoc = RuntimeJavadoc.getJavadoc(method);
            if (!methodJavadoc.isEmpty()) {
                return methodJavadoc;
            }

            clazz = clazz.getSuperclass();
            if (clazz == null) {
                return methodJavadoc;
            }

            try {
                method = clazz.getMethod(method.getName(), method.getParameterTypes());
            } catch (NoSuchMethodException ignored) {
            }
        }

        return MethodJavadoc.createEmpty(method);
    }

    public static List<Method> getMaskMethods(Class<? extends Mask<?, ?>> maskClass) {
        return MASK_METHOD_MAP.get(maskClass);
    }

    public static Constructor<? extends Mask<?, ?>> getMaskConstructor(Class<? extends Mask<?, ?>> maskClass) {
        return MASK_CONSTRUCTOR_MAP.get(maskClass);
    }

    public static List<Class<? extends Mask<?, ?>>> getMaskClasses() {
        return new ArrayList<>(MASK_METHOD_MAP.keySet());
    }

    private static List<Class<? extends Mask<?, ?>>> getAllMaskClasses() {
        return List.of(BooleanMask.class, FloatMask.class, NormalMask.class, IntegerMask.class, Vector2Mask.class,
                       Vector3Mask.class, Vector4Mask.class);
    }

    public static Class<?> getActualTypeClass(Class<? extends Mask<?, ?>> maskClass, Type type) {
        if (type instanceof TypeVariable) {
            return MASK_GENERIC_MAP.get(maskClass).get(((TypeVariable<?>) type).getName());
        }

        return (Class<?>) type;
    }

    public static String getExecutableString(Executable executable) {
        String parametersString = Arrays.stream(executable.getParameters())
                                        .limit(4)
                                        .map(Parameter::getName)
                                        .collect(Collectors.joining(", "));
        String parametersEllipsis = executable.getParameters().length > 4 ? "..." : "";
        if (executable instanceof Constructor) {
            return String.format("%s(%s%s)", executable.getDeclaringClass().getSimpleName(), parametersString,
                                 parametersEllipsis);
        } else {
            return String.format("%s(%s%s)", executable.getName(), parametersString, parametersEllipsis);
        }
    }

    public static boolean classIsNumeric(Class<?> clazz) {
        return Number.class.isAssignableFrom(clazz)
               || int.class.equals(clazz)
               || float.class.equals(clazz)
               || double.class.equals(clazz)
               || byte.class.equals(clazz)
               || short.class.equals(clazz);
    }
}
