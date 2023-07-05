package edu.alibaba.mpc4j.dp.service.fo.ue;

import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.dp.service.fo.AbstractFoLdpClient;
import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;

import java.util.Random;
import java.util.stream.IntStream;

/**
 * Symmetric Unary Encoding (SUE) Frequency Oracle LDP client.
 *
 * @author Weiran Liu
 * @date 2023/1/16
 */
public class SueFoLdpClient extends AbstractFoLdpClient {
    /**
     * p = e^(ε/2) / (e^(ε/2) - 1)
     */
    private final double p;
    /**
     * q = 1 / (e^(ε/2) - 1)
     */
    private final double q;

    public SueFoLdpClient(FoLdpConfig config) {
        super(config);
        double expHalfEpsilon = Math.exp(epsilon / 2);
        p = expHalfEpsilon / (expHalfEpsilon + 1);
        q = 1 / (expHalfEpsilon + 1);
    }

    @Override
    public byte[] randomize(String item, Random random) {
        checkItemInDomain(item);
        int itemIndex = domain.getItemIndex(item);
        // Encode(v) = [0,...,0,1,0,...,0], a length-d binary vector where only the v-th position is 1.
        BitVector v = BitVectorFactory.createZeros(d);
        IntStream.range(0, d).forEach(bitIndex -> {
            double sample = random.nextDouble();
            if (bitIndex == itemIndex) {
                if (sample < p) {
                    // Pr[B'[i] = 1] = p, if B[i] = 1
                    v.set(bitIndex, true);
                }
            } else {
                if (sample < q) {
                    // Pr[B'[i] = 1] = q, if B[i] = 0
                    v.set(bitIndex, true);
                }
            }
        });
        return v.getBytes();
    }
}
