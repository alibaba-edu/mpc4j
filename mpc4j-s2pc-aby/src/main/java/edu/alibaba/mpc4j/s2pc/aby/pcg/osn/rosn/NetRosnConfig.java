package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn;

import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotConfig;

/**
 * Network ROSN config.
 *
 * @author Weiran Liu
 * @date 2024/5/10
 */
public interface NetRosnConfig extends RosnConfig {
    /**
     * Gets COT config.
     *
     * @return COT config.
     */
    CotConfig getCotConfig();

    /**
     * Gets pre-computed COT config.
     *
     * @return pre-computed COT config.
     */
    PreCotConfig getPreCotConfig();
}
