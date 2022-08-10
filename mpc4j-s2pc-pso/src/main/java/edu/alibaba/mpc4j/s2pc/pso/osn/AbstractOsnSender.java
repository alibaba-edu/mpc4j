package edu.alibaba.mpc4j.s2pc.pso.osn;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.benes.BenesNetworkUtils;

import java.util.Vector;
import java.util.stream.Collectors;

/**
 * OSN协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/02/09
 */
public abstract class AbstractOsnSender extends AbstractSecureTwoPartyPto implements OsnSender {
    /**
     * 配置项
     */
    protected final OsnConfig config;
    /**
     * 交换网络输入向量最大长度
     */
    protected int maxN;
    /**
     * 交换网络最大层数
     */
    protected int maxLevel;
    /**
     * 交换网络最大宽度
     */
    protected int maxWidth;
    /**
     * 交换网络最大交换门数量
     */
    protected int maxSwitchNum;
    /**
     * 交换网络输入向量长度
     */
    protected int n;
    /**
     * 输入/分享字节长度
     */
    protected int byteLength;
    /**
     * 输入向量
     */
    protected Vector<byte[]> inputVector;
    /**
     * Benes网络层数
     */
    protected int level;
    /**
     * Benes网络宽度
     */
    protected int width;
    /**
     * 交换门总数量
     */
    protected int switchNum;

    protected AbstractOsnSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, OsnConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        this.config = config;
    }

    @Override
    public OsnFactory.OsnType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(int maxN) {
        assert maxN > 1;
        this.maxN = maxN;
        this.maxLevel = BenesNetworkUtils.getLevel(maxN);
        this.maxWidth = BenesNetworkUtils.getWidth(maxN);
        this.maxSwitchNum = maxLevel * maxWidth;
        initialized = false;
    }

    protected void setPtoInput(Vector<byte[]> inputVector, int byteLength) throws MpcAbortException {
        assert byteLength >= CommonConstants.STATS_BYTE_LENGTH;
        this.byteLength = byteLength;
        assert inputVector.size() > 1 && inputVector.size() <= maxN;
        this.inputVector = inputVector.stream()
            .peek(input -> {
                assert input.length == byteLength;
            })
            .collect(Collectors.toCollection(Vector::new));
        n = inputVector.size();
        level = BenesNetworkUtils.getLevel(n);
        width = BenesNetworkUtils.getWidth(n);
        switchNum = level * width;
        extraInfo++;
    }
}
