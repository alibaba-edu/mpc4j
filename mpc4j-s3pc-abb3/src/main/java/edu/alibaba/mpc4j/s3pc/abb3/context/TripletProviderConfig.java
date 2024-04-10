package edu.alibaba.mpc4j.s3pc.abb3.context;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.cr.S3pcCrProviderConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.RpMtProviderFactory;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.RpZ2MtpConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.RpLongMtpConfig;

/**
 * configure of provider for 3p sharing
 *
 * @author Feng Han
 * @date 2024/01/08
 */
public class TripletProviderConfig extends AbstractMultiPartyPtoConfig {
    /**
     * z2 mtp config
     */
    private final S3pcCrProviderConfig crProviderConfig;
    /**
     * z2 mtp config
     */
    private final RpZ2MtpConfig rpZ2MtpConfig;
    /**
     * zl64 mtp config
     */
    private final RpLongMtpConfig rpLongMtpConfig;

    public TripletProviderConfig(Builder builder) {
        super(builder.isMalicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST);
        crProviderConfig = builder.crProviderConfig;
        rpZ2MtpConfig = builder.rpZ2MtpConfig;
        rpLongMtpConfig = builder.rpLongMtpConfig;
    }

    public S3pcCrProviderConfig getCrProviderConfig() {
        return crProviderConfig;
    }

    public RpZ2MtpConfig getRpZ2MtpConfig() {
        return rpZ2MtpConfig;
    }

    public RpLongMtpConfig getRpZl64MtpConfig() {
        return rpLongMtpConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<TripletProviderConfig> {
        /**
         * whether the current protocol is malicious secure
         */
        private final boolean isMalicious;
        /**
         * z2 mtp config
         */
        private final S3pcCrProviderConfig crProviderConfig;
        /**
         * z2 mtp config
         */
        private RpZ2MtpConfig rpZ2MtpConfig;
        /**
         * zl64 mtp config
         */
        private RpLongMtpConfig rpLongMtpConfig;

        public Builder(boolean isMalicious) {
            this.isMalicious = isMalicious;
            crProviderConfig = new S3pcCrProviderConfig.Builder().build();
            if(isMalicious){
                rpZ2MtpConfig = RpMtProviderFactory.createDefaultZ2MtpConfig();
                rpLongMtpConfig = RpMtProviderFactory.createDefaultZl64MtpConfig();
            }else{
                rpZ2MtpConfig = null;
                rpLongMtpConfig = null;
            }
        }

        public Builder setRpZ2MtpConfig(RpZ2MtpConfig rpZ2MtpConfig) {
            assert isMalicious;
            this.rpZ2MtpConfig = rpZ2MtpConfig;
            return this;
        }

        public Builder setRpZl64MtpConfig(RpLongMtpConfig rpLongMtpConfig) {
            assert isMalicious;
            this.rpLongMtpConfig = rpLongMtpConfig;
            return this;
        }

        @Override
        public TripletProviderConfig build() {
            return new TripletProviderConfig(this);
        }
    }
}
