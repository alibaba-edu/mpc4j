package edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.aprr24;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.F23SowOprfConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.F23SowOprfFactory.F23SowOprfType;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.F23WprfMatrixFactory.F23WprfMatrixType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.bea95.Bea95PreCotConfig;

/**
 * APRR24 (F2, F3)-sowOPRF with core COT config.
 *
 * @author Weiran Liu
 * @date 2024/10/22
 */
public class Aprr24F23SowOprfConfig extends AbstractMultiPartyPtoConfig implements F23SowOprfConfig {
    /**
     * core COT config
     */
    private final CoreCotConfig coreCotConfig;
    /**
     * COT config
     */
    private final CotConfig cotConfig;
    /**
     * pre-computed COT config
     */
    private final PreCotConfig preCotConfig;
    /**
     * matrix type
     */
    private final F23WprfMatrixType matrixType;

    private Aprr24F23SowOprfConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.coreCotConfig);
        coreCotConfig = builder.coreCotConfig;
        cotConfig = builder.cotConfig;
        preCotConfig = builder.preCotConfig;
        matrixType = builder.matrixType;
    }

    public CoreCotConfig getCoreCotConfig() {
        return coreCotConfig;
    }

    public CotConfig getCotConfig() {
        return cotConfig;
    }

    public PreCotConfig getPreCotConfig() {
        return preCotConfig;
    }

    @Override
    public F23SowOprfType getPtoType() {
        return F23SowOprfType.APRR24;
    }

    @Override
    public F23WprfMatrixType getMatrixType() {
        return matrixType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Aprr24F23SowOprfConfig> {
        /**
         * core COT config
         */
        private final CoreCotConfig coreCotConfig;
        /**
         * COT config
         */
        private final CotConfig cotConfig;
        /**
         * pre-computed COT config
         */
        private final PreCotConfig preCotConfig;
        /**
         * matrix type
         */
        private F23WprfMatrixType matrixType;

        public Builder(boolean silent) {
            coreCotConfig = CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            cotConfig = CotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            preCotConfig = new Bea95PreCotConfig.Builder().build();
            matrixType = F23WprfMatrixType.LONG;
        }

        public Builder setMatrixType(F23WprfMatrixType matrixType) {
            this.matrixType = matrixType;
            return this;
        }

        @Override
        public Aprr24F23SowOprfConfig build() {
            return new Aprr24F23SowOprfConfig(this);
        }
    }
}
