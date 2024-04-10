package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.nc.direct;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.nc.AbstractNcLnotReceiver;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * direct no-choice 1-out-of-n (with n = 2^l) OT receiver.
 *
 * @author Weiran Liu
 * @date 2023/4/11
 */
public class DirectNcLnotReceiver extends AbstractNcLnotReceiver {
    /**
     * LCOT receiver
     */
    private final LcotReceiver lcotReceiver;
    /**
     * key derivation function
     */
    private final Kdf kdf;

    public DirectNcLnotReceiver(Rpc receiverRpc, Party senderParty, DirectNcLnotConfig config) {
        super(DirectNcLnotPtoDesc.getInstance(), receiverRpc, senderParty, config);
        lcotReceiver = LcotFactory.createReceiver(receiverRpc, senderParty, config.getLcotConfig());
        addSubPto(lcotReceiver);
        kdf = KdfFactory.createInstance(envType);
    }

    @Override
    public void init(int l, int num) throws MpcAbortException {
        setInitInput(l, num);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        lcotReceiver.init(l, num);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public LnotReceiverOutput receive() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // randomly generate choice array
        int[] choiceArray = IntStream.range(0, num)
            .map(index -> secureRandom.nextInt(n))
            .toArray();
        int offset = Integer.BYTES - byteL;
        byte[][] choiceBytesArray = IntStream.range(0, num)
            .mapToObj(index -> {
                byte[] choiceBytes = IntUtils.intToByteArray(choiceArray[index]);
                byte[] fixedChoiceBytes = new byte[byteL];
                System.arraycopy(choiceBytes, offset, fixedChoiceBytes, 0, byteL);
                return fixedChoiceBytes;
            })
            .toArray(byte[][]::new);
        LcotReceiverOutput lcotReceiverOutput = lcotReceiver.receive(choiceBytesArray);
        stopWatch.stop();
        long lcotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, lcotTime);

        stopWatch.start();
        // convert LCOT receiver output to be LNOT receiver output
        byte[][] rbArray = IntStream.range(0, num)
            .mapToObj(index -> {
                byte[] rb = lcotReceiverOutput.getRb(index);
                return kdf.deriveKey(rb);
            })
            .toArray(byte[][]::new);
        LnotReceiverOutput receiverOutput = LnotReceiverOutput.create(l, choiceArray, rbArray);
        stopWatch.stop();
        long convertTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, convertTime);

        logPhaseInfo(PtoState.PTO_END);
        return receiverOutput;
    }
}
