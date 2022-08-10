package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp;

import edu.alibaba.mpc4j.common.rpc.pto.SecurePtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.SspCotFactory.SspCotType;

/**
 * 单一单点关联不经意传输（Single Single-Point Correlated Oblivious Transfer，SSP-COT）协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/01/14
 */
public interface SspCotConfig extends SecurePtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    SspCotType getPtoType();
}
