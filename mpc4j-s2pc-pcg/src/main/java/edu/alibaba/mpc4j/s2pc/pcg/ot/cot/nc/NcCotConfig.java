package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc;

import edu.alibaba.mpc4j.common.rpc.pto.SecurePtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotFactory.NcCotType;

/**
 * 无选择COT（No-Choice COT, NC-COT）协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/01/26
 */
public interface NcCotConfig extends SecurePtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    NcCotType getPtoType();

    /**
     * 返回最大支持数量。
     *
     * @return 最大支持数量。
     */
    int maxAllowNum();
}
