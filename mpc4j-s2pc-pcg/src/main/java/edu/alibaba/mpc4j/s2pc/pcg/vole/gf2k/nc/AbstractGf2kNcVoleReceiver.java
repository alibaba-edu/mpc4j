package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

/**
 * abstract no-choice GF2K-VOLE receiver.
 *
 * @author Weiran Liu
 * @date 2023/7/23
 */
public abstract class AbstractGf2kNcVoleReceiver extends AbstractTwoPartyPto implements Gf2kNcVoleReceiver {
    /**
     * config
     */
    private final Gf2kNcVoleConfig config;
    /**
     * GF2K instance
     */
    protected final Gf2k gf2k;
    /**
     * Δ
     */
    protected byte[] delta;
    /**
     * num
     */
    protected int num;

    protected AbstractGf2kNcVoleReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, Gf2kNcVoleConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        this.config = config;
        gf2k = Gf2kFactory.createInstance(envType);
    }

    protected void setInitInput(byte[] delta, int num) {
        MathPreconditions.checkEqual("Δ.length", "λ(B)", delta.length, CommonConstants.BLOCK_BYTE_LENGTH);
        this.delta = BytesUtils.clone(delta);
        MathPreconditions.checkPositiveInRangeClosed("num", num, config.maxNum());
        this.num = num;
        initState();
    }

    protected void setPtoInput() {
        checkInitialized();
        extraInfo++;
    }
}
