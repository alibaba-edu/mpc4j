package edu.alibaba.mpc4j.dp.service.fo.lh;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.hash.IntHash;
import edu.alibaba.mpc4j.common.tool.hash.IntHashFactory;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.dp.service.fo.AbstractFoLdpClient;
import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;
import edu.alibaba.mpc4j.dp.service.fo.config.OlhFoLdpConfig;

import java.nio.ByteBuffer;
import java.util.Random;

/**
 * Optimal Local Hash (OLH) Frequency Oracle LDP client. See section 4.5 of the paper:
 * <p>
 * Wang, Tianhao, and Jeremiah Blocki. Locally differentially private protocols for frequency estimation.
 * USENIX Security 2017.
 * </p>
 * The client-side algorithm is as follows:
 * <p>
 * Let \mathbb{H} be a universal hash function family such that each H ∈ \mathbb{H} outputs a value in [g], where
 * g = e^ε + 1. Given the input value v:
 * <p>1. Encode(v) = &lt;H, x&gt;</p>, where H ∈ \mathbb{H} is chosen uniformly at random, and x = H(v).</p>
 * <p>2. Perturb(&lt;H, x&gt;) = (&lt;H, y&gt;), where ∀ i ∈ [g], Pr[y = i] = e^ε / (e^ε + g - 1) if x = i, and
 * Pr[y = i] = 1 / (e^ε + g - 1) for x ≠ i.</p>
 *
 * @author Weiran Liu
 * @date 2023/2/2
 */
public class OlhFoLdpClient extends AbstractFoLdpClient {
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
     * p = e^ε / (e^ε + g - 1)
     */
    private final double p;
    /**
     * q = 1 / (e^ε + g - 1)
     */
    private final double q;

    public OlhFoLdpClient(FoLdpConfig config) {
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
        // p = e^ε / (e^ε + g - 1)
        p = expEpsilon / (expEpsilon + g - 1);
        // q = 1 / (e^ε + g - 1)
        q = 1 / (expEpsilon + g - 1);
        intHash = IntHashFactory.fastestInstance();
    }

    @Override
    public byte[] randomize(String item, Random random) {
        checkItemInDomain(item);
        ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.BYTES + gByteLength);
        // Encode(v) = <H, x>, where H ∈ \mathbb{H} is chosen uniformly at random, and x = H(v).
        int seed = random.nextInt();
        byteBuffer.putInt(seed);
        byte[] itemIndexBytes = IntUtils.intToByteArray(domain.getItemIndex(item));
        int x = Math.abs(intHash.hash(itemIndexBytes, seed) % g);
        // Perturb x to y with probability 1 - e^ε / (e^ε + g - 1)
        double u = random.nextDouble();
        if (u > p - q) {
            x = random.nextInt(g);
        }
        byteBuffer.put(IntUtils.boundedNonNegIntToByteArray(x, g));
        return byteBuffer.array();
    }
}
