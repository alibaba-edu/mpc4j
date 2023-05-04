package edu.alibaba.mpc4j.s2pc.opf.oprp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.util.Arrays;

/**
 * OPRP协议接收方。
 *
 * @author Weiran Liu
 * @date 2022/02/14
 */
public abstract class AbstractOprpReceiver extends AbstractTwoPartyPto implements OprpReceiver {
    /**
     * 最大批处理数量
     */
    protected int maxBatchSize;
    /**
     * 取整最大批处理数量
     */
    protected int maxRoundBatchSize;
    /**
     * 明文
     */
    protected byte[][] messages;
    /**
     * 批处理数量
     */
    protected int batchSize;
    /**
     * 批处理字节数量
     */
    protected int batchByteSize;
    /**
     * 取整批处理数量
     */
    protected int roundBatchSize;

    protected AbstractOprpReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, OprpConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
    }

    protected void setInitInput(int maxBatchSize) {
        MathPreconditions.checkPositive("maxBatchSize", maxBatchSize);
        this.maxBatchSize = maxBatchSize;
        maxRoundBatchSize = CommonUtils.getByteLength(this.maxBatchSize) * Byte.SIZE;
        initState();
    }

    protected void setPtoInput(byte[][] messages) throws MpcAbortException {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("batchSize", messages.length, maxBatchSize);
        batchSize = messages.length;
        batchByteSize = CommonUtils.getByteLength(batchSize);
        roundBatchSize = batchByteSize * Byte.SIZE;
        this.messages = Arrays.stream(messages)
            .peek(message ->
                MathPreconditions.checkEqual(
                    "message.length", "λ(B)", message.length, CommonConstants.BLOCK_BYTE_LENGTH
                )
            )
            .toArray(byte[][]::new);
        extraInfo++;
    }
}
