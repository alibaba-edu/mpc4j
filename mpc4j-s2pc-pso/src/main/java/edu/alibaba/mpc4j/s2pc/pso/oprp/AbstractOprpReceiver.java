package edu.alibaba.mpc4j.s2pc.pso.oprp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.util.Arrays;

/**
 * OPRP协议接收方。
 *
 * @author Weiran Liu
 * @date 2022/02/14
 */
public abstract class AbstractOprpReceiver extends AbstractSecureTwoPartyPto implements OprpReceiver {
    /**
     * 配置项
     */
    private final OprpConfig config;
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
        this.config = config;
    }

    @Override
    public OprpFactory.OprpType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(int matchBatchSize) {
        assert matchBatchSize > 0;
        this.maxBatchSize = matchBatchSize;
        maxRoundBatchSize = CommonUtils.getByteLength(maxBatchSize) * Byte.SIZE;
        initialized = false;
    }

    protected void setPtoInput(byte[][] messages) throws MpcAbortException {
        assert messages.length > 0 && messages.length <= maxBatchSize;
        batchSize = messages.length;
        batchByteSize = CommonUtils.getByteLength(batchSize);
        roundBatchSize = batchByteSize * Byte.SIZE;
        this.messages = Arrays.stream(messages)
            .peek(message -> {
                assert message.length == CommonConstants.BLOCK_BYTE_LENGTH;
            })
            .toArray(byte[][]::new);
        extraInfo++;
    }
}
