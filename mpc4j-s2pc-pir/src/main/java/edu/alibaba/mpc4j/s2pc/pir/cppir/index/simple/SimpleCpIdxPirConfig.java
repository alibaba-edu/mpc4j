package edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.GaussianLweParam;
import edu.alibaba.mpc4j.s2pc.pir.cppir.GaussianLwePirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.CpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.CpIdxPirFactory.CpIdxPirType;

/**
 * Simple client-specific preprocessing index PIR config.
 *
 * @author Liqiang Peng
 * @date 2023/9/18
 */
public class SimpleCpIdxPirConfig extends AbstractMultiPartyPtoConfig implements CpIdxPirConfig, GaussianLwePirConfig {
    /**
     * Gaussian LWE parameter
     */
    private final GaussianLweParam gaussianLweParam;

    public SimpleCpIdxPirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
        gaussianLweParam = builder.gaussianLweParam;
    }

    @Override
    public CpIdxPirType getPtoType() {
        return CpIdxPirType.SIMPLE;
    }

    @Override
    public GaussianLweParam getGaussianLweParam() {
        return gaussianLweParam;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<SimpleCpIdxPirConfig> {
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
        public SimpleCpIdxPirConfig build() {
            return new SimpleCpIdxPirConfig(this);
        }
    }
}
