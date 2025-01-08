package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.DosnFactory.DosnType;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnFactory.RosnType;

/**
 * OSN协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/02/09
 */
public interface DosnConfig extends MultiPartyPtoConfig {
    /**
     * Gets protocol type.
     *
     * @return protocol type.
     */
    DosnType getPtoType();

    /**
     * Gets ROSN protocol type.
     *
     * @return ROSN type.
     */
    RosnType getRosnType();
}
