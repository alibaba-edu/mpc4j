package edu.alibaba.mpc4j.s2pc.opf;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Oblivious Private Function utilities.
 *
 * @author Weiran Liu
 * @date 2023/4/9
 */
public class OpfUtils {
    /**
     * private constructor.
     */
    private OpfUtils() {
        // empty
    }

    /**
     * 将总集合大小切分成4份
     */
    private static final int SPLIT_NUM = 4;
    /**
     * 第一份的索引值
     */
    private static final int FIRST_SPLIT_INDEX = 1;
    /**
     * 最后一份的索引值
     */
    private static final int LAST_SPLIT_INDEX = 3;

    /**
     * Generates bytes sets.生成参与方的测试集合。
     *
     * @param serverSize server set size.
     * @param clientSize client set size.
     * @param elementByteLength element byte length.
     * @return parties' sets.
     */
    public static ArrayList<Set<ByteBuffer>> generateBytesSets(int serverSize, int clientSize, int elementByteLength) {
        assert serverSize >= 1 : "server must have at least 2 elements";
        assert clientSize >= 1 : "client must have at least 2 elements";
        assert elementByteLength >= CommonConstants.STATS_BYTE_LENGTH;
        // 放置各个参与方的集合
        Set<ByteBuffer> serverSet = new HashSet<>(serverSize);
        Set<ByteBuffer> clientSet = new HashSet<>(clientSize);
        // 按照最小集合大小添加部分交集元素
        int minSize = Math.min(serverSize, clientSize);
        IntStream.range(0, minSize).forEach(index -> {
            if (index < minSize / SPLIT_NUM * FIRST_SPLIT_INDEX) {
                // 两个集合添加整数值[0, 0, 0, index]
                ByteBuffer intersectionByteBuffer = ByteBuffer.allocate(elementByteLength);
                intersectionByteBuffer.putInt(elementByteLength - Integer.BYTES, index);
                byte[] intersectionBytes = intersectionByteBuffer.array();
                serverSet.add(ByteBuffer.wrap(BytesUtils.clone(intersectionBytes)));
                clientSet.add(ByteBuffer.wrap(BytesUtils.clone(intersectionBytes)));
            } else if (index < minSize / SPLIT_NUM * LAST_SPLIT_INDEX) {
                // 服务端集合添加整数值[0, 0, 1, index]
                // 客户端集合添加整数值[0, 0, 2, index]
                ByteBuffer serverByteBuffer = ByteBuffer.allocate(elementByteLength);
                serverByteBuffer.putInt(elementByteLength - Integer.BYTES * 2, 1);
                serverByteBuffer.putInt(elementByteLength - Integer.BYTES, index);
                serverSet.add(serverByteBuffer);
                ByteBuffer clientByteBuffer = ByteBuffer.allocate(elementByteLength);
                clientByteBuffer.putInt(elementByteLength - Integer.BYTES * 2, 2);
                clientByteBuffer.putInt(elementByteLength - Integer.BYTES, index);
                clientSet.add(clientByteBuffer);
            } else {
                // 两个集合添加整数值[0, 0, 0, index]
                ByteBuffer intersectionByteBuffer = ByteBuffer.allocate(elementByteLength);
                intersectionByteBuffer.putInt(elementByteLength - Integer.BYTES, index);
                byte[] intersectionBytes = intersectionByteBuffer.array();
                serverSet.add(ByteBuffer.wrap(BytesUtils.clone(intersectionBytes)));
                clientSet.add(ByteBuffer.wrap(BytesUtils.clone(intersectionBytes)));
            }
        });
        // 补足集合剩余的元素
        if (serverSize > minSize) {
            IntStream.range(minSize, serverSize).forEach(index -> {
                ByteBuffer serverByteBuffer = ByteBuffer.allocate(elementByteLength);
                serverByteBuffer.putInt(elementByteLength - Integer.BYTES * 2, 1);
                serverByteBuffer.putInt(elementByteLength - Integer.BYTES, index);
                serverSet.add(serverByteBuffer);
            });
        }
        if (clientSize > minSize) {
            IntStream.range(minSize, clientSize).forEach(index -> {
                ByteBuffer clientByteBuffer = ByteBuffer.allocate(elementByteLength);
                clientByteBuffer.putInt(elementByteLength - Integer.BYTES * 2, 2);
                clientByteBuffer.putInt(elementByteLength - Integer.BYTES, index);
                clientSet.add(clientByteBuffer);
            });
        }
        // 构建返回结果
        ArrayList<Set<ByteBuffer>> byteArraySetArrayList = new ArrayList<>(2);
        byteArraySetArrayList.add(serverSet);
        byteArraySetArrayList.add(clientSet);

        return byteArraySetArrayList;
    }
}
