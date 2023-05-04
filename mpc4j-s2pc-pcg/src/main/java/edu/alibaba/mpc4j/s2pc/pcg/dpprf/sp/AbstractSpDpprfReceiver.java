package edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

import java.util.stream.IntStream;

/**
 * abstract single-point DPPRF receiver.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public abstract class AbstractSpDpprfReceiver extends AbstractTwoPartyPto implements SpDpprfReceiver {
    /**
     * config
     */
    protected final SpDpprfConfig config;
    /**
     * max α upper bound
     */
    protected int maxAlphaBound;
    /**
     * max α bit length
     */
    protected int maxH;
    /**
     * α upper bound
     */
    protected int alphaBound;
    /**
     * α array
     */
    protected int alpha;
    /**
     * binary α
     */
    protected boolean[] binaryAlpha;
    /**
     * negative binary α
     */
    protected boolean[] notBinaryAlpha;
    /**
     * α bit length
     */
    protected int h;

    protected AbstractSpDpprfReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, SpDpprfConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        this.config = config;
    }

    protected void setInitInput(int maxAlphaBound) {
        MathPreconditions.checkPositive("maxAlphaBound", maxAlphaBound);
        this.maxAlphaBound = maxAlphaBound;
        maxH = LongUtils.ceilLog2(maxAlphaBound, 1);
        initState();
    }

    protected void setPtoInput(int alpha, int alphaBound) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("alphaBound", alphaBound, maxAlphaBound);
        this.alphaBound = alphaBound;
        h = LongUtils.ceilLog2(alphaBound, 1);
        MathPreconditions.checkNonNegativeInRange("alpha", alpha, alphaBound);
        this.alpha = alpha;
        int offset = Integer.SIZE - h;
        binaryAlpha = new boolean[h];
        notBinaryAlpha = new boolean[h];
        byte[] alphaBytes = IntUtils.intToByteArray(alpha);
        IntStream.range(0, h).forEach(i -> {
            // parse α in binary format
            binaryAlpha[i] = BinaryUtils.getBoolean(alphaBytes, offset + i);
            notBinaryAlpha[i] = !binaryAlpha[i];
        });
        extraInfo++;
    }

    protected void setPtoInput(int alpha, int alphaBound, CotReceiverOutput preReceiverOutput) {
        setPtoInput(alpha, alphaBound);
        MathPreconditions.checkGreaterOrEqual(
            "preCotNum", preReceiverOutput.getNum(), SpDpprfFactory.getPrecomputeNum(config, alphaBound)
        );
    }
}
