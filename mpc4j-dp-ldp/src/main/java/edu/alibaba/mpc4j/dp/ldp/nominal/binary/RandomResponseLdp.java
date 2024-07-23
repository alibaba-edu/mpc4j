package edu.alibaba.mpc4j.dp.ldp.nominal.binary;

import edu.alibaba.mpc4j.dp.ldp.LdpConfig;

/**
 * random response LDP mechanism.
 *
 * @author Weiran Liu
 * @date 2024/4/26
 */
public class RandomResponseLdp implements BinaryLdp {
    /**
     * config
     */
    private RandomResponseLdpConfig config;
    /**
     * probability of not flipping
     */
    private double p;

    @Override
    public void setup(LdpConfig ldpConfig) {
        assert ldpConfig instanceof RandomResponseLdpConfig;
        config = (RandomResponseLdpConfig) ldpConfig;
        double epsilon = config.getBaseEpsilon();
        double expEpsilon = Math.exp(epsilon);
        p = expEpsilon / (expEpsilon + 1);
    }

    @Override
    public boolean randomize(boolean value) {
        double randomSample = config.getRandom().nextDouble();
        if (randomSample <= p) {
            return value;
        } else {
            return !value;
        }
    }

    @Override
    public double getEpsilon() {
        return config.getBaseEpsilon();
    }

    @Override
    public void reseed(long seed) throws UnsupportedOperationException {
        config.getRandom().setSeed(seed);
    }

    @Override
    public RandomResponseLdpConfig getLdpConfig() {
        return config;
    }
}
