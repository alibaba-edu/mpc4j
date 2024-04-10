package edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Factory.PtoType;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Factory.ShareType;

/**
 * Configure for three-party zl64c
 *
 * @author Feng Han
 * @date 2024/01/08
 */
public interface TripletLongConfig extends MultiPartyPtoConfig {

    /**
     * Gets protocol type.
     *
     * @return protocol type.
     */
    PtoType getPtoType();

    /**
     * Gets sharing type.
     *
     * @return sharing type.
     */
    default ShareType getShareType(){
        return ShareType.ARITHMETIC;
    }

}
