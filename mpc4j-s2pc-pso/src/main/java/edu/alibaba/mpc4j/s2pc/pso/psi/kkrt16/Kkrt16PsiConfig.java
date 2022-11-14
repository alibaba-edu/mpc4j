package edu.alibaba.mpc4j.s2pc.pso.psi.kkrt16;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.filter.FilterFactory;
import edu.alibaba.mpc4j.common.tool.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfConfig;
import edu.alibaba.mpc4j.s2pc.pso.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory;

/**
 * KKRT16-PSI协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/9/19
 */
public class Kkrt16PsiConfig implements PsiConfig {
    /**
     * OPRF配置项
     */
    private final OprfConfig oprfConfig;
    /**
     * 布谷鸟哈希类型
     */
    private final CuckooHashBinType cuckooHashBinType;
    /**
     * 过滤器类型
     */
    private final FilterType filterType;

    private Kkrt16PsiConfig(Builder builder) {
        oprfConfig = builder.oprfConfig;
        cuckooHashBinType = builder.cuckooHashBinType;
        filterType = builder.filterType;
    }

    @Override
    public PsiFactory.PsiType getPtoType() {
        return PsiFactory.PsiType.KKRT16;
    }

    @Override
    public void setEnvType(EnvType envType) {
        oprfConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return oprfConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.SEMI_HONEST;
        if (oprfConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = oprfConfig.getSecurityModel();
        }
        return securityModel;
    }

    public OprfConfig getOprfConfig() {
        return oprfConfig;
    }

    public CuckooHashBinType getCuckooHashBinType() {
        return cuckooHashBinType;
    }

    public FilterType getFilterType() {
        return filterType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Kkrt16PsiConfig> {
        /**
         * OPRF类型
         */
        private OprfConfig oprfConfig;
        /**
         * 布谷鸟哈希类型
         */
        private CuckooHashBinType cuckooHashBinType;
        /**
         * 过滤器类型
         */
        private FilterFactory.FilterType filterType;

        public Builder() {
            oprfConfig = OprfFactory.createOprfDefaultConfig(SecurityModel.SEMI_HONEST);
            cuckooHashBinType = CuckooHashBinType.NAIVE_3_HASH;
            filterType = FilterType.SET_FILTER;
        }

        public Builder setOprfConfig(OprfConfig oprfConfig) {
            this.oprfConfig = oprfConfig;
            return this;
        }

        public Builder setCuckooHashBinType(CuckooHashBinType cuckooHashBinType) {
            this.cuckooHashBinType = cuckooHashBinType;
            return this;
        }

        public Builder setFilterType(FilterType filterType) {
            this.filterType = filterType;
            return this;
        }

        @Override
        public Kkrt16PsiConfig build() {
            return new Kkrt16PsiConfig(this);
        }
    }
}
