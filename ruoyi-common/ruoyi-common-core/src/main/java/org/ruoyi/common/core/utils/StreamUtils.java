package org.ruoyi.common.core.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * stream 流工具类
 *
 * @author Lion Li
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StreamUtils {

    /**
     * 将collection过滤
     *
     * @param collection 需要转化的集合
     * @param function   过滤方法
     * @return 过滤后的list
     */
    public static <E> List<E> filter(Collection<E> collection, Predicate<E> function) {
        if (CollUtil.isEmpty(collection)) {
            return CollUtil.newArrayList();
        }
        return collection.stream()
            .filter(function)
            // 注意此处不要使用 .toList() 新语法 因为返回的是不可变List 会导致序列化问题
            .collect(Collectors.toList());
    }

    /**
     * 找到流中满足条件的第一个元素
     *
     * @param collection 需要查询的集合
     * @param function   过滤方法
     * @return 找到符合条件的第一个元素，没有则返回 Optional.empty()
     */
    public static <E> Optional<E> findFirst(Collection<E> collection, Predicate<E> function) {
        if (CollUtil.isEmpty(collection)) {
            return Optional.empty();
        }
        return collection.stream()
            .filter(function)
            .findFirst();
    }

    /**
     * 找到流中满足条件的第一个元素值
     *
     * @param collection 需要查询的集合
     * @param function   过滤方法
     * @return 找到符合条件的第一个元素，没有则返回 null
     */
    public static <E> E findFirstValue(Collection<E> collection, Predicate<E> function) {
        return findFirst(collection,function).orElse(null);
    }

    /**
     * 找到流中任意一个满足条件的元素
     *
     * @param collection 需要查询的集合
     * @param function   过滤方法
     * @return 找到符合条件的任意一个元素，没有则返回 Optional.empty()
     */
    public static <E> Optional<E> findAny(Collection<E> collection, Predicate<E> function) {
        if (CollUtil.isEmpty(collection)) {
            return Optional.empty();
        }
        return collection.stream()
            .filter(function)
            .findAny();
    }

    /**
     * 找到流中任意一个满足条件的元素值
     *
     * @param collection 需要查询的集合
     * @param function   过滤方法
     * @return 找到符合条件的任意一个元素，没有则返回null
     */
    public static <E> E findAnyValue(Collection<E> collection, Predicate<E> function) {
        return findAny(collection,function).orElse(null);
    }

    /**
     * 将collection拼接
     *
     * @param collection 需要转化的集合
     * @param function   拼接方法
     * @return 拼接后的list
     */
    public static <E> String join(Collection<E> collection, Function<E, String> function) {
        return join(collection, function, StringUtils.SEPARATOR);
    }

    /**
     * 将collection拼接
     *
     * @param collection 需要转化的集合
     * @param function   拼接方法
     * @param delimiter  拼接符
     * @return 拼接后的list
     */
    public static <E> String join(Collection<E> collection, Function<E, String> function, CharSequence delimiter) {
        if (CollUtil.isEmpty(collection)) {
            return StringUtils.EMPTY;
        }
        return collection.stream()
            .map(function)
            .filter(Objects::nonNull)
            .collect(Collectors.joining(delimiter));
    }

    /**
     * 将collection排序
     *
     * @param collection 需要转化的集合
     * @param comparing  排序方法
     * @return 排序后的list
     */
    public static <E> List<E> sorted(Collection<E> collection, Comparator<E> comparing) {
        if (CollUtil.isEmpty(collection)) {
            return CollUtil.newArrayList();
        }
        return collection.stream()
            .filter(Objects::nonNull)
            .sorted(comparing)
            // 注意此处不要使用 .toList() 新语法 因为返回的是不可变List 会导致序列化问题
            .collect(Collectors.toList());
    }

    /**
     * 将collection转化为类型不变的map<br>
     * <B>{@code Collection<V>  ---->  Map<K,V>}</B>
     *
     * @param collection 需要转化的集合
     * @param key        V类型转化为K类型的lambda方法
     * @param <V>        collection中的泛型
     * @param <K>        map中的key类型
     * @return 转化后的map
     */
    public static <V, K> Map<K, V> toIdentityMap(Collection<V> collection, Function<V, K> key) {
        if (CollUtil.isEmpty(collection)) {
            return MapUtil.newHashMap();
        }
        return collection.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(key, Function.identity(), (l, r) -> l));
    }

    /**
     * 将Collection转化为map(value类型与collection的泛型不同)<br>
     * <B>{@code Collection<E> -----> Map<K,V>  }</B>
     *
     * @param collection 需要转化的集合
     * @param key        E类型转化为K类型的lambda方法
     * @param value      E类型转化为V类型的lambda方法
     * @param <E>        collection中的泛型
     * @param <K>        map中的key类型
     * @param <V>        map中的value类型
     * @return 转化后的map
     */
    public static <E, K, V> Map<K, V> toMap(Collection<E> collection, Function<E, K> key, Function<E, V> value) {
        if (CollUtil.isEmpty(collection)) {
            return MapUtil.newHashMap();
        }
        return collection.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(key, value, (l, r) -> l));
    }

    /**
     * 获取 map 中的数据作为新 Map 的 value ，key 不变
     * @param map 需要处理的map
     * @param take 取值函数
     * @param <K> map中的key类型
     * @param <E> map中的value类型
     * @param <V> 新map中的value类型
     * @return 新的map
     */
    public static <K, E, V> Map<K, V> toMap(Map<K, E> map, BiFunction<K, E, V> take) {
        if (CollUtil.isEmpty(map)) {
            return MapUtil.newHashMap();
        }
        return toMap(map.entrySet(), Map.Entry::getKey, entry -> take.apply(entry.getKey(), entry.getValue()));
    }

    /**
     * 将collection按照规则(比如有相同的班级id)分类成map<br>
     * <B>{@code Collection<E> -------> Map<K,List<E>> } </B>
     *
     * @param collection 需要分类的集合
     * @param key        分类的规则
     * @param <E>        collection中的泛型
     * @param <K>        map中的key类型
     * @return 分类后的map
     */
    public static <E, K> Map<K, List<E>> groupByKey(Collection<E> collection, Function<E, K> key) {
        if (CollUtil.isEmpty(collection)) {
            return MapUtil.newHashMap();
        }
        return collection.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(key, LinkedHashMap::new, Collectors.toList()));
    }

    /**
     * 将collection按照两个规则(比如有相同的年级id,班级id)分类成双层map<br>
     * <B>{@code Collection<E>  --->  Map<T,Map<U,List<E>>> } </B>
     *
     * @param collection 需要分类的集合
     * @param key1       第一个分类的规则
     * @param key2       第二个分类的规则
     * @param <E>        集合元素类型
     * @param <K>        第一个map中的key类型
     * @param <U>        第二个map中的key类型
     * @return 分类后的map
     */
    public static <E, K, U> Map<K, Map<U, List<E>>> groupBy2Key(Collection<E> collection, Function<E, K> key1, Function<E, U> key2) {
        if (CollUtil.isEmpty(collection)) {
            return MapUtil.newHashMap();
        }
        return collection.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(key1, LinkedHashMap::new, Collectors.groupingBy(key2, LinkedHashMap::new, Collectors.toList())));
    }

    /**
     * 将collection按照两个规则(比如有相同的年级id,班级id)分类成双层map<br>
     * <B>{@code Collection<E>  --->  Map<T,Map<U,E>> } </B>
     *
     * @param collection 需要分类的集合
     * @param key1       第一个分类的规则
     * @param key2       第二个分类的规则
     * @param <T>        第一个map中的key类型
     * @param <U>        第二个map中的key类型
     * @param <E>        collection中的泛型
     * @return 分类后的map
     */
    public static <E, T, U> Map<T, Map<U, E>> group2Map(Collection<E> collection, Function<E, T> key1, Function<E, U> key2) {
        if (CollUtil.isEmpty(collection)) {
            return MapUtil.newHashMap();
        }
        return collection.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(key1, LinkedHashMap::new, Collectors.toMap(key2, Function.identity(), (l, r) -> l)));
    }

    /**
     * 将collection转化为List集合，但是两者的泛型不同<br>
     * <B>{@code Collection<E>  ------>  List<T> } </B>
     *
     * @param collection 需要转化的集合
     * @param function   collection中的泛型转化为list泛型的lambda表达式
     * @param <E>        collection中的泛型
     * @param <T>        List中的泛型
     * @return 转化后的list
     */
    public static <E, T> List<T> toList(Collection<E> collection, Function<E, T> function) {
        if (CollUtil.isEmpty(collection)) {
            return CollUtil.newArrayList();
        }
        return collection.stream()
            .map(function)
            .filter(Objects::nonNull)
            // 注意此处不要使用 .toList() 新语法 因为返回的是不可变List 会导致序列化问题
            .collect(Collectors.toList());
    }

    /**
     * 将collection转化为Set集合，但是两者的泛型不同<br>
     * <B>{@code Collection<E>  ------>  Set<T> } </B>
     *
     * @param collection 需要转化的集合
     * @param function   collection中的泛型转化为set泛型的lambda表达式
     * @param <E>        collection中的泛型
     * @param <T>        Set中的泛型
     * @return 转化后的Set
     */
    public static <E, T> Set<T> toSet(Collection<E> collection, Function<E, T> function) {
        if (CollUtil.isEmpty(collection)) {
            return CollUtil.newHashSet();
        }
        return collection.stream()
            .map(function)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }


    /**
     * 合并两个相同key类型的map
     *
     * @param map1  第一个需要合并的 map
     * @param map2  第二个需要合并的 map
     * @param merge 合并的lambda，将key  value1 value2合并成最终的类型,注意value可能为空的情况
     * @param <K>   map中的key类型
     * @param <X>   第一个 map的value类型
     * @param <Y>   第二个 map的value类型
     * @param <V>   最终map的value类型
     * @return 合并后的map
     */
    public static <K, X, Y, V> Map<K, V> merge(Map<K, X> map1, Map<K, Y> map2, BiFunction<X, Y, V> merge) {
        if (CollUtil.isEmpty(map1) && CollUtil.isEmpty(map2)) {
            // 如果两个 map 都为空，则直接返回空的 map
            return MapUtil.newHashMap();
        } else if (CollUtil.isEmpty(map1)) {
            // 如果 map1 为空，则直接处理返回 map2
            return toMap(map2.entrySet(), Map.Entry::getKey, entry -> merge.apply(null, entry.getValue()));
        } else if (CollUtil.isEmpty(map2)) {
            // 如果 map2 为空，则直接处理返回 map1
            return toMap(map1.entrySet(), Map.Entry::getKey, entry -> merge.apply(entry.getValue(), null));
        }
        Set<K> keySet = new HashSet<>();
        keySet.addAll(map1.keySet());
        keySet.addAll(map2.keySet());
        return toMap(keySet, key -> key, key -> merge.apply(map1.get(key), map2.get(key)));
    }

}
