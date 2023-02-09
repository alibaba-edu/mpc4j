package edu.alibaba.mpc4j.dp.service.heavyhitter.hg;

import edu.alibaba.mpc4j.dp.service.heavyhitter.config.HhLdpConfig;
import edu.alibaba.mpc4j.dp.service.heavyhitter.utils.HhLdpServerContext;
import edu.alibaba.mpc4j.dp.service.heavyhitter.utils.EmptyHhLdpServerContext;
import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpServerState;

import java.util.*;

/**
 * Basic HeavyGuardian-based Heavy Hitter LDP server.
 *
 * @author Weiran Liu
 * @date 2022/11/18
 */
public class BasicHgHhLdpServer extends AbstractHgHhLdpServer {
    /**
     * p = e^ε / (e^ε + d - 1)
     */
    private final double p;
    /**
     * q = 1 / (e^ε + d - 1)
     */
    private final double q;

    public BasicHgHhLdpServer(HhLdpConfig config) {
        super(config);
        double expWindowEpsilon = Math.exp(windowEpsilon);
        p = expWindowEpsilon / (expWindowEpsilon + d - 1);
        q = 1 / (expWindowEpsilon + d - 1);
    }

    @Override
    public void stopWarmup() {
        checkState(HhLdpServerState.WARMUP);
        // bias all counts
        for (Map<String, Double> bucket : buckets) {
            for (Map.Entry<String, Double> entry : bucket.entrySet()) {
                String item = entry.getKey();
                double value = entry.getValue();
                value = value * (p - q);
                bucket.put(item, value);
            }
        }
        hhLdpServerState = HhLdpServerState.STATISTICS;
    }

    @Override
    public HhLdpServerContext getServerContext() {
        return new EmptyHhLdpServerContext();
    }

    @Override
    protected double updateCount(int bucketIndex, double count) {
        return count - currentNums[bucketIndex] * q;
    }

    @Override
    protected double debiasCount(int bucketIndex, double count) {
        return updateCount(bucketIndex, count) / (p - q);
    }
}
