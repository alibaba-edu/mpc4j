package edu.alibaba.mpc4j.common.sampler.binary.bernoulli;

import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.util.Precision;

/**
 * 应用Apache的采样工具实现的伯努利采样。
 *
 * @author Weiran Liu
 * @date 2021/03/02
 */
public class ApacheBernoulliSampler implements BernoulliSampler {
    /**
     * 随机数生成器
     */
    private final RandomGenerator randomGenerator;
    /**
     * 取值为1的概率
     */
    private final double p;

    public ApacheBernoulliSampler(double p) {
        this(new JDKRandomGenerator(), p);
    }

    public ApacheBernoulliSampler(RandomGenerator randomGenerator, double p) {
        assert p >= 0 && p <= 1 : "p must be in range [0, 1]";
        this.p = p;
        this.randomGenerator = randomGenerator;
    }

    @Override
    public double getP() {
        return p;
    }

    @Override
    public boolean sample() {
        if (Precision.equals(p, 0.0, DoubleUtils.PRECISION)) {
            return false;
        } else if (Precision.equals(p, 1.0, DoubleUtils.PRECISION)) {
            return true;
        }
        double u = randomGenerator.nextDouble();

        return u <= p;
    }

    @Override
    public void reseed(long seed) {
        randomGenerator.setSeed(seed);
    }

    @Override
    public String toString() {
        return "(p = " + p + ")-" + getClass().getSimpleName();
    }
}
