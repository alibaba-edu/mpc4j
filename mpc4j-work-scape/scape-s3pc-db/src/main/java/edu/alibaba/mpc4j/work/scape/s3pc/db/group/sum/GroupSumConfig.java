package edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum;

import edu.alibaba.mpc4j.work.scape.s3pc.db.group.GroupConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.sum.GroupSumFactory.GroupSumPtoType;

/**
 * configure of group sum
 *
 * @author Feng Han
 * @date 2025/2/24
 */
public interface GroupSumConfig extends GroupConfig {
    /**
     * get the protocol type
     *
     * @return the protocol type
     */
    GroupSumPtoType getPtoType();
}
