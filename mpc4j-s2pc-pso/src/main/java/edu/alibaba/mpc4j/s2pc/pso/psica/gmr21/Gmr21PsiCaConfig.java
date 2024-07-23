package edu.alibaba.mpc4j.s2pc.pso.psica.gmr21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.gmr21.Gmr21MqRpmtConfig;
import edu.alibaba.mpc4j.s2pc.pso.psica.PsiCaConfig;
import edu.alibaba.mpc4j.s2pc.pso.psica.PsiCaFactory.PsiCaType;

/**
 * GMR21-PSI-CA config.
 *
 * @author Weiran Liu, Liqiang Peng
 * @date 2022/02/15
 */
public class Gmr21PsiCaConfig extends AbstractMultiPartyPtoConfig implements PsiCaConfig {
    /**
     * GMR21-mqRPMT
     */
    private final Gmr21MqRpmtConfig gmr21MqRpmtConfig;

    private Gmr21PsiCaConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.gmr21MqRpmtConfig);
        gmr21MqRpmtConfig = builder.gmr21MqRpmtConfig;
    }

    @Override
    public PsiCaType getPtoType() {
        return PsiCaType.GMR21;
    }

    public Gmr21MqRpmtConfig getGmr21MqRpmtConfig() {
        return gmr21MqRpmtConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Gmr21PsiCaConfig> {
        /**
         * GMR21-mqRPMT
         */
        private final Gmr21MqRpmtConfig gmr21MqRpmtConfig;

        public Builder(boolean silent) {
            gmr21MqRpmtConfig = new Gmr21MqRpmtConfig.Builder(silent).build();
        }

        @Override
        public Gmr21PsiCaConfig build() {
            return new Gmr21PsiCaConfig(this);
        }
    }
}
