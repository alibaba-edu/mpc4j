package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl;

import edu.alibaba.mpc4j.common.rpc.pto.SecurePtoConfig;

/**
 * l比特三元组生成协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/8/11
 */
public interface ZlMtgConfig extends SecurePtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    ZlMtgFactory.ZlMtgType getPtoType();

    /**
     * 返回底层协议最大数量。
     *
     * @return 底层协议最大数量。
     */
    int maxBaseNum();
}
