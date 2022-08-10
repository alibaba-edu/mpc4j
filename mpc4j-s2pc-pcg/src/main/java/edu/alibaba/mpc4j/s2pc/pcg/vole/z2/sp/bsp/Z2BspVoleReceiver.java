package edu.alibaba.mpc4j.s2pc.pcg.vole.z2.sp.bsp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.SecurePto;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.vole.z2.Z2VoleReceiverOutput;

/**
 * Z2-BSP-VOLE接收方接口。
 *
 * @author Weiran Liu
 * @date 2022/6/22
 */
public interface Z2BspVoleReceiver extends TwoPartyPto, SecurePto {
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
     * @param delta    关联值Δ。
     * @param maxBatch 最大批处理数量。
     * @param maxNum   最大数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(boolean delta, int maxBatch, int maxNum) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param batch 批处理数量。
     * @param num   数量。
     * @return 接收方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    Z2BspVoleReceiverOutput receive(int batch, int num) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param batch             批处理数量。
     * @param num               数量。
     * @param preReceiverOutput 预计算接收方方输出。
     * @return 接收方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    Z2BspVoleReceiverOutput receive(int batch, int num, Z2VoleReceiverOutput preReceiverOutput) throws MpcAbortException;
}
