package edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate;

import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Factory.PtoType;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.TripletLongConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate.TripletRpLongCpFactory.RpZl64PtoType;

/**
 * Interface for three-party replicate zl64c configure
 *
 * @author Feng Han
 * @date 2024/01/26
 */
public interface TripletRpLongConfig extends TripletLongConfig {
    /**
     * Gets protocol type.
     *
     * @return protocol type.
     */
    RpZl64PtoType getRpZl64PtoType();

    @Override
    default PtoType getPtoType(){
        return PtoType.REPLICATE;
    }
}
