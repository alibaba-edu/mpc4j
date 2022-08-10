package edu.alibaba.mpc4j.dp.cdp.numeric.real.unbound;

import com.google.privacy.differentialprivacy.LaplaceNoise;
import edu.alibaba.mpc4j.dp.cdp.CdpConfig;

/**
 * Apache拉普拉斯CDP机制，直接使用谷歌给出的实现方案。源代码参见：
 * <p>
 * https://github.com/google/differential-privacy/
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/4/20
 */
class GoogleLaplaceCdp implements UnboundRealCdp {
    /**
     * 配置项
     */
    private GoogleLaplaceCdpConfig googleLaplaceCdpConfig;
    /**
     * 谷歌拉普拉斯噪声
     */
    private LaplaceNoise laplaceNoise;

    @Override
    public void setup(CdpConfig cdpConfig) {
        assert cdpConfig instanceof GoogleLaplaceCdpConfig;
        googleLaplaceCdpConfig = (GoogleLaplaceCdpConfig) cdpConfig;
        // 设置谷歌Laplace采样器
        laplaceNoise = new LaplaceNoise();
        // 尝试采样一次
        laplaceNoise.addNoise(0.0, 1.0, googleLaplaceCdpConfig.getBaseEpsilon(), null);
    }

    @Override
    public void reseed(long seed) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public CdpConfig getCdpConfig() {
        return googleLaplaceCdpConfig;
    }

    @Override
    public double getEpsilon() {
        // Δf * ε
        return googleLaplaceCdpConfig.getSensitivity() * googleLaplaceCdpConfig.getBaseEpsilon();
    }

    @Override
    public double getDelta() {
        return 0.0;
    }

    @Override
    public double randomize(double value) {
        return laplaceNoise.addNoise(value, 1.0, googleLaplaceCdpConfig.getBaseEpsilon(), null);
    }
}
