package edu.alibaba.mpc4j.dp.service.fo.lh;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.hash.IntHash;
import edu.alibaba.mpc4j.common.tool.hash.IntHashFactory;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.dp.service.fo.AbstractFoLdpServer;
import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Binary Local Hash (BLH) Frequency Oracle LDP server. See Section 4.4 of the paper:
 * <p>
 * Wang, Tianhao, and Jeremiah Blocki. Locally differentially private protocols for frequency estimation.
 * USENIX Security 2017.
 * </p>
 * The client-side algorithm is as follows:
 * <p>
 * Let \mathbb{H} be a universal hash function family such that each H ∈ \mathbb{H} outputs a value into one bit. Given
 * the input value v:
 * <p>1. Encode(v) = &lt;H, b&gt;</p>, where H ∈ \mathbb{H} is chosen uniformly at random, and b = H(v).</p>
 * <p>2. Perturb(&lt;H, b&gt;) = (&lt;H, b'&gt;), where Pr[b' = 1] = e^ε / (e^ε + 1) if b = 1, and
 * Pr[b' = 0] = 1 / (e^ε + 1) if b = 0.</p>
 *
 * @author Weiran Liu
 * @date 2023/1/17
 */
public class BlhFoLdpServer extends AbstractFoLdpServer {
    /**
     * q* = 0.5
     */
    private static final double Q_STAR = 0.5;
    /**
     * IntHash
     */
    private final IntHash intHash;
    /**
     * the bucket
     */
    private final int[] budget;
    /**
     * p* = e^ε / (e^ε + 1)
     */
    private final double pStar;

    public BlhFoLdpServer(FoLdpConfig config) {
        super(config);
        double expEpsilon = Math.exp(epsilon);
        pStar = expEpsilon / (expEpsilon + 1);
        intHash = IntHashFactory.fastestInstance();
        // init budget
        budget = new int[d];
    }

    @Override
    public void insert(byte[] itemBytes) {
        MathPreconditions.checkEqual(
            "actual byte length", "expect byte length", itemBytes.length, Integer.BYTES + 1
        );
        byte[] seedBytes = new byte[Integer.BYTES];
        System.arraycopy(itemBytes, 0, seedBytes, 0, seedBytes.length);
        int seed = IntUtils.byteArrayToInt(seedBytes);
        byte byteB = itemBytes[Integer.BYTES];
        assert byteB == 0x00 || byteB == 0x01;
        // each reported ⟨H,b⟩ supports all values that are hashed by H to b, which are half of the input values.
        IntStream.range(0, d)
            .forEach(itemIndex -> {
                byte[] itemIndexBytes = IntUtils.intToByteArray(itemIndex);
                byte itemB = (byte)(Math.abs(intHash.hash(itemIndexBytes, seed)) % 2);
                if (itemB == byteB) {
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
                itemIndex -> (budget[itemIndex] - num * Q_STAR) / (pStar - Q_STAR)
            ));
    }
}
