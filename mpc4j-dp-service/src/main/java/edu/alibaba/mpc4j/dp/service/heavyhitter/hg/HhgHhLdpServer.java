package edu.alibaba.mpc4j.dp.service.heavyhitter.hg;

/**
 * Hot HeavyGuardian-based Heavy Hitter LDP client.
 *
 * @author Weiran Liu
 * @date 2022/11/20
 */
public interface HhgHhLdpServer extends HgHhLdpServer {
    /**
     * Get the value of α.
     *
     * @return the value of α.
     */
    double getAlpha();
}
