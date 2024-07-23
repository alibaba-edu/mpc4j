package edu.alibaba.mpc4j.s2pc.opf.sqoprf.pssw09;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprp.OprpConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprp.OprpFactory;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfFactory;

/**
 * PSSW09 single-query OPRF config.
 *
 * @author Qixian Zhou
 * @date 2023/4/17
 */
public class Pssw09SqOprfConfig extends AbstractMultiPartyPtoConfig implements SqOprfConfig {
    /**
     * circuit config
     */
    private final Z2cConfig z2cConfig;
    /**
     * OPRP config
     */
    private final OprpConfig oprpConfig;

    private Pssw09SqOprfConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.z2cConfig, builder.oprpConfig);
        z2cConfig = builder.z2cConfig;
        oprpConfig = builder.oprpConfig;
    }

    public Z2cConfig getZ2cConfig() {
        return z2cConfig;
    }

    public OprpConfig getOprpConfig() {
        return oprpConfig;
    }

    @Override
    public SqOprfFactory.SqOprfType getPtoType() {
        return SqOprfFactory.SqOprfType.PSSW09;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Pssw09SqOprfConfig> {
        /**
         * circuit config
         */
        private final Z2cConfig z2cConfig;
        /**
         * OPRP Config
         */
        private OprpConfig oprpConfig;

        public Builder(SecurityModel securityModel) {
            z2cConfig = Z2cFactory.createDefaultConfig(securityModel, true);
            oprpConfig = OprpFactory.createDefaultConfig();
        }

        public Builder setOprpConfig(OprpConfig oprpConfig) {
            this.oprpConfig = oprpConfig;
            return this;
        }

        @Override
        public Pssw09SqOprfConfig build() {
            return new Pssw09SqOprfConfig(this);
        }
    }
}
