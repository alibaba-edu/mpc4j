package edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.bsp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.SecurePto;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.Z2VoleSenderOutput;

/**
 * Z2-BSP-VOLE发送方接口。
 *
 * @author Weiran Liu
 * @date 2022/6/22
 */
public interface Z2BspVoleSender extends TwoPartyPto, SecurePto {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    @Override
    Z2BspVoleFactory.Z2BspVoleType getPtoType();

    /**
     * 初始化协议。
     *
     * @param maxBatch 最大批处理数量。
     * @param maxNum   最大数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int maxBatch, int maxNum) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param alphaArray α数组。
     * @param num        数量。
     * @return 发送方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    Z2BspVoleSenderOutput send(int[] alphaArray, int num) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param alphaArray      α数组。
     * @param num             数量。
     * @param preSenderOutput 预计算发送方输出。
     * @return 发送方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    Z2BspVoleSenderOutput send(int[] alphaArray, int num, Z2VoleSenderOutput preSenderOutput) throws MpcAbortException;
}
