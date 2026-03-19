package edu.alibaba.mpc4j.work.db.sketch.utils.pop;

import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Party;
import edu.alibaba.mpc4j.work.db.sketch.utils.pop.naive.NaivePopConfig;
import edu.alibaba.mpc4j.work.db.sketch.utils.pop.naive.NaivePopParty;

/**
 * factory of pop protocol.
 */
public class PopFactory {
    public enum PopPtoType {
        /**
         * NAIVE
         */
        NAIVE
    }

    public static PopParty createParty(Abb3Party abb3Party, PopConfig config) {
        // noinspection EnhancedSwitchStatement
        switch (config.getPtoType()) {
            case NAIVE:
                return new NaivePopParty(abb3Party, (NaivePopConfig) config);
            default:
                throw new IllegalArgumentException("Invalid PopPtoType: " + config.getPtoType().name());
        }
    }

    public static PopConfig createDefaultConfig(boolean malicious) {
        return new NaivePopConfig.Builder(malicious).build();
    }
}
