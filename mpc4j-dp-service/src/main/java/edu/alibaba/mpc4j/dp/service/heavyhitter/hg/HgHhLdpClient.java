package edu.alibaba.mpc4j.dp.service.heavyhitter.hg;

import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpClient;

/**
 * HeavyGuardian-based Heavy Hitter LDP client.
 *
 * @author Weiran Liu
 * @date 2023/1/5
 */
public interface HgHhLdpClient extends HhLdpClient {
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
