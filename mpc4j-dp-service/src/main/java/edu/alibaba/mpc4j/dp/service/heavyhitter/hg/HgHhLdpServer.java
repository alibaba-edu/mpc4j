package edu.alibaba.mpc4j.dp.service.heavyhitter.hg;

import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpServer;

/**
 * HeavyGuardian-based Heavy Hitter LDP server.
 *
 * @author Weiran Liu
 * @date 2022/11/20
 */
public interface HgHhLdpServer extends HhLdpServer {
    /**
     * Return the bucket num w.
     *
     * @return the bucket num w.
     */
    int getW();

    /**
     * Return the cell num λ_h in the heavy part.
     *
     * @return the cell num λ_h in the heavy part.
     */
    int getLambdaH();
}
