package edu.alibaba.mpc4j.s2pc.aby.pcg.st.pst.cgp20;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.BstReceiverOutput;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.bst.cgp20.Cgp20BstReceiver;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.pst.AbstractPstReceiver;
import edu.alibaba.mpc4j.s2pc.aby.pcg.st.pst.PstReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

import java.util.concurrent.TimeUnit;

/**
 * direct use CGP20 BST
 *
 * @author Feng Han
 * @date 2024/8/6
 */
public class Cgp20PstReceiver extends AbstractPstReceiver implements PstReceiver {
    /**
     * CGP20 BST receiver
     */
    private final Cgp20BstReceiver bstReceiver;

    public Cgp20PstReceiver(Rpc receiverRpc, Party senderParty, Cgp20PstConfig config) {
        super(Cgp20PstPtoDesc.getInstance(), receiverRpc, senderParty, config);
        bstReceiver = new Cgp20BstReceiver(receiverRpc, senderParty, config.getBstConfig());
        addSubPto(bstReceiver);
    }

    @Override
    public void init() throws MpcAbortException {
        setInitInput();
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        bstReceiver.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public BstReceiverOutput shareTranslate(int batchNum, int eachNum, int byteLength, boolean isLeft) throws MpcAbortException {
        return bstReceiver.shareTranslate(batchNum, eachNum, byteLength);
    }

    @Override
    public BstReceiverOutput shareTranslate(int batchNum, int eachNum, int byteLength, CotSenderOutput preSenderOutput, boolean isLeft) throws MpcAbortException {
        return bstReceiver.shareTranslate(batchNum, eachNum, byteLength, preSenderOutput);
    }
}
