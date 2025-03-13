package edu.alibaba.mpc4j.work.scape.s3pc.db.tools.fillper;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.fillper.FillPermutationFactory.FillType;

/**
 * config of the permutation fill protocol
 *
 * @author Feng Han
 * @date 2025/2/17
 */
public interface FillPermutationConfig extends MultiPartyPtoConfig {
    /**
     * the type of permutation fill protocol
     */
    FillType getFillType();
}
