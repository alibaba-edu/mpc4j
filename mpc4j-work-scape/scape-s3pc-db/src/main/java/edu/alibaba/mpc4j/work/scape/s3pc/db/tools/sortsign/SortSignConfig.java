package edu.alibaba.mpc4j.work.scape.s3pc.db.tools.sortsign;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.sortsign.SortSignFactory.SortSignType;

/**
 * SortSign Config
 *
 * @author Feng Han
 * @date 2025/2/19
 */
public interface SortSignConfig extends MultiPartyPtoConfig {
    /**
     * get the type of sortSign
     *
     * @return the type of sortSign
     */
    SortSignType getSortSignType();
}
