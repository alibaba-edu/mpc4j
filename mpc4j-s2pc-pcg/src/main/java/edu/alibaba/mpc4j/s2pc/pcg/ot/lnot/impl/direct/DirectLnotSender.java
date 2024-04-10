package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.impl.direct;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.AbstractLnotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotSenderOutput;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * direct 1-out-of-n (with n = 2^l) OT sender.
 *
 * @author Weiran Liu
 * @date 2023/4/13
 */
public class DirectLnotSender extends AbstractLnotSender {
    /**
     * LCOT sender
     */
    private final LcotSender lcotSender;
    /**
     * key derivation function
     */
    private final Kdf kdf;
    /**
     * Δ
     */
    private byte[] delta;

    public DirectLnotSender(Rpc senderRpc, Party receiverParty, DirectLnotConfig config) {
        super(DirectLnotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        lcotSender = LcotFactory.createSender(senderRpc, receiverParty, config.getLcotConfig());
        addSubPto(lcotSender);
        kdf = KdfFactory.createInstance(envType);
    }

    @Override
    public void init(int l, int updateNum) throws MpcAbortException {
        setInitInput(l, updateNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        delta = lcotSender.init(l, updateNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public LnotSenderOutput send(int num) throws MpcAbortException {
        setPtoInput(num);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        LcotSenderOutput lcotSenderOutput = LcotSenderOutput.createEmpty(l, delta);
        if (num <= updateNum) {
            lcotSenderOutput.merge(lcotSender.send(num));
        } else {
            int currentNum = lcotSenderOutput.getNum();
            while (currentNum < num) {
                int roundNum = Math.min((num - currentNum), updateNum);
                lcotSenderOutput.merge(lcotSender.send(roundNum));
                currentNum = lcotSenderOutput.getNum();
            }
        }
        stopWatch.stop();
        long lcotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, lcotTime);

        stopWatch.start();
        // convert LCOT sender output to be LNOT sender output
        int offset = Integer.BYTES - byteL;
        byte[][][] rsArray = IntStream.range(0, num)
            .mapToObj(index -> {
                byte[][] rs = new byte[n][];
                byte[] choiceBytes;
                byte[] fixedChoiceBytes = new byte[byteL];
                for (int choice = 0; choice < n; choice++) {
                    choiceBytes = IntUtils.intToByteArray(choice);
                    System.arraycopy(choiceBytes, offset, fixedChoiceBytes, 0, byteL);
                    byte[] ri = lcotSenderOutput.getRb(index, fixedChoiceBytes);
                    rs[choice] = kdf.deriveKey(ri);
                }
                return rs;
            })
            .toArray(byte[][][]::new);
        LnotSenderOutput senderOutput = LnotSenderOutput.create(l, rsArray);
        stopWatch.stop();
        long convertTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, convertTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }
}
