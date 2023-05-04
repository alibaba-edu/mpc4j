package edu.alibaba.mpc4j.dp.service.heavyhitter.config;

/**
 * Hot HeavyGuardian-based Heavy Hitter LDP config.
 *
 * @author Weiran Liu
 * @date 2023/3/21
 */
public interface HhgHhLdpConfig extends HgHhLdpConfig {
    /**
     * Gets the privacy allocation parameter α.
     *
     * @return the privacy allocation parameter α.
     */
    double getAlpha();

    /**
     * Gets γ_h. It can be negative if we do not manually set.
     *
     * @return γ_h.
     */
    double getGammaH();
}
