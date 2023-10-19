package edu.alibaba.mpc4j.s2pc.pso.psi;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory.PsiType;

/**
 * PSI protocol configure
 *
 * @author Weiran Liu
 * @date 2022/9/19
 */
public interface PsiConfig extends MultiPartyPtoConfig {
    /**
     * Gets protocol type.
     *
     * @return protocol type.
     */
    PsiType getPtoType();
}
