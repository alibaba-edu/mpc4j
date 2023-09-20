package edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi;

import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtConfig;
import edu.alibaba.mpc4j.s2pc.opf.opprf.batch.BopprfConfig;

/**
 * batched OPPRF-based client-payload circuit PSI config.
 *
 * @author Weiran Liu
 * @date 2023/7/27
 */
public interface BopprfCcpsiConfig extends CcpsiConfig {
    /**
     * Gets batched OPPRF config.
     *
     * @return batched OPPRF config.
     */
    BopprfConfig getBopprfConfig();

    /**
     * Gets PEQT config.
     *
     * @return PEQT config.
     */
    PeqtConfig getPeqtConfig();

    /**
     * Gets cuckoo hash bin type.
     *
     * @return cuckoo hash bin type.
     */
    CuckooHashBinType getCuckooHashBinType();
}
