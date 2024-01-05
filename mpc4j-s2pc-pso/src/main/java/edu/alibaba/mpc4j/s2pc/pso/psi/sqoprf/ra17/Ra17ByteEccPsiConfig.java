package edu.alibaba.mpc4j.s2pc.pso.psi.sqoprf.ra17;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.ra17.Ra17ByteEccSqOprfConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory.PsiType;
import edu.alibaba.mpc4j.s2pc.pso.psi.sqoprf.SqOprfPsiConfig;

/**
 * RA17-BYTE_ECC-PSI config.
 *
 * @author Weiran Liu
 * @date 2023/9/10
 */
public class Ra17ByteEccPsiConfig extends AbstractMultiPartyPtoConfig implements SqOprfPsiConfig {
    /**
     * sq-OPRF config
     */
    private final SqOprfConfig sqOprfConfig;
    /**
     * filter type
     */
    private final FilterType filterType;

    private Ra17ByteEccPsiConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.sqOprfConfig);
        sqOprfConfig = builder.sqOprfConfig;
        filterType = builder.filterType;
    }

    @Override
    public PsiType getPtoType() {
        return PsiType.RA17_BYTE_ECC;
    }

    @Override
    public SqOprfConfig getSqOprfConfig() {
        return sqOprfConfig;
    }

    @Override
    public FilterType getFilterType() {
        return filterType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Ra17ByteEccPsiConfig> {
        /**
         * sq-OPRF config
         */
        private final SqOprfConfig sqOprfConfig;
        /**
         * filter type
         */
        private FilterType filterType;

        public Builder() {
            sqOprfConfig = new Ra17ByteEccSqOprfConfig.Builder().build();
            filterType = FilterType.CUCKOO_FILTER;
        }

        public Builder setFilterType(FilterType filterType) {
            this.filterType = filterType;
            return this;
        }

        @Override
        public Ra17ByteEccPsiConfig build() {
            return new Ra17ByteEccPsiConfig(this);
        }
    }
}
