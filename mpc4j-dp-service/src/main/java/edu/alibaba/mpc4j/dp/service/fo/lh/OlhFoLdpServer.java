package edu.alibaba.mpc4j.dp.service.fo.lh;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.hash.IntHash;
import edu.alibaba.mpc4j.common.tool.hash.IntHashFactory;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.dp.service.fo.AbstractFoLdpServer;
import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;
import edu.alibaba.mpc4j.dp.service.fo.config.OlhFoLdpConfig;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Optimal Local Hash (OLH) Frequency Oracle LDP server. See section 4.5 of the paper:
 * <p>
 * Wang, Tianhao, and Jeremiah Blocki. Locally differentially private protocols for frequency estimation.
 * USENIX Security 2017.
 * </p>
 * The server-side algorithm is that:
 * <p>
 * Support(&lt;H, y&gt;) = {i | H(v) = i}, that is, each reported &lt;H, y&gt; supports all values that are hashed by
 * H to y.
 * </p>
 * When p* = 1/2 and q* = 1 / (e^ε + 1), we have the optimal variance.
 *
 * @author Weiran Liu
 * @date 2023/2/2
 */
public class OlhFoLdpServer extends AbstractFoLdpServer {
    /**
     * g = e^ε + 1
     */
    private final int g;
    /**
     * g byte length
     */
    private final int gByteLength;
    /**
     * IntHash
     */
    private final IntHash intHash;
    /**
     * the bucket
     */
    private final int[] budget;
    /**
     * p^* = e^ε / (e^ε + g - 1)
     */
    private final double pStar;
    /**
     * q* = 1 / g
     */
    private final double qStar;

    public OlhFoLdpServer(FoLdpConfig config) {
        super(config);
        Preconditions.checkArgument(
            config instanceof OlhFoLdpConfig,
            "config must be an instance of %s", OlhFoLdpConfig.class.getSimpleName()
        );
        double expEpsilon = Math.exp(epsilon);
        // g = e^ε + 1
        g = (int)Math.round(expEpsilon) + 1;
        assert g > 1: "g must be greater than 1: " + g;
        gByteLength = IntUtils.boundedNonNegIntByteLength(g);
        // p* = e^ε / (e^ε + g - 1)
        pStar = expEpsilon / (expEpsilon + g - 1);
        // q* = 1 / g
        qStar = 1.0 / g;
        intHash = IntHashFactory.fastestInstance();
        // init budget
        budget = new int[d];
    }

    @Override
    public void insert(byte[] itemBytes) {
        MathPreconditions.checkEqual(
            "actual byte length", "expect byte length", itemBytes.length, Integer.BYTES + gByteLength
        );
        byte[] seedBytes = new byte[Integer.BYTES];
        System.arraycopy(itemBytes, 0, seedBytes, 0, seedBytes.length);
        int seed = IntUtils.byteArrayToInt(seedBytes);
        byte[] yBytes = new byte[gByteLength];
        System.arraycopy(itemBytes, Integer.BYTES, yBytes, 0, yBytes.length);
        int y = IntUtils.byteArrayToBoundedNonNegInt(yBytes, g);
        MathPreconditions.checkNonNegativeInRange("y", y, g);
        // each reported ⟨H, y⟩ supports all values that are hashed by H to y
        IntStream.range(0, d)
            .forEach(itemIndex -> {
                byte[] itemIndexBytes = IntUtils.intToByteArray(itemIndex);
                int hv = Math.abs(intHash.hash(itemIndexBytes, seed) % g);
                if (hv == y) {
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
