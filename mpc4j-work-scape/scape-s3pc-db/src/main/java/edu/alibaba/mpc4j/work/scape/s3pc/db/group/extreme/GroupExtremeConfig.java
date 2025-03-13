package edu.alibaba.mpc4j.work.scape.s3pc.db.group.extreme;

import edu.alibaba.mpc4j.work.scape.s3pc.db.group.GroupConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.group.extreme.GroupExtremeFactory.GroupExtremePtoType;

/**
 * configure of group extreme
 *
 * @author Feng Han
 * @date 2025/2/24
 */
public interface GroupExtremeConfig extends GroupConfig {
    /**
     * get the protocol type
     *
     * @return the protocol type
     */
    GroupExtremePtoType getPtoType();
}
