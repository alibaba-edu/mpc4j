package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp;

import edu.alibaba.mpc4j.common.rpc.pto.SecurePtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotFactory.MspCotType;

/**
 * 多点关联不经意传输（Multi Single-Point Correlated Oblivious Transfer，MSP-COT）协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/01/22
 */
public interface MspCotConfig extends SecurePtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    MspCotType getPtoType();
}
