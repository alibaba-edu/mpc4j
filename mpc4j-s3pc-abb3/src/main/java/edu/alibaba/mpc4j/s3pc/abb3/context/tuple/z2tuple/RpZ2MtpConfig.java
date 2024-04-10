package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.RpMtProviderFactory.MtProviderType;

/**
 * configure of z2 mtp for replicated 3p sharing
 *
 * @author Feng Han
 * @date 2024/01/25
 */
public interface RpZ2MtpConfig extends MultiPartyPtoConfig {
    /**
     * multiplication tuple provider type
     */
    MtProviderType getProviderType();
}
