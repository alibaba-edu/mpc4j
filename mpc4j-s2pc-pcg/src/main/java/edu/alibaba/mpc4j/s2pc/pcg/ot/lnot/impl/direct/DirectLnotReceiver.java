package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.impl.direct;

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
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.AbstractLnotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotReceiverOutput;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * abstract 1-out-of-n (with n = 2^l) OT receiver.
 *
 * @author Weiran Liu
 * @date 2023/4/13
 */
public class DirectLnotReceiver extends AbstractLnotReceiver {
    /**
     * LCOT receiver
     */
    private final LcotReceiver lcotReceiver;
    /**
     * key derivation function
     */
    private final Kdf kdf;
    /**
     * output bit length
     */
    private int outputBitLength;

    public DirectLnotReceiver(Rpc receiverRpc, Party senderParty, DirectLnotConfig config) {
        super(DirectLnotPtoDesc.getInstance(), receiverRpc, senderParty, config);
        lcotReceiver = LcotFactory.createReceiver(receiverRpc, senderParty, config.getLcotConfig());
        addSubPto(lcotReceiver);
        kdf = KdfFactory.createInstance(envType);
    }

    @Override
    public void init(int l, int updateNum) throws MpcAbortException {
        setInitInput(l, updateNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        outputBitLength = lcotReceiver.init(l, updateNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public LnotReceiverOutput receive(int[] choiceArray) throws MpcAbortException {
        setPtoInput(choiceArray);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        int offset = Integer.BYTES - byteL;
        byte[][] choices = IntStream.range(0, num)
            .mapToObj(index -> {
                byte[] choiceBytes = IntUtils.intToByteArray(choiceArray[index]);
                byte[] fixedChoiceBytes = new byte[byteL];
                System.arraycopy(choiceBytes, offset, fixedChoiceBytes, 0, byteL);
                return fixedChoiceBytes;
            })
            .toArray(byte[][]::new);
        LcotReceiverOutput lcotReceiverOutput = LcotReceiverOutput.createEmpty(l, outputBitLength);
        if (num <= updateNum) {
            // we only need to run single round
            lcotReceiverOutput.merge(lcotReceiver.receive(choices));
        } else {
            // we need to run multiple round
            int currentNum = lcotReceiverOutput.getNum();
            int round = 0;
            while (currentNum < num) {
                int roundNum = Math.min((num - currentNum), updateNum);
                byte[][] roundChoices = new byte[roundNum][];
                System.arraycopy(choices, round * updateNum, roundChoices, 0, roundNum);
                lcotReceiverOutput.merge(lcotReceiver.receive(roundChoices));
                round++;
                currentNum = lcotReceiverOutput.getNum();
            }
        }
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
