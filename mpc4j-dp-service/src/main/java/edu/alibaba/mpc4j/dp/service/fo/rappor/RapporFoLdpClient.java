package edu.alibaba.mpc4j.dp.service.fo.rappor;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.hash.IntHash;
import edu.alibaba.mpc4j.common.tool.hash.IntHashFactory;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.dp.service.fo.AbstractFoLdpClient;
import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;
import edu.alibaba.mpc4j.dp.service.fo.config.RapporFoLdpConfig;

import java.nio.ByteBuffer;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * RAPPOR Frequency Oracle LDP client.
 *
 * @author Weiran Liu
 * @date 2023/1/17
 */
public class RapporFoLdpClient extends AbstractFoLdpClient {
    /**
     * number of cohorts.
     */
    private final int cohortNum;
    /**
     * hash seeds
     */
    private final int[][] hashSeeds;
    /**
     * the size of the bloom filter
     */
    private final int m;
    /**
     * the byte size of the bloom filter
     */
    private final int mByteLength;
    /**
     * the IntHash
     */
    private final IntHash intHash;
    /**
     * p = 1 - 0.5 * f
     */
    private final double p;
    /**
     * q = 0.5 * f
     */
    private final double q;

    public RapporFoLdpClient(FoLdpConfig config) {
        super(config);
        RapporFoLdpConfig rapporConfig = (RapporFoLdpConfig)config;
        cohortNum = rapporConfig.getCohortNum();
        hashSeeds = rapporConfig.getHashSeeds();
        m = rapporConfig.getM();
        mByteLength = CommonUtils.getByteLength(m);
        intHash = IntHashFactory.fastestInstance();
        double f = rapporConfig.getF();
        p = 1 - 0.5 * f;
        q = 0.5 * f;
    }

    @Override
    public byte[] randomize(String item, Random random) {
        checkItemInDomain(item);
        // encode
        int cohortIndex = random.nextInt(cohortNum);
        BitVector bloomFilter = BitVectorFactory.createZeros(m);
        int hashNum = hashSeeds[cohortIndex].length;
        MathPreconditions.checkGreaterOrEqual("m", m, hashNum);
        byte[] itemIndexBytes = IntUtils.intToByteArray(domain.getItemIndex(item));
        int[] hashPositions = new int[hashNum];
        for (int hashIndex = 0; hashIndex < hashNum; hashIndex++) {
            hashPositions[hashIndex] = Math.abs(intHash.hash(itemIndexBytes, hashSeeds[cohortIndex][hashIndex]) % m);
        }
        for (int hashPosition : hashPositions) {
            bloomFilter.set(hashPosition, true);
        }
        // randomize
        IntStream.range(0, m).forEach(hashPosition -> {
            double u = random.nextDouble();
            boolean bit = bloomFilter.get(hashPosition);
            if (bit && u > p) {
                bloomFilter.set(hashPosition, false);
            } else if (!bit && u < q) {
                bloomFilter.set(hashPosition, true);
            }
        });
        int cohortIndexByteLength = IntUtils.boundedNonNegIntByteLength(cohortNum);
        byte[] cohortIndexBytes = IntUtils.boundedNonNegIntToByteArray(cohortIndex, cohortNum);
        return ByteBuffer.allocate(mByteLength + cohortIndexByteLength)
            .put(bloomFilter.getBytes())
            .put(cohortIndexBytes)
            .array();
    }
}
