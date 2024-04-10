package edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.nc.direct;

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
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.nc.AbstractNcLnotSender;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * direct no-choice 1-out-of-n (with n = 2^l) OT sender.
 *
 * @author Weiran Liu
 * @date 2023/4/11
 */
public class DirectNcLnotSender extends AbstractNcLnotSender {
    /**
     * LCOT sender
     */
    private final LcotSender lcotSender;
    /**
     * key derivation function
     */
    private final Kdf kdf;

    public DirectNcLnotSender(Rpc senderRpc, Party receiverParty, DirectNcLnotConfig config) {
        super(DirectNcLnotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        lcotSender = LcotFactory.createSender(senderRpc, receiverParty, config.getLcotConfig());
        addSubPto(lcotSender);
        kdf = KdfFactory.createInstance(envType);
    }

    @Override
    public void init(int l, int num) throws MpcAbortException {
        setInitInput(l, num);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        lcotSender.init(l, num);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public LnotSenderOutput send() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        LcotSenderOutput lcotSenderOutput = lcotSender.send(num);
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
