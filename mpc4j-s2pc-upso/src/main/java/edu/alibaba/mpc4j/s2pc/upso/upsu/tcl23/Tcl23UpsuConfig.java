package edu.alibaba.mpc4j.s2pc.upso.upsu.tcl23;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.opf.pmpeqt.PmPeqtConfig;
import edu.alibaba.mpc4j.s2pc.opf.pmpeqt.PmPeqtFactory;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.ra17.Ra17ByteEccSqOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.ra17.Ra17EccSqOprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.upso.upsu.UpsuConfig;

import static edu.alibaba.mpc4j.s2pc.upso.upsu.UpsuFactory.UpsuType;

/**
 * TCL23 UPSU config.
 *
 * @author Liqiang Peng
 * @date 2024/3/7
 */
public class Tcl23UpsuConfig extends AbstractMultiPartyPtoConfig implements UpsuConfig {
    /**
     * single-query OPRF config
     */
    private final SqOprfConfig sqOprfConfig;
    /**
     * Permute Matrix PEQT config
     */
    private final PmPeqtConfig pmPeqtConfig;
    /**
     * core COT config
     */
    private final CoreCotConfig coreCotConfig;

    public Tcl23UpsuConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.sqOprfConfig, builder.pmPeqtConfig, builder.coreCotConfig);
        sqOprfConfig = builder.sqOprfConfig;
        pmPeqtConfig = builder.pmPeqtConfig;
        coreCotConfig = builder.coreCotConfig;
    }

    @Override
    public UpsuType getPtoType() {
        return UpsuType.TCL23;
    }

    /**
     * get single-query OPRF config.
     *
     * @return single-query OPRF config.
     */
    public SqOprfConfig getSqOprfConfig() {
        return sqOprfConfig;
    }

    /**
     * get Permute Matrix PEQT config.
     *
     * @return Permute Matrix PEQT config.
     */
    public PmPeqtConfig getPmPeqtConfig() {
        return pmPeqtConfig;
    }

    /**
     * get core COT config.
     *
     * @return core COT config.
     */
    public CoreCotConfig getCoreCotConfig() {
        return coreCotConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Tcl23UpsuConfig> {
        /**
         * single-query OPRF config
         */
        private SqOprfConfig sqOprfConfig;
        /**
         * pm-PEQT
         */
        private PmPeqtConfig pmPeqtConfig;
        /**
         * core COT config
         */
        private CoreCotConfig coreCotConfig;

        public Builder() {
            sqOprfConfig = SqOprfFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            pmPeqtConfig = PmPeqtFactory.createPmPeqtDefaultConfig(SecurityModel.SEMI_HONEST);
            coreCotConfig = CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setSqOprfConfig(SqOprfConfig sqOprfConfig) {
            this.sqOprfConfig = sqOprfConfig;
            return this;
        }

        public Builder setPmPeqtConfig(PmPeqtConfig pmPeqtConfig) {
            this.pmPeqtConfig = pmPeqtConfig;
            return this;
        }

        public Builder setCoreCotConfig(CoreCotConfig coreCotConfig) {
            this.coreCotConfig = coreCotConfig;
            return this;
        }

        @Override
        public Tcl23UpsuConfig build() {
            return new Tcl23UpsuConfig(this);
        }
    }
}
