package edu.alibaba.mpc4j.dp.service.fo.lh;

import edu.alibaba.mpc4j.common.tool.hash.IntHash;
import edu.alibaba.mpc4j.common.tool.hash.IntHashFactory;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.dp.service.fo.AbstractFoLdpClient;
import edu.alibaba.mpc4j.dp.service.fo.config.FlhFoLdpConfig;
import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;

import java.nio.ByteBuffer;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * Fast Local Hash Frequency Oracle LDP client. See section 3.4 of the paper:
 * <p>
 * Cormode, Graham, Samuel Maddock, and Carsten Maple. Frequency estimation under local differential privacy. VLDB 2021.
 * </p>
 * The basic idea is as follows:
 * <p>
 * On the client-side, instead of sampling a hash function uniformly at random from some universal hash family, we
 * introduce a new parameter k′ and restrict clients to uniformly choosing from k′ hash functions. Hence, we sacrifice
 * some theoretical guarantees on accuracy in order to achieve computational gains on the server-side aggregation.
 * </p>
 * The client-side algorithm is described as follows:
 * <p>
 * On the client-side, we sample a hash function uniformly at random from {h_1, ..., h_{k'}}.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/2/2
 */
public class FlhFoLdpClient extends AbstractFoLdpClient {
    /**
     * g = e^ε + 1
     */
    private final int g;
    /**
     * g byte length
     */
    private final int gByteLength;
    /**
     * maximum number of candidate hash functions k'
     */
    private final int k;
    /**
     * k byte length
     */
    private final int kByteLength;
    /**
     * pre-compute a k′ × d matrix
     */
    private final int[][] hashMap;
    /**
     * p = e^ε / (e^ε + g - 1)
     */
    private final double p;
    /**
     * q = 1 / (e^ε + g - 1)
     */
    private final double q;

    public FlhFoLdpClient(FoLdpConfig config) {
        super(config);
        FlhFoLdpConfig flhFoLdpConfig = (FlhFoLdpConfig) config;
        double expEpsilon = Math.exp(epsilon);
        // g = e^ε + 1
        g = (int)Math.round(expEpsilon) + 1;
        assert g > 1: "g must be greater than 1: " + g;
        gByteLength = IntUtils.boundedNonNegIntByteLength(g);
        // set k and hash map
        IntHash intHash = IntHashFactory.fastestInstance();
        k = flhFoLdpConfig.getK();
        kByteLength = IntUtils.boundedNonNegIntByteLength(k);
        int[] hashSeeds = flhFoLdpConfig.getHashSeeds();
        hashMap = new int[k][d];
        byte[][] itemIndexBytesArray = IntStream.range(0, d)
            .mapToObj(IntUtils::intToByteArray)
            .toArray(byte[][]::new);
        for (int i = 0; i < k; i++) {
            for (int j = 0; j < d; j++) {
                hashMap[i][j] = Math.abs(intHash.hash(itemIndexBytesArray[j], hashSeeds[i]) % g);
            }
        }
        // p = e^ε / (e^ε + g - 1)
        p = expEpsilon / (expEpsilon + g - 1);
        // q = 1 / (e^ε + g - 1)
        q = 1 / (expEpsilon + g - 1);
    }

    @Override
    public byte[] randomize(String item, Random random) {
        checkItemInDomain(item);
        ByteBuffer byteBuffer = ByteBuffer.allocate(kByteLength + gByteLength);
        // Encode(v) = <H, x>, where H ∈ \mathbb{H} is chosen uniformly at random, and x = H(v).
        int hashIndex = random.nextInt(k);
        byteBuffer.put(IntUtils.boundedNonNegIntToByteArray(hashIndex, k));
        int x = hashMap[hashIndex][domain.getItemIndex(item)];
        // Perturb x to y with probability 1 - e^ε / (e^ε + g - 1)
        double u = random.nextDouble();
        if (u > p - q) {
            x = random.nextInt(g);
        }
        byteBuffer.put(IntUtils.boundedNonNegIntToByteArray(x, g));
        return byteBuffer.array();
    }
}
