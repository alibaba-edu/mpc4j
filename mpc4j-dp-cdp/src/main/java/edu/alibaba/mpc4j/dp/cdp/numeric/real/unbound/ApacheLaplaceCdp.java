package edu.alibaba.mpc4j.dp.cdp.numeric.real.unbound;

import edu.alibaba.mpc4j.common.sampler.real.laplace.ApacheLaplaceSampler;
import edu.alibaba.mpc4j.dp.cdp.CdpConfig;

/**
 * Apache拉普拉斯CDP机制。此实现包括了扩展的近似(ε,δ)-差分隐私机制，由Holohan等人提出。
 * <p>
 * Holohan N, Leith D J, Mason O. Differential privacy in metric spaces: Numerical, categorical and functional data
 * under the one roof. Information Sciences, 2015, 305: 256-268.
 * </p>
 * 源代码参考：
 * <p>
 * https://github.com/IBM/differential-privacy-library/blob/master/diffprivlib/mechanisms/laplace.py
 * </p>
 *
 * @author Weiran Liu, Xiaodong Zhang
 * @date 2022/4/20
 */
class ApacheLaplaceCdp implements UnboundRealCdp {
    /**
     * 配置项
     */
    private ApacheLaplaceCdpConfig apacheLaplaceCdpConfig;
    /**
     * Apache拉普拉斯分布采样器
     */
    private ApacheLaplaceSampler apacheLaplaceSampler;

    @Override
    public void setup(CdpConfig cdpConfig) {
        assert cdpConfig instanceof ApacheLaplaceCdpConfig;
        apacheLaplaceCdpConfig = (ApacheLaplaceCdpConfig) cdpConfig;
        // 计算放缩系数b = 1 / (ε - ln(1 - δ))
        double b = 1.0 / (apacheLaplaceCdpConfig.getBaseEpsilon() - Math.log(1 - apacheLaplaceCdpConfig.getDelta()));
        // 设置Laplace采样器
        apacheLaplaceSampler = new ApacheLaplaceSampler(apacheLaplaceCdpConfig.getRandomGenerator(), 0.0, b);
    }

    @Override
    public void reseed(long seed) throws UnsupportedOperationException {
        apacheLaplaceSampler.reseed(seed);
    }

    @Override
    public CdpConfig getCdpConfig() {
        return apacheLaplaceCdpConfig;
    }

    @Override
    public double getEpsilon() {
        // Δf * ε
        return apacheLaplaceCdpConfig.getSensitivity() * apacheLaplaceCdpConfig.getBaseEpsilon();
    }

    @Override
    public double getDelta() {
        return apacheLaplaceCdpConfig.getDelta();
    }

    @Override
    public double randomize(double value) {
        return apacheLaplaceSampler.sample() + value;
    }
}
