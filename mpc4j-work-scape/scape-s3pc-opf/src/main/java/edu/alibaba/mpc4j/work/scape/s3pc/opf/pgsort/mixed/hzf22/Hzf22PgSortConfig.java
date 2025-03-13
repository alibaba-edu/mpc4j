package edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.mixed.hzf22;

import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory.ComparatorType;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortFactory.PgSortType;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.bitonic.BitonicPgSortConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.radix.RadixPgSortConfig;

/**
 * mixed sorting party config, choice sorting method based on input and strategy in HZF22
 *
 * @author Feng Han
 * @date 2024/02/28
 */
public class Hzf22PgSortConfig extends AbstractMultiPartyPtoConfig implements PgSortConfig {
    /**
     * configure of radix sorter
     */
    private final RadixPgSortConfig radixPgSortConfig;
    /**
     * configure of quick sorter
     */
    private final BitonicPgSortConfig bitonicPgSortConfig;

    private Hzf22PgSortConfig(Builder builder) {
        super(builder.malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST);
        radixPgSortConfig = builder.radixPgSortConfig;
        bitonicPgSortConfig = builder.bitonicPgSortConfig;
    }

    @Override
    public PgSortType getSortType() {
        return PgSortType.HZF22_PG_SORT;
    }

    @Override
    public boolean isStable() {
        return false;
    }

    @Override
    public void setComparatorType(ComparatorType comparatorType) {
        bitonicPgSortConfig.setComparatorType(comparatorType);
    }

    public RadixPgSortConfig getRadixPgSortConfig() {
        return radixPgSortConfig;
    }

    public BitonicPgSortConfig getBitonicPgSortConfig() {
        return bitonicPgSortConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Hzf22PgSortConfig> {
        /**
         * whether malicious secure or not
         */
        private final boolean malicious;
        /**
         * configure of radix sorter
         */
        private final RadixPgSortConfig radixPgSortConfig;
        /**
         * configure of quick sorter
         */
        private BitonicPgSortConfig bitonicPgSortConfig;

        public Builder(boolean malicious) {
            this.malicious = malicious;
            radixPgSortConfig = new RadixPgSortConfig.Builder(malicious).build();
            bitonicPgSortConfig = new BitonicPgSortConfig.Builder(malicious).build();
        }

        public Builder setBitonicPgSortConfig(BitonicPgSortConfig bitonicPgSortConfig) {
            this.bitonicPgSortConfig = bitonicPgSortConfig;
            return this;
        }

        @Override
        public Hzf22PgSortConfig build() {
            return new Hzf22PgSortConfig(this);
        }
    }
}

