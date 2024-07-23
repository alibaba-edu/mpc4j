package edu.alibaba.mpc4j.s2pc.opf.mqrpmt.czz24;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtConfig;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtFactory.MqRpmtType;

/**
 * CZZ24 cwOPRF-based mqRPMT config.
 *
 * @author Weiran Liu
 * @date 2022/9/10
 */
public class Czz24CwOprfMqRpmtConfig extends AbstractMultiPartyPtoConfig implements MqRpmtConfig {
    /**
     * filter type
     */
    private final FilterType filterType;

    private Czz24CwOprfMqRpmtConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST);
        filterType = builder.filterType;
    }

    @Override
    public MqRpmtType getPtoType() {
        return MqRpmtType.CZZ24_CW_OPRF;
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

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Czz24CwOprfMqRpmtConfig> {
        /**
         * filter type
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
        public Czz24CwOprfMqRpmtConfig build() {
            return new Czz24CwOprfMqRpmtConfig(this);
        }
    }
}
