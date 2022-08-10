package edu.alibaba.mpc4j.common.rpc.desc;

/**
 * 协议描述信息。
 *
 * @author Weiran Liu
 * @date 2021/12/09
 */
public interface PtoDesc {
    /**
     * 返回协议ID。
     *
     * @return 协议ID。
     */
    int getPtoId();

    /**
     * 返回协议名称。
     *
     * @return 协议名称。
     */
    String getPtoName();
}
