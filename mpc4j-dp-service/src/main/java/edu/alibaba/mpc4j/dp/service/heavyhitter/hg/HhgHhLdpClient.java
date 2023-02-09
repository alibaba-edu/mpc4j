package edu.alibaba.mpc4j.dp.service.heavyhitter.hg;

/**
 * Hot HeavyGuardian-based Heavy Hitter LDP server.
 *
 * @author Weiran Liu
 * @date 2023/1/5
 */
public interface HhgHhLdpClient extends HgHhLdpClient {
    /**
     * Get the value of α.
     *
     * @return the value of α.
     */
    double getAlpha();
}
