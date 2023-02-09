package edu.alibaba.mpc4j.s2pc.aby.hamming;

import edu.alibaba.mpc4j.common.rpc.pto.SecurePtoConfig;

/**
 * 汉明距离协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/11/22
 */
public interface HammingConfig extends SecurePtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    HammingFactory.HammingType getPtoType();
    /**
     * 返回底层协议支持的最大比特数量。
     *
     * @return 底层协议支持的最大比特数量。
     */
    int maxAllowBitNum();
}
