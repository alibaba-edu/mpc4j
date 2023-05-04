package edu.alibaba.mpc4j.dp.service.fo.lh;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.hash.IntHash;
import edu.alibaba.mpc4j.common.tool.hash.IntHashFactory;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.dp.service.fo.AbstractFoLdpServer;
import edu.alibaba.mpc4j.dp.service.fo.config.FlhFoLdpConfig;
import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;

import java.util.Map;
import java.util.stream.Collectors;
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
 * The server-side algorithm is described as follows:
 * <p>
 * On the server-side, we pre-compute a k′ × d matrix where each row corresponds to a hash function and each column
 * refers to a domain value. We set the (i, j)-th entry to h_i(j) to reduce the total number of hash function calls
 * from O(nd) to O(k'd). The server collates all reports for the same hash function, and process them in a batch.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/2/2
 */
public class FlhFoLdpServer extends AbstractFoLdpServer {
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
     * the bucket
     */
    private final int[] budget;
    /**
     * p* = e^ε / (e^ε + g - 1)
     */
    private final double pStar;
    /**
     * q* = 1 / g
     */
    private final double qStar;

    public FlhFoLdpServer(FoLdpConfig config) {
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
        pStar = expEpsilon / (expEpsilon + g - 1);
        // q^* = 1 / g
        qStar = 1.0 / g;
        // init budget
        budget = new int[d];
    }

    @Override
    public void insert(byte[] itemBytes) {
        MathPreconditions.checkEqual(
            "actual byte length", "expect byte length", itemBytes.length, kByteLength + gByteLength
        );
        byte[] hashIndexBytes = new byte[kByteLength];
        System.arraycopy(itemBytes, 0, hashIndexBytes, 0, hashIndexBytes.length);
        int hashIndex = IntUtils.byteArrayToBoundedNonNegInt(hashIndexBytes, k);
        MathPreconditions.checkNonNegativeInRange("hash index", hashIndex, k);
        byte[] yBytes = new byte[gByteLength];
        System.arraycopy(itemBytes, kByteLength, yBytes, 0, yBytes.length);
        int y = IntUtils.byteArrayToBoundedNonNegInt(yBytes, g);
        MathPreconditions.checkNonNegativeInRange("y", y, g);
        // each reported ⟨H, y⟩ supports all values that are hashed by H to y
        IntStream.range(0, d)
            .forEach(itemIndex -> {
                if (hashMap[hashIndex][itemIndex] == y) {
                    budget[itemIndex]++;
                }
            });
        num++;
    }

    @Override
    public Map<String, Double> estimate() {
        return IntStream.range(0, d)
            .boxed()
            .collect(Collectors.toMap(
                domain::getIndexItem,
                itemIndex -> (budget[itemIndex] - num * qStar) / (pStar - qStar)
            ));
    }
}
