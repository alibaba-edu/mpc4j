package edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Factory.PtoType;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Factory.ShareType;

/**
 * Interface for three-party z2c
 *
 * @author Feng Han
 * @date 2023/12/25
 */
public interface TripletZ2cConfig extends MultiPartyPtoConfig {
    /**
     * Gets protocol type.
     *
     * @return protocol type.
     */
    PtoType getPtoType();

    /**
     * Gets protocol type.
     *
     * @return protocol type.
     */
    default ShareType getShareType(){
        return ShareType.BINARY;
    }
}
