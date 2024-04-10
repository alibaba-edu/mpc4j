package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.generator;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.generator.RpZ2MtgFactory.Z2MtgType;

/**
 * configure of replicated 3p sharing z2 mt generator
 *
 * @author Feng Han
 * @date 2024/01/08
 */
public interface RpZ2MtgConfig extends MultiPartyPtoConfig {
    /**
     * get the type of mtg
     */
    Z2MtgType getMtgType();
    /**
     * the log of maximum size of tuples in one generation
     */
    int getNumOfResultBalls();
}
