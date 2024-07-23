package edu.alibaba.mpc4j.s2pc.aby.operator.row.lut.zl.rrgg21;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.lut.zl.AbstractZlLutReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotReceiverOutput;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.aby.operator.row.lut.zl.rrgg21.Rrgg21ZlLutPtoDesc.PtoStep;
import static edu.alibaba.mpc4j.s2pc.aby.operator.row.lut.zl.rrgg21.Rrgg21ZlLutPtoDesc.getInstance;

/**
 * RRGG21 Zl lookup table protocol receiver.
 *
 * @author Liqiang Peng
 * @date 2024/5/30
 */
public class Rrgg21ZlLutReceiver extends AbstractZlLutReceiver {
    /**
     * 1-out-of-n (with n = 2^l) OT receiver
     */
    private final LnotReceiver lnotReceiver;
    /**
     * prg
     */
    private Prg prg;

    public Rrgg21ZlLutReceiver(Rpc receiverRpc, Party senderParty, Rrgg21ZlLutConfig config) {
        super(getInstance(), receiverRpc, senderParty, config);
        lnotReceiver = LnotFactory.createReceiver(receiverRpc, senderParty, config.getLnotConfig());
        addSubPto(lnotReceiver);
    }


    @Override
    public void init(int maxM, int maxN, int maxNum) throws MpcAbortException {
        setInitInput(maxM, maxN, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        lnotReceiver.init(maxM, maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public byte[][] lookupTable(byte[][] inputs, int m, int n) throws MpcAbortException {
        setPtoInput(inputs, m, n);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        int[] choices = IntStream.range(0, num)
            .map(i -> IntUtils.fixedByteArrayToNonNegInt(inputs[i]))
            .toArray();
        LnotReceiverOutput lnotReceiverOutput = lnotReceiver.receive(choices);
        stopWatch.stop();
        long lnotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, lnotTime);

        DataPacketHeader encHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SENDS_ENC_ELEMENTS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        ArrayList<byte[]> encPayload = (ArrayList<byte[]>) rpc.receive(encHeader).getPayload();

        stopWatch.start();
        prg = PrgFactory.createInstance(envType, byteN);
        IntStream intStream = IntStream.range(0, num);
        intStream = parallel ? intStream.parallel() : intStream;
        byte[][] result = intStream
            .mapToObj(i -> {
                byte[] bytes = lnotReceiverOutput.getRb(i);
                return BytesUtils.xor(prg.extendToBytes(bytes), encPayload.get(i * (1 << m) + choices[i]));
            })
            .toArray(byte[][]::new);
        stopWatch.stop();
        long evaluationTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, evaluationTime);

        logPhaseInfo(PtoState.PTO_END);
        return result;
    }
}
