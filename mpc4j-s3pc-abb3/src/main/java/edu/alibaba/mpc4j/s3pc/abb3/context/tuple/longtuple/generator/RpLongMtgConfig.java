package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.generator;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.generator.RpLongMtgFactory.Zl64MtgType;

/**
 * configure of replicated 3p sharing zl64 mt generator
 *
 * @author Feng Han
 * @date 2024/01/08
 */
public interface RpLongMtgConfig extends MultiPartyPtoConfig {
    /**
     * get the type of mtg
     */
    Zl64MtgType getMtgType();
    /**
     * the log of maximum size of tuples in one generation
     */
    int getNumOfResultBalls();
}
