package edu.alibaba.mpc4j.s2pc.pir.index;

import edu.alibaba.mpc4j.common.rpc.pto.SecurePtoConfig;

/**
 * 索引PIR协议配置项接口。
 *
 * @author Liqiang Peng
 * @date 2022/8/24
 */
public interface IndexPirConfig extends SecurePtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    IndexPirFactory.IndexPirType getProType();
}
