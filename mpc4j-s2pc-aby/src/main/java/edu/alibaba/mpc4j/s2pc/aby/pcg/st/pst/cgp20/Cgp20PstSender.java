package edu.alibaba.mpc4j.s2pc.aby.pcg.st.pst.cgp20;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.BstSenderOutput;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.cgp20.Cgp20BstSender;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.pst.AbstractPstSender;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.pst.PstSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

import java.util.concurrent.TimeUnit;

/**
 * direct use CGP20 BST
 *
 * @author Feng Han
 * @date 2024/8/6
 */
public class Cgp20PstSender extends AbstractPstSender implements PstSender {
    /**
     * CGP20 BST receiver
     */
    private final Cgp20BstSender bstSender;

    public Cgp20PstSender(Rpc senderRpc, Party receiverParty, Cgp20PstConfig config) {
        super(Cgp20PstPtoDesc.getInstance(), senderRpc, receiverParty, config);
        bstSender = new Cgp20BstSender(senderRpc, receiverParty, config.getBstConfig());
        addSubPto(bstSender);
    }

    @Override
    public void init() throws MpcAbortException {
        setInitInput();
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        bstSender.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public BstSenderOutput shareTranslate(int[][] piArray, int byteLength, boolean isLeft) throws MpcAbortException {
        return bstSender.shareTranslate(piArray, byteLength);
    }

    @Override
    public BstSenderOutput shareTranslate(int[][] piArray, int byteLength, CotReceiverOutput preReceiverOutput, boolean isLeft) throws MpcAbortException {
        return bstSender.shareTranslate(piArray, byteLength, preReceiverOutput);
    }
}
