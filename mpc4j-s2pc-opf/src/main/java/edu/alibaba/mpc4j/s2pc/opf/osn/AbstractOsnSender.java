package edu.alibaba.mpc4j.s2pc.opf.osn;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkUtils;

import java.util.Vector;
import java.util.stream.Collectors;

/**
 * OSN协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/02/09
 */
public abstract class AbstractOsnSender extends AbstractTwoPartyPto implements OsnSender {
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

    protected void setInitInput(int maxN) {
        MathPreconditions.checkGreater("maxN", maxN, 1);
        this.maxN = maxN;
        maxLevel = PermutationNetworkUtils.getLevel(maxN);
        maxWidth = PermutationNetworkUtils.getMaxWidth(maxN);
        maxSwitchNum = maxLevel * maxWidth;
        initState();
    }

    protected void setPtoInput(Vector<byte[]> inputVector, int byteLength) throws MpcAbortException {
        checkInitialized();
        MathPreconditions.checkGreaterOrEqual("byteLength", byteLength, CommonConstants.STATS_BYTE_LENGTH);
        this.byteLength = byteLength;
        MathPreconditions.checkLessOrEqual("n", inputVector.size(), maxN);
        this.inputVector = inputVector.stream()
            .peek(input -> MathPreconditions.checkEqual("input.length", "byteLength", input.length, byteLength))
            .collect(Collectors.toCollection(Vector::new));
        n = inputVector.size();
        level = PermutationNetworkUtils.getLevel(n);
        width = PermutationNetworkUtils.getMaxWidth(n);
        switchNum = level * width;
        extraInfo++;
    }
}
