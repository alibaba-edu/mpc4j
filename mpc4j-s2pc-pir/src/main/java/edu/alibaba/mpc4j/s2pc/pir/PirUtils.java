package edu.alibaba.mpc4j.s2pc.pir;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * PIR协议工具类。
 *
 * @author Liqiang Peng
 * @date 2022/8/1
 */
public class PirUtils {
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 私有构造函数
     */
    private PirUtils() {
        // empty
    }

    /**
     * 生成参与方的测试集合。
     *
     * @param serverSetSize 服务端集合大小。
     * @param clientSetSize 客户端集合大小。
     * @param repeatTime    客户端集合个数。
     * @return 各个参与方的集合。
     */
    public static ArrayList<Set<String>> generateStringSets(int serverSetSize, int clientSetSize, int repeatTime) {
        assert serverSetSize >= 1 : "server must have at least 1 elements";
        assert clientSetSize >= 1 : "client must have at least 1 elements";
        assert repeatTime >= 1 : "repeat time must be greater than or equal to 1: " + repeatTime;
        // 构建服务端集合
        Set<String> serverSet = IntStream.range(0, serverSetSize)
            .mapToObj(index -> "ID_" + index)
            .collect(Collectors.toSet());
        ArrayList<String> serverArrayList = new ArrayList<>(serverSet);
        // 构建客户端集合
        ArrayList<Set<String>> clientSets = IntStream.range(0, repeatTime)
            .mapToObj(repeatIndex -> {
                if (clientSetSize > 1) {
                    // 如果客户端集合大于1，则随机挑选一些元素放置在集合中
                    int matchedItemSize = clientSetSize / 2;
                    Set<String> clientSet = new HashSet<>(clientSetSize);
                    for (int index = 0; index < matchedItemSize; index++) {
                        clientSet.add(serverArrayList.get(index));
                    }
                    for (int index = matchedItemSize; index < clientSetSize; index++) {
                        clientSet.add("ID_" + index + "_DISTINCT");
                    }
                    return clientSet;
                } else {
                    // 如果客户端集合小于1，则随机选择是否把元素放置在集合中
                    Set<String> clientSet = new HashSet<>(clientSetSize);
                    int index = SECURE_RANDOM.nextInt(serverSetSize);
                    if (SECURE_RANDOM.nextBoolean()) {
                        clientSet.add(serverArrayList.get(index));
                    } else {
                        clientSet.add("ID_" + index + "_DISTINCT");
                    }
                    return clientSet;
                }
            })
            .collect(Collectors.toCollection(ArrayList::new));
        // 构建返回结果
        ArrayList<Set<String>> results = new ArrayList<>(2);
        results.add(serverSet);
        results.addAll(clientSets);
        return results;
    }

    /**
     * 生成参与方的测试集合。
     *
     * @param keywordSet      关键词集合。
     * @param labelByteLength 标签字节长度。
     * @return 关键词和标签映射。
     */
    public static Map<String, ByteBuffer> generateKeywordLabelMap(Set<String> keywordSet, int labelByteLength) {
        return keywordSet.stream()
            .collect(Collectors.toMap(
                keyword -> keyword,
                keyword -> {
                    byte[] label = new byte[labelByteLength];
                    SECURE_RANDOM.nextBytes(label);
                    return ByteBuffer.wrap(label);
                }
            ));
    }

    /**
     * 生成随机元素数组。
     *
     * @param elementSize       元素数量。
     * @param elementByteLength 元素字节长度。
     * @return 关键词和标签映射。
     */
    public static ArrayList<ByteBuffer> generateElementArrayList(int elementSize, int elementByteLength) {
        return IntStream.range(0, elementSize)
            .mapToObj(i -> {
                byte[] element = new byte[elementByteLength];
                SECURE_RANDOM.nextBytes(element);
                return ByteBuffer.wrap(element);
            })
            .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 生成索引值列表。
     *
     * @param elementSize   元素数量。
     * @param setSize       集合数量。
     * @return 索引值列表。
     */
    public static ArrayList<Integer> generateRetrievalIndexList(int elementSize, int setSize) {
        return IntStream.range(0, setSize)
            .mapToObj(i -> SECURE_RANDOM.nextInt(elementSize))
            .collect(Collectors.toCollection(ArrayList::new));
    }
}
