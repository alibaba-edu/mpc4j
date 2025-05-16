package edu.alibaba.mpc4j.crypto.phe;

/**
 * PHE security level. The definition comes from <a href="https://csrc.nist.gov/pubs/sp/800/57/pt1/r5/final">
 * NIST: Recommendation for key management, Special Publication 800-57</a>. Specifically,
 * <ul>
 *     <li>λ =  80，log_2(p) = 512.</li>
 *     <li>λ = 112，log_2(p) = 1024.</li>
 *     <li>λ = 128，log_2(p) = 1536.</li>
 *     <li>λ = 192，log_2(p) = 3840.</li>
 *
 * @author Weiran Liu
 * @date 2021/12/27
 */
public enum PheSecLevel {
    /**
     * λ = 40, only used for testing.
     */
    LAMBDA_40,
    /**
     * λ = 80
     */
    LAMBDA_80,
    /**
     * λ = 112
     */
    LAMBDA_112,
    /**
     * λ = 128
     */
    LAMBDA_128,
    /**
     * λ = 192
     */
    LAMBDA_192,
}
