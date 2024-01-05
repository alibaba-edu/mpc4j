package edu.alibaba.mpc4j.work;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

import static edu.alibaba.mpc4j.work.BatchPirFactory.*;

/**
 * batch PIR config interface.
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public interface BatchPirConfig extends MultiPartyPtoConfig {
    /**
     * protocol type.
     *
     * @return protocol type.
     */
    BatchIndexPirType getPtoType();
}
