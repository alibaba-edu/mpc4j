package edu.alibaba.mpc4j.work.db.sketch.utils.agg;

import edu.alibaba.mpc4j.common.circuit.z2.comparator.ComparatorFactory.ComparatorType;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.work.db.sketch.utils.agg.hzf22.Hzf22AggConfig;
import edu.alibaba.mpc4j.work.db.sketch.utils.agg.hzf22.Hzf22AggParty;

/**
 * aggregate function factory.
 */
public class AggFactory {

    public enum AggPtoType {
        /**
         * HZF22
         */
        HZF22
    }

    public static AggParty createParty(Abb3Party abb3Party, AggConfig config) {
        // noinspection EnhancedSwitchStatement
        switch (config.getPtoType()) {
            case HZF22:
                return new Hzf22AggParty(abb3Party, (Hzf22AggConfig) config);
            default:
                throw new IllegalArgumentException("Invalid AggPtoType: " + config.getPtoType().name());
        }
    }

    public static AggConfig createDefaultConfig(boolean malicious) {
        return new Hzf22AggConfig.Builder(malicious).setComparatorType(ComparatorType.TREE_COMPARATOR).build();
    }
}
