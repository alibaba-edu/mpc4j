package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp;

import edu.alibaba.mpc4j.common.rpc.pto.SecurePtoConfig;

/**
 * 批处理单点关联不经意传输（Batched Single-Point Correlated Oblivious Transfer，BSP-COT）协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/01/22
 */
public interface BspCotConfig extends SecurePtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    BspCotFactory.BspCotType getPtoType();
}
