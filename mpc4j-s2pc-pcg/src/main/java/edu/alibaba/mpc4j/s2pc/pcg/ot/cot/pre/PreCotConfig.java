package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre;

import edu.alibaba.mpc4j.common.rpc.pto.SecurePtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotFactory.PreCotType;

/**
 * 预计算关联不经意传输（Precompute Correlated Oblivious Transfer，预计算COT）协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/01/14
 */
public interface PreCotConfig extends SecurePtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    PreCotType getPtoType();
}
