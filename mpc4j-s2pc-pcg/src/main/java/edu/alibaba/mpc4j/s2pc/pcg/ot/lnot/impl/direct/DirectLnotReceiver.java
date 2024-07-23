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
     * round num
     */
    private int roundNum;

    public DirectLnotReceiver(Rpc receiverRpc, Party senderParty, DirectLnotConfig config) {
        super(DirectLnotPtoDesc.getInstance(), receiverRpc, senderParty, config);
        lcotReceiver = LcotFactory.createReceiver(receiverRpc, senderParty, config.getLcotConfig());
        addSubPto(lcotReceiver);
        kdf = KdfFactory.createInstance(envType);
    }

    @Override
    public void init(int l, int expectNum) throws MpcAbortException {
        setInitInput(l, expectNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        roundNum = Math.min(config.defaultRoundNum(l), expectNum);
        lcotReceiver.init(l);
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
        IntStream intStream = parallel ? IntStream.range(0, num).parallel() : IntStream.range(0, num);
        byte[][] choices = intStream
            .mapToObj(index -> {
                byte[] choiceBytes = IntUtils.intToByteArray(choiceArray[index]);
                byte[] fixedChoiceBytes = new byte[byteL];
                System.arraycopy(choiceBytes, offset, fixedChoiceBytes, 0, byteL);
                return fixedChoiceBytes;
            })
            .toArray(byte[][]::new);
        LcotReceiverOutput lcotReceiverOutput = LcotReceiverOutput.createEmpty(l);
        while (num > lcotReceiverOutput.getNum()) {
            int gapNum = num - lcotReceiverOutput.getNum();
            int eachNum = Math.min(gapNum, roundNum);
            byte[][] roundChoices = new byte[eachNum][];
            System.arraycopy(choices, lcotReceiverOutput.getNum(), roundChoices, 0, eachNum);
            lcotReceiverOutput.merge(lcotReceiver.receive(roundChoices));
        }
        stopWatch.stop();
        long roundTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, roundTime);

        stopWatch.start();
        // convert LCOT receiver output to be LNOT receiver output
        intStream = parallel ? IntStream.range(0, num).parallel() : IntStream.range(0, num);
        byte[][] rbArray = intStream
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
