package edu.alibaba.mpc4j.s2pc.pso.psi.mqrpmt.gmr21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtConfig;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.gmr21.Gmr21MqRpmtConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pso.psi.mqrpmt.MqRpmtPsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory.PsiType;

/**
 * GMR21-PSI config.
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/08/11
 */
public class Gmr21PsiConfig extends AbstractMultiPartyPtoConfig implements MqRpmtPsiConfig {
    /**
     * mq-RPMT config
     */
    private final MqRpmtConfig mqRpmtConfig;
    /**
     * core COT config
     */
    private final CoreCotConfig coreCotConfig;

    private Gmr21PsiConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.mqRpmtConfig, builder.coreCotConfig);
        mqRpmtConfig = builder.mqRpmtConfig;
        coreCotConfig = builder.coreCotConfig;
    }

    @Override
    public PsiType getPtoType() {
        return PsiType.GMR21;
    }

    @Override
    public MqRpmtConfig getMqRpmtConfig() {
        return mqRpmtConfig;
    }

    @Override
    public CoreCotConfig getCoreCotConfig() {return coreCotConfig; }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Gmr21PsiConfig> {
        /**
         * mq-RPMT config
         */
        private final MqRpmtConfig mqRpmtConfig;
        /**
         * core COT config
         */
        private CoreCotConfig coreCotConfig;

        public Builder(boolean silent) {
            mqRpmtConfig = new Gmr21MqRpmtConfig.Builder(silent).build();
            coreCotConfig = CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setCoreCotConfig(CoreCotConfig coreCotConfig) {
            this.coreCotConfig = coreCotConfig;
            return this;
        }

        @Override
        public Gmr21PsiConfig build() {
            return new Gmr21PsiConfig(this);
        }
    }
}