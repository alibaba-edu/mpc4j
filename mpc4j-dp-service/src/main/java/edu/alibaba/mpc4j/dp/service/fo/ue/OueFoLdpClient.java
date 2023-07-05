package edu.alibaba.mpc4j.dp.service.fo.ue;

import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.dp.service.fo.AbstractFoLdpClient;
import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;

import java.util.Random;
import java.util.stream.IntStream;

/**
 * Optimized Unary Encoding (OUE) Frequency Oracle LDP client.
 *
 * @author Weiran Liu
 * @date 2023/1/16
 */
public class OueFoLdpClient extends AbstractFoLdpClient {
    /**
     * p = 1 / 2
     */
    private static final double CONSTANT_P = 1.0 / 2;
    /**
     * q = 1 / (e^ε + 1)
     */
    private final double q;

    public OueFoLdpClient(FoLdpConfig config) {
        super(config);
        q = 1 / (Math.exp(epsilon) + 1);
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
                if (sample < CONSTANT_P) {
                    // if B[i] = 1, Pr[B'[i] = 1] = p
                    v.set(bitIndex, true);
                }
            } else {
                if (sample < q) {
                    // if B[i] = 0, Pr[B'[i] = 1] = q
                    v.set(bitIndex, true);
                }
            }
        });
        return v.getBytes();
    }
}
