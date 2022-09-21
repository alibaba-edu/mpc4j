package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core;

import edu.alibaba.mpc4j.common.rpc.pto.SecurePtoConfig;

/**
 * 核l比特三元组生成协议信息。
 *
 * @author Weiran Liu
 * @date 2022/8/11
 */
public interface ZlCoreMtgConfig extends SecurePtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    ZlCoreMtgFactory.ZlCoreMtgType getPtoType();

    /**
     * 返回乘法三元组比特长度。
     *
     * @return 乘法三元组比特长度。
     */
    int getL();

    /**
     * 返回最大支持数量。
     *
     * @return 最大支持数量。
     */
    int maxAllowNum();
}
