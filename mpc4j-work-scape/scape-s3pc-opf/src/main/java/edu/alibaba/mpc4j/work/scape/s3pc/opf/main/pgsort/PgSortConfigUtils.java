package edu.alibaba.mpc4j.work.scape.s3pc.opf.main.pgsort;

import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory.ComparatorType;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.main.permutation.PermutationMain;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteFactory.PermuteType;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.PgSortFactory.PgSortType;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.bitonic.BitonicPgSortConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.mixed.hzf22.Hzf22PgSortConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.mixed.opt.OptPgSortConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.quick.QuickPgSortConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.pgsort.radix.RadixPgSortConfig;

import java.util.Properties;

/**
 * configure utils for 3p oblivious sorting party.
 *
 * @author Feng Han
 * @date 2025/2/28
 */
public class PgSortConfigUtils {
    /**
     * comparator type key.
     */
    private static final String COMPARATOR_TYPE = "comparator_type";

    /**
     * private constructor.
     */
    private PgSortConfigUtils() {
        // empty
    }

    /**
     * Creates config.
     *
     * @param properties properties.
     * @return config.
     */
    public static PgSortConfig createConfig(Properties properties) {
        PgSortType pgSortType = MainPtoConfigUtils.readEnum(PgSortType.class, properties, PgSortMain.PTO_NAME_KEY);
        return switch (pgSortType) {
            case BITONIC_PG_SORT -> generateBitonicPgSortConfig(properties);
            case OPT_PG_SORT -> generateOptPgSortConfig(properties);
            case HZF22_PG_SORT -> generateHzf22PgSortConfig(properties);
            case QUICK_PG_SORT -> generateQuickPgSortConfig(properties);
            case RADIX_PG_SORT -> generateRadixPgSortConfig(properties);
            default ->
                throw new IllegalArgumentException("Invalid " + PermuteType.class.getSimpleName() + ": " + pgSortType.name());
        };
    }

    private static BitonicPgSortConfig generateBitonicPgSortConfig(Properties properties) {
        boolean malicious = PropertiesUtils.readBoolean(properties, PermutationMain.IS_MALICIOUS);
        ComparatorType comparatorType = MainPtoConfigUtils.readEnum(ComparatorType.class, properties, COMPARATOR_TYPE);
        return new BitonicPgSortConfig.Builder(malicious).setComparatorType(comparatorType).build();
    }

    private static OptPgSortConfig generateOptPgSortConfig(Properties properties) {
        boolean malicious = PropertiesUtils.readBoolean(properties, PermutationMain.IS_MALICIOUS);
        ComparatorType comparatorType = MainPtoConfigUtils.readEnum(ComparatorType.class, properties, COMPARATOR_TYPE);
        return new OptPgSortConfig.Builder(malicious)
            .setQuickPgSortConfig(new QuickPgSortConfig.Builder(malicious).setComparatorType(comparatorType).build())
            .build();
    }

    private static Hzf22PgSortConfig generateHzf22PgSortConfig(Properties properties) {
        boolean malicious = PropertiesUtils.readBoolean(properties, PermutationMain.IS_MALICIOUS);
        ComparatorType comparatorType = MainPtoConfigUtils.readEnum(ComparatorType.class, properties, COMPARATOR_TYPE);
        return new Hzf22PgSortConfig.Builder(malicious)
            .setBitonicPgSortConfig(new BitonicPgSortConfig.Builder(malicious).setComparatorType(comparatorType).build())
            .build();
    }

    private static QuickPgSortConfig generateQuickPgSortConfig(Properties properties) {
        boolean malicious = PropertiesUtils.readBoolean(properties, PermutationMain.IS_MALICIOUS);
        ComparatorType comparatorType = MainPtoConfigUtils.readEnum(ComparatorType.class, properties, COMPARATOR_TYPE);
        return new QuickPgSortConfig.Builder(malicious).setComparatorType(comparatorType).build();
    }

    private static RadixPgSortConfig generateRadixPgSortConfig(Properties properties) {
        boolean malicious = PropertiesUtils.readBoolean(properties, PermutationMain.IS_MALICIOUS);
        return new RadixPgSortConfig.Builder(malicious).build();
    }
}
