package edu.alibaba.mpc4j.s2pc.pir.index.batch;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * batch index PIR config interface.
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public interface BatchIndexPirConfig extends MultiPartyPtoConfig {
    /**
     * protocol type.
     *
     * @return protocol type.
     */
    BatchIndexPirFactory.BatchIndexPirType getPtoType();
}
