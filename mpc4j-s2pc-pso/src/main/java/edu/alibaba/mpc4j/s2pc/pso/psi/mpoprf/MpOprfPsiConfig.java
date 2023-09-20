package edu.alibaba.mpc4j.s2pc.pso.psi.mpoprf;

import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.FilterPsiConfig;

/**
 * mp-OPRF-based PSI config.
 *
 * @author Weiran Liu
 * @date 2023/9/10
 */
public interface MpOprfPsiConfig extends FilterPsiConfig {
    /**
     * Gets mp-OPRF config.
     *
     * @return mp-OPRF config.
     */
    MpOprfConfig getMpOprfConfig();
}
