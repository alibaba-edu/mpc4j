package edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

import java.util.stream.IntStream;

/**
 * abstract single-point RDPPRF receiver.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public abstract class AbstractSpRdpprfReceiver extends AbstractTwoPartyPto implements SpRdpprfReceiver {
    /**
     * config
     */
    protected final SpRdpprfConfig config;
    /**
     * n
     */
    protected int num;
    /**
     * log(n)
     */
    protected int logNum;
    /**
     * α
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

    protected AbstractSpRdpprfReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, SpRdpprfConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        this.config = config;
    }

    protected void setInitInput() {
        initState();
    }

    protected void setPtoInput(int alpha, int num) {
        checkInitialized();
        MathPreconditions.checkPositive("num", num);
        this.num = num;
        logNum = SpRdpprfFactory.getPrecomputeNum(config, num);
        MathPreconditions.checkNonNegativeInRange("α", alpha, num);
        this.alpha = alpha;
        int offset = Integer.SIZE - logNum;
        binaryAlpha = new boolean[logNum];
        notBinaryAlpha = new boolean[logNum];
        byte[] alphaBytes = IntUtils.intToByteArray(alpha);
        IntStream.range(0, logNum).forEach(i -> {
            // parse α in binary format
            binaryAlpha[i] = BinaryUtils.getBoolean(alphaBytes, offset + i);
            notBinaryAlpha[i] = !binaryAlpha[i];
        });
        extraInfo++;
    }

    protected void setPtoInput(int alpha, int num, CotReceiverOutput preReceiverOutput) {
        setPtoInput(alpha, num);
        if (preReceiverOutput != null) {
            MathPreconditions.checkGreaterOrEqual(
                "preCotNum", preReceiverOutput.getNum(), SpRdpprfFactory.getPrecomputeNum(config, num));
        }
    }
}
