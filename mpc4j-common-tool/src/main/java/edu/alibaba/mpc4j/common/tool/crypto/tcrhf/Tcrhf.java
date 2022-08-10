package edu.alibaba.mpc4j.common.tool.crypto.tcrhf;

import edu.alibaba.mpc4j.common.tool.crypto.tcrhf.TcrhfFactory.TcrhfType;

/**
 * 可调抗关联哈希函数（Tweakable Circular Correlation Robustness）接口。可调抗关联哈希函数以128比特和索引值为输入，输出128比特哈希值。
 *
 * @author Weiran Liu
 * @date 2022/01/11
 */
public interface Tcrhf {
    /**
     * 将输入的分组哈希为输出分组。
     *
     * @param index 索引值。
     * @param block 输入分组。
     * @return 哈希结果。
     */
    byte[] hash(int index, byte[] block);

    /**
     * 将如数的分组哈希为输出分组。
     *
     * @param leftIndex 左侧索引值。
     * @param rightIndex 右侧索引值。
     * @param block 输入分组。
     * @return 哈希结果。
     */
    byte[] hash(int leftIndex, int rightIndex, byte[] block);

    /**
     * 返回TCRHF类型。
     *
     * @return TCRHF类型。
     */
    TcrhfType getTcrhfType();
}
