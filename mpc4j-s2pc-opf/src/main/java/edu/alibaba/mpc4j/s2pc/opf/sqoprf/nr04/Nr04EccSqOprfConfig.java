package edu.alibaba.mpc4j.s2pc.opf.sqoprf.nr04;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;

/**
 * NR04 ECC single-query OPRF config.
 *
 * @author Qixian Zhou
 * @date 2023/4/12
 */
public class Nr04EccSqOprfConfig extends AbstractMultiPartyPtoConfig implements SqOprfConfig {
    /**
     * use compressed encoding
     */
    private final boolean compressEncode;
    /**
     * COT config
     */
    private final CotConfig cotConfig;

    private Nr04EccSqOprfConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.cotConfig);
        compressEncode = builder.compressEncode;
        cotConfig = builder.cotConfig;
    }

    public CotConfig getCotConfig() {
        return cotConfig;
    }

    @Override
    public SqOprfFactory.SqOprfType getPtoType() {
        return SqOprfFactory.SqOprfType.NR04_ECC;
    }

    public boolean getCompressEncode() {
        return compressEncode;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Nr04EccSqOprfConfig> {
        /**
         * use compressed encoding
         */
        private boolean compressEncode;
        /**
         * COT config
         */
        private CotConfig cotConfig;

        public Builder() {
            compressEncode = true;
            cotConfig = CotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, true);
        }

        public Builder setCompressEncode(boolean compressEncode) {
            this.compressEncode = compressEncode;
            return this;
        }

        public Builder setCotConfig(CotConfig cotConfig) {
            this.cotConfig = cotConfig;
            return this;
        }

        @Override
        public Nr04EccSqOprfConfig build() {
            return new Nr04EccSqOprfConfig(this);
        }
    }
}
