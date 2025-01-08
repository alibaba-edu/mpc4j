package edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.GaussianLweParam;
import edu.alibaba.mpc4j.s2pc.pir.cppir.GaussianLwePirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.CpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.CpIdxPirFactory.CpIdxPirType;

/**
 * Double client-specific preprocessing index PIR config.
 *
 * @author Weiran Liu
 * @date 2024/7/8
 */
public class DoubleCpIdxPirConfig extends AbstractMultiPartyPtoConfig implements CpIdxPirConfig, GaussianLwePirConfig {
    /**
     * Gaussian LWE parameter
     */
    private final GaussianLweParam gaussianLweParam;

    public DoubleCpIdxPirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
        gaussianLweParam = builder.gaussianLweParam;
    }

    @Override
    public CpIdxPirType getPtoType() {
        return CpIdxPirType.DOUBLE;
    }

    @Override
    public GaussianLweParam getGaussianLweParam() {
        return gaussianLweParam;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<DoubleCpIdxPirConfig> {
        /**
         * Gaussian LWE parameter
         */
        private final GaussianLweParam gaussianLweParam;

        public Builder() {
            this(GaussianLweParam.N_1024_SIGMA_6_4);
        }

        public Builder(GaussianLweParam gaussianLweParam) {
            this.gaussianLweParam = gaussianLweParam;
        }

        @Override
        public DoubleCpIdxPirConfig build() {
            return new DoubleCpIdxPirConfig(this);
        }
    }
}
