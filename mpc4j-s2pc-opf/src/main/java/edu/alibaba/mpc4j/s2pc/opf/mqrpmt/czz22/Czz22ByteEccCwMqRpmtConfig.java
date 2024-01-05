package edu.alibaba.mpc4j.s2pc.opf.mqrpmt.czz22;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtConfig;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtFactory;

/**
 * ZZL22-字节椭圆曲线mqRPMT协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/9/10
 */
public class Czz22ByteEccCwMqRpmtConfig extends AbstractMultiPartyPtoConfig implements MqRpmtConfig {
    /**
     * 过滤器类型
     */
    private final FilterType filterType;

    private Czz22ByteEccCwMqRpmtConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST);
        filterType = builder.filterType;
    }

    @Override
    public MqRpmtFactory.MqRpmtType getPtoType() {
        return MqRpmtFactory.MqRpmtType.CZZ22_BYTE_ECC_CW;
    }

    @Override
    public int getVectorLength(int serverElementSize, int clientElementSize) {
        MathPreconditions.checkGreater("server_element_size", serverElementSize, 1);
        MathPreconditions.checkGreater("client_element_size", clientElementSize, 1);
        return serverElementSize;
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
