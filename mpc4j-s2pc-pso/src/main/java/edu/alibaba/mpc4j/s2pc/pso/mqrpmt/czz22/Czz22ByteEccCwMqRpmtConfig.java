package edu.alibaba.mpc4j.s2pc.pso.mqrpmt.czz22;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.s2pc.pso.mqrpmt.MqRpmtConfig;
import edu.alibaba.mpc4j.s2pc.pso.mqrpmt.MqRpmtFactory;

/**
 * ZZL22-字节椭圆曲线mqRPMT协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/9/10
 */
public class Czz22ByteEccCwMqRpmtConfig implements MqRpmtConfig {
    /**
     * 过滤器类型
     */
    private final FilterType filterType;
    /**
     * 环境类型
     */
    private EnvType envType;

    private Czz22ByteEccCwMqRpmtConfig(Builder builder) {
        filterType = builder.filterType;
        envType = EnvType.STANDARD;
    }

    @Override
    public MqRpmtFactory.MqRpmtType getPtoType() {
        return MqRpmtFactory.MqRpmtType.CZZ22_BYTE_ECC_CW;
    }

    @Override
    public void setEnvType(EnvType envType) {
        this.envType = envType;
    }

    @Override
    public EnvType getEnvType() {
        return envType;
    }

    @Override
    public SecurityModel getSecurityModel() {
        return SecurityModel.SEMI_HONEST;
    }

    public FilterType getFilterType() {
        return filterType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Czz22ByteEccCwMqRpmtConfig> {
        /**
         * 过滤器类型
         */
        private FilterType filterType;

        public Builder() {
            filterType = FilterType.SET_FILTER;
        }

        public Builder setFilterType(FilterType filterType) {
            this.filterType = filterType;
            return this;
        }

        @Override
        public Czz22ByteEccCwMqRpmtConfig build() {
            return new Czz22ByteEccCwMqRpmtConfig(this);
        }
    }
}
