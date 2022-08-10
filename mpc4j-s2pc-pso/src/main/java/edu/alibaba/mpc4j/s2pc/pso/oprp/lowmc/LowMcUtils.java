package edu.alibaba.mpc4j.s2pc.pso.oprp.lowmc;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.LongSquareDenseBitMatrix;
import edu.alibaba.mpc4j.common.tool.crypto.prp.JdkLongsLowMcPrp;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import org.bouncycastle.util.encoders.Hex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;

/**
 * LOWMC协议工具类。
 *
 * @author Weiran Liu
 * @date 2022/02/11
 */
class LowMcUtils {
    /**
     * 私有构造函数
     */
    private LowMcUtils() {
        // empty
    }

    /**
     * LowMC配置文件存储路径
     */
    static final String LOW_MC_FILE = "low_mc/lowmc_128_128_20.txt";
    /**
     * 线性变换矩阵前缀
     */
    static final String LINEAR_MATRIX_PREFIX = "L_";
    /**
     * 密钥扩展矩阵前缀
     */
    static final String KEY_MATRIX_PREFIX = "K_";
    /**
     * 常数矩阵
     */
    static final String CONSTANT_PREFIX = "C_";
    /**
     * 10个sbox
     */
    static final int SBOX_NUM = 10;
    /**
     * 总轮数
     */
    static final int ROUND = 20;
    /**
     * 轮密钥矩阵，一共有r + 1组，每组128个128比特的布尔元素
     */
    static final LongSquareDenseBitMatrix[] KEY_MATRICES;
    /**
     * 轮线性矩阵，一共有r组，每组为128个128比特的布尔元素
     */
    static final LongSquareDenseBitMatrix[] LINEAR_MATRICES;
    /**
     * 轮常数加值，一共有r个，每组为128比特的布尔元素
     */
    static final long[][] CONSTANTS;

    static {
        // 打开lowMc配置文件
        try {
            InputStream lowMcInputStream = Objects.requireNonNull(
                JdkLongsLowMcPrp.class.getClassLoader().getResourceAsStream(LOW_MC_FILE)
            );
            InputStreamReader lowMcInputStreamReader = new InputStreamReader(lowMcInputStream);
            BufferedReader lowMcBufferedReader = new BufferedReader(lowMcInputStreamReader);
            // 读取线性变换矩阵，共有r组
            LINEAR_MATRICES = new LongSquareDenseBitMatrix[ROUND];
            for (int roundIndex = 0; roundIndex < ROUND; roundIndex++) {
                // 第一行是标识位
                String label = lowMcBufferedReader.readLine();
                assert label.equals(LINEAR_MATRIX_PREFIX + roundIndex);
                // 后面跟着BLOCK_BIT_LENGTH行数据
                byte[][] squareMatrix = new byte[CommonConstants.BLOCK_BIT_LENGTH][];
                for (int bitIndex = 0; bitIndex < CommonConstants.BLOCK_BIT_LENGTH; bitIndex++) {
                    String line = lowMcBufferedReader.readLine();
                    squareMatrix[bitIndex] = Hex.decode(line);
                    assert squareMatrix[bitIndex].length == CommonConstants.BLOCK_BYTE_LENGTH;
                }
                LINEAR_MATRICES[roundIndex] = LongSquareDenseBitMatrix.fromDense(squareMatrix);
            }
            // 读取密钥扩展矩阵，共有r + 1组
            KEY_MATRICES = new LongSquareDenseBitMatrix[ROUND + 1];
            for (int roundIndex = 0; roundIndex < ROUND + 1; roundIndex++) {
                // 第一行是标识位
                String label = lowMcBufferedReader.readLine();
                assert label.equals(KEY_MATRIX_PREFIX + (roundIndex));
                // 后面跟着BLOCK_BIT_LENGTH行数据
                byte[][] squareMatrix = new byte[CommonConstants.BLOCK_BIT_LENGTH][];
                for (int bitIndex = 0; bitIndex < CommonConstants.BLOCK_BIT_LENGTH; bitIndex++) {
                    String line = lowMcBufferedReader.readLine();
                    squareMatrix[bitIndex] = Hex.decode(line);
                    assert squareMatrix[bitIndex].length == CommonConstants.BLOCK_BYTE_LENGTH;
                }
                KEY_MATRICES[roundIndex] = LongSquareDenseBitMatrix.fromDense(squareMatrix);
            }
            // 读取常数，共有r组
            CONSTANTS = new long[ROUND][];
            for (int roundIndex = 0; roundIndex < ROUND; roundIndex++) {
                // 第一行是标识位
                String label = lowMcBufferedReader.readLine();
                assert label.equals(CONSTANT_PREFIX + roundIndex);
                // 第二行是常数
                String line = lowMcBufferedReader.readLine();
                CONSTANTS[roundIndex] = LongUtils.byteArrayToLongArray(Hex.decode(line));
                assert CONSTANTS[roundIndex].length == CommonConstants.BLOCK_LONG_LENGTH;
            }
            lowMcBufferedReader.close();
            lowMcInputStreamReader.close();
            lowMcInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Failed to read LowMc file: " + LOW_MC_FILE);
        }
    }
}
