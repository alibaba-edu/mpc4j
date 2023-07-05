package edu.alibaba.mpc4j.common.tool.crypto.prp;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.LongDenseBitMatrix;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory.PrpType;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import org.bouncycastle.util.encoders.Hex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * 用Java实现的分组长度128比特、密钥长度128比特、S盒数量m = 10的LowMC，底层使用long[]完成运算。
 * 实现细节：
 * 1. Sbox从右到左构造，即byte[16]的0、3、...、27比特为a，第1、4、7、...、28比特为b，第2、5、8、...、29比特为c。
 * 2. 总轮数为r，则密钥扩展矩阵K_0、...、K_r，线性变换矩阵L_1、...、L_r、常数值C_1、...、C_r以Hex编码的形式保存在文件中。
 * 文件名称为lowmc_128_128_r.txt。
 *
 * - 源代码参考：https://github.com/LowMC/lowmc
 *
 * LowMPC相关原理：
 * - 加密原理：Albrecht M R, Rechberger C, Schneider T, et al. Ciphers for MPC and FHE. EUROCRYPT 2015, Springer, Cham,
 * pp. 430-454.
 *
 * The Sbox is specified in Figure 2, and coincides with the Sbox used for PRINTcipher.
 * S(a, b, c) = (a ⊕ (b ⊙ c), a ⊕ b ⊕ (a ⊙ c), a ⊕ b ⊕ c ⊕ (a ⊙ b).
 *
 * - 解密电路：Liu F, Isobe T, Meier W. Cryptanalysis of full LowMC and LowMC-M with algebraic techniques. CRYPTO 2021.
 * Springer, Cham, pp. 368-401.
 *
 * Denote the 3-bit input and output of the S-box by (x_0, x_1, x_2) and (z_0, z_1, z_2), respectively. Based on
 * the definition of the S-box, the following relations hold:
 * z_0 = x_0 ⊕ (x_1 ⊙ x_2), z_1 = x_0 ⊕ x_1 ⊕ (x_0 ⊙ x_2), z_2 = x_0 ⊕ x_1 ⊕ x_2 ⊕ (x_0 ⊙ x_1).
 * Therefore, for the inverse of the S-box, there will exist
 * x_0 = z_0 ⊕ z_1 ⊕ (z_1 ⊙ z_2), x_1 = z_1 ⊕ (z_0 ⊙ z_2), x_2 = z_0 ⊕ z_1 ⊕ z_2 ⊕ (z_0 ⊙ z1).
 *
 * @author Weiran Liu
 * @date 2022/01/16
 */
public class JdkLongsLowMcPrp implements Prp {
    /**
     * LowMC配置文件存储路径
     */
    private static final String LOW_MC_RESOURCE_FILE_PATH = "low_mc/";
    /**
     * LowMC配置参数前缀
     */
    private static final String LOW_MC_FILE_PREFIX = "lowmc_128_128_";
    /**
     * LowMC配置参数后缀
     */
    private static final String LOW_MC_FILE_SUFFIX = ".txt";
    /**
     * 线性变换矩阵前缀
     */
    private static final String LINEAR_MATRIX_PREFIX = "L_";
    /**
     * 密钥扩展矩阵前缀
     */
    private static final String KEY_MATRIX_PREFIX = "K_";
    /**
     * 常数矩阵
     */
    private static final String CONSTANT_PREFIX = "C_";
    /**
     * 取m = 10个a的掩码值：1001 0010 0100 1001 0010 0100 1001 0000 0000 ... 0000
     */
    private static final long[] MASK_A_10 = new long[] { 0x9249249000000000L, 0x0000000000000000, };
    /**
     * 取m = 10个b的掩码值：0100 1001 0010 0100 1001 0010 0100 1000 0000 ... 0000
     */
    private static final long[] MASK_B_10 = new long[] { 0x4924924800000000L, 0x0000000000000000, };
    /**
     * 取m = 10个c的掩码值：0010 0100 1001 0010 0100 1001 0010 0100 0000 ... 0000
     */
    private static final long[] MASK_C_10 = new long[] { 0x2492492400000000L, 0x0000000000000000, };
    /**
     * 取n - 3m比特的掩码值：0000 0000 0000 0000 0000 0000 0000 0011 1111 ... 1111
     */
    private static final long[] MASK_MASK = new long[] { 0x00000003FFFFFFFFL, 0xFFFFFFFFFFFFFFFFL, };
    /**
     * size = 128
     */
    private static final int SIZE = CommonConstants.BLOCK_BIT_LENGTH;
    /**
     * byte size
     */
    private static final int BYTE_SIZE = CommonConstants.BLOCK_BYTE_LENGTH;
    /**
     * long size
     */
    private static final int LONG_SIZE = CommonConstants.BLOCK_LONG_LENGTH;
    /**
     * 总轮数
     */
    private final int round;
    /**
     * 轮密钥矩阵，一共有r + 1组，每组128个128比特的布尔元素
     */
    private final LongDenseBitMatrix[] keyMatrices;
    /**
     * 轮线性矩阵，一共有r组，每组为128个128比特的布尔元素
     */
    private final LongDenseBitMatrix[] linearMatrices;
    /**
     * 轮线性逆矩阵，一共有r组，每组为128个128比特的布尔元素
     */
    private final LongDenseBitMatrix[] invertLinearMatrices;
    /**
     * 轮常数加值，一共有r个，每组为128比特的布尔元素
     */
    private final long[][] constants;
    /**
     * 初始变换密钥
     */
    private long[] initKey;
    /**
     * 轮密钥取值，一共有r组，每组为128比特的布尔元素
     */
    private long[][] roundKeys;

    /**
     * 构造LowMc伪随机置换。
     *
     * @param round 轮数，只能为20、21、23、32、192、208、287轮。
     */
    JdkLongsLowMcPrp(int round) {
        assert round == 20 || round == 21 || round == 23 || round == 32 || round == 192 || round == 208 || round == 287;
        this.round = round;
        // 打开对应的配置文件
        String lowMcFileName = LOW_MC_RESOURCE_FILE_PATH + LOW_MC_FILE_PREFIX + round + LOW_MC_FILE_SUFFIX;
        try {
            InputStream lowMcInputStream = Objects.requireNonNull(
                JdkLongsLowMcPrp.class.getClassLoader().getResourceAsStream(lowMcFileName)
            );
            InputStreamReader lowMcInputStreamReader = new InputStreamReader(lowMcInputStream);
            BufferedReader lowMcBufferedReader = new BufferedReader(lowMcInputStreamReader);
            // 读取线性变换矩阵，共有r组
            linearMatrices = new LongDenseBitMatrix[round];
            invertLinearMatrices = new LongDenseBitMatrix[round];
            for (int roundIndex = 0; roundIndex < round; roundIndex++) {
                // 第一行是标识位
                String label = lowMcBufferedReader.readLine();
                assert label.equals(LINEAR_MATRIX_PREFIX + roundIndex);
                // 后面跟着BLOCK_BIT_LENGTH行数据
                byte[][] squareMatrix = new byte[SIZE][];
                for (int bitIndex = 0; bitIndex < SIZE; bitIndex++) {
                    String line = lowMcBufferedReader.readLine();
                    squareMatrix[bitIndex] = Hex.decode(line);
                    assert squareMatrix[bitIndex].length == BYTE_SIZE;
                }
                linearMatrices[roundIndex] = LongDenseBitMatrix.createFromDense(SIZE, squareMatrix);
                invertLinearMatrices[roundIndex] = (LongDenseBitMatrix) linearMatrices[roundIndex].inverse();
            }
            // 读取密钥扩展矩阵，共有r + 1组
            keyMatrices = new LongDenseBitMatrix[round + 1];
            for (int roundIndex = 0; roundIndex < round + 1; roundIndex++) {
                // 第一行是标识位
                String label = lowMcBufferedReader.readLine();
                assert label.equals(KEY_MATRIX_PREFIX + roundIndex);
                // 后面跟着BLOCK_BIT_LENGTH行数据
                byte[][] squareMatrix = new byte[SIZE][];
                for (int bitIndex = 0; bitIndex < SIZE; bitIndex++) {
                    String line = lowMcBufferedReader.readLine();
                    squareMatrix[bitIndex] = Hex.decode(line);
                    assert squareMatrix[bitIndex].length == BYTE_SIZE;
                }
                keyMatrices[roundIndex] = LongDenseBitMatrix.createFromDense(SIZE, squareMatrix);
            }
            // 读取常数，共有r组
            constants = new long[round][];
            for (int roundIndex = 0; roundIndex < round; roundIndex++) {
                // 第一行是标识位
                String label = lowMcBufferedReader.readLine();
                assert label.equals(CONSTANT_PREFIX + roundIndex);
                // 第二行是常数
                String line = lowMcBufferedReader.readLine();
                constants[roundIndex] = LongUtils.byteArrayToLongArray(Hex.decode(line));
                assert constants[roundIndex].length == LONG_SIZE;
            }
            lowMcBufferedReader.close();
            lowMcInputStreamReader.close();
            lowMcInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("Failed to read LowMC configuration file" + lowMcFileName);
        }
    }

    @Override
    public void setKey(byte[] key) {
        assert key.length == BYTE_SIZE;
        // LowMC内部不存储密钥，只存储扩展密钥，因此密钥得到了拷贝
        // 初始扩展密钥
        long[] longKey = LongUtils.byteArrayToLongArray(key);
        initKey = keyMatrices[0].leftMultiply(longKey);
        // 根据轮数扩展密钥
        roundKeys = IntStream.range(0, round)
            .mapToObj(roundIndex -> keyMatrices[roundIndex + 1].leftMultiply(longKey))
            .toArray(long[][]::new);
    }

    @Override
    public byte[] prp(byte[] plaintext) {
        assert (initKey != null && roundKeys != null);
        assert plaintext.length == BYTE_SIZE;
        long[] state = LongUtils.byteArrayToLongArray(plaintext);
        // initial whitening
        // state = plaintext + MultiplyWithGF2Matrix(KMatrix(0),key)
        LongUtils.xori(state, initKey);
        for (int roundIndex = 0; roundIndex < round; roundIndex++) {
            // m computations of 3-bit sbox, remaining n-3m bits remain the same
            sboxLayer(state);
            // affine layer, state = MultiplyWithGF2Matrix(LMatrix(i),state)
            state = linearMatrices[roundIndex].leftMultiply(state);
            // state = state + Constants(i)
            LongUtils.xori(state, constants[roundIndex]);
            // generate round key and add to the state
            LongUtils.xori(state, roundKeys[roundIndex]);
        }
        // ciphertext = state
        return LongUtils.longArrayToByteArray(state);
    }

    @Override
    public byte[] invPrp(byte[] ciphertext) {
        assert (initKey != null && roundKeys != null);
        assert ciphertext.length == BYTE_SIZE;
        long[] state = LongUtils.byteArrayToLongArray(ciphertext);
        for (int roundIndex = round - 1; roundIndex >= 0; roundIndex--) {
            // generate round key and add to the state
            LongUtils.xori(state, roundKeys[roundIndex]);
            // state = state + Constants(i)
            LongUtils.xori(state, constants[roundIndex]);
            // affine layer, state = MultiplyWithGF2Matrix(LMatrix(i),state)
            state = invertLinearMatrices[roundIndex].leftMultiply(state);
            // m computations of 3-bit sbox, remaining n-3m bits remain the same
            sboxInvLayer(state);
        }
        // initial whitening
        // state = ciphertext + MultiplyWithGF2Matrix(KMatrix(0),key)
        LongUtils.xori(state, initKey);
        // ciphertext = state
        return LongUtils.longArrayToByteArray(state);
    }

    private void sboxLayer(long[] state) {
        long a = LongUtils.and(state, MASK_A_10)[0];
        long b = LongUtils.and(state, MASK_B_10)[0] << 1;
        long c = LongUtils.and(state, MASK_C_10)[0] << 2;
        // a = a ⊕ (b ☉ c)
        long aSbox = b & c;
        aSbox = aSbox ^ a;
        // b = a ⊕ b ⊕ (a ☉ c)
        long bSbox = a & c;
        bSbox = bSbox ^ b;
        bSbox = bSbox ^ a;
        bSbox = bSbox >>> 1;
        // c = a ⊕ b ⊕ c ⊕ (a ☉ b)
        long cSbox = a & b;
        cSbox = cSbox ^ c;
        cSbox = cSbox ^ b;
        cSbox = cSbox ^ a;
        cSbox = cSbox >>> 2;
        // 设置结果
        LongUtils.andi(state, MASK_MASK);
        state[0] = state[0] ^ aSbox;
        state[0] = state[0] ^ bSbox;
        state[0] = state[0] ^ cSbox;
    }

    private void sboxInvLayer(long[] state) {
        long aSbox = LongUtils.and(state, MASK_A_10)[0];
        long bSbox = LongUtils.and(state, MASK_B_10)[0] << 1;
        long cSbox = LongUtils.and(state, MASK_C_10)[0] << 2;
        // a = a ⊕ b ⊕ (b ☉ c)
        long a = bSbox & cSbox;
        a = a ^ bSbox;
        a = a ^ aSbox;
        // b = b ⊕ (a ☉ c)
        long b = aSbox & cSbox;
        b = b ^ bSbox;
        b = b >>> 1;
        // c = a ⊕ b ⊕ c ⊕ (a ☉ b)
        long c = aSbox & bSbox;
        c = c ^ cSbox;
        c = c ^ bSbox;
        c = c ^ aSbox;
        c = c >>> 2;
        // 设置结果
        LongUtils.andi(state, MASK_MASK);
        state[0] = state[0] ^ a;
        state[0] = state[0] ^ b;
        state[0] = state[0] ^ c;
    }

    @Override
    public PrpType getPrpType() {
        switch (round) {
            case 20:
                return PrpType.JDK_LONGS_LOW_MC_20;
            case 21:
                return PrpType.JDK_LONGS_LOW_MC_21;
            case 23:
                return PrpType.JDK_LONGS_LOW_MC_23;
            case 32:
                return PrpType.JDK_LONGS_LOW_MC_32;
            case 192:
                return PrpType.JDK_LONGS_LOW_MC_192;
            case 208:
                return PrpType.JDK_LONGS_LOW_MC_208;
            case 287:
                return PrpType.JDK_LONGS_LOW_MC_287;
            default:
                throw new IllegalStateException("Unknown LowMc type with round = " + round);

        }
    }
}
