package edu.alibaba.mpc4j.s2pc.pcg.ot.lot.nc;

import edu.alibaba.mpc4j.common.rpc.pto.SecurePtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lot.nc.NcLotFactory.NcLotType;

/**
 * NC-2^l选1-OT协议配置项。
 *
 * @author Hanwen Feng
 * @date 2022/0816
 */
public interface NcLotConfig extends SecurePtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    NcLotType getPtoType();

    /**
     * 一次可生成的最大OT数量。
     *
     * @return 最大OT数量。
     */
    int maxAllowNum();
}
