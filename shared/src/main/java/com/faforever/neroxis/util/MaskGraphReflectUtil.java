package com.faforever.neroxis.util;

import com.faforever.neroxis.annotations.GraphMethod;
import com.faforever.neroxis.annotations.GraphParameter;
import com.faforever.neroxis.mask.*;
import com.github.therapi.runtimejavadoc.MethodJavadoc;
import com.github.therapi.runtimejavadoc.RuntimeJavadoc;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

public class MaskGraphReflectUtil {
    private static final Map<Class<? extends Mask<?, ?>>, Constructor<? extends Mask<?, ?>>> MASK_GRAPH_CONSTRUCTOR_MAP;
    private static final Map<Class<? extends Mask<?, ?>>, List<Method>> MASK_GRAPH_METHOD_MAP;
    private static final Map<Class<? extends Mask<?, ?>>, Map<String, Class<?>>> MASK_GENERIC_MAP;
    private static final Map<Method, Method> OVERRIDDEN_GRAPH_METHOD_MAP;

    static {
        Map<Class<? extends Mask<?, ?>>, Map<String, Class<?>>> genericsMap = new HashMap<>();
        Map<Class<? extends Mask<?, ?>>, List<Method>> maskGraphMethodsMap = new HashMap<>();
        Map<Class<? extends Mask<?, ?>>, Constructor<? extends Mask<?, ?>>> graphConstructorsMap = new HashMap<>();
        Map<Method, Method> overriddenGraphMethodsMap = new HashMap<>();

        MASK_GENERIC_MAP = Collections.unmodifiableMap(genericsMap);
        MASK_GRAPH_METHOD_MAP = Collections.unmodifiableMap(maskGraphMethodsMap);
        MASK_GRAPH_CONSTRUCTOR_MAP = Collections.unmodifiableMap(graphConstructorsMap);
        OVERRIDDEN_GRAPH_METHOD_MAP = Collections.unmodifiableMap(overriddenGraphMethodsMap);

        getConcreteMaskClasses().forEach(clazz -> {
            if (Mask.class.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers())) {
                Map<String, Class<?>> genericMap = new HashMap<>();

                ParameterizedType parameterizedType = (ParameterizedType) clazz.getGenericSuperclass();
                TypeVariable<?>[] typeVariables = ((Class<?>) parameterizedType.getRawType()).getTypeParameters();
                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                for (int i = 0; i < actualTypeArguments.length; ++i) {
                    genericMap.put(typeVariables[i].getName(), (Class<?>) actualTypeArguments[i]);
                }

                genericsMap.put(clazz, Collections.unmodifiableMap(genericMap));

                Set<Method> maskGraphMethods = new HashSet<>();

                for (Method method : clazz.getMethods()) {
                    Method overriddenMethod = findOverriddenMethod(method);
                    Method overridingMethod = findOverridingMethod(clazz, overriddenMethod);

                    if (overriddenMethod.getAnnotation(GraphMethod.class) != null) {
                        maskGraphMethods.add(overridingMethod);

                        if (!overridingMethod.equals(overriddenMethod)) {
                            overriddenGraphMethodsMap.put(overridingMethod, overriddenMethod);
                        }
                    }
                }

                maskGraphMethodsMap.put(clazz, maskGraphMethods.stream()
                                                               .sorted(Comparator.comparing(Method::getName))
                                                               .collect(Collectors.toUnmodifiableList()));

                Arrays.stream(clazz.getConstructors())
                      .filter(constructor -> constructor.isAnnotationPresent(GraphMethod.class))
                      .findFirst()
                      .ifPresent(constructor -> graphConstructorsMap.put(clazz,
                              (Constructor<? extends Mask<?, ?>>) constructor));
            }
        });
    }

    private static Method findOverridingMethod(Class<? extends Mask<?, ?>> clazz, Method method) {
        Class<?>[] parameterTypes = Arrays.stream(method.getGenericParameterTypes())
                                          .map(type -> MaskGraphReflectUtil.getActualTypeClass(clazz, type))
                                          .toArray(Class<?>[]::new);
        try {
            return clazz.getMethod(method.getName(), parameterTypes);
        } catch (NoSuchMethodException e) {
            return method;
        }
    }

    private static Method findOverriddenMethod(Method method) {
        Class<?> superclass = method.getDeclaringClass().getSuperclass();
        Method overriddenMethod = method;
        while (superclass != null && !superclass.equals(Object.class)) {
            try {
                overriddenMethod = superclass.getMethod(method.getName(), method.getParameterTypes());
            } catch (NoSuchMethodException ignored) {
            } finally {
                superclass = superclass.getSuperclass();
            }
        }
        return overriddenMethod;
    }

    public static MethodJavadoc getJavadoc(Executable executable) {
        if (executable instanceof Method) {
            return RuntimeJavadoc.getJavadoc((Method) executable);
        } else {
            return RuntimeJavadoc.getJavadoc((Constructor<?>) executable);
        }
    }

    public static GraphParameter[] getGraphParameterAnnotations(Executable executable) {
        if (executable instanceof Constructor) {
            return executable.getAnnotationsByType(GraphParameter.class);
        }

        return getOverriddenMethod((Method) executable).getAnnotationsByType(GraphParameter.class);
    }

    public static Method getOverriddenMethod(Method method) {
        return OVERRIDDEN_GRAPH_METHOD_MAP.getOrDefault(method, method);
    }

    public static List<Method> getMaskGraphMethods(Class<? extends Mask<?, ?>> maskClass) {
        return MASK_GRAPH_METHOD_MAP.getOrDefault(maskClass, List.of());
    }

    public static Constructor<? extends Mask<?, ?>> getMaskGraphConstructor(Class<? extends Mask<?, ?>> maskClass) {
        return MASK_GRAPH_CONSTRUCTOR_MAP.get(maskClass);
    }

    public static List<Class<? extends Mask<?, ?>>> getConcreteMaskClasses() {
        return List.of(BooleanMask.class, FloatMask.class, NormalMask.class, IntegerMask.class, Vector2Mask.class,
                Vector3Mask.class, Vector4Mask.class);
    }

    public static Class<?> getActualTypeClass(Class<? extends Mask<?, ?>> maskClass, Type type) {
        if (type instanceof TypeVariable) {
            return MASK_GENERIC_MAP.get(maskClass).get(((TypeVariable<?>) type).getName());
        }

        if (type instanceof ParameterizedType) {
            return (Class<?>) ((ParameterizedType) type).getRawType();
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

    public static Class<?> getClassFromString(String className) throws ClassNotFoundException {
        return switch (className) {
            case "int" -> int.class;
            case "boolean" -> boolean.class;
            case "long" -> long.class;
            case "short" -> short.class;
            case "float" -> float.class;
            default -> Class.forName(className);
        };
    }
}
