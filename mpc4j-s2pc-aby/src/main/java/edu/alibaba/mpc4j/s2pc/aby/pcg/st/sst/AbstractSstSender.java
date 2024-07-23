package edu.alibaba.mpc4j.s2pc.aby.pcg.st.sst;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

/**
 * abstract Single Share Translation sender.
 *
 * @author Weiran Liu
 * @date 2024/4/22
 */
public abstract class AbstractSstSender extends AbstractTwoPartyPto implements SstSender {
    /**
     * config
     */
    protected final SstConfig config;
    /**
     * n
     */
    protected int num;
    /**
     * permutation π
     */
    protected int[] pi;
    /**
     * element byte length
     */
    protected int byteLength;
    /**
     * log(n)
     */
    protected int cotNum;

    protected AbstractSstSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, SstConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        this.config = config;
    }

    protected void setInitInput() {
        initState();
    }

    protected void setPtoInput(int[] pi, int byteLength) {
        checkInitialized();
        Preconditions.checkArgument(PermutationNetworkUtils.validPermutation(pi));
        num = pi.length;
        this.pi = pi;
        MathPreconditions.checkPositive("byte_length", byteLength);
        this.byteLength = byteLength;
        cotNum = SstFactory.getPrecomputeNum(config, num);
        extraInfo++;
    }

    protected void setPtoInput(int[] pi, int byteLength, CotReceiverOutput preReceiverOutput) {
        setPtoInput(pi, byteLength);
        if (preReceiverOutput != null) {
            MathPreconditions.checkGreaterOrEqual(
                "preCotNum", preReceiverOutput.getNum(), SstFactory.getPrecomputeNum(config, num)
            );
        }

    }
}
