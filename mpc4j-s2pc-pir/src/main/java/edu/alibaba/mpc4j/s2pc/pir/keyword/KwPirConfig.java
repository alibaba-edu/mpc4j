package edu.alibaba.mpc4j.s2pc.pir.keyword;

import edu.alibaba.mpc4j.common.rpc.pto.SecurePtoConfig;

/**
 * 关键词索引协议配置项。
 *
 * @author Liqiang Peng
 * @date 2022/6/20
 */
public interface KwPirConfig extends SecurePtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    KwPirFactory.KwPirType getProType();
}
