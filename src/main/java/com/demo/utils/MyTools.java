package com.demo.utils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.function.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 个人工具集
 */
@Slf4j
public class MyTools {

    public static class MyMap<K, V> {

        private Map<K, V> map;

        private List<K> keys = new ArrayList<>();

        private List<V> values = new ArrayList<>();

        private MyMap() {
            this.map = new HashMap();
        }

        public static <K, V> MyMap<K, V> builder() {
            return new MyMap<>();
        }

        public MyMap<K, V> of(K k, V v) {
            this.map.put(k, v);
            return this;
        }

        public MyMap<K, V> ofs(Map<K, V> map) {
            if (isNotEmpty(map)) this.map.putAll(map);
            return this;
        }

        @SafeVarargs
        public final MyMap<K, V> keys(K... ks) {
            if (MyArrays.isNotEmpty(ks)) this.keys.addAll(Arrays.asList(ks));
            return this;
        }


        @SafeVarargs
        public final MyMap<K, V> values(V... vs) {
            if (MyArrays.isNotEmpty(vs)) this.values.addAll(Arrays.asList(vs));
            return this;
        }

        public Map<K, V> build() {
            if (null != keys && keys.size() > 0 && null != values && values.size() > 0 && keys.size() == values.size())
                IntStream.range(0, keys.size()).forEach(i -> this.map.put(keys.get(i), values.get(i)));
            return this.map;
        }

        public static <K, V> boolean isNotEmpty(Map<K, V> map) {
            if (null != map && map.size() > 0) return true;
            return false;
        }

        public static <K, V> Map<V, K> valueNotRepeatReversal(Map<K, V> map) {
            if (isEmpty(map)) return new HashMap<>();
            Map<V, List<K>> reversal = reversal(map);
            return reversal.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, vo -> vo.getValue().get(0)));
        }

        public static <K, V> Map<V, List<K>> reversal(Map<K, V> map) {
            if (isEmpty(map)) return new HashMap<>();
            Map<V, List<Map.Entry<K, V>>> collect = map.entrySet().stream().collect(Collectors.groupingBy(Map.Entry::getValue));
            Map<V, List<K>> result = collect.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, vo -> vo.getValue().stream().map(Map.Entry::getKey).collect(Collectors.toList())));
            return result;
        }

        public static <K, V> boolean isEmpty(Map<K, V> map) {
            return !isNotEmpty(map);
        }

        /**
         * 交集
         */
        public static <K, V> Map<K, V> intersection(Map<K, V> src, Map<K, V> dst) {
            if (isEmpty(src) && isEmpty(dst)) return new HashMap<>();
            if (isEmpty(src)) return new HashMap<>();
            if (isEmpty(dst)) return new HashMap<>();
            MyMap<K, V> builder = MyMap.builder();
            Iterator<K> iterator = src.keySet().iterator();
            Set<K> ks = dst.keySet();
            while (iterator.hasNext()) {
                K next = iterator.next();
                if (ks.contains(next)) builder.of(next, src.get(next));
            }
            return builder.build();
        }

        /**
         * 并集
         */
        public static <K, V> Map<K, V> union(Map<K, V> map1, Map<K, V> map2) {
            if (isEmpty(map1)) map1 = new HashMap<>();
            if (isEmpty(map2)) map2 = new HashMap<>();
            if (isEmpty(map1) && isEmpty(map2)) return map1;
            if (null == map1) return map2;
            if (null == map2) return map1;
            map1.putAll(map2);
            return map1;
        }

        /**
         * 差集
         */
        public static <K, V> Map<K, V> complement(Map<K, V> src, Map<K, V> dst) {
            if (isEmpty(src) && isEmpty(dst)) return new HashMap<>();
            if (isEmpty(src)) return dst;
            if (isEmpty(dst)) return src;
            Map<K, V> intersection = intersection(src, dst);
            Map<K, V> union = union(src, dst);
            MyMap<K, V> builder = MyMap.builder();
            Iterator<K> iterator = union.keySet().iterator();
            Set<K> ks = intersection.keySet();
            if (iterator.hasNext()) {
                K next = iterator.next();
                if (!ks.contains(next)) builder.of(next, union.get(next));
            }
            return builder.build();
        }
    }

    public static class MyString {

        private StringBuilder sb = new StringBuilder();

        public MyString() {
        }

        public static MyString builder() {
            return new MyString();
        }

        public MyString weave(Object str) {
            sb.append(str);
            return this;
        }

        public static String weave(String stitch, Supplier<String>... suppliers) {
            MyString builder = MyString.builder();
            if (MyArrays.isNotEmpty(suppliers)) for (Supplier<String> supplier : suppliers) {
                String s = supplier.get();
                if (isBlank(s)) builder.weave(s);
                if (isBlank(stitch)) builder.weave(stitch);
            }
            return builder.build();
        }

        public String build() {
            return sb.toString();
        }

        private static Pattern linePattern = Pattern.compile("_(\\w)");

        public static String underline2Camel(String str) {
            str = str.toLowerCase();
            Matcher matcher = linePattern.matcher(str);
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) matcher.appendReplacement(sb, matcher.group(1).toUpperCase());
            matcher.appendTail(sb);
            return sb.toString();
        }

        private static Pattern firstPattern = Pattern.compile("^(\\w)");

        public static String underline2CamelAndFirstUpper(String str) {
            str = underline2Camel(str);
            Matcher matcher = firstPattern.matcher(str);
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) matcher.appendReplacement(sb, matcher.group(1).toUpperCase());
            matcher.appendTail(sb);
            return sb.toString();
        }

        private static Pattern humpPattern = Pattern.compile("[A-Z]");

        public static String camel2Underline(String str) {
            Matcher matcher = humpPattern.matcher(str);
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) matcher.appendReplacement(sb, "_" + matcher.group(0).toLowerCase());
            matcher.appendTail(sb);
            return sb.toString();
        }

        public static String camel2Underline2(String str) {
            return str.replaceAll("[A-Z]", "_$0").toLowerCase();
        }

        public static boolean equals(String str1, String str2) {
            if (null != str1 && null != str2 && str1.equals(str2)) return true;
            return false;
        }

        public static boolean isNotBlank(String string) {
            if (null != string && !"".equals(string.trim())) return true;
            return false;
        }

        public static boolean isBlank(String string) {
            return !isNotBlank(string);
        }

        public static String replaceAll(String str, String regex, String replacement) {
            if (isNotBlank(str)) {
                str = str.replaceAll(regex, replacement);
                if (str.contains(regex)) replaceAll(str, regex, replacement);
            }
            return str;
        }

        public static Boolean matchAll(String str1, String... str2) {
            if (isNotBlank(str1) && MyArrays.isNotEmpty(str2))
                for (String str : str2) if (!str1.matches(str)) return false;
            return true;
        }

        public static Boolean matchAny(String str1, String... str2) {
            if (isNotBlank(str1) && MyArrays.isNotEmpty(str2))
                for (String str : str2) if (str1.matches(str)) return true;
            return false;
        }

        public static String[] split2Line(String str) {
            if (isNotBlank(str)) return MyArrays.delBlank(str.split("\\r?\\n", -1));
            return new String[]{};
        }


        public static String up(Collection<String> list, String param) {
            return MyCollection.up(list, param, s -> s);
        }

        public static String next(Collection<String> list, String param) {
            return MyCollection.next(list, param, s -> s);
        }

        public static List<Integer> getIndex4Pattern(String str, char pattern) {
            char[] chars = str.toCharArray();
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < chars.length; i++) {
                char aChar = chars[i];
                if (aChar == pattern) indices.add(i);
            }
            return indices;
        }


        public static List<String> splitAndDeleteBlank(String str, String s) {
            List<String> list = new ArrayList<>();
            if (isNotBlank(str)) {
                String[] split = str.split(s);
                if (MyArrays.isNotEmpty(split))
                    for (String s1 : split) if (isNotBlank(s1)) list.add(s1);
            }
            return list;
        }


    }

    public static class MyCollection<T> {

        private Collection<T> collection;

        private MyCollection() {
            collection = new ArrayList<>();
        }

        public static <T> MyCollection<T> builder() {
            return new MyCollection<>();
        }

        public MyCollection<T> of(T... ts) {
            if (null != ts && ts.length > 0) for (T t : ts) collection.add(t);
            return this;
        }

        public MyCollection<T> of(Collection<T> ts) {
            if (null != ts && ts.size() > 0) for (T t : ts) of(t);
            return this;
        }

        public Collection<T> build() {
            return this.collection;
        }

        public List<T> build2List() {
            return toList(this.collection);
        }

        public Set<T> build2Set() {
            return toSet(this.collection);
        }

        public T[] toArray(Class<T> tClass) {
            return MyArrays.toArray(collection, tClass);
        }

        public static <T> T[] toArray(Collection<T> collection, Class<T> tClass) {
            return MyArrays.toArray(collection, tClass);
        }

        public static <T> List<T> toList(T... ts) {
            List<T> tList = MyCollection.<T>builder().of(ts).build2List();
            return tList;
        }

        public static <T> Set<T> toSet(T... ts) {
            Set<T> tSet = MyCollection.<T>builder().of(ts).build2Set();
            return tSet;
        }

        public static <T> List<T> toList(Collection<T> collection) {
            if (isNotEmpty(collection)) return new ArrayList<>(collection);
            return new ArrayList<>();
        }

        public static <T> Set<T> toSet(Collection<T> collection) {
            if (isNotEmpty(collection)) return new HashSet<>(collection);
            return new HashSet<>();
        }

        public static <T> boolean isNotEmpty(Collection<T> collection) {
            if (null != collection && collection.size() > 0) return true;
            return false;
        }

        public static <T> boolean isEmpty(Collection<T> collection) {
            return !isNotEmpty(collection);
        }

        public static <T> Collection<T> merge(Collection<T> list, T... ts) {
            return merge(list, toList(ts));
        }

        public static <T> Collection<T> merge(Collection<T> src, Collection<T> dst) {
            Collection<T> ts = new ArrayList<>();
            if (isNotEmpty(src)) ts.addAll(src);
            if (isNotEmpty(dst)) ts.addAll(dst);
            return ts;
        }

        public static <T> Collection<T> distinctMerge(Collection<T> src, Collection<T> dst) {
            Collection<T> collection = merge(src, dst);
            HashSet<T> ts = new HashSet<>(collection);
            return ts;
        }


        //交集
        public static <T> Collection<T> intersect(Collection<T> src, Collection<T> dst) {
            if (isEmpty(src) && isEmpty(dst)) return new HashSet<>();
            if (isEmpty(src)) return new HashSet<>();
            if (isEmpty(dst)) return new HashSet<>();
            MyCollection<T> builder = MyCollection.builder();
            if (isNotEmpty(src) && isNotEmpty(dst)) {
                Iterator<T> iterator = src.iterator();
                while (iterator.hasNext()) {
                    T next = iterator.next();
                    if (dst.contains(next)) builder.of(next);
                }
            }
            return new HashSet<>(builder.build());
        }


        // 并集
        public static <T> Collection<T> union(Collection<T> src, Collection<T> dst) {
            if (isEmpty(src) && isEmpty(dst)) return new HashSet<>();
            if (isEmpty(src)) return new HashSet(dst);
            if (isEmpty(dst)) return new HashSet(src);
            Collection<T> collection = merge(src, dst);
            if (isNotEmpty(collection)) return new HashSet(collection);
            return collection;
        }

        //差集
        public static <T> Collection<T> except(Collection<T> src, Collection<T> dst) {
            if (isEmpty(src) && isEmpty(dst)) return new HashSet<>();
            if (isEmpty(src)) return new HashSet(dst);
            if (isEmpty(dst)) return new HashSet(src);
            Collection<T> intersection = intersect(src, dst);
            Collection<T> union = union(src, dst);
            MyCollection<T> builder = MyCollection.builder();
            Iterator<T> iterator = union.iterator();
            if (iterator.hasNext()) {
                T next = iterator.next();
                if (!intersection.contains(next)) builder.of(next);
            }
            return builder.build();
        }

        public static <T> Boolean contains(Collection<T> src, T... dst) {
            return contains(src, Arrays.asList(dst));
        }

        public static <T> Boolean contains(Collection<T> src, Collection<T> dst) {
            if (isEmpty(src) || isEmpty(dst)) return false;
            Iterator<T> iterator = dst.iterator();
            int i = 1;
            while (iterator.hasNext()) {
                T next = iterator.next();
                if (src.contains(next)) i++;
            }
            if (i == dst.size()) return true;
            return false;
        }

        public static <T> List<List<T>> slice(Collection<T> collection, Integer pageNum) {
            if (isEmpty(collection)) return new ArrayList<>();
            if (null == pageNum || pageNum <= 0) pageNum = 20;
            Iterator<T> iterator = collection.iterator();
            List<List<T>> listList = new ArrayList<>();
            int init = 1;
            List<T> list = new ArrayList<>();
            while (iterator.hasNext()) {
                if (init > pageNum) {
                    init = 1;
                    listList.add(list);
                    list = new ArrayList<>();
                }
                list.add(iterator.next());
                init++;
            }
            if (!list.isEmpty()) listList.add(list);
            return listList;
        }

        public static <T> T up(Collection<T> list, T t, Function<T, String> fun) {
            T up = null;
            boolean flag = false;
            String str = fun.apply(t);
            if (MyCollection.isNotEmpty(list) && null != t) {
                for (T param : list) {
                    if (MyString.equals(str, fun.apply(param))) {
                        flag = true;
                        break;
                    }
                    up = param;
                }
            }
            return flag ? up : null;
        }

        public static <T> T next(Collection<T> list, T t, Function<T, String> fun) {
            T next = null;
            String str = fun.apply(t);
            if (MyCollection.isNotEmpty(list) && null != t) {
                Iterator<T> iterator = list.iterator();
                while (iterator.hasNext()) {
                    T param = iterator.next();
                    if (MyString.equals(str, fun.apply(param))) {
                        if (iterator.hasNext()) next = iterator.next();
                    }
                }
            }
            return next;
        }

    }

    public static class MyArrays<T> {

        private Collection<T> collection;

        private Class<T> clazz;

        public MyArrays() {
            collection = new ArrayList<>();
        }

        public static <T> MyArrays<T> builder() {
            return new MyArrays<>();
        }

        public MyArrays<T> of(T... ts) {
            if (null != ts && ts.length > 0) {
                if (null == clazz) clazz = (Class<T>) ts[0].getClass();
                Arrays.stream(ts).forEach(t -> collection.add(t));
            }
            return this;
        }

        public MyArrays<T> of(Collection<T> ts) {
            if (null != ts && ts.size() > 0) ts.forEach(this::of);
            return this;
        }

        public T[] build() {
            return toArray(collection, clazz);
        }

        public List<T> build2List() {
            return new ArrayList<>(this.collection);
        }

        public Set<T> build2Set() {
            return new HashSet<>(this.collection);
        }

        public static <T> T[] createFixedLengthArray(Class<T> clazz, int length) {
            return (T[]) java.lang.reflect.Array.newInstance(clazz, length);
        }

        public static <T> T[] getEmptyArray(Class<T> clazz) {
            return createFixedLengthArray(clazz, 0);
        }


        public static <T> boolean isNotEmpty(T... ts) {
            if (null != ts && ts.length > 0) return true;
            return false;
        }

        public static <T> boolean isEmpty(T... ts) {
            return !isNotEmpty(ts);
        }

        public static <T> T[] toArray(Collection<T> collection, Class<T> clazz) {
            if (MyCollection.isEmpty(collection)) return createFixedLengthArray(clazz, 0);
            return toArray(clazz).apply(collection);
        }

        public static String[] toArray(Collection<String> collection) {
            return toArray(collection, String.class);
        }

        public static <T> T[] merge(T[] src, T[] dst, Class<T> clazz) {
            return merge(clazz).apply(src).apply(dst);
        }

        public static String[] merge(String[] src, String[] dst) {
            return merge(String.class).apply(src).apply(dst);
        }

        public static String[] merge(String[] src, String dst) {
            return merge(String.class).apply(src).apply(new String[]{dst});
        }

        public static String[] delBlank(String[] src) {
            List<String> list = new ArrayList<>();
            if (isNotEmpty(src))
                list = Arrays.stream(src).filter(s -> !MyString.equals("", s)).collect(Collectors.toList());
            return toArray(list);
        }

        public static <T> T[] toArray(Collection<T> src, Collection<T> dst, Class<T> clazz) {
            if (MyCollection.isEmpty(src) && MyCollection.isEmpty(dst)) return createFixedLengthArray(clazz, 0);
            return mergeCollect(clazz).apply(src).apply(dst);
        }

        public static <T> Function<Collection<T>, T[]> toArray(Class<T> clazz) {
            return vo -> {
                if (isNotEmpty(vo)) {
                    T[] ts = createFixedLengthArray(clazz, vo.size());
                    Iterator<T> iterator = vo.iterator();
                    int i = 0;
                    while (iterator.hasNext()) {
                        ts[i] = iterator.next();
                        i++;
                    }
                    return ts;
                }
                return createFixedLengthArray(clazz, 0);
            };

        }

        public static <T> Function<T[], Function<T[], T[]>> merge(Class<T> clazz) {
            return src -> dst -> {
                if (null == src || src.length == 0) return dst;
                if (null == dst || dst.length == 0) return src;
                HashSet<T> srcSet = new HashSet(Arrays.asList(src));
                srcSet.addAll(new HashSet(Arrays.asList(dst)));
                T[] arr = (T[]) java.lang.reflect.Array.newInstance(clazz, srcSet.size());
                ArrayList list = new ArrayList<>(srcSet);
                T[] objects = (T[]) list.toArray(arr);
                return objects;
            };
        }

        public static <T> Function<Collection<T>, Function<Collection<T>, T[]>> mergeCollect(Class<T> clazz) {
            return src -> dst -> {
                T[] ts1 = createFixedLengthArray(clazz, 0);
                if (null != src && src.size() > 0) ts1 = toArray(src, clazz);
                T[] ts2 = createFixedLengthArray(clazz, 0);
                if (null != dst && dst.size() > 0) ts2 = toArray(src, clazz);
                return merge(clazz).apply(ts1).apply(ts2);
            };
        }
    }

    public static class MyCSV {

        public static Boolean writeFileAppend(String filePath, List<String[]> contents) {
            boolean isSuccess = false;
            if (MyString.isNotBlank(filePath) && MyCollection.isNotEmpty(contents)) {
                Writer writer = null;
                try {
                    File file = new File(filePath);
                    if (!file.exists()) file.createNewFile();
                    writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true), "UTF-8"));
                    for (String[] content : contents) {
                        StringBuffer sb = new StringBuffer();
                        for (int i = 0; i < content.length; i++) {
                            String data = content[i];
                            sb.append("\"").append(data);
                            if (i < content.length - 1) sb.append("\",");
                        }
                        writer.write(sb.append("\"").append("\r\n").toString());
                    }
                    writer.flush();
                    isSuccess = true;
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (writer != null) writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return isSuccess;
        }
    }


    public static class CycleExecute {

        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class BorderlessExecute<E> {

            private final int gradient = 1;

            private BiFunction<Integer, E, E> actuator;

            private BiPredicate<Integer, E> terminationCondition;

            private int init = 0;

            public List<E> execute() {
                List<E> result = new ArrayList<>();
                if (null != terminationCondition && null != actuator) {
                    E e = null;
                    while (true) {
                        e = actuator.apply(init, e);
                        if (terminationCondition.test(init, e)) break;
                        result.add(e);
                        init += gradient;
                    }
                }
                return result;
            }
        }

        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class CycleExecuteFun<T, E> {

            private List<T> inputSource;

            private final int pageNum = 50;

            private Predicate<T> paramFilter;

            private Function<List<T>, E> actuator;

            private Predicate<E> resultFilter;

            private final Boolean parallel = false;

            public List<E> execute() {
                List<E> result = new ArrayList<>();
                if (MyCollection.isNotEmpty(inputSource) && null != actuator) {
                    List<T> collect = inputSource;
                    if (null != paramFilter)
                        collect = inputSource.stream().filter(paramFilter).collect(Collectors.toList());
                    List<List<T>> group = MyCollection.slice(collect, pageNum);
                    Stream<List<T>> stream;
                    if (parallel) stream = group.parallelStream();
                    else stream = group.stream();
                    result = stream.map(actuator).collect(Collectors.toList());
                } else log.error("集合或者函数为空，请检查！");
                if (null != resultFilter) return result.stream().filter(resultFilter).collect(Collectors.toList());
                return result;
            }
        }

        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class CycleExecuteCon<T> {

            private List<T> inputSource;

            private final int pageNum = 50;

            private Predicate<T> paramFilter;

            private Consumer<List<T>> actuator;

            private final boolean parallel = false;

            public void execute() {
                List<T> collect = inputSource;
                if (MyCollection.isNotEmpty(inputSource) && null != actuator) {
                    if (null != paramFilter)
                        collect = inputSource.stream().filter(paramFilter).collect(Collectors.toList());
                    List<List<T>> group = MyCollection.slice(collect, pageNum);
                    Stream<List<T>> stream;
                    if (parallel) stream = group.parallelStream();
                    else stream = group.stream();
                    stream.forEach(actuator);
                } else log.error("集合或者函数为空，请检查！");
            }
        }
    }

    public static class MyZip {

        private static final int BUFFER_SIZE = 2 * 1024;

        /**
         * 压缩成ZIP 方法1
         *
         * @param srcDir           压缩文件夹路径
         * @param outFilePath      压缩文件输出路径,即zip文件全路径名
         * @param KeepDirStructure 是否保留原来的目录结构,true:保留目录结构;    false:所有文件跑到压缩包根目录下(注意：不保留目录结构可能会出现同名文件,会压缩失败)
         * @throws RuntimeException 压缩失败会抛出运行时异常
         */
        public static Boolean toZip(String srcDir, String outFilePath, boolean KeepDirStructure) {
            long start = System.currentTimeMillis();
            ZipOutputStream zos = null;
            Boolean isFinish = false;
            try {
                FileOutputStream out = new FileOutputStream(new File(outFilePath));
                zos = new ZipOutputStream(out);
                File sourceFile = new File(srcDir);
                compress(sourceFile, zos, sourceFile.getName(), KeepDirStructure);
                long end = System.currentTimeMillis();
                log.info("压缩完成,耗时：" + (end - start) + " ms");
                isFinish = true;
            } catch (Exception e) {
                throw new RuntimeException("zip error from ZipUtils", e);
            } finally {
                if (zos != null) {
                    try {
                        zos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return isFinish;
        }

        /**
         * 压缩成ZIP 方法2
         *
         * @param srcFiles    需要压缩的文件列表
         * @param outFilePath 压缩文件输出流
         * @throws RuntimeException 压缩失败会抛出运行时异常
         */
        public static Boolean toZip(List<File> srcFiles, String outFilePath) throws RuntimeException {
            long start = System.currentTimeMillis();
            ZipOutputStream zos = null;
            Boolean isFinish = false;
            try {
                FileOutputStream out = new FileOutputStream(new File(outFilePath));
                zos = new ZipOutputStream(out);
                for (File srcFile : srcFiles) {
                    byte[] buf = new byte[BUFFER_SIZE];
                    zos.putNextEntry(new ZipEntry(srcFile.getName()));
                    int len;
                    FileInputStream in = new FileInputStream(srcFile);
                    while ((len = in.read(buf)) != -1) {
                        zos.write(buf, 0, len);
                    }
                    zos.closeEntry();
                    in.close();
                }
                long end = System.currentTimeMillis();
                log.info("压缩完成,耗时：" + (end - start) + " ms");
                isFinish = true;
            } catch (Exception e) {
                throw new RuntimeException("zip error from ZipUtils", e);
            } finally {
                if (zos != null) {
                    try {
                        zos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return isFinish;
        }

        /**
         * 递归压缩方法
         *
         * @param sourceFile       源文件
         * @param zos              zip输出流
         * @param name             压缩后的名称
         * @param KeepDirStructure 是否保留原来的目录结构, true:保留目录结构;   false:所有文件跑到压缩包根目录下(注意：不保留目录结构可能会出现同名文件,会压缩失败)
         * @throws Exception
         */

        private static void compress(File sourceFile, ZipOutputStream zos, String name, boolean KeepDirStructure) throws Exception {
            byte[] buf = new byte[BUFFER_SIZE];
            if (sourceFile.isFile()) {
                // 向zip输出流中添加一个zip实体,构造器中name为zip实体的文件的名字
                zos.putNextEntry(new ZipEntry(name));
                // copy文件到zip输出流中
                int len;
                FileInputStream in = new FileInputStream(sourceFile);
                while ((len = in.read(buf)) != -1) {
                    zos.write(buf, 0, len);
                }
                // Complete the entry
                zos.closeEntry();
                in.close();
            } else {
                File[] listFiles = sourceFile.listFiles();
                if (listFiles == null || listFiles.length == 0) {
                    // 需要保留原来的文件结构时,需要对空文件夹进行处理
                    if (KeepDirStructure) {
                        // 空文件夹的处理
                        zos.putNextEntry(new ZipEntry(name + "/"));
                        // 没有文件,不需要文件的copy
                        zos.closeEntry();
                    }
                } else {
                    for (File file : listFiles) {
                        // 判断是否需要保留原来的文件结构
                        if (KeepDirStructure) {
                            // 注意：file.getName()前面需要带上父文件夹的名字加一斜杠,
                            // 不然最后压缩包中就不能保留原来的文件结构,即：所有文件都跑到压缩包根目录下了
                            compress(file, zos, name + "/" + file.getName(), KeepDirStructure);
                        } else {
                            compress(file, zos, file.getName(), KeepDirStructure);
                        }
                    }
                }
            }
        }
    }

    public static class MyNumeric {

        public static Boolean isNumeric(String str) {
            if (MyString.isBlank(str)) return false;
            char[] chars = str.toCharArray();
            int i = 0;
            int j = 0;
            for (char c : chars) {
                if (c != '-' && c != '.' && (c < '0' || c > '9')) return false;
                if (c == '-') i++;
                if (c == '.') j++;
            }
            if (i > 1 || j > 1) return false;
            return true;
        }


        public static Double divideAndKeepTwoDecimalPlaces(Number n1, Number n2) {
            return twoDecimalPlaces(divide(n1, n2));
        }

        public static Double divide(Number n1, Number n2) {
            return new Double(n1.toString()) / new Double(n2.toString());
        }

        public static Double keepDecimalPlaces(Double d, Integer place) {
            double pow = Math.pow(10, place);
            Long l = new Double(pow).longValue();
            return Math.round(d * l) / pow;
        }

        public static Double twoDecimalPlaces(Double d) {
            return keepDecimalPlaces(d, 2);
        }

        public static int compare(Number o1, Number o2) {
            return compareObj(o1.toString(), o2.toString());
        }

        public static int compareDesc(Number o1, Number o2) {
            return -compareObj(o1, o2);
        }

        public static int compareObj(Object o1, Object o2) {
            return Double.compare(obj2Double(o1), obj2Double(o2));
        }

        public static int compareObjDesc(Object o1, Object o2) {
            return -compareObj(o1, o2);
        }

        public static Double plus(Number o1, Number o2) {
            return plusObj(o1.toString(), o2.toString());
        }

        public static Double plusObj(Object o1, Object o2) {
            return obj2Double(o1) + obj2Double(o2);
        }

        private static Double obj2Double(Object o) {
            Double d1 = 0.0;
            try {
                if (null != o) {
                    if (o instanceof Number)
                        d1 = new Double(o.toString());
                    else if (o instanceof String)
                        if (isNumeric(String.valueOf(o))) d1 = Double.parseDouble(String.valueOf(o));
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            return d1;
        }
    }

    public static class MyStream<T> {

        Stream<T> stream;

        public static <T> MyStream<T> builder() {
            return new MyStream();
        }

        public MyStream<T> of(Stream<T> stream) {
            if (null != stream) {
                if (null == this.stream) this.stream = stream;
                else this.stream = Stream.concat(this.stream, stream);
            }
            return this;
        }

        public MyStream<T> of(Stream<T>... stream) {
            if (null != stream && stream.length > 0) for (Stream<T> tStream : stream) of(tStream);
            return this;
        }

        public MyStream<T> of(Collection<Stream<T>> stream) {
            if (null != stream && stream.size() > 0) for (Stream<T> tStream : stream) of(tStream);
            return this;
        }

        public Stream<T> get() {
            return stream;
        }


        public static <T> Stream<T> merge(Stream<T> s1, Stream<T> s2) {
            return Stream.concat(s1, s2);
        }

        public static <T> Stream<T> flatMap(List<T> ts) {
            return ts.stream();
        }
    }

    public static class MyClass {

        public static <T> T newInstance(Class<T> clazz) {
            try {
                T newT = (T) clazz.newInstance();
                return newT;
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return null;
        }

        public static Map<String, Field> dissection(Class clazz) {
            Map<String, Field> map = new HashMap<>();
            if (null != clazz) {
                try {
                    Field[] declaredFields = clazz.getDeclaredFields();
                    if (MyArrays.isNotEmpty(declaredFields)) {
                        for (Field field : declaredFields) {
                            field.setAccessible(true);
                            String fieldName = field.getName();
                            map.put(fieldName, field);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return map;
        }

        public static Map<String, Object> dissection(Object t) {
            Map<String, Object> map = new HashMap<>();
            if (null != t) {
                try {
                    Class<?> clazz = t.getClass();
                    Field[] declaredFields = clazz.getDeclaredFields();
                    if (MyArrays.isNotEmpty(declaredFields)) {
                        for (Field field : declaredFields) {
                            field.setAccessible(true);
                            String fieldName = field.getName();
                            Object o = field.get(t);
                            map.put(fieldName, o);
                        }
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            return map;
        }

        public static void inject(Object t, String filedName, Object e) {
            if (null != t && null != e && MyString.isNotBlank(filedName)) {
                try {
                    Class<?> clazz = t.getClass();
                    Field field = clazz.getDeclaredField(filedName);
                    field.setAccessible(true);
                    if (null != field) field.set(t, e);
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        }

        public static List<Class<?>> getSuperClass(Class<?> clazz) {
            List<Class<?>> classList = new ArrayList<>();
            Class<?> suCl = clazz.getSuperclass();
            while (suCl != null) {
                classList.add(suCl);
                suCl = suCl.getSuperclass();
            }
            return classList;
        }

        public static Boolean isSonClass(Class<?> father, Class<?> son) {
            List<Class<?>> superClass = getSuperClass(son);
            if (MyCollection.isNotEmpty(superClass)) {
                for (Class<?> clazz : superClass) {
                    if (clazz.getName().equals(father.getName())) return true;
                }
            }
            return false;
        }
    }

    public static class DistributedLock<T> {

        /**
         * 自定义锁要实现这三个方法
         */
        public interface CommLock {

            Boolean lock(String lockKey, Long lockValue);

            Boolean unlock(String lockKey);

            Long lockResult(String lockKey);
        }

        /**
         * 是否有返回值
         */
        private Boolean hasResult = true;
        /**
         * 分布式环境下加锁方法,需要注意的是,在诸如redis或者之类存在key过期方法的容器，使用超时设置
         */
        private Function<Long, Boolean> lock;
        /**
         * 分布式环境下解锁方法
         */
        private Supplier<Boolean> unlock;
        /**
         * 分布式环境下获取锁的结果
         */
        private Supplier<Long> lockResult;
        /**
         * 有返回值的任务
         */
        private Supplier<T> task;
        private Runnable runnableTask;
        /**
         * 无返回值的任务
         */
        private Callable<T> callableTask;
        /**
         * 线程等待时间
         */
        private Supplier<Long> sleepTime = () -> 10000l;

        private final static ExecutorService execute = Executors.newFixedThreadPool(1);

        private T result;

        public static class Builder<T> {

            private DistributedLock<T> distributedLock;

            private Builder() {
                distributedLock = new DistributedLock<T>();
            }

            public static <T> Builder<T> builder() {
                return new Builder<>();
            }


            //一般锁，如基于zk锁，etcd锁，db锁，同一位置文件的权限锁(只读，存在，内容等等)，只需要实现当前的Lock接口
            public Builder<T> commLock(CommLock commLock, String lockKey, Long expireTime) {
                distributedLock.lock = (lockValue -> commLock.lock(lockKey, lockValue));
                distributedLock.unlock = (() -> commLock.unlock(lockKey));
                distributedLock.lockResult = (() -> commLock.lockResult(lockKey));
                distributedLock.sleepTime = () -> expireTime * 2 / 3;
                return this;
            }

            //任务
            public Builder<T> task(Supplier<T> task) {
                distributedLock.hasResult = true;
                distributedLock.task = task;
                return this;
            }

            //任务
            public Builder<T> task(Runnable runnableTask) {
                distributedLock.hasResult = false;
                distributedLock.runnableTask = runnableTask;
                return this;
            }

            //任务
            public Builder<T> task(Callable<T> callableTask) {
                distributedLock.hasResult = true;
                distributedLock.callableTask = callableTask;
                return this;
            }

            public DistributedLock<T> build(Lock lock) {
                if (lock.tryLock())
                    try {
                        distributedLock.execute();
                    } catch (Exception e) {
                        log.error("", e);
                    } finally {
                        lock.unlock();
                    }
                return distributedLock;
            }
        }

        public T getResult() {
            return result;
        }

        private void execute() {
            if (null == lock || null == unlock || null == lockResult || null == sleepTime || (null == task && null == runnableTask && null == callableTask))
                return;
            if (canNotContinue()) return;
            lock.apply(Instant.now(Clock.systemDefaultZone()).toEpochMilli());
            hasResultTask();
            notResultTask();
            unlock.get();
        }

        private void hasResultTask() {
            if (!hasResult) return;
            Future<T> future = null;
            if (null != task) future = execute.submit(() -> task.get());
            else if (null != callableTask) future = execute.submit(callableTask);
            Future<T> finalFuture = future;
            if (null != future) {
                List<T> list = CycleExecute.BorderlessExecute.<T>builder().actuator((i, j) -> {
                    try {
                        lock.apply(Instant.now(Clock.systemDefaultZone()).toEpochMilli());
                        if (finalFuture.isDone()) return finalFuture.get();
                        Thread.sleep(sleepTime.get());
                    } catch (Exception e) {
                        log.error("", e);
                    }
                    return null;
                }).terminationCondition((i, t) -> null != t).build().execute();
                if (null != list && list.size() > 0) result = list.get(0);
            } else log.error("没有可执行的任务!");
        }

        private void notResultTask() {
            if (hasResult) return;
            Future<?> future = null;
            if (null != runnableTask) future = execute.submit(runnableTask);
            Future<?> finalFuture = future;
            if (null != future)
                CycleExecute.BorderlessExecute.<Boolean>builder().actuator((i, j) -> {
                    try {
                        lock.apply(Instant.now(Clock.systemDefaultZone()).toEpochMilli());
                        if (finalFuture.isDone()) return true;
                        if (finalFuture.isCancelled()) return true;
                        Thread.sleep(sleepTime.get());
                    } catch (Exception e) {
                        log.error("", e);
                        return true;
                    }
                    return false;
                }).terminationCondition((i, b) -> b).build().execute();
            else log.error("没有可执行的任务!");
        }


        private boolean canNotContinue() {
            Long lastTimeMill = lockResult.get();
            if (null != lastTimeMill) {//有超时机制的无需比较上一个时间戳和当前时间戳的值
                long currentMill = Instant.now(Clock.systemDefaultZone()).toEpochMilli();
                return currentMill - lastTimeMill < sleepTime.get();
            }
            return false;
        }
    }
}
