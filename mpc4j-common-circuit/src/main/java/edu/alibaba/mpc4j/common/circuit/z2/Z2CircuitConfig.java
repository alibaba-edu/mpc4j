package edu.alibaba.mpc4j.common.circuit.z2;

import edu.alibaba.mpc4j.common.circuit.CircuitConfig;
import edu.alibaba.mpc4j.common.circuit.z2.adder.AdderFactory;
import edu.alibaba.mpc4j.common.circuit.z2.multiplier.MultiplierFactory;
import edu.alibaba.mpc4j.common.circuit.z2.psorter.PsorterFactory;
import edu.alibaba.mpc4j.common.circuit.z2.psorter.PsorterFactory.SorterTypes;
import edu.alibaba.mpc4j.common.circuit.z2.sorter.SorterFactory;

/**
 * Z2 Integer Circuit Config.
 *
 * @author Li Peng
 * @date 2023/6/2
 */
public class Z2CircuitConfig implements CircuitConfig {
    /**
     * Adder type.
     */
    private AdderFactory.AdderTypes adderType;
    /**
     * Multiplier type.
     */
    private MultiplierFactory.MultiplierTypes multiplierType;
    /**
     * Sorter type.
     */
    private SorterFactory.SorterTypes sorterType;
    /**
     * Permutable Sorter type.
     */
    private final SorterTypes pSorterType;

    private Z2CircuitConfig(Builder builder) {
        setAdderType(builder.adderType);
        setMultiplierType(builder.multiplierType);
        setSorterType(builder.sorterType);
        this.pSorterType = builder.pSorterType;
    }

    public AdderFactory.AdderTypes getAdderType() {
        return adderType;
    }

    public void setAdderType(AdderFactory.AdderTypes adderType) {
        this.adderType = adderType;
    }

    public MultiplierFactory.MultiplierTypes getMultiplierType() {
        return multiplierType;
    }

    public void setMultiplierType(MultiplierFactory.MultiplierTypes multiplierType) {
        this.multiplierType = multiplierType;
    }

    public SorterFactory.SorterTypes getSorterType() {
        return sorterType;
    }
    public PsorterFactory.SorterTypes getPsorterType() {
        return pSorterType;
    }

    public void setSorterType(SorterFactory.SorterTypes sorterType) {
        this.sorterType = sorterType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Z2CircuitConfig> {
        /**
         * Adder type.
         */
        private AdderFactory.AdderTypes adderType;
        /**
         * Multiplier type.
         */
        private MultiplierFactory.MultiplierTypes multiplierType;
        /**
         * Sorter type.
         */
        private SorterFactory.SorterTypes sorterType;
        /**
         * Permutable Sorter type.
         */
        private PsorterFactory.SorterTypes pSorterType;

        public Builder() {
            adderType = AdderFactory.AdderTypes.RIPPLE_CARRY;
            multiplierType = MultiplierFactory.MultiplierTypes.SHIFT_ADD;
            sorterType = SorterFactory.SorterTypes.BITONIC;
            pSorterType = PsorterFactory.SorterTypes.BITONIC;
        }

        public Builder setAdderType(AdderFactory.AdderTypes adderType) {
            this.adderType = adderType;
            return this;
        }

        public Builder setMultiplierType(MultiplierFactory.MultiplierTypes multiplierType) {
            this.multiplierType = multiplierType;
            return this;
        }

        public Builder setSorterType(SorterFactory.SorterTypes sorterType) {
            this.sorterType = sorterType;
            return this;
        }

        public Builder setPsorterType(PsorterFactory.SorterTypes pSorterType) {
            this.pSorterType = pSorterType;
            return this;
        }

        @Override
        public Z2CircuitConfig build() {
            return new Z2CircuitConfig(this);
        }
    }
}
