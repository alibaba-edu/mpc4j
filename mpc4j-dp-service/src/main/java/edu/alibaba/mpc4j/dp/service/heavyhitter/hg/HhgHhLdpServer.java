package edu.alibaba.mpc4j.dp.service.heavyhitter.hg;

import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpServer;

/**
 * Hot Heavy Hitter LDP server.
 *
 * @author Weiran Liu
 * @date 2023/3/21
 */
public interface HhgHhLdpServer extends HhLdpServer {
    /**
     * Gets γ_h.
     *
     * @return γ_h.
     */
    double getGammaH();
}
