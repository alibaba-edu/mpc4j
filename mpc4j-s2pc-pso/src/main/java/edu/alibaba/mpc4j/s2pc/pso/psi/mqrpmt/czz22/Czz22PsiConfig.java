package edu.alibaba.mpc4j.s2pc.pso.psi.mqrpmt.czz22;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtConfig;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.czz24.Czz24CwOprfMqRpmtConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.mqrpmt.MqRpmtPsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory.PsiType;

/**
 * CZZ22-PSI config.
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/08/11
 */
public class Czz22PsiConfig extends AbstractMultiPartyPtoConfig implements MqRpmtPsiConfig {
    /**
     * mq-RPMT config
     */
    private final MqRpmtConfig mqRpmtConfig;

    private Czz22PsiConfig(Czz22PsiConfig.Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.mqRpmtConfig);
        mqRpmtConfig = builder.mqRpmtConfig;
    }

    @Override
    public PsiType getPtoType() {
        return PsiType.CZZ22;
    }

    @Override
    public MqRpmtConfig getMqRpmtConfig() {
        return mqRpmtConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Czz22PsiConfig> {
        /**
         * mq-RPMT config
         */
        private final MqRpmtConfig mqRpmtConfig;

        public Builder() {
            mqRpmtConfig = new Czz24CwOprfMqRpmtConfig.Builder().build();
        }

        @Override
        public Czz22PsiConfig build() {
            return new Czz22PsiConfig(this);
        }
    }
}
