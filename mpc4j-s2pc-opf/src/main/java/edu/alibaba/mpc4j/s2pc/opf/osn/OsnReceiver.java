package edu.alibaba.mpc4j.s2pc.opf.osn;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * OSN协议接收方接口。
 *
 * @author Weiran Liu
 * @date 2022/02/09
 */
public interface OsnReceiver extends TwoPartyPto {
    /**
     * 初始化协议。
     *
     * @param maxN 最大元素数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int maxN) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param permutationMap 置换映射。
     * @param byteLength 输入向量/分享向量元素字节长度。
     * @return 接收方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    OsnPartyOutput osn(int[] permutationMap, int byteLength) throws MpcAbortException;
}
