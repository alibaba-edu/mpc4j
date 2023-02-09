package edu.alibaba.mpc4j.dp.service.fo.de;

import edu.alibaba.mpc4j.dp.service.fo.AbstractFoLdpClient;
import edu.alibaba.mpc4j.dp.service.fo.FoLdpFactory;
import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;

import java.util.Random;

/**
 * Direct Encoding (DE) Frequency Oracle LDP client. The item is encoded via string.
 *
 * @author Weiran Liu
 * @date 2023/1/14
 */
public class DeStringFoLdpClient extends AbstractFoLdpClient {
    /**
     * p = e^ε / (e^ε + d - 1)
     */
    private final double p;
    /**
     * q = 1 / (e^ε + d - 1)
     */
    private final double q;

    public DeStringFoLdpClient(FoLdpConfig config) {
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
            return domain.getIndexItem(randomIndex).getBytes(FoLdpFactory.DEFAULT_CHARSET);
        } else {
            // answer the true item
            return item.getBytes(FoLdpFactory.DEFAULT_CHARSET);
        }
    }
}
