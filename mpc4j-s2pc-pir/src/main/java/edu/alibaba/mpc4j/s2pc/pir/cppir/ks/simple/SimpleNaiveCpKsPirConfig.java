package edu.alibaba.mpc4j.s2pc.pir.cppir.ks.simple;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.GaussianLweParam;
import edu.alibaba.mpc4j.s2pc.pir.cppir.GaussianLwePirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.CpKsPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.CpKsPirFactory;

/**
 * Simple naive client-specific preprocessing KSPIR protocol description.
 *
 * @author Liqiang Peng
 * @date 2024/8/2
 */
public class SimpleNaiveCpKsPirConfig extends AbstractMultiPartyPtoConfig implements CpKsPirConfig, GaussianLwePirConfig {
    /**
     * Gaussian LWE parameter
     */
    private final GaussianLweParam gaussianLweParam;

    public SimpleNaiveCpKsPirConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST);
        gaussianLweParam = builder.gaussianLweParam;
    }

    @Override
    public CpKsPirFactory.CpKsPirType getPtoType() {
        return CpKsPirFactory.CpKsPirType.SIMPLE_NAIVE;
    }

    @Override
    public GaussianLweParam getGaussianLweParam() {
        return gaussianLweParam;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<SimpleNaiveCpKsPirConfig> {
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
        public SimpleNaiveCpKsPirConfig build() {
            return new SimpleNaiveCpKsPirConfig(this);
        }
    }
}
