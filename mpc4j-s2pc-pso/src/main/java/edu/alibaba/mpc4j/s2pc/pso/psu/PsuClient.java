package edu.alibaba.mpc4j.s2pc.pso.psu;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory.PsuType;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * PSU协议客户端接口。
 *
 * @author Weiran Liu
 * @date 2022/02/15
 */
public interface PsuClient extends TwoPartyPto {
    /**
     * 初始化协议。
     *
     * @param maxClientElementSize 客户端最大元素数量。
     * @param maxServerElementSize 服务端最大元素数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param clientElementSet 客户端元素集合。
     * @param serverElementSize  服务端元素数量。
     * @param elementByteLength  元素字节长度。
     * @return 协议输出结果。
     * @throws MpcAbortException 如果协议异常中止。
     */
    Set<ByteBuffer> psu(Set<ByteBuffer> clientElementSet, int serverElementSize, int elementByteLength)
        throws MpcAbortException;
}
