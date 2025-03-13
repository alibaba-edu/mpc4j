package edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.SoprpFactory.SoprpType;

/**
 * Interface for three-party soprp configure
 *
 * @author Feng Han
 * @date 2024/02/26
 */
public interface SoprpConfig extends MultiPartyPtoConfig {
    /**
     * Gets protocol type.
     *
     * @return protocol type.
     */
    SoprpType getPrpType();
}
