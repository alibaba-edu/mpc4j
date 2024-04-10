package edu.alibaba.mpc4j.common.structure.okve.dokvs.zp;

import edu.alibaba.mpc4j.common.structure.okve.dokvs.H3BlazeGctDovsUtils;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.H3ClusterBlazeGctDokvsUtils;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.MaxBinSizeUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * abstract clustering blazing fast DOKVS using garbled cuckoo table with 3 hash functions.
 *
 * @author Weiran Liu
 * @date 2024/3/6
 */
abstract class AbstractH3ClusterBlazeGctZpDokvs<T> extends AbstractZpDokvs<T> {
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
    protected final ArrayList<H3BlazeGctZpDokvs<T>> bins;

    AbstractH3ClusterBlazeGctZpDokvs(EnvType envType, BigInteger p, int n, byte[][] keys, SecureRandom secureRandom) {
        super(envType, p, n, H3ClusterBlazeGctDokvsUtils.getM(n), secureRandom);
        // calculate bin_num and bin_size
        binNum = CommonUtils.getUnitNum(n, H3ClusterBlazeGctDokvsUtils.EXPECT_BIN_SIZE);
        binN = MaxBinSizeUtils.approxMaxBinSize(n, binNum);
        binLm = H3BlazeGctDovsUtils.getLm(binN);
        binRm = H3BlazeGctDovsUtils.getRm(binN);
        binM = binLm + binRm;
        // clone keys
        MathPreconditions.checkEqual("keys.length", "hash_num", keys.length, H3ClusterBlazeGctDokvsUtils.HASH_KEY_NUM);
        // init bin hash
        binHash = PrfFactory.createInstance(envType, Integer.BYTES);
        binHash.setKey(keys[0]);
        byte[][] cloneKeys = new byte[H3ClusterBlazeGctDokvsUtils.HASH_KEY_NUM - 1][];
        for (int keyIndex = 0; keyIndex < H3ClusterBlazeGctDokvsUtils.HASH_KEY_NUM - 1; keyIndex++) {
            cloneKeys[keyIndex] = BytesUtils.clone(keys[keyIndex + 1]);
        }
        // create bins
        Kdf kdf = KdfFactory.createInstance(envType);
        bins = IntStream.range(0, binNum)
            .mapToObj(binIndex -> {
                for (int keyIndex = 0; keyIndex < H3ClusterBlazeGctDokvsUtils.HASH_KEY_NUM - 1; keyIndex++) {
                    cloneKeys[keyIndex] = kdf.deriveKey(cloneKeys[keyIndex]);
                }
                return new H3BlazeGctZpDokvs<T>(envType, p, binN, cloneKeys, secureRandom);
            })
            .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public int maxPositionNum() {
        return H3ClusterBlazeGctDokvsUtils.SPARSE_HASH_NUM + binNum * binRm;
    }
}
