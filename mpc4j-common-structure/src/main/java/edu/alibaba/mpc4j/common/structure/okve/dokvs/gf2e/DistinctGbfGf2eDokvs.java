package edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;

import java.security.SecureRandom;

/**
 * Distinct Garbled Bloom Filter (GBF) DOKVS. The original scheme is described in the following paper:
 * <p>
 * Dong C, Chen L, Wen Z. When private set intersection meets big data: an efficient and scalable protocol. CCS 2013.
 * ACM, 2013 pp. 789-800.
 * </p>
 * In this implementation, we require that any inputs have constant distinct positions in the Garbled Bloom Filter.
 * This requirement is used in the following paper:
 * <p>
 * Lepoint, Tancrede, Sarvar Patel, Mariana Raykova, Karn Seth, and Ni Trieu. Private join and compute from PIR with
 * default. ASIACRYPT 2021, Part II, pp. 605-634. Cham: Springer International Publishing, 2021.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/7/3
 */
public class DistinctGbfGf2eDokvs<T> extends AbstractGbfGf2eDokvs<T> implements SparseConstantGf2eDokvs<T> {

    public DistinctGbfGf2eDokvs(EnvType envType, int n, int l, byte[] key) {
        super(envType, n, l, key, new SecureRandom());
    }

    public DistinctGbfGf2eDokvs(EnvType envType, int n, int l, byte[] key, SecureRandom secureRandom) {
        super(envType, n, l, key, secureRandom);
    }

    @Override
    public int[] sparsePositions(T key) {
        byte[] keyBytes = ObjectUtils.objectToByteArray(key);
        int[] hashes = IntUtils.byteArrayToIntArray(hash.getBytes(keyBytes));
        // we now use the method provided in VOLE-PSI to get distinct hash indexes
        for (int j = 0; j < SPARSE_HASH_NUM; j++) {
            // hj = r % (m - j)
            int modulus = m - j;
            int hj = Math.abs(hashes[j] % modulus);
            int i = 0;
            int end = j;
            // for each previous hi <= hj, we set hj = hj + 1.
            while (i != end) {
                if (hashes[i] <= hj) {
                    hj++;
                } else {
                    break;
                }
                i++;
            }
            // now we now that all hi > hj, we place the value
            while (i != end) {
                hashes[end] = hashes[end - 1];
                end--;
            }
            hashes[i] = hj;
        }
        return hashes;
    }

    @Override
    public int sparsePositionNum() {
        return SPARSE_HASH_NUM;
    }

    @Override
    public Gf2eDokvsFactory.Gf2eDokvsType getType() {
        return Gf2eDokvsFactory.Gf2eDokvsType.DISTINCT_GBF;
    }
}
