package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleReceiverOutput;

import java.util.Arrays;

/**
 * abstract GF2K-SSP-VOLE receiver.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public abstract class AbstractGf2kSspVoleReceiver extends AbstractTwoPartyPto implements Gf2kSspVoleReceiver {
    /**
     * config
     */
    protected final Gf2kSspVoleConfig config;
    /**
     * GF2K instance
     */
    protected final Gf2k gf2k;
    /**
     * Δ
     */
    protected byte[] delta;
    /**
     * max num
     */
    private int maxNum;
    /**
     * num
     */
    protected int num;

    protected AbstractGf2kSspVoleReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, Gf2kSspVoleConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        this.config = config;
        gf2k = Gf2kFactory.createInstance(envType);
    }

    protected void setInitInput(byte[] delta, int maxNum) {
        MathPreconditions.checkEqual("Δ.length", "λ(B)", delta.length, CommonConstants.BLOCK_BYTE_LENGTH);
        this.delta = BytesUtils.clone(delta);
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
        initState();
    }

    protected void setPtoInput(int num) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("num", num, maxNum);
        this.num = num;
        extraInfo++;
    }

    protected void setPtoInput(int num, Gf2kVoleReceiverOutput preReceiverOutput) {
        setPtoInput(num);
        Preconditions.checkArgument(Arrays.equals(delta, preReceiverOutput.getDelta()));
        MathPreconditions.checkGreaterOrEqual(
            "preNum", preReceiverOutput.getNum(), Gf2kSspVoleFactory.getPrecomputeNum(config, num)
        );
    }
}
