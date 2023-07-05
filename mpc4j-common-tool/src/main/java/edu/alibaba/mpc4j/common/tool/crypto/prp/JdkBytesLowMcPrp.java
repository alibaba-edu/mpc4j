package edu.alibaba.mpc4j.common.tool.crypto.prp;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.ByteDenseBitMatrix;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.bouncycastle.util.encoders.Hex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * 用Java实现的分组长度128比特、密钥长度128比特、S盒数量m = 10的LowMC，底层使用byte[]完成运算。
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
 * @date 2021/09/12
 */
public class JdkBytesLowMcPrp implements Prp {
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
    private static final byte[] MASK_A_10 = new byte[] {
        (byte)0x92, (byte)0x49, (byte)0x24, (byte)0x90, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
    };
    /**
     * 取m = 10个b的掩码值：0100 1001 0010 0100 1001 0010 0100 1000 0000 ... 0000
     */
    private static final byte[] MASK_B_10 = new byte[] {
        (byte)0x49, (byte)0x24, (byte)0x92, (byte)0x48, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
    };
    /**
     * 取m = 10个c的掩码值：0010 0100 1001 0010 0100 1001 0010 0100 0000 ... 0000
     */
    private static final byte[] MASK_C_10 = new byte[] {
        (byte)0x24, (byte)0x92, (byte)0x49, (byte)0x24, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
    };
    /**
     * 取n - 3m比特的掩码值：0000 0000 0000 0000 0000 0000 0000 0011 1111 ... 1111
     */
    private static final byte[] MASK_MASK = new byte[] {
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x03, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
        (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
    };
    /**
     * size = 128
     */
    private static final int SIZE = CommonConstants.BLOCK_BIT_LENGTH;
    /**
     * byte size
     */
    private static final int BYTE_SIZE = CommonConstants.BLOCK_BYTE_LENGTH;
    /**
     * 总轮数
     */
    private final int round;
    /**
     * 轮密钥矩阵，一共有r + 1组，每组128个128比特的布尔元素
     */
    private final ByteDenseBitMatrix[] keyMatrices;
    /**
     * 轮线性矩阵，一共有r组，每组为128个128比特的布尔元素
     */
    private final ByteDenseBitMatrix[] linearMatrices;
    /**
     * 轮线性逆矩阵，一共有r组，每组为128个128比特的布尔元素
     */
    private final ByteDenseBitMatrix[] invertLinearMatrices;
    /**
     * 轮常数加值，一共有r个，每组为128比特的布尔元素
     */
    private final byte[][] constants;
    /**
     * 初始变换密钥
     */
    private byte[] initKey;
    /**
     * 轮密钥取值，一共有r组，每组为128比特的布尔元素
     */
    private byte[][] roundKeys;

    /**
     * 构造LowMc伪随机置换。
     *
     * @param round 轮数，只能为20、21、23、32、192、208、287轮。
     */
    JdkBytesLowMcPrp(int round) {
        assert round == 20 || round == 21 || round == 23 || round == 32 || round == 192 || round == 208 || round == 287;
        this.round = round;
        // 打开对应的配置文件
        String lowMcFileName = LOW_MC_RESOURCE_FILE_PATH + LOW_MC_FILE_PREFIX + round + LOW_MC_FILE_SUFFIX;
        try {
            InputStream lowMcInputStream = Objects.requireNonNull(
                JdkBytesLowMcPrp.class.getClassLoader().getResourceAsStream(lowMcFileName)
            );
            InputStreamReader lowMcInputStreamReader = new InputStreamReader(lowMcInputStream);
            BufferedReader lowMcBufferedReader = new BufferedReader(lowMcInputStreamReader);
            // 读取线性变换矩阵，共有r组
            linearMatrices = new ByteDenseBitMatrix[round];
            invertLinearMatrices = new ByteDenseBitMatrix[round];
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
                linearMatrices[roundIndex] = ByteDenseBitMatrix.createFromDense(SIZE, squareMatrix);
                invertLinearMatrices[roundIndex] = linearMatrices[roundIndex].inverse();
            }
            // 读取密钥扩展矩阵，共有r + 1组
            keyMatrices = new ByteDenseBitMatrix[round + 1];
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
                keyMatrices[roundIndex] = ByteDenseBitMatrix.createFromDense(SIZE, squareMatrix);
            }
            // 读取常数，共有r组
            constants = new byte[round][];
            for (int roundIndex = 0; roundIndex < round; roundIndex++) {
                // 第一行是标识位
                String label = lowMcBufferedReader.readLine();
                assert label.equals(CONSTANT_PREFIX + roundIndex);
                // 第二行是常数
                String line = lowMcBufferedReader.readLine();
                constants[roundIndex] = Hex.decode(line);
                assert constants[roundIndex].length == BYTE_SIZE;
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
        initKey = keyMatrices[0].leftMultiply(key);
        // 根据轮数扩展密钥
        roundKeys = IntStream.range(0, round)
            .mapToObj(roundIndex -> keyMatrices[roundIndex + 1].leftMultiply(key))
            .toArray(byte[][]::new);
    }

    @Override
    public byte[] prp(byte[] plaintext) {
        assert (initKey != null && roundKeys != null);
        assert plaintext.length == BYTE_SIZE;
        byte[] state = BytesUtils.clone(plaintext);
        // initial whitening
        // state = plaintext + MultiplyWithGF2Matrix(KMatrix(0),key)
        BytesUtils.xori(state, initKey);
        for (int roundIndex = 0; roundIndex < round; roundIndex++) {
            // m computations of 3-bit sbox, remaining n-3m bits remain the same
            sboxLayer(state);
            // affine layer, state = MultiplyWithGF2Matrix(LMatrix(i),state)
            state = linearMatrices[roundIndex].leftMultiply(state);
            // state = state + Constants(i)
            BytesUtils.xori(state, constants[roundIndex]);
            // generate round key and add to the state
            BytesUtils.xori(state, roundKeys[roundIndex]);
        }
        // ciphertext = state
        return state;
    }

    @Override
    public byte[] invPrp(byte[] ciphertext) {
        assert (initKey != null && roundKeys != null);
        assert ciphertext.length == BYTE_SIZE;
        byte[] state = BytesUtils.clone(ciphertext);
        for (int roundIndex = round - 1; roundIndex >= 0; roundIndex--) {
            // generate round key and add to the state
            BytesUtils.xori(state, roundKeys[roundIndex]);
            // state = state + Constants(i)
            BytesUtils.xori(state, constants[roundIndex]);
            // affine layer, state = MultiplyWithGF2Matrix(LMatrix(i),state)
            state = invertLinearMatrices[roundIndex].leftMultiply(state);
            // m computations of 3-bit sbox, remaining n-3m bits remain the same
            sboxInvLayer(state);
        }
        // initial whitening
        // state = ciphertext + MultiplyWithGF2Matrix(KMatrix(0),key)
        BytesUtils.xori(state, initKey);
        // ciphertext = state
        return state;
    }

    private void sboxLayer(byte[] state) {
        byte[] a = BytesUtils.and(state, MASK_A_10);
        byte[] b = BytesUtils.and(state, MASK_B_10);
        BytesUtils.shiftLefti(b, 1);
        byte[] c = BytesUtils.and(state, MASK_C_10);
        BytesUtils.shiftLefti(c, 2);
        // a = a ⊕ (b ☉ c)
        byte[] aSbox = BytesUtils.and(b, c);
        BytesUtils.xori(aSbox, a);
        // b = a ⊕ b ⊕ (a ☉ c)
        byte[] bSbox = BytesUtils.and(a, c);
        BytesUtils.xori(bSbox, b);
        BytesUtils.xori(bSbox, a);
        BytesUtils.shiftRighti(bSbox, 1);
        // c = a ⊕ b ⊕ c ⊕ (a ☉ b)
        byte[] cSbox = BytesUtils.and(a, b);
        BytesUtils.xori(cSbox, c);
        BytesUtils.xori(cSbox, b);
        BytesUtils.xori(cSbox, a);
        BytesUtils.shiftRighti(cSbox, 2);
        // 设置结果
        BytesUtils.andi(state, MASK_MASK);
        BytesUtils.xori(state, aSbox);
        BytesUtils.xori(state, bSbox);
        BytesUtils.xori(state, cSbox);
    }

    private void sboxInvLayer(byte[] state) {
        byte[] aSbox = BytesUtils.and(state, MASK_A_10);
        byte[] bSbox = BytesUtils.and(state, MASK_B_10);
        BytesUtils.shiftLefti(bSbox, 1);
        byte[] cSbox = BytesUtils.and(state, MASK_C_10);
        BytesUtils.shiftLefti(cSbox, 2);
        // a = a ⊕ b ⊕ (b ☉ c)
        byte[] a = BytesUtils.and(bSbox, cSbox);
        BytesUtils.xori(a, bSbox);
        BytesUtils.xori(a, aSbox);
        // b = b ⊕ (a ☉ c)
        byte[] b = BytesUtils.and(aSbox, cSbox);
        BytesUtils.xori(b, bSbox);
        BytesUtils.shiftRighti(b, 1);
        // c = a ⊕ b ⊕ c ⊕ (a ☉ b)
        byte[] c = BytesUtils.and(aSbox, bSbox);
        BytesUtils.xori(c, cSbox);
        BytesUtils.xori(c, bSbox);
        BytesUtils.xori(c, aSbox);
        BytesUtils.shiftRighti(c, 2);
        // 设置结果
        BytesUtils.andi(state, MASK_MASK);
        BytesUtils.xori(state, a);
        BytesUtils.xori(state, b);
        BytesUtils.xori(state, c);
    }

    @Override
    public PrpFactory.PrpType getPrpType() {
        switch (round) {
            case 20:
                return PrpFactory.PrpType.JDK_BYTES_LOW_MC_20;
            case 21:
                return PrpFactory.PrpType.JDK_BYTES_LOW_MC_21;
            case 23:
                return PrpFactory.PrpType.JDK_BYTES_LOW_MC_23;
            case 32:
                return PrpFactory.PrpType.JDK_BYTES_LOW_MC_32;
            case 192:
                return PrpFactory.PrpType.JDK_BYTES_LOW_MC_192;
            case 208:
                return PrpFactory.PrpType.JDK_BYTES_LOW_MC_208;
            case 287:
                return PrpFactory.PrpType.JDK_BYTES_LOW_MC_287;
            default:
                throw new IllegalStateException("Unknown LowMc type with round = " + round);

        }
    }
}
