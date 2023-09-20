package edu.alibaba.mpc4j.s2pc.pso.psi.sqoprf;

import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.FilterPsiConfig;

/**
 * sq-OPRF-based PSI config.
 *
 * @author Weiran Liu
 * @date 2023/9/10
 */
public interface SqOprfPsiConfig extends FilterPsiConfig {
    /**
     * Gets sq-OPRF config.
     *
     * @return sq-OPRF config.
     */
    SqOprfConfig getSqOprfConfig();
}
