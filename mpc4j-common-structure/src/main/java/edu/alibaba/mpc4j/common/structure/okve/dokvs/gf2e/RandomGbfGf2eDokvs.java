package edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;

import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Random Garbled Bloom Filter (GBF) DOKVS. The original scheme is described in the following paper:
 * <p>
 * Dong C, Chen L, Wen Z. When private set intersection meets big data: an efficient and scalable protocol. CCS 2013.
 * ACM, 2013 pp. 789-800.
 * </p>
 * In this implementation, we randomly generate positions (may be duplicated) in the Garbled Bloom Filter.
 *
 * @author Weiran Liu
 * @date 2023/7/10
 */
public class RandomGbfGf2eDokvs<T> extends AbstractGbfGf2eDokvs<T> {

    RandomGbfGf2eDokvs(EnvType envType, int n, int l, byte[] key) {
        super(envType, n, l, key, new SecureRandom());
    }

    RandomGbfGf2eDokvs(EnvType envType, int n, int l, byte[] key, SecureRandom secureRandom) {
        super(envType, n, l, key, secureRandom);
    }

    @Override
    public int[] sparsePositions(T key) {
        byte[] keyBytes = ObjectUtils.objectToByteArray(key);
        return Arrays.stream(IntUtils.byteArrayToIntArray(hash.getBytes(keyBytes)))
            .map(hi -> Math.abs(hi % m))
            .distinct()
            .toArray();
    }

    @Override
    public Gf2eDokvsFactory.Gf2eDokvsType getType() {
        return Gf2eDokvsFactory.Gf2eDokvsType.RANDOM_GBF;
    }
}
