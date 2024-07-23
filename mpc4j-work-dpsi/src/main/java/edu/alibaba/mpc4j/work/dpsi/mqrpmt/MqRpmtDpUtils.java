package edu.alibaba.mpc4j.work.dpsi.mqrpmt;

import edu.alibaba.mpc4j.common.sampler.integral.geometric.JdkGeometricSampler;
import edu.alibaba.mpc4j.common.tool.CommonConstants;

import java.security.SecureRandom;

/**
 * DPSI based on mqRPMT utils.
 *
 * @author Feng Han
 * @date 2024/5/22
 */
public class MqRpmtDpUtils {
    /**
     * return the max noise size
     *
     * @param epsilon ε
     * @param delta   δ
     */
    public static int getMaxDummySize(double epsilon, double delta) {
        double expEpsilon = Math.exp(epsilon);
        // η^0 = 1 - ln(δ(e^ε + 1)) / ε
        double eta0 = 1 - Math.log((expEpsilon + 1) * delta) / epsilon;
        // Pr[η > m] = (e^{-ε(m - η^0 - 1)}) / (e^ε + 1) = 2^{-σ}
        int formula = (int) Math.ceil(
            (-1 * Math.log(Math.pow(2, -1 * CommonConstants.STATS_BIT_LENGTH) * (expEpsilon + 1)) / epsilon) + 1 + eta0
        );
        return (formula <= 0) ? 1 : formula;
    }

    /**
     * sample a noise, randomize the input, and return the max(input, noisyInput)
     *
     * @param sensitivity △c
     * @param epsilon     ε
     * @param delta       δ
     * @param value       input
     */
    public static int randomize(int sensitivity, double epsilon, double delta, int value) {
        int mu = (int) (sensitivity - sensitivity * Math.log(delta * Math.exp(epsilon / sensitivity) + delta) / epsilon);
        JdkGeometricSampler discreteGeometricSampler = new JdkGeometricSampler(
            new SecureRandom(), mu, sensitivity / epsilon
        );
        int noiseValue = discreteGeometricSampler.sample() + value;
        return Math.max(value, noiseValue);
    }
}
