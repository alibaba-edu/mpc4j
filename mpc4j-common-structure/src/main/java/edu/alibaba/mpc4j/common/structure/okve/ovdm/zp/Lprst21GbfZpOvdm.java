package edu.alibaba.mpc4j.common.structure.okve.ovdm.zp;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.Zp;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpFactory;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * LPRST21 Garbled Bloom Filter (GBF) OVDM in Zp. The original scheme is described in the following paper:
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
 * @date 2023/5/7
 */
class Lprst21GbfZpOvdm<T> implements SparseZpOvdm<T> {
    /**
     * Garbled Bloom Filter needs 40 hashes
     */
    static int HASH_NUM = CommonConstants.STATS_BIT_LENGTH;

    /**
     * Gets m for the given n.
     *
     * @param n number of key-value pairs.
     * @return m.
     */
    static int getM(int n) {
        assert n > 0;
        // m = n / ln(2) * σ, flooring so that m % Byte.SIZE = 0.
        return CommonUtils.getByteLength((int) Math.ceil(n * CommonConstants.STATS_BIT_LENGTH / Math.log(2))) * Byte.SIZE;
    }

    /**
     * number of key-value pairs
     */
    protected final int n;
    /**
     * OVDM length, with {@code m % Byte.SIZE == 0}.
     */
    protected final int m;
    /**
     * hashes
     */
    private final Prf[] hashes;
    /**
     * Zp
     */
    protected final Zp zp;
    /**
     * the random state
     */
    protected final SecureRandom secureRandom;

    public Lprst21GbfZpOvdm(EnvType envType, BigInteger prime, int n, byte[][] keys) {
        zp = ZpFactory.createInstance(envType, prime);
        MathPreconditions.checkPositive("n", n);
        this.n = n;
        m = getM(n);
        MathPreconditions.checkEqual("keys.length", "hashNum", keys.length, HASH_NUM);
        hashes = IntStream.range(0, HASH_NUM)
            .mapToObj(hashIndex -> {
                Prf hash = PrfFactory.createInstance(envType, Integer.BYTES);
                hash.setKey(keys[hashIndex]);
                return hash;
            })
            .toArray(Prf[]::new);
        secureRandom = new SecureRandom();
    }

    @Override
    public int[] sparsePositions(T key) {
        byte[] keyBytes = ObjectUtils.objectToByteArray(key);
        int[] sparsePositions = new int[HASH_NUM];
        Set<Integer> positionSet = new HashSet<>(HASH_NUM);
        sparsePositions[0] = hashes[0].getInteger(0, keyBytes, m);
        positionSet.add(sparsePositions[0]);
        // generate k distinct positions
        for (int i = 1; i < HASH_NUM; i++) {
            int hiIndex = 0;
            do {
                sparsePositions[i] = hashes[i].getInteger(hiIndex, keyBytes, m);
                hiIndex++;
            } while (positionSet.contains(sparsePositions[i]));
            positionSet.add(sparsePositions[i]);
        }
        return sparsePositions;
    }

    @Override
    public int sparsePositionNum() {
        return HASH_NUM;
    }

    @Override
    public boolean[] densePositions(T key) {
        return new boolean[0];
    }

    @Override
    public int maxDensePositionNum() {
        return 0;
    }

    @Override
    public BigInteger[] encode(Map<T, BigInteger> keyValueMap) throws ArithmeticException {
        // we allow insert <= n elements.
        MathPreconditions.checkLessOrEqual("key-value pairs num", keyValueMap.size(), n);
        keyValueMap.values().forEach(value -> Preconditions.checkArgument(zp.validateElement(value)));
        Set<T> keySet = keyValueMap.keySet();
        // compute positions for all keys, create shares.
        BigInteger[] storage = new BigInteger[m];
        for (T key : keySet) {
            BigInteger finalShare = keyValueMap.get(key);
            int[] sparsePositions = sparsePositions(key);
            int emptySlot = -1;
            for (int position : sparsePositions) {
                if (storage[position] == null && emptySlot == -1) {
                    // if we find an empty position, reserve the location for finalShare）
                    emptySlot = position;
                } else if (storage[position] == null) {
                    // if the current position is null, generate a new share
                    storage[position] = zp.createRandom(secureRandom);
                    finalShare = zp.sub(finalShare, storage[position]);
                } else {
                    // if the current position is not null, reuse the share
                    finalShare = zp.sub(finalShare, storage[position]);
                }
            }
            if (emptySlot == -1) {
                // we cannot find an empty position, which happens with probability 1 - 2^{-λ}
                throw new ArithmeticException("Failed to encode Key-Value Map, cannot find empty slot");
            }
            storage[emptySlot] = finalShare;
        }
        // pad random elements in all empty positions.
        for (int i = 0; i < m; i++) {
            if (storage[i] == null) {
                storage[i] = zp.createRandom(secureRandom);
            }
        }
        return storage;
    }

    @Override
    public BigInteger decode(BigInteger[] storage, T key) {
        // here we do not verify bit length for each storage, otherwise decode would require O(n) computation.
        MathPreconditions.checkEqual("storage.length", "m", storage.length, getM());
        int[] sparsePositions = sparsePositions(key);
        BigInteger value = zp.createZero();
        for (int position : sparsePositions) {
            value = zp.add(value, storage[position]);
        }
        return value;
    }

    @Override
    public int getN() {
        return n;
    }

    @Override
    public int getM() {
        return m;
    }

    @Override
    public ZpOvdmFactory.ZpOvdmType getZpOvdmType() {
        return ZpOvdmFactory.ZpOvdmType.LPRST21_GBF;
    }

    @Override
    public int getNegLogFailureProbability() {
        return CommonConstants.STATS_BIT_LENGTH;
    }
}
