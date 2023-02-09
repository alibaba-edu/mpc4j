package edu.alibaba.mpc4j.dp.service.fo.de;

import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.dp.service.fo.AbstractFoLdpClient;
import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;

import java.util.Random;

/**
 * Direct Encoding (DE) Frequency Oracle LDP server. The item is encoded via index.
 *
 * @author Weiran Liu
 * @date 2023/1/16
 */
public class DeIndexFoLdpClient extends AbstractFoLdpClient {
    /**
     * p = e^ε / (e^ε + d - 1)
     */
    private final double p;
    /**
     * q = 1 / (e^ε + d - 1)
     */
    private final double q;

    public DeIndexFoLdpClient(FoLdpConfig config) {
        super(config);
        double expEpsilon = Math.exp(epsilon);
        p = expEpsilon / (expEpsilon + d - 1);
        q = 1 / (expEpsilon + d - 1);
    }

    @Override
    public byte[] randomize(String item, Random random) {
        checkItemInDomain(item);
        double randomSample = random.nextDouble();
        // Randomly sample an integer in [0, d)
        int randomIndex = random.nextInt(d);
        if (randomSample > p - q) {
            // answer a random item
            return IntUtils.boundedNonNegIntToByteArray(randomIndex, d);
        } else {
            // answer the true item
            return IntUtils.boundedNonNegIntToByteArray(domain.getItemIndex(item), d);
        }
    }
}
