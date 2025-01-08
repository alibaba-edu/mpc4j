package edu.alibaba.mpc4j.s2pc.pir.cppir.ks.simple;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * parameter tests for client-preprocessing keyword PIR using SimpleBin.
 *
 * @author Weiran Liu
 * @date 2024/9/1
 */
@Ignore
public class SimpleCpKsPirParamsTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleCpKsPirParamsTest.class);
    /**
     * byteL
     */
    private static final int[] BYTE_L_ARRAY = new int[]{32, 64, 128, 256};
    /**
     * log(n)
     */
    private static final int[] LOG_N_ARRAY = new int[]{18, 20, 22};

    @Test
    public void testSimpleBinParams() {
        for (int logN : LOG_N_ARRAY) {
            int n = 1 << logN;
            for (int byteL : BYTE_L_ARRAY) {
                int[] result = SimpleBinCpKsPirDesc.getEstimateMatrixSize(n, byteL);
                // rows is Exact MaxBinSize, columns is BinNum
                LOGGER.info("(n, byteL, BinNum, MaxBinSize) = ({}, {}, {}, {})", n, byteL, result[1], result[0]);
            }
        }
    }

    @Test
    public void testMatrixSize() {
        for (int logN : LOG_N_ARRAY) {
            int n = 1 << logN;
            for (int byteL : BYTE_L_ARRAY) {
                int[] simpleNativeMatrixSizes = simpleNaiveMatrixSize(n, byteL);
                LOGGER.info(
                    "SimpleNaive: (n, byteL, rows, columns) = ({}, {}, {}, {})",
                    n, byteL, simpleNativeMatrixSizes[0], simpleNativeMatrixSizes[1]
                );
                int[] simpleBinMatrixSizes = simpleBinEstimateMatrixSize(n, byteL);
                LOGGER.info(
                    "SimpleBin: (n, byteL, rows, columns) = ({}, {}, {}, {})",
                    n, byteL, simpleBinMatrixSizes[0], simpleBinMatrixSizes[1]
                );
                int[] simplePgmMatrixSizes = SimplePgmMatrixSizes(n, byteL);
                LOGGER.info(
                    "SimplePGM: (n, byteL, rows, columns) = ({}, {}, {}, {})",
                    n, byteL, simplePgmMatrixSizes[0], simplePgmMatrixSizes[1]
                );
            }
        }
    }

    private int[] SimplePgmMatrixSizes(int n, int byteL) {
        int[] sizes = SimplePgmCpKsPirDesc.getMatrixSize(n, byteL);
        return new int[]{sizes[0] * sizes[2], sizes[1]};
    }

    /**
     * Gets matrix size for SimpleBin scheme.
     *
     * @param n     n.
     * @param byteL byteL.
     * @return [rows, columns, partition], where rows is Exact MaxBinSize, columns is BinNum.
     */
    private int[] simpleBinEstimateMatrixSize(int n, int byteL) {
        int[] size = SimpleBinCpKsPirDesc.getEstimateMatrixSize(n, byteL);
        return new int[]{size[0] * size[2], size[1]};
    }

    /**
     * Gets matrix size for SimpleNaive scheme.
     *
     * @param n     n.
     * @param byteL byteL.
     * @return [rows, columns, partition].
     */
    private int[] simpleNaiveMatrixSize(int n, int byteL) {
        int[] size = SimpleNaiveCpKsPirDesc.getMatrixSize(n, byteL);
        return new int[]{size[0] * size[2], size[1]};
    }
}
