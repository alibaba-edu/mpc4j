package edu.alibaba.mpc4j.s2pc.pso.psi.other.prty20;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotFactory;
import edu.alibaba.mpc4j.s2pc.pso.psi.FilterPsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory.PsiType;

/**
 * PRTY20 PSI config.
 *
 * @author Weiran Liu
 * @date 2023/9/10
 */
public class Prty20PsiConfig extends AbstractMultiPartyPtoConfig implements FilterPsiConfig {
    /**
     * LCOT config
     */
    private final LcotConfig lcotConfig;
    /**
     * PaXoS type
     */
    private final Gf2eDokvsType paxosType;
    /**
     * filter type
     */
    private final FilterType filterType;


    private Prty20PsiConfig(Builder builder) {
        super(builder.securityModel, builder.lcotConfig);
        lcotConfig = builder.lcotConfig;
        paxosType = builder.paxosType;
        filterType = builder.filterType;
    }

    @Override
    public PsiType getPtoType() {
        return PsiType.PRTY20;
    }

    public LcotConfig getLcotConfig() {
        return lcotConfig;
    }

    public Gf2eDokvsType getPaxosType() {return paxosType; }

    @Override
    public FilterType getFilterType() {
        return filterType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Prty20PsiConfig> {
        /**
         * security model
         */
        private final SecurityModel securityModel;
        /**
         * LCOT config
         */
        private final LcotConfig lcotConfig;
        /**
         * PaXoS type
         */
        private Gf2eDokvsType paxosType;
        /**
         * filter type
         */
        private FilterType filterType;

        public Builder(SecurityModel securityModel) {
            this.securityModel = securityModel;
            lcotConfig = LcotFactory.createDefaultConfig(securityModel);
            paxosType = Gf2eDokvsType.H2_TWO_CORE_GCT;
            filterType = FilterType.SET_FILTER;
        }

        public Builder setPaxosType(Gf2eDokvsType paxosType) {
            Preconditions.checkArgument(Gf2eDokvsFactory.isBinary(paxosType));
            this.paxosType = paxosType;
            return this;
        }

        public Builder setFilterType(FilterType filterType) {
            this.filterType = filterType;
            return this;
        }

        @Override
        public Prty20PsiConfig build() {
            return new Prty20PsiConfig(this);
        }
    }
}
