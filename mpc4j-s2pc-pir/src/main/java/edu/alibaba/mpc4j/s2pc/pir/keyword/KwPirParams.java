package edu.alibaba.mpc4j.s2pc.pir.keyword;

/**
 * 关键字PIR参数。
 *
 * @author Weiran Liu
 * @date 2022/8/8
 */
public interface KwPirParams {
    /**
     * 返回最大检索数量。
     *
     * @return 最大检索数量。
     */
    int maxRetrievalSize();

    /**
     * 返回预估服务端数据量。实际使用时可以超过预估量。
     *
     * @return 预估服务端数据量。
     */
    int expectServerSize();
}
