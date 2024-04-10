package edu.alibaba.mpc4j.s3pc.abb3.basic;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.conversion.replicate.Aby3ConvConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.conversion.replicate.Aby3ConvFactory;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.replicate.Aby3Z2cConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate.TripletRpLongConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.shuffle.replicate.Aby3ShuffleConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.shuffle.replicate.Aby3ShuffleFactory;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.replicate.Aby3Z2cFactory;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate.TripletRpLongCpFactory;
import edu.alibaba.mpc4j.s3pc.abb3.context.TripletProviderConfig;

/**
 * Configure for three zl64c party in replicated-sharing form
 *
 * @author Feng Han
 * @date 2024/01/08
 */
public class Abb3RpConfig extends AbstractMultiPartyPtoConfig implements Abb3Config {
    /**
     * the triplet context config
     */
    private final TripletProviderConfig tripletProviderConfig;
    /**
     * the directory to buffer the unverified and tuples
     */
    private final Aby3Z2cConfig z2cConfig;
    /**
     * the maximum number of bytes in each buffer vectors
     */
    private final TripletRpLongConfig zLongCpConfig;
    /**
     * the maximum number of bytes in each buffer vectors
     */
    private final TripletRpLongConfig macConfig;
    /**
     * the maximum number of memoryBuffer vectors
     */
    private final Aby3ConvConfig convConfig;
    /**
     * the maximum number of vectors can be verified at once
     */
    private final Aby3ShuffleConfig shuffleConfig;

    private Abb3RpConfig(Builder builder) {
        super(builder.malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST);
        tripletProviderConfig = builder.tripletProviderConfig;
        z2cConfig = builder.z2cConfig;
        zLongCpConfig = builder.zLongCpConfig;
        macConfig = builder.macConfig;
        convConfig = builder.convConfig;
        shuffleConfig = builder.shuffleConfig;
    }

    public TripletProviderConfig getTripletProviderConfig() {
        return tripletProviderConfig;
    }

    @Override
    public TripletRpLongConfig getZl64cConfig() {
        return zLongCpConfig;
    }

    @Override
    public Aby3Z2cConfig getZ2cConfig() {
        return z2cConfig;
    }

    @Override
    public Aby3ConvConfig getConvConfig() {
        return convConfig;
    }

    @Override
    public Aby3ShuffleConfig getShuffleConfig() {
        return shuffleConfig;
    }

    public TripletRpLongConfig getMacConfig() {
        return macConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Abb3RpConfig> {
        /**
         * whether malicious secure or not
         */
        private final boolean malicious;
        /**
         * the triplet context config
         */
        private TripletProviderConfig tripletProviderConfig;
        /**
         * the directory to buffer the unverified and tuples
         */
        private Aby3Z2cConfig z2cConfig;
        /**
         * the maximum number of bytes in each buffer vectors
         */
        private TripletRpLongConfig zLongCpConfig;
        /**
         * the maximum number of bytes in each buffer vectors
         */
        private TripletRpLongConfig macConfig;
        /**
         * the maximum number of memoryBuffer vectors
         */
        private Aby3ConvConfig convConfig;
        /**
         * the maximum number of vectors can be verified at once
         */
        private Aby3ShuffleConfig shuffleConfig;

        public Builder(boolean malicious, boolean defaultUseMac) {
            this.malicious = malicious;
            SecurityModel securityModel = malicious ? SecurityModel.MALICIOUS :SecurityModel.SEMI_HONEST;
            tripletProviderConfig = new TripletProviderConfig.Builder(malicious).build();
            z2cConfig = Aby3Z2cFactory.createDefaultConfig(securityModel);
            if (defaultUseMac){
                assert malicious;
                zLongCpConfig = TripletRpLongCpFactory.createDefaultMacConfig(securityModel);
                macConfig = zLongCpConfig;
            }else{
                zLongCpConfig = TripletRpLongCpFactory.createDefaultConfig(securityModel);
                macConfig = malicious ? TripletRpLongCpFactory.createDefaultMacConfig(securityModel) : null;
            }
            convConfig = Aby3ConvFactory.createDefaultConfig(securityModel);
            shuffleConfig = Aby3ShuffleFactory.createDefaultConfig(securityModel);
        }

        public Builder setTripletProviderConfig(TripletProviderConfig tripletProviderConfig) {
            this.tripletProviderConfig = tripletProviderConfig;
            return this;
        }

        public Builder setZ2cConfig(Aby3Z2cConfig z2cConfig) {
            this.z2cConfig = z2cConfig;
            return this;
        }

        public Builder setZl64cConfig(TripletRpLongConfig zLongCpConfig) {
            this.zLongCpConfig = zLongCpConfig;
            return this;
        }

        public Builder setMacConfig(TripletRpLongConfig macConfig) {
            this.macConfig = macConfig;
            return this;
        }

        public Builder setConvConfig(Aby3ConvConfig convConfig) {
            this.convConfig = convConfig;
            return this;
        }

        public Builder setShuffleConfig(Aby3ShuffleConfig shuffleConfig) {
            this.shuffleConfig = shuffleConfig;
            return this;
        }

        @Override
        public Abb3RpConfig build() {
            return new Abb3RpConfig(this);
        }
    }
}
