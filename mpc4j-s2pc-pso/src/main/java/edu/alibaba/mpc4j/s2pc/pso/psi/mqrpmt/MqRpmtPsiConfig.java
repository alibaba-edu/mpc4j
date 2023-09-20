package edu.alibaba.mpc4j.s2pc.pso.psi.mqrpmt;

import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiConfig;

/**
 * mq-RPMT-based PSI config.
 *
 * @author Weiran Liu
 * @date 2023/9/9
 */
public interface MqRpmtPsiConfig extends PsiConfig {
    /**
     * Gets mq-RPMT config.
     *
     * @return mq-RPMT config.
     */
    MqRpmtConfig getMqRpmtConfig();

    /**
     * Gets core COT config.
     *
     * @return core COT config.
     */
    CoreCotConfig getCoreCotConfig();
}
