package edu.alibaba.mpc4j.dp.service.heavyhitter.hg;

import edu.alibaba.mpc4j.dp.service.heavyhitter.config.HgHhLdpConfig;
import edu.alibaba.mpc4j.dp.service.heavyhitter.config.HhLdpConfig;
import edu.alibaba.mpc4j.dp.service.heavyhitter.utils.HgHhLdpServerContext;
import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpServerState;

import java.util.*;

/**
 * Advanced Hot HeavyGuardian-based Heavy Hitter LDP server.
 *
 * @author Weiran Liu
 * @date 2022/11/19
 */
public class AdvHhgHhLdpServer extends AbstractHgHhLdpServer implements HhgHhLdpServer {
    /**
     * the privacy parameter allocation parameter α
     */
    private final double alpha;
    /**
     * p1 = e^ε_1 / (e^ε_1 + 1)
     */
    protected double p1;
    /**
     * q1 = 1 / (e^ε_1 + 1)
     */
    protected double q1;
    /**
     * p2 = e^ε_2 / (e^ε_2 + λ_h - 1)
     */
    protected double p2;
    /**
     * q2 = 1 / (e^ε_2 + λ_h - 1)
     */
    protected double q2;
    /**
     * γ_h, proportion of hot items
     */
    protected double gammaH;

    public AdvHhgHhLdpServer(HhLdpConfig config) {
        super(config);
        HgHhLdpConfig hgHhLdpConfig = (HgHhLdpConfig) config;
        alpha = hgHhLdpConfig.getAlpha();
        double alphaWindowEpsilon = windowEpsilon * alpha;
        double remainedWindowEpsilon = windowEpsilon - alphaWindowEpsilon;
        // compute p1 and p1
        double expAlphaWindowEpsilon = Math.exp(alphaWindowEpsilon);
        p1 = expAlphaWindowEpsilon / (expAlphaWindowEpsilon + 1);
        q1 = 1 / (expAlphaWindowEpsilon + 1);
        // compute p2 and q2
        double expRemainedWindowEpsilon = Math.exp(remainedWindowEpsilon);
        p2 = expRemainedWindowEpsilon / (expRemainedWindowEpsilon + lambdaH - 1);
        q2 = 1 / (expRemainedWindowEpsilon + lambdaH - 1);
        hhLdpServerState = HhLdpServerState.WARMUP;
        gammaH = 0;
    }

    @Override
    public void stopWarmup() {
        checkState(HhLdpServerState.WARMUP);
        double hotNum = 0;
        for (int budgetIndex = 0; budgetIndex < w; budgetIndex++) {
            Map<String, Double> budget = buckets.get(budgetIndex);
            // bias all counts and calculate λ
            for (Map.Entry<String, Double> entry : budget.entrySet()) {
                String item = entry.getKey();
                double value = entry.getValue();
                hotNum += value;
                value = value * p1 * (p2 - q2);
                budget.put(item, value);
            }
            // note that here the bucket may contain # of elements that is less than lambdaH
        }
        gammaH = hotNum / num;
        assert gammaH >= 0 && gammaH <= 1 : "γ_h must be in range [0, 1]: " + gammaH;
        hhLdpServerState = HhLdpServerState.STATISTICS;
    }

    @Override
    protected double updateCount(int bucketIndex, double count) {
        return count - currentNums[bucketIndex] * (gammaH * p1 * q2 + (1 - gammaH) * q1 / k);
    }

    @Override
    protected double debiasCount(int bucketIndex, double count) {
        return updateCount(bucketIndex, count) / (p1 * (p2 - q2));
    }

    @Override
    public double getAlpha() {
        return alpha;
    }

    @Override
    public HgHhLdpServerContext getServerContext() {
        return new HgHhLdpServerContext(buckets);
    }
}
