package edu.alibaba.mpc4j.s2pc.pso.osn;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.benes.BenesNetwork;
import edu.alibaba.mpc4j.common.tool.benes.BenesNetworkFactory;
import edu.alibaba.mpc4j.common.tool.benes.BenesNetworkUtils;

/**
 * OSN协议接收方。
 *
 * @author Weiran Liu
 * @date 2022/02/09
 */
public abstract class AbstractOsnReceiver extends AbstractSecureTwoPartyPto implements OsnReceiver {
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
     * 输入/分享字节长度
     */
    protected int byteLength;
    /**
     * 交换网络输入向量长度
     */
    protected int n;
    /**
     * benes网络
     */
    protected BenesNetwork<byte[]> benesNetwork;
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

    protected AbstractOsnReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, OsnConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
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

    protected void setPtoInput(int[] permutationMap, int byteLength) {
        assert byteLength >= CommonConstants.STATS_BYTE_LENGTH;
        this.byteLength = byteLength;
        assert BenesNetworkUtils.validPermutation(permutationMap) && permutationMap.length <= maxN;
        n = permutationMap.length;
        benesNetwork = BenesNetworkFactory.createInstance(envType, permutationMap);
        level = benesNetwork.getLevel();
        width = benesNetwork.getWidth();
        switchNum = level * width;
        extraInfo++;
    }
}
