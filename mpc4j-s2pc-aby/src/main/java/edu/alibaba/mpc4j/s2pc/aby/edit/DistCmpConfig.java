package edu.alibaba.mpc4j.s2pc.aby.edit;

import edu.alibaba.mpc4j.s2pc.aby.edit.EditDistFactory.EditDistType;

/**
 * Edit distance config.
 *
 * @author Li Peng
 * @date 2024/4/8
 */
public interface DistCmpConfig {

    /**
     * get protocol type
     */
    EditDistType getEditDistType();
}
