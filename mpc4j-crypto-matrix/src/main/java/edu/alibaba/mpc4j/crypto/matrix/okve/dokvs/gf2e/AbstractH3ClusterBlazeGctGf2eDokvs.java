package edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.MaxBinSizeUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.H3BlazeGctDovsUtils;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * abstract clustering blazing fast DOKVS using garbled cuckoo table with 3 hash functions.
 *
 * @author Weiran Liu
 * @date 2023/7/11
 */
abstract class AbstractH3ClusterBlazeGctGf2eDokvs<T> extends AbstractGf2eDokvs<T> implements BinaryGf2eDokvs<T> {
    /**
     * number of sparse hashes
     */
    static final int SPARSE_HASH_NUM = AbstractH3GctGf2eDokvs.SPARSE_HASH_NUM;
    /**
     * number of hash keys, one more key for bin
     */
    static final int HASH_KEY_NUM = AbstractH3GctGf2eDokvs.HASH_KEY_NUM + 1;
    /**
     * expected bin size, i.e., m^* = 2^14
     */
    private static final int EXPECT_BIN_SIZE = 1 << 14;

    /**
     * Gets m.
     *
     * @param n number of key-value pairs.
     * @return m.
     */
    static int getM(int n) {
        MathPreconditions.checkPositive("n", n);
        int binNum = CommonUtils.getUnitNum(n, EXPECT_BIN_SIZE);
        int binN = MaxBinSizeUtils.approxMaxBinSize(n, binNum);
        int binLm = H3BlazeGctDovsUtils.getLm(binN);
        int binRm = H3BlazeGctDovsUtils.getRm(binN);
        int binM = binLm + binRm;
        return binNum * binM;
    }

    /**
     * number of bins
     */
    protected final int binNum;
    /**
     * number of key-value pairs in each bin
     */
    protected final int binN;
    /**
     * left m in each bin
     */
    protected final int binLm;
    /**
     * right m in each bin
     */
    protected final int binRm;
    /**
     * m for each bin
     */
    protected final int binM;
    /**
     * bin hash
     */
    protected final Prf binHash;
    /**
     * bins
     */
    protected final ArrayList<H3BlazeGctGf2eDokvs<T>> bins;

    AbstractH3ClusterBlazeGctGf2eDokvs(EnvType envType, int n, int l, byte[][] keys, SecureRandom secureRandom) {
        super(n, getM(n), l, secureRandom);
        // calculate bin_num and bin_size
        binNum = CommonUtils.getUnitNum(n, EXPECT_BIN_SIZE);
        binN = MaxBinSizeUtils.approxMaxBinSize(n, binNum);
        binLm = H3BlazeGctDovsUtils.getLm(binN);
        binRm = H3BlazeGctDovsUtils.getRm(binN);
        binM = binLm + binRm;
        // clone keys
        MathPreconditions.checkEqual("keys.length", "hash_num", keys.length, HASH_KEY_NUM);
        // init bin hash
        binHash = PrfFactory.createInstance(envType, Integer.BYTES);
        binHash.setKey(keys[0]);
        byte[][] cloneKeys = new byte[HASH_KEY_NUM - 1][];
        for (int keyIndex = 0; keyIndex < HASH_KEY_NUM - 1; keyIndex++) {
            cloneKeys[keyIndex] = BytesUtils.clone(keys[keyIndex + 1]);
        }
        // create bins
        Kdf kdf = KdfFactory.createInstance(envType);
        bins = IntStream.range(0, binNum)
            .mapToObj(binIndex -> {
                for (int keyIndex = 0; keyIndex < HASH_KEY_NUM - 1; keyIndex++) {
                    cloneKeys[keyIndex] = kdf.deriveKey(cloneKeys[keyIndex]);
                }
                return new H3BlazeGctGf2eDokvs<T>(envType, binN, l, cloneKeys, secureRandom);
            })
            .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public int maxPositionNum() {
        return SPARSE_HASH_NUM + binNum * binRm;
    }
}
