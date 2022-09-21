package edu.alibaba.mpc4j.common.tool.lpn.llc;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.prp.Prp;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;

import java.nio.ByteBuffer;
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
     * 编码矩阵有d个位置为1
     */
    private static final int D = 10;
    /**
     * 生成d个随机整数所需要的分组数量
     */
    private static final int RANDOM_BLOCK_NUM = (int) Math.ceil((double) D * Integer.BYTES
        / CommonConstants.BLOCK_BYTE_LENGTH);
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
        assert k > D : "k must be greater than D = " + D + ": " + k;
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
        IntStream rowIndexIntStream = IntStream.range(0, n);
        rowIndexIntStream = parallel ? rowIndexIntStream.parallel() : rowIndexIntStream;
        rowIndexIntStream.forEach(rowIndex -> {
            int[] sparseRow = generateSparseRow(rowIndex);
            for (int j = 0; j < D; j++) {
                int position = Math.abs(sparseRow[j] % k);
                outputs[rowIndex] ^= inputs[position];
            }
        });
        return outputs;
    }

    /**
     * 给定k个GF2E域的输入，编码得到n个GF2E域的输出。
     *
     * @param inputs k个输入。
     * @return n个输出。
     */
    public byte[][] gf2eEncode(byte[][] inputs) {
        assert inputs.length == k;
        int inputByteLength = inputs[0].length;
        // 不需要验证输入长度，xor的时候会自动监测
        IntStream rowIndexIntStream = IntStream.range(0, n);
        rowIndexIntStream = parallel ? rowIndexIntStream.parallel() : rowIndexIntStream;
        return rowIndexIntStream
            .mapToObj(rowIndex -> {
                int[] sparseRow = generateSparseRow(rowIndex);
                byte[] output = new byte[inputByteLength];
                for (int j = 0; j < D; j++) {
                    int position = Math.abs(sparseRow[j] % k);
                    BytesUtils.xori(output, inputs[position]);
                }
                return output;
            })
            .toArray(byte[][]::new);
    }

    /**
     * 给定k个Z2域的输入，编码得到n个Z2域的输出，输出结果压缩为字节数组。
     *
     * @param inputs k个输入。
     * @return n个输出。
     */
    public byte[] z2Encode(byte[] inputs) {
        assert inputs.length == byteK : "input byte length must be equal to " + byteK + ": " + inputs.length;
        assert BytesUtils.isReduceByteArray(inputs, k) : "input must contains at most " + k + " bits";
        boolean[] outputs = new boolean[n];
        IntStream rowIndexIntStream = IntStream.range(0, n);
        rowIndexIntStream = parallel ? rowIndexIntStream.parallel() : rowIndexIntStream;
        rowIndexIntStream.forEach(rowIndex -> {
            int[] sparseRow = generateSparseRow(rowIndex);
            for (int j = 0; j < D; j++) {
                int position = Math.abs(sparseRow[j] % k);
                outputs[rowIndex] ^= BinaryUtils.getBoolean(inputs, offsetK + position);
            }
        });
        return BinaryUtils.binaryToRoundByteArray(outputs);
    }

    private int[] generateSparseRow(int rowIndex) {
        // block tmp[3]
        ByteBuffer indexByteBuffer = ByteBuffer.allocate(CommonConstants.BLOCK_BYTE_LENGTH * RANDOM_BLOCK_NUM);
        // 不重复分配内存
        ByteBuffer blockByteBuffer = ByteBuffer.allocate(CommonConstants.BLOCK_BYTE_LENGTH);
        for (int blockIndex = 0; blockIndex < RANDOM_BLOCK_NUM; blockIndex++) {
            // tmp[m] = makeBlock(i, m)
            byte[] block = blockByteBuffer
                .putInt(0, rowIndex)
                .putInt(CommonConstants.BLOCK_BYTE_LENGTH / 2, blockIndex)
                .array();
            // prp->permute_block(tmp, 3)
            indexByteBuffer.put(prp.prp(block));
        }
        return IntUtils.byteArrayToIntArray(indexByteBuffer.array());
    }
}
