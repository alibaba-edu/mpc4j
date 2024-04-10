package edu.alibaba.mpc4j.common.structure.okve.dokvs;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.hashbin.MaxBinSizeUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

/**
 * clustering blazing fast DOKVS using garbled cuckoo table with 2 hash functions utilities.
 *
 * @author Weiran Liu
 * @date 2024/3/6
 */
public class H2ClusterBlazeGctDokvsUtils {
    /**
     * private constructor
     */
    private H2ClusterBlazeGctDokvsUtils() {
        // empty
    }

    /**
     * number of sparse hashes
     */
    public static final int SPARSE_HASH_NUM = H2GctDokvsUtils.SPARSE_HASH_NUM;
    /**
     * number of hash keys, one more key for bin
     */
    public static final int HASH_KEY_NUM = H2GctDokvsUtils.HASH_KEY_NUM + 1;
    /**
     * expected bin size, i.e., m^* = 2^14
     */
    public static final int EXPECT_BIN_SIZE = 1 << 14;

    /**
     * Gets m.
     *
     * @param n number of key-value pairs.
     * @return m.
     */
    public static int getM(int n) {
        MathPreconditions.checkPositive("n", n);
        int binNum = CommonUtils.getUnitNum(n, EXPECT_BIN_SIZE);
        int binN = MaxBinSizeUtils.approxMaxBinSize(n, binNum);
        int binLm = H2BlazeGctDokvsUtils.getLm(binN);
        int binRm = H2BlazeGctDokvsUtils.getRm(binN);
        int binM = binLm + binRm;
        return binNum * binM;
    }
}
