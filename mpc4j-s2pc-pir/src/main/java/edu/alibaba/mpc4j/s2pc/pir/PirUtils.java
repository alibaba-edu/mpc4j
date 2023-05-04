package edu.alibaba.mpc4j.s2pc.pir;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.crypto.matrix.database.NaiveDatabase;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(PirUtils.class);

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
     * @return 随机元素数组。
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
     * Creates a random database.
     *
     * @param elementSize      database size.
     * @param elementBitLength element bit length.
     * @return random database.
     */
    public static NaiveDatabase generateDataBase(int elementSize, int elementBitLength) {
        return NaiveDatabase.createRandom(elementBitLength, elementSize, SECURE_RANDOM);
    }

    /**
     * 生成随机元素数组。
     *
     * @param elementSize      元素数量。
     * @param elementBitLength 元素比特长度。
     * @return 随机元素数组。
     */
    public static byte[][] generateElementArray(int elementSize, int elementBitLength) {
        int elementByteLength = CommonUtils.getByteLength(elementBitLength);
        return IntStream.range(0, elementSize)
            .mapToObj(i -> BytesUtils.randomByteArray(elementByteLength, elementBitLength, SECURE_RANDOM))
            .toArray(byte[][]::new);
    }

    /**
     * 生成索引值列表。
     *
     * @param elementSize 元素数量。
     * @param setSize     集合数量。
     * @return 索引值列表。
     */
    public static ArrayList<Integer> generateRetrievalIndexList(int elementSize, int setSize) {
        return IntStream.range(0, setSize)
            .mapToObj(i -> SECURE_RANDOM.nextInt(elementSize))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 生成索引值。
     *
     * @param elementSize 元素数量。
     * @return 索引值。
     */
    public static int generateRetrievalIndex(int elementSize) {
        return SECURE_RANDOM.nextInt(elementSize);
    }

    /**
     * 生成索引值集合。
     *
     * @param elementSize   元素数量。
     * @param retrievalSize 集合数量。
     * @return 索引值集合。
     */
    public static Set<Integer> generateRetrievalIndexSet(int elementSize, int retrievalSize) {
        Set<Integer> indexSet = new HashSet<>();
        while (indexSet.size() < retrievalSize) {
            int index = SECURE_RANDOM.nextInt(elementSize);
            indexSet.add(index);
        }
        return indexSet;
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
     * @param setSize          集合大小。
     * @param elementBitLength 元素比特长度。
     * @throws IOException 如果出现IO异常。
     */
    public static void generateBytesInputFiles(int setSize, int elementBitLength)
        throws IOException {
        MathPreconditions.checkPositive("elementBitLength", elementBitLength);
        File serverInputFile = new File(getServerFileName(BYTES_SERVER_PREFIX, setSize, elementBitLength));
        if (serverInputFile.exists()) {
            // 文件都存在，跳过生成阶段
            return;
        }
        LOGGER.info("Lost some / all files, generate byte[] set files.");
        if (serverInputFile.exists()) {
            LOGGER.info("Delete server byte[] set file.");
            Preconditions.checkArgument(
                serverInputFile.delete(), "Fail to delete file: %s", serverInputFile.getName()
            );
        }
        // 生成文件
        byte[][] elementArray = generateElementArray(setSize, elementBitLength);
        // 写入服务端输入
        FileWriter serverFileWriter = new FileWriter(serverInputFile);
        PrintWriter serverPrintWriter = new PrintWriter(serverFileWriter, true);
        IntStream.range(0, setSize)
            .mapToObj(i -> Hex.toHexString(elementArray[i]))
            .forEach(serverPrintWriter::println);
        serverPrintWriter.close();
        serverFileWriter.close();
    }

    /**
     * 生成检索值输入文件。
     *
     * @param retrievalSize 检索数目。
     * @throws IOException 如果出现IO异常。
     */
    public static void generateIndexInputFiles(int elementSize, int retrievalSize)
        throws IOException {
        MathPreconditions.checkPositive("retrievalSize", retrievalSize);
        File clientInputFile = new File(getClientFileName(BYTES_CLIENT_PREFIX, retrievalSize));
        if (clientInputFile.exists()) {
            // 文件都存在，跳过生成阶段
            return;
        }
        LOGGER.info("Lost some / all files, generate byte[] set files.");
        if (clientInputFile.exists()) {
            LOGGER.info("Delete server byte[] set file.");
            Preconditions.checkArgument(
                clientInputFile.delete(), "Fail to delete file: %s", clientInputFile.getName()
            );
        }
        // 生成文件
        Set<Integer> retrievalIndexSet = generateRetrievalIndexSet(elementSize, retrievalSize);
        // 写入服务端输入
        FileWriter clientFileWriter = new FileWriter(clientInputFile);
        PrintWriter clientPrintWriter = new PrintWriter(clientFileWriter, true);
        retrievalIndexSet.stream()
            .map(IntUtils::intToByteArray)
            .map(Hex::toHexString)
            .forEach(clientPrintWriter::println);
        clientPrintWriter.close();
        clientPrintWriter.close();
    }

    public static String getServerFileName(String prefix, int setSize, int elementBitLength) {
        return prefix + "_" + elementBitLength + "_" + setSize + ".txt";
    }

    public static String getClientFileName(String prefix, int setSize) {
        return prefix + "_" + setSize + ".txt";
    }

    /**
     * 返回多项式包含的元素数量。
     *
     * @param elementByteLength 元素字节长度。
     * @param polyModulusDegree 多项式阶。
     * @param coeffBitLength    系数比特长度。
     * @return 多项式包含的元素数量。
     */
    public static int elementSizeOfPlaintext(int elementByteLength, int polyModulusDegree, int coeffBitLength) {
        int coeffSizeOfElement = coeffSizeOfElement(elementByteLength, coeffBitLength);
        int elementSizeOfPlaintext = polyModulusDegree / coeffSizeOfElement;
        assert elementSizeOfPlaintext > 0 :
            "N should be larger than the of coefficients needed to represent a database element";
        return elementSizeOfPlaintext;
    }

    /**
     * 返回表示单个元素所需的系数个数。
     *
     * @param elementByteLength 元素字节长度。
     * @param coeffBitLength    系数比特长度。
     * @return 表示单个元素所需的系数个数。
     */
    public static int coeffSizeOfElement(int elementByteLength, int coeffBitLength) {
        return (int) Math.ceil(Byte.SIZE * elementByteLength / (double) coeffBitLength);
    }

    /**
     * 将字节数组转换为指定比特长度的long型数组。
     *
     * @param limit     long型数值的比特长度。
     * @param offset    移位。
     * @param size      待转换的字节数组长度。
     * @param byteArray 字节数组。
     * @return long型数组。
     */
    public static long[] convertBytesToCoeffs(int limit, int offset, int size, byte[] byteArray) {
        // 需要使用的系数个数
        int longArraySize = (int) Math.ceil(Byte.SIZE * size / (double) limit);
        long[] longArray = new long[longArraySize];
        int room = limit;
        int flag = 0;
        for (int i = 0; i < size; i++) {
            int src = byteArray[i+offset];
            if (src < 0) {
                src &= 0xFF;
            }
            int rest = Byte.SIZE;
            while (rest != 0) {
                if (room == 0) {
                    flag++;
                    room = limit;
                }
                int shift = Math.min(room, rest);
                long temp = longArray[flag] << shift;
                longArray[flag] = temp | (src >> (Byte.SIZE - shift));
                int remain = (1 << (Byte.SIZE - shift)) - 1;
                src = (src & remain) << shift;
                room -= shift;
                rest -= shift;
            }
        }
        longArray[flag] = longArray[flag] << room;
        return longArray;
    }


    /**
     * 将long型数组转换为字节数组。
     *
     * @param longArray long型数组。
     * @param logt      系数比特长度。
     * @return 字节数组。
     */
    public static byte[] convertCoeffsToBytes(long[] longArray, int logt) {
        int longArrayLength = longArray.length;
        byte[] byteArray = new byte[longArrayLength * logt / Byte.SIZE];
        int room = Byte.SIZE;
        int j = 0;
        for (long l : longArray) {
            long src = l;
            int rest = logt;
            while (rest != 0 && j < byteArray.length) {
                int shift = Math.min(room, rest);
                byteArray[j] = (byte) (byteArray[j] << shift);
                byteArray[j] = (byte) (byteArray[j] | (src >> (logt - shift)));
                src = src << shift;
                room -= shift;
                rest -= shift;
                if (room == 0) {
                    j++;
                    room = Byte.SIZE;
                }
            }
        }
        return byteArray;
    }

    /**
     * 计算各维度的坐标。
     *
     * @param retrievalIndex 索引值。
     * @param dimensionSize  各维度的长度。
     * @return 各维度的坐标。
     */
    public static int[] computeIndices(int retrievalIndex, int[] dimensionSize) {
        long product = Arrays.stream(dimensionSize).asLongStream().reduce(1, (a, b) -> a * b);
        int[] indices = new int[dimensionSize.length];
        for (int i = 0; i < dimensionSize.length; i++) {
            product /= dimensionSize[i];
            int ji = (int) (retrievalIndex / product);
            indices[i] = ji;
            retrievalIndex -= ji * product;
        }
        return indices;
    }



    /**
     * 返回输入数据的比特长度。
     *
     * @param input 输入数据。
     * @return 比特长度。
     */
    public static int getBitLength(int input) {
        int count = 0;
        while (input != 0) {
            count++;
            input /= 2;
        }
        return count;
    }

    /**
     * 返回与输入数据临近的2的次方。
     *
     * @param input 输入数据。
     * @return 与输入数据临近的2的次方。
     */
    public static int getNextPowerOfTwo(int input) {
        if ((input & (input - 1)) == 0) {
            return input;
        }
        int numberOfBits = getBitLength(input);
        return (1 << numberOfBits);
    }

    /**
     * 明文多项式移位。
     *
     * @param coeffs 多项式系数。
     * @param offset 移位。
     * @return 移位后的多项式。
     */
    public static long[][] plaintextRotate(long[][] coeffs, int offset) {
        return Arrays.stream(coeffs).map(coeff -> plaintextRotate(coeff, offset)).toArray(long[][]::new);
    }

    /**
     * 明文多项式移位。
     *
     * @param coeffs 多项式系数。
     * @param offset 移位。
     * @return 移位后的多项式。
     */
    public static long[] plaintextRotate(long[] coeffs, int offset) {
        int rowCount = coeffs.length / 2;
        long[] rotatedCoeffs = new long[coeffs.length];
        IntStream.range(0, rowCount).forEach(j -> rotatedCoeffs[j] = coeffs[(rowCount - offset + j) % rowCount]);
        IntStream.range(0, rowCount)
            .forEach(j -> rotatedCoeffs[j + rowCount] = coeffs[(rowCount - offset + j) % rowCount + rowCount]);
        return rotatedCoeffs;
    }

    /**
     * 返回数据库编码后每个维度的长度。
     *
     * @param elementSize 元素数量。
     * @return 数据库编码后每个维度的长度。
     */
    public static int[] computeDimensionLength(int elementSize, int dimension) {
        int[] dimensionLength = IntStream.range(0, dimension)
            .map(i -> (int) Math.max(2, Math.floor(Math.pow(elementSize, 1.0 / dimension))))
            .toArray();
        int product = 1;
        int j = 0;
        // if plaintext_num is not a d-power
        if (dimensionLength[0] != Math.pow(elementSize, 1.0 / dimension)) {
            while (product < elementSize && j < dimension) {
                product = 1;
                dimensionLength[j++]++;
                for (int i = 0; i < dimension; i++) {
                    product *= dimensionLength[i];
                }
            }
        }
        return dimensionLength;
    }

    /**
     * 返回数据库编码后每个维度的长度。
     *
     * @param elementSize 元素数量。
     * @return 数据库编码后每个维度的长度。
     */
    public static int[] computeDimensionLength(int elementSize, int firstDimensionSize, int subsequentDimensionSize) {
        ArrayList<Integer> dimensionLength = new ArrayList<>();
        dimensionLength.add(firstDimensionSize);
        int product = firstDimensionSize;
        for (int i = elementSize / firstDimensionSize; i >= subsequentDimensionSize; i /= subsequentDimensionSize) {
            dimensionLength.add(subsequentDimensionSize);
            product *= subsequentDimensionSize;
        }
        int dimensionSize = dimensionLength.size();
        int[] dimensionArray = IntStream.range(0, dimensionSize).map(dimensionLength::get).toArray();
        while (product < elementSize) {
            dimensionArray[dimensionSize - 1]++;
            product = 1;
            product *= Arrays.stream(dimensionArray, 0, dimensionSize).reduce(1, (a, b) -> a * b);
        }
        if (dimensionSize == 1 && dimensionArray[0] > firstDimensionSize) {
            dimensionArray = new int[] {firstDimensionSize, subsequentDimensionSize};
        }
        return dimensionArray;
    }
}
