package edu.alibaba.mpc4j.common.circuit.z2;

import edu.alibaba.mpc4j.common.circuit.CircuitConfig;
import edu.alibaba.mpc4j.common.circuit.z2.adder.AdderFactory;
import edu.alibaba.mpc4j.common.circuit.z2.multiplier.MultiplierFactory;
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

    private Z2CircuitConfig(Builder builder) {
        setAdderType(builder.adderType);
        setMultiplierType(builder.multiplierType);
        setSorterType(builder.sorterType);
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

        public Builder() {
            adderType = AdderFactory.AdderTypes.RIPPLE_CARRY;
            multiplierType = MultiplierFactory.MultiplierTypes.SHIFT_ADD;
            sorterType = SorterFactory.SorterTypes.BITONIC;
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

        @Override
        public Z2CircuitConfig build() {
            return new Z2CircuitConfig(this);
        }
    }
}
