package edu.alibaba.mpc4j.common.tool.lpn.llc;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.prp.Prp;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * d-本地线性编码（d-local linear code）。
 * <p>
 * 此编码的生成矩阵为n * k比特，其中每k行中只有d < k行的值为1，其余值均为0。给定k个输入，本地线性编码将对这k个输入进行编码，得到n个输出。
 * </p>
 * 本实现参考emp-ot中的lpn_f2.h。
 *
 * @author Weiran Liu
 * @date 2022/01/31
 */
public class LocalLinearCoder {
    /**
     * 不安全转换函数，参见https://stackoverflow.com/questions/43079234/convert-a-byte-array-into-an-int-array-in-java
     */
    private static final Unsafe UNSAFE;

    static {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            UNSAFE = (Unsafe) theUnsafe.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }

    /**
     * 编码输出行数
     */
    private final int n;
    /**
     * 编码输入行数
     */
    private final int k;
    /**
     * 编码数组字节行数
     */
    private final int byteK;
    /**
     * 编码输入行数偏移量
     */
    private final int offsetK;
    /**
     * 是否并发执行
     */
    private final boolean parallel;
    /**
     * 伪随机置换
     */
    private final Prp prp;

    /**
     * 构造本地线性编码器。
     *
     * @param k        输入行数。
     * @param n        输出行数。
     * @param seed     种子。
     * @param parallel 是否并行计算。
     */
    public LocalLinearCoder(int k, int n, byte[] seed, boolean parallel) {
        this(EnvType.STANDARD, k, n, seed, parallel);
    }

    /**
     * 构造本地线性编码器。
     *
     * @param envType  环境类型。
     * @param k        输入行数。
     * @param n        输出行数。
     * @param seed     种子。
     * @param parallel 是否并行计算。
     */
    public LocalLinearCoder(EnvType envType, int k, int n, byte[] seed, boolean parallel) {
        assert k > LocalLinearCoderUtils.D : "k must be greater than D = " + LocalLinearCoderUtils.D + ": " + k;
        this.k = k;
        byteK = CommonUtils.getByteLength(k);
        offsetK = byteK * Byte.SIZE - k;
        assert n > 0 : "n must be greater than 0: " + n;
        this.n = n;
        // 设置伪随机函数并初始化密钥
        prp = PrpFactory.createInstance(envType);
        prp.setKey(seed);
        // 设置并发
        this.parallel = parallel;
    }

    /**
     * 给定k个布尔输入，编码得到n个布尔输出。
     *
     * @param inputs k个输入。
     * @return n个输出。
     */
    public boolean[] binaryEncode(boolean[] inputs) {
        assert inputs.length == k;
        boolean[] outputs = new boolean[n];
        IntStream encodeIntStream = IntStream.range(0, n);
        encodeIntStream = parallel ? encodeIntStream.parallel() : encodeIntStream;
        encodeIntStream.forEach(index -> {
            // block tmp[3]
            ByteBuffer indexByteBuffer = ByteBuffer.allocate(
                CommonConstants.BLOCK_BYTE_LENGTH * LocalLinearCoderUtils.RANDOM_BLOCK_NUM
            );
            // 不重复分配内存
            ByteBuffer blockByteBuffer = ByteBuffer.allocate(CommonConstants.BLOCK_BYTE_LENGTH);
            for (int blockIndex = 0; blockIndex < LocalLinearCoderUtils.RANDOM_BLOCK_NUM; blockIndex++) {
                // tmp[m] = makeBlock(i, m)
                byte[] block = blockByteBuffer
                    .putInt(0, index)
                    .putInt(CommonConstants.BLOCK_BYTE_LENGTH / 2, blockIndex)
                    .array();
                // prp->permute_block(tmp, 3)
                indexByteBuffer.put(prp.prp(block));
            }
            byte[] indexBytes = indexByteBuffer.array();
            int[] encode = new int[LocalLinearCoderUtils.RANDOM_INT_NUM];
            UNSAFE.copyMemory(
                indexBytes, Unsafe.ARRAY_BYTE_BASE_OFFSET, encode, Unsafe.ARRAY_INT_BASE_OFFSET, indexBytes.length
            );
            // uint32_t* r = (uint32_t*)(tmp)
            for (int j = 0; j < LocalLinearCoderUtils.D; j++) {
                int position = Math.abs(encode[j] % k);
                outputs[index] ^= inputs[position];
            }
        });
        return outputs;
    }

    /**
     * 给定k个分组输入，编码得到n个分组输出。
     *
     * @param inputs k个输入。
     * @return n个输出。
     */
    public byte[][] blockEncode(byte[][] inputs) {
        assert inputs.length == k;
        Arrays.stream(inputs).forEach(input -> {
            assert input.length == CommonConstants.BLOCK_BYTE_LENGTH;
        });
        IntStream encodeIntStream = IntStream.range(0, n);
        encodeIntStream = parallel ? encodeIntStream.parallel() : encodeIntStream;
        return encodeIntStream
            .mapToObj(index -> {
                // block tmp[3]
                ByteBuffer indexByteBuffer = ByteBuffer.allocate(
                    CommonConstants.BLOCK_BYTE_LENGTH * LocalLinearCoderUtils.RANDOM_BLOCK_NUM
                );
                // 不重复分配内存
                ByteBuffer blockByteBuffer = ByteBuffer.allocate(CommonConstants.BLOCK_BYTE_LENGTH);
                for (int blockIndex = 0; blockIndex < LocalLinearCoderUtils.RANDOM_BLOCK_NUM; blockIndex++) {
                    // tmp[m] = makeBlock(i, m)
                    byte[] block = blockByteBuffer
                        .putInt(0, index)
                        .putInt(CommonConstants.BLOCK_BYTE_LENGTH / 2, blockIndex)
                        .array();
                    // prp->permute_block(tmp, 3)
                    indexByteBuffer.put(prp.prp(block));
                }
                byte[] indexBytes = indexByteBuffer.array();
                int[] encode = new int[LocalLinearCoderUtils.RANDOM_INT_NUM];
                UNSAFE.copyMemory(
                    indexBytes, Unsafe.ARRAY_BYTE_BASE_OFFSET, encode, Unsafe.ARRAY_INT_BASE_OFFSET, indexBytes.length
                );
                // uint32_t* r = (uint32_t*)(tmp)
                byte[] output = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                for (int j = 0; j < LocalLinearCoderUtils.D; j++) {
                    int position = Math.abs(encode[j] % k);
                    BytesUtils.xori(output, inputs[position]);
                }
                return output;
            })
            .toArray(byte[][]::new);
    }

    /**
     * 给定k个Z2输入，编码得到n个Z2输出，输出结果压缩为字节数组。
     *
     * @param inputs k个输入。
     * @return n个输出。
     */
    public byte[] z2Encode(byte[] inputs) {
        assert inputs.length == byteK : "input byte length must be equal to " + byteK + ": " + inputs.length;
        assert BytesUtils.isReduceByteArray(inputs, k) : "input must contains at most " + k + " bits";
        boolean[] outputs = new boolean[n];
        IntStream encodeIntStream = IntStream.range(0, n);
        encodeIntStream = parallel ? encodeIntStream.parallel() : encodeIntStream;
        encodeIntStream.forEach(index -> {
            // block tmp[3]
            ByteBuffer indexByteBuffer = ByteBuffer.allocate(
                CommonConstants.BLOCK_BYTE_LENGTH * LocalLinearCoderUtils.RANDOM_BLOCK_NUM
            );
            // 不重复分配内存
            ByteBuffer blockByteBuffer = ByteBuffer.allocate(CommonConstants.BLOCK_BYTE_LENGTH);
            for (int blockIndex = 0; blockIndex < LocalLinearCoderUtils.RANDOM_BLOCK_NUM; blockIndex++) {
                // tmp[m] = makeBlock(i, m)
                byte[] block = blockByteBuffer
                    .putInt(0, index)
                    .putInt(CommonConstants.BLOCK_BYTE_LENGTH / 2, blockIndex)
                    .array();
                // prp->permute_block(tmp, 3)
                indexByteBuffer.put(prp.prp(block));
            }
            byte[] indexBytes = indexByteBuffer.array();
            int[] encode = new int[LocalLinearCoderUtils.RANDOM_INT_NUM];
            UNSAFE.copyMemory(
                indexBytes, Unsafe.ARRAY_BYTE_BASE_OFFSET, encode, Unsafe.ARRAY_INT_BASE_OFFSET, indexBytes.length
            );
            // uint32_t* r = (uint32_t*)(tmp)
            for (int j = 0; j < LocalLinearCoderUtils.D; j++) {
                int position = Math.abs(encode[j] % k);
                outputs[index] ^= BinaryUtils.getBoolean(inputs, offsetK + position);
            }
        });
        return BinaryUtils.binaryToRoundByteArray(outputs);
    }
}
