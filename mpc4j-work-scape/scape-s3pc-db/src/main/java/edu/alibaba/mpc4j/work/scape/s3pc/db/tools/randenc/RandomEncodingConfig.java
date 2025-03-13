package edu.alibaba.mpc4j.work.scape.s3pc.db.tools.randenc;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.randenc.RandomEncodingFactory.EncodingPtoType;

/**
 * interface for random encoding config
 *
 * @author Feng Han
 * @date 2025/2/25
 */
public interface RandomEncodingConfig extends MultiPartyPtoConfig {
    /**
     * get the type of random encoding
     *
     * @return the type of random encoding
     */
    EncodingPtoType getPtoType();
}
