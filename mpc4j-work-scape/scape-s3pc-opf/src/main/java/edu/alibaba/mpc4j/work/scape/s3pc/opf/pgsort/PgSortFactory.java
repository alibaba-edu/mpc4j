package edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.bitonic.BitonicPgSortConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.bitonic.BitonicPgSortParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.mixed.hzf22.Hzf22PgSortConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.mixed.hzf22.Hzf22PgSortParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.mixed.opt.OptPgSortConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.mixed.opt.OptPgSortParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.quick.QuickPgSortConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.quick.QuickPgSortParty;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.radix.RadixPgSortConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.radix.RadixPgSortParty;

/**
 * 3p oblivious sorting party factory.
 *
 * @author Feng Han
 * @date 2024/02/26
 */
public class PgSortFactory implements PtoFactory {

    /**
     * the protocol type
     */
    public enum PgSortType {
        /**
         * bitonic sort
         */
        BITONIC_PG_SORT,
        /**
         * quick sort
         */
        QUICK_PG_SORT,
        /**
         * radix sort
         */
        RADIX_PG_SORT,
        /**
         * optimal choice for various input data domain
         */
        OPT_PG_SORT,
        /**
         * optimal choice for various input data domain
         */
        HZF22_PG_SORT,
    }

    /**
     * Creates a permutation generation sorting party.
     *
     * @param config    the config.
     * @param abb3Party abb3 party
     * @return a permutation generation sorting party.
     */
    public static PgSortParty createParty(Abb3Party abb3Party, PgSortConfig config) {
        switch (config.getSortType()) {
            case BITONIC_PG_SORT:
                return new BitonicPgSortParty(abb3Party, (BitonicPgSortConfig) config);
            case QUICK_PG_SORT:
                return new QuickPgSortParty(abb3Party, (QuickPgSortConfig) config);
            case RADIX_PG_SORT:
                return new RadixPgSortParty(abb3Party, (RadixPgSortConfig) config);
            case OPT_PG_SORT:
                return new OptPgSortParty(abb3Party, (OptPgSortConfig) config);
            case HZF22_PG_SORT:
                return new Hzf22PgSortParty(abb3Party, (Hzf22PgSortConfig) config);
            default:
                throw new IllegalArgumentException("Invalid config.getSortType() in creating PgSortParty");
        }
    }

    /**
     * Creates a permutation generation sorting party config
     * @param securityModel security model
     */
    public static PgSortConfig createDefaultConfig(SecurityModel securityModel) {
        return new OptPgSortConfig.Builder(securityModel.equals(SecurityModel.MALICIOUS)).build();
    }

}
