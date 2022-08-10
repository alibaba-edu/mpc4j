package edu.alibaba.mpc4j.common.tool.crypto.crhf;

/**
 * 抗关联哈希函数（Correlation Robustness Hash Function，CRHF）接口。抗关联哈希函数以128比特为输入，输出128比特的哈希结果。
 * 下述论文给出了抗关联哈希函数的2种实现：
 * - MMO(x) = π(x) ⊕ x（满足抗关联性）。
 * - MMO_σ(x) = π(σ(x)) ⊕ σ(x)（满足电路抗关联性），其中σ是一个自同构映射。
 * Guo C, Katz J, Wang X, et al. Efficient and secure multiparty computation from fixed-key block ciphers.
 * 2020 IEEE Symposium on Security and Privacy (SP). IEEE, 2020: 825-841.
 *
 * @author Weiran Liu
 * @date 2022/01/11
 */
public interface Crhf {
    /**
     * 将输入的分组哈希为输出分组。
     *
     * @param block 输入分组。
     * @return 哈希结果。
     */
    byte[] hash(byte[] block);

    /**
     * 返回抗关联哈希函数类型。
     *
     * @return 抗关联哈希函数类型。
     */
    CrhfFactory.CrhfType getCrhfType();
}
