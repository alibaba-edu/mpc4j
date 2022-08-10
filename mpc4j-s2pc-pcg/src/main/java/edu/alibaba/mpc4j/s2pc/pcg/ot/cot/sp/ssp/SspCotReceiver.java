package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.SecurePto;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.ssp.SspCotFactory.SspCotType;

/**
 * SSP-COT协议接收方接口。
 *
 * @author Weiran Liu
 * @date 2022/02/03
 */
public interface SspCotReceiver extends TwoPartyPto, SecurePto {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    @Override
    SspCotType getPtoType();

    /**
     * 初始化协议。
     *
     * @param maxNum 最大数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int maxNum) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param alpha α。
     * @param num   数量。
     * @return 接收方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    SspCotReceiverOutput receive(int alpha, int num) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param alpha             α。
     * @param num               数量。
     * @param preReceiverOutput 预计算接收方输出。
     * @return 接收方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    SspCotReceiverOutput receive(int alpha, int num, CotReceiverOutput preReceiverOutput) throws MpcAbortException;
}
