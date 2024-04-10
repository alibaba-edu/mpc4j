package edu.alibaba.mpc4j.s2pc.pso;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * PSO协议工具类。
 *
 * 半诚实模型下相等性验证所需字节长度来自于下述论文第4.4节：
 * Pinkas, Benny, Thomas Schneider, and Michael Zohner. Scalable private set intersection based on OT extension. ACM
 * Transactions on Privacy and Security (TOPS) 21.2 (2018): 1-35.
 *
 * P1 can perform a plaintext AES evaluation on his elements and only needs to send n1 collision-resistant strings
 * length of l = λ + log(n1) + log(n2) bit.
 *
 * 恶意安全模型下相等性验证所需字节长度来自于下述论文第3.3节：
 * Chase, Melissa, and Peihan Miao. Private set intersection in the internet setting from lightweight oblivious PRF.
 * CRYPTO 2020, Springer, Cham, 2020.
 *
 * Choice of l_2. The parameter l_2 is the output length of the hash function H_2, which controls the collision
 * probability of the PSI protocol. For security against malicious P_2, it can be computed similarly as
 * l_2 = σ + log_2(Q_2·n_2) where Q_2 is the maximum number of queries the adversary can make to H_2.
 *
 * 本实现令Q_2 = 2^128，上述公式变为：l_2 = σ + 128 + log_2(n_2)。
 *
 * @author Weiran Liu
 * @date 2022/01/21
 */
public class PsoUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsoUtils.class);
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
     * 私有构造函数
     */
    private PsoUtils() {
        // empty
    }

    /**
     * 生成参与方的测试集合，各个集合的大小相等。
     *
     * @param prefix    前缀。
     * @param partyNum  参与方数量。
     * @param partySize 参与方集合大小。
     * @return 各个参与方的集合。
     */
    public static ArrayList<Set<String>> generateEqualStringSets(String prefix, int partyNum, int partySize) {
        int[] partySizes = new int[partyNum];
        Arrays.fill(partySizes, partySize);

        return generateStringSets(prefix, partySizes);
    }

    /**
     * 生成参与方的测试集合。
     *
     * @param prefix 前缀。
     * @param sizes  各个参与方的集合大小。
     * @return 各个参与方的集合。
     */
    public static ArrayList<Set<String>> generateStringSets(String prefix, int... sizes) {
        // 每一个参与方的集合大小都应该大于0
        for (int size : sizes) {
            assert size > 0;
        }
        int minSize = Arrays.stream(sizes).min().orElse(1);
        ArrayList<Set<String>> stringSetArrayList = new ArrayList<>(sizes.length);
        // 放置各个参与方的集合
        IntStream.range(0, sizes.length).forEach(partySizeIndex ->
            stringSetArrayList.add(new HashSet<>(sizes[partySizeIndex]))
        );
        // 按照最小集合大小添加部分交集元素
        IntStream.range(0, minSize).forEach(index -> {
            if (index < minSize / SPLIT_NUM * FIRST_SPLIT_INDEX) {
                // 所有集合添加ID_i
                stringSetArrayList.forEach(set -> set.add(prefix + "_" + index));
            } else if (index < minSize / SPLIT_NUM * LAST_SPLIT_INDEX) {
                // 各个集合添加不同的元素
                IntStream.range(0, sizes.length).forEach(partyIndex ->
                    stringSetArrayList.get(partyIndex).add(prefix + "_PARTY_" + partyIndex + "_" + index)
                );
            } else {
                // 所有集合添加ID_i
                stringSetArrayList.forEach(set -> set.add(prefix + "_" + index));
            }
        });
        // 补足各个参与方的集合
        IntStream.range(0, sizes.length).forEach(partyIndex -> {
            Set<String> partySet = stringSetArrayList.get(partyIndex);
            while (partySet.size() < sizes[partyIndex]) {
                partySet.add(prefix + "_PARTY_" + partyIndex + "_" + partySet.size());
            }
        });
        return stringSetArrayList;
    }

    /**
     * 生成参与方的测试集合，各个集合的大小相等。
     *
     * @param setSize 参与方集合大小。
     * @param elementByteLength 元素字节长度。
     * @return 各个参与方的集合
     */
    public static ArrayList<Set<ByteBuffer>> generateBytesSets(int setSize, int elementByteLength) {
        return generateBytesSets(setSize, setSize, elementByteLength);
    }

    /**
     * 生成参与方的测试集合。
     *
     * @param serverSize 服务端集合大小。
     * @param clientSize 客户端集合大小。
     * @param elementByteLength 元素字节长度。
     * @return 各个参与方的集合。
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

    /**
     * 发送方字节文件前缀
     */
    public static final String BYTES_SERVER_PREFIX = "BYTES_SERVER";
    /**
     * 接收方字节文件前缀
     */
    public static final String BYTES_CLIENT_PREFIX = "BYTES_CLIENT";

    /**
     * 生成字节数组输入文件。
     *
     * @param setSize 集合大小。
     * @param elementByteLength 元素字节长度。
     * @throws IOException 如果出现IO异常。
     */
    public static void generateBytesInputFiles(int setSize, int elementByteLength) throws IOException {
        generateBytesInputFiles(setSize, setSize, elementByteLength);
    }

    /**
     * 生成字节数组输入文件。
     *
     * @param serverSetSize 服务端集合大小。
     * @param clientSetSize 客户端集合大小。
     * @param elementByteLength 元素字节长度。
     * @throws IOException 如果出现IO异常。
     */
    public static void generateBytesInputFiles(int serverSetSize, int clientSetSize, int elementByteLength)
        throws IOException {
        assert elementByteLength >= CommonConstants.STATS_BYTE_LENGTH;
        File inputFolder = new File(getFileFolderName());
        if (!inputFolder.exists()) {
            boolean success = inputFolder.mkdir();
            assert success;
        }
        File serverInputFile = new File(getBytesFileName(BYTES_SERVER_PREFIX, serverSetSize, elementByteLength));
        File clientInputFile = new File(getBytesFileName(BYTES_CLIENT_PREFIX, clientSetSize, elementByteLength));

        if (serverInputFile.exists() && clientInputFile.exists()) {
            // skip if file exists
            return;
        }
        LOGGER.info("Lost some / all files, generate byte[] set files.");
        if (serverInputFile.exists()) {
            LOGGER.info("Delete server byte[] set file.");
            Preconditions.checkArgument(
                serverInputFile.delete(), "Fail to delete file: %s", serverInputFile.getName()
            );
        }
        if (clientInputFile.exists()) {
            LOGGER.info("Delete client byte[] set file.");
            Preconditions.checkArgument(
                clientInputFile.delete(), "Fail to delete file: %s", clientInputFile.getName()
            );
        }
        // 生成文件
        ArrayList<Set<ByteBuffer>> sets = generateBytesSets(serverSetSize, clientSetSize, elementByteLength);
        Set<ByteBuffer> serverSet = sets.get(0);
        Set<ByteBuffer> clientSet = sets.get(1);
        // 写入服务端输入
        FileWriter serverFileWriter = new FileWriter(serverInputFile);
        PrintWriter serverPrintWriter = new PrintWriter(serverFileWriter, true);
        serverSet.stream().map(ByteBuffer::array).map(Hex::toHexString).forEach(serverPrintWriter::println);
        serverPrintWriter.close();
        serverFileWriter.close();
        // 写入客户端输入
        FileWriter clientFileWriter = new FileWriter(clientInputFile);
        PrintWriter clientPrintWriter = new PrintWriter(clientFileWriter, true);
        clientSet.stream().map(ByteBuffer::array).map(Hex::toHexString).forEach(clientPrintWriter::println);
        clientPrintWriter.close();
        clientFileWriter.close();
    }

    /**
     * Gets the input folder name.
     *
     * @return input folder name.
     */
    public static String getFileFolderName() {
        return "temp" + File.separator;
    }

    public static String getBytesFileName(String prefix, int setSize, int elementByteLength) {
        return getFileFolderName() + prefix + "_" + (elementByteLength * Byte.SIZE) + "_" + setSize + ".input";
    }
}
