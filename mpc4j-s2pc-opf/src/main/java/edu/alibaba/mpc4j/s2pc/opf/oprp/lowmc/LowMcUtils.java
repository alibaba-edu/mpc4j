package edu.alibaba.mpc4j.s2pc.opf.oprp.lowmc;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.LongDenseBitMatrix;
import edu.alibaba.mpc4j.common.tool.crypto.prp.JdkLongsLowMcPrp;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import org.bouncycastle.util.encoders.Hex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;

/**
 * LowMc utilities.
 *
 * @author Weiran Liu
 * @date 2022/02/11
 */
class LowMcUtils {
    /**
     * private constructor.
     */
    private LowMcUtils() {
        // empty
    }

    /**
     * LowMC config fire path
     */
    static final String LOW_MC_FILE = "low_mc/lowmc_128_128_20.txt";
    /**
     * prefix string of linear transform matrix
     */
    static final String LINEAR_MATRIX_PREFIX = "L_";
    /**
     * prefix string of key extension matrix
     */
    static final String KEY_MATRIX_PREFIX = "K_";
    /**
     * prefix string of constant
     */
    static final String CONSTANT_PREFIX = "C_";
    /**
     * size = 128
     */
    private static final int SIZE = CommonConstants.BLOCK_BIT_LENGTH;
    /**
     * byte size = 16
     */
    private static final int BYTE_SIZE = CommonConstants.BLOCK_BYTE_LENGTH;
    /**
     * long size = 2
     */
    private static final int LONG_SIZE = CommonConstants.BLOCK_LONG_LENGTH;
    /**
     * number of sboxes
     */
    static final int SBOX_NUM = 10;
    /**
     * number of rounds
     */
    static final int ROUND = 20;
    /**
     * r + 1 key extension matrices, each contains 128 × 128 bits.
     */
    static final LongDenseBitMatrix[] KEY_MATRICES;
    /**
     * r linear transform matrices, each contains 128 × 128 bits.
     */
    static final LongDenseBitMatrix[] LINEAR_MATRICES;
    /**
     * r constants, each contains 128 bits
     */
    static final long[][] CONSTANTS;

    static {
        // open LowMc config file
        try {
            InputStream lowMcInputStream = Objects.requireNonNull(
                JdkLongsLowMcPrp.class.getClassLoader().getResourceAsStream(LOW_MC_FILE)
            );
            InputStreamReader lowMcInputStreamReader = new InputStreamReader(lowMcInputStream);
            BufferedReader lowMcBufferedReader = new BufferedReader(lowMcInputStreamReader);
            // read r linear transform matrices
            LINEAR_MATRICES = new LongDenseBitMatrix[ROUND];
            for (int roundIndex = 0; roundIndex < ROUND; roundIndex++) {
                // the first line is a flag
                String label = lowMcBufferedReader.readLine();
                assert label.equals(LINEAR_MATRIX_PREFIX + roundIndex);
                // the following 128 lines are 128-bit data
                byte[][] squareMatrix = new byte[SIZE][];
                for (int bitIndex = 0; bitIndex < SIZE; bitIndex++) {
                    String line = lowMcBufferedReader.readLine();
                    squareMatrix[bitIndex] = Hex.decode(line);
                    assert squareMatrix[bitIndex].length == BYTE_SIZE;
                }
                LINEAR_MATRICES[roundIndex] = LongDenseBitMatrix.createFromDense(SIZE, squareMatrix);
            }
            // read r + 1 key extension matrices
            KEY_MATRICES = new LongDenseBitMatrix[ROUND + 1];
            for (int roundIndex = 0; roundIndex < ROUND + 1; roundIndex++) {
                // the first line is a flag
                String label = lowMcBufferedReader.readLine();
                assert label.equals(KEY_MATRIX_PREFIX + (roundIndex));
                // the following 128 lines are 128-bit data
                byte[][] squareMatrix = new byte[SIZE][];
                for (int bitIndex = 0; bitIndex < SIZE; bitIndex++) {
                    String line = lowMcBufferedReader.readLine();
                    squareMatrix[bitIndex] = Hex.decode(line);
                    assert squareMatrix[bitIndex].length == BYTE_SIZE;
                }
                KEY_MATRICES[roundIndex] = LongDenseBitMatrix.createFromDense(SIZE, squareMatrix);
            }
            // read r constants
            CONSTANTS = new long[ROUND][];
            for (int roundIndex = 0; roundIndex < ROUND; roundIndex++) {
                // the first line is a flag
                String label = lowMcBufferedReader.readLine();
                assert label.equals(CONSTANT_PREFIX + roundIndex);
                // the following line is 128-bit data
                String line = lowMcBufferedReader.readLine();
                CONSTANTS[roundIndex] = LongUtils.byteArrayToLongArray(Hex.decode(line));
                assert CONSTANTS[roundIndex].length == LONG_SIZE;
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
