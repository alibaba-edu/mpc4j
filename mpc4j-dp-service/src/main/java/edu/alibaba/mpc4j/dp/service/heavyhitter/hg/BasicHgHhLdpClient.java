package edu.alibaba.mpc4j.dp.service.heavyhitter.hg;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.HhLdpConfig;
import edu.alibaba.mpc4j.dp.service.heavyhitter.utils.HhLdpServerContext;
import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpFactory;
import edu.alibaba.mpc4j.dp.service.heavyhitter.utils.EmptyHhLdpServerContext;

import java.util.Random;

/**
 * Basic HeavyGuardian-based Heavy Hitter LDP client.
 *
 * @author Weiran Liu
 * @date 2023/1/5
 */
public class BasicHgHhLdpClient extends AbstractHgHhLdpClient {
    /**
     * the universal domain size d, i.e., |Ω|.
     */
    private final int d;
    /**
     * p = e^ε / (e^ε + d - 1)
     */
    private final double p;
    /**
     * q = 1 / (e^ε + d - 1)
     */
    private final double q;

    public BasicHgHhLdpClient(HhLdpConfig config) {
        super(config);
        double expWindowEpsilon = Math.exp(windowEpsilon);
        d = bucketDomain.getUniversalD();
        p = expWindowEpsilon / (expWindowEpsilon + d - 1);
        q = 1 / (expWindowEpsilon + d - 1);
    }

    @Override
    public byte[] randomize(HhLdpServerContext serverContext, String item, Random random) {
        Preconditions.checkArgument(serverContext instanceof EmptyHhLdpServerContext);
        checkItemInDomain(item);
        // basic HeavyGuardian solution does not consider the current data structure
        double randomSample = random.nextDouble();
        // Randomly sample an integer in [0, d)
        int randomIndex = random.nextInt(d);
        if (randomSample > p - q) {
            // answer a random item
            return bucketDomain.getUniversalIndexItem(randomIndex).getBytes(HhLdpFactory.DEFAULT_CHARSET);
        } else {
            // answer the true item
            return item.getBytes(HhLdpFactory.DEFAULT_CHARSET);
        }
    }
}
