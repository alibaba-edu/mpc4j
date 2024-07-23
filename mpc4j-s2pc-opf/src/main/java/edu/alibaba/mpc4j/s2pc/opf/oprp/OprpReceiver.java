package edu.alibaba.mpc4j.s2pc.opf.oprp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory.PrpType;
import edu.alibaba.mpc4j.s2pc.opf.oprp.OprpFactory.OprpType;

/**
 * Oprp接收方接口。
 *
 * @author Weiran Liu
 * @date 2022/02/11
 */
public interface OprpReceiver extends TwoPartyPto {
    /**
     * Gets protocol type.
     *
     * @return protocol type.
     */
    OprpType getType();
    
    /**
     * 返回PRP类型。
     *
     * @return PRP类型。
     */
    PrpType getPrpType();

    /**
     * 初始化协议。
     *
     * @param maxBatchSize 最大批处理数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int maxBatchSize) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param messages 明文。
     * @return 接收方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    OprpReceiverOutput oprp(byte[][] messages) throws MpcAbortException;
}
