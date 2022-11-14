package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.core;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kGadget;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

/**
 * GF2K-核VOLE接收方抽象类。
 *
 * @author Weiran Liu
 * @date 2022/9/22
 */
public abstract class AbstractGf2kCoreVoleReceiver extends AbstractSecureTwoPartyPto implements Gf2kCoreVoleReceiver {
    /**
     * 配置项
     */
    private final Gf2kCoreVoleConfig config;
    /**
     * GF2K算法
     */
    protected final Gf2k gf2k;
    /**
     * 元素比特长度
     */
    protected final int l;
    /**
     * 元素字节长度
     */
    protected final int byteL;
    /**
     * GF2K小工具
     */
    protected final Gf2kGadget gf2kGadget;
    /**
     * 关联值Δ
     */
    protected byte[] delta;
    /**
     * 关联值Δ的比特表示
     */
    protected boolean[] deltaBinary;
    /**
     * 最大数量
     */
    private int maxNum;
    /**
     * 数量
     */
    protected int num;

    protected AbstractGf2kCoreVoleReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, Gf2kCoreVoleConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        this.config = config;
        gf2k = Gf2kFactory.createInstance(envType);
        l = gf2k.getL();
        byteL = gf2k.getByteL();
        gf2kGadget = new Gf2kGadget(gf2k);
    }

    @Override
    public Gf2kCoreVoleFactory.Gf2kCoreVoleType getPtoType() {
        return config.getPtoType();
    }

    protected void setInitInput(byte[] delta, int maxNum) {
        assert delta.length == byteL : "Δ byte length must be " + byteL + ": " + delta.length;
        // 拷贝一份
        this.delta = BytesUtils.clone(delta);
        deltaBinary = gf2kGadget.bitDecomposition(delta);
        assert maxNum > 0 : "max num must be greater than 0: " + maxNum;
        this.maxNum = maxNum;
        initialized = false;
    }

    protected void setPtoInput(int num) {
        if (!initialized) {
            throw new IllegalStateException("Need init ...");
        }
        assert num > 0 && num <= maxNum : "num must be in range (0, " + maxNum + "]: " + num;
        this.num = num;
        extraInfo++;
    }
}
