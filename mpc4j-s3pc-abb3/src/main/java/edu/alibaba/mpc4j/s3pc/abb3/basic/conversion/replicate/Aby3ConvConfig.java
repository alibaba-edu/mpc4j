package edu.alibaba.mpc4j.s3pc.abb3.basic.conversion.replicate;

import edu.alibaba.mpc4j.common.circuit.z2.adder.AdderFactory.AdderTypes;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.conversion.ConvConfig;

/**
 * Replicated-sharing type conversion party config.
 *
 * @author Feng Han
 * @date 2024/01/18
 */
public class Aby3ConvConfig extends AbstractMultiPartyPtoConfig implements ConvConfig {
    /**
     * which type of adder should be used
     */
    private final AdderTypes adderType;

    private Aby3ConvConfig(Builder builder) {
        super(builder.malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST);
        adderType = builder.adderType;
    }

    public AdderTypes getAdderType() {
        return adderType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Aby3ConvConfig> {
        /**
         * whether malicious secure or not
         */
        private final boolean malicious;
        /**
         * which type of adder should be used
         */
        private AdderTypes adderType;

        public Builder(boolean malicious) {
            this.malicious = malicious;
            adderType = AdderTypes.BRENT_KUNG;
        }

        public void setAdderType(AdderTypes adderType) {
            this.adderType = adderType;
        }

        @Override
        public Aby3ConvConfig build() {
            return new Aby3ConvConfig(this);
        }
    }
}
