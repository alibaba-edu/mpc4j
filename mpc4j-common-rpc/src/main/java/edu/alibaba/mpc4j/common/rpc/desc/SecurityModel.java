package edu.alibaba.mpc4j.common.rpc.desc;

/**
 * 安全模型。
 *
 * @author Weiran Liu
 * @date 2021/12/09
 */
public enum SecurityModel {
    /**
     * 理想世界，无安全性
     */
    IDEAL,
    /**
     * 半可信安全性
     */
    SEMI_HONEST,
    /**
     * 隐蔽安全性
     */
    COVERT,
    /**
     * 恶意安全性
     */
    MALICIOUS,
}
