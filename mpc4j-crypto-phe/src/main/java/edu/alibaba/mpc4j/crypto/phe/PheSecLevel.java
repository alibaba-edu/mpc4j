package edu.alibaba.mpc4j.crypto.phe;

/**
 * 半同态加密安全等级。定义来自于：NIST: Recommendation for key management, Special Publication 800-57
 * <p>λ =  80，log_2(p) = 512.</p>
 * <p>λ = 112，log_2(p) = 1024. </p>
 * <p>λ = 128，log_2(p) = 1536. </p>
 * <p>λ = 192，log_2(p) = 3840. </p>
 *
 * @author Weiran Liu
 * @date 2021/12/27
 */
public enum PheSecLevel {
    /**
     * 40比特安全常数
     */
    LAMBDA_40,
    /**
     * 80比特安全常数
     */
    LAMBDA_80,
    /**
     * 112比特安全常数
     */
    LAMBDA_112,
    /**
     * 128比特安全常数
     */
    LAMBDA_128,
    /**
     * 192比特安全常数
     */
    LAMBDA_192,
}
