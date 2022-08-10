package edu.alibaba.mpc4j.s2pc.pso.upsi;

/**
 * 非平衡PSI协议参数。
 *
 * @author Weiran Liu
 * @date 2022/8/7
 */
public interface UpsiParams {
    /**
     * 返回最大客户端元素数量。
     *
     * @return 最大客户端元素数量。
     */
    int maxClientSize();

    /**
     * 返回预估服务端元素数量。实际使用时可以超过预估数量。
     *
     * @return 预估服务端元素数量。
     */
    int expectServerSize();
}
