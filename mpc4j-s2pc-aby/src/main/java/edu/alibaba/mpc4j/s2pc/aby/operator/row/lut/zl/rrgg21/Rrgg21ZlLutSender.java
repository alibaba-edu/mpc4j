package edu.alibaba.mpc4j.s2pc.aby.operator.row.lut.zl.rrgg21;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.lut.zl.AbstractZlLutSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotSenderOutput;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.aby.operator.row.lut.zl.rrgg21.Rrgg21ZlLutPtoDesc.PtoStep;
import static edu.alibaba.mpc4j.s2pc.aby.operator.row.lut.zl.rrgg21.Rrgg21ZlLutPtoDesc.getInstance;

/**
 * RRGG21 Zl lookup table protocol sender.
 *
 * @author Liqiang Peng
 * @date 2024/5/30
 */
public class Rrgg21ZlLutSender extends AbstractZlLutSender {
    /**
     * 1-out-of-n (with n = 2^l) OT sender
     */
    private final LnotSender lnotSender;
    /**
     * prg
     */
    private Prg prg;

    public Rrgg21ZlLutSender(Rpc senderRpc, Party receiverParty, Rrgg21ZlLutConfig config) {
        super(getInstance(), senderRpc, receiverParty, config);
        lnotSender = LnotFactory.createSender(senderRpc, receiverParty, config.getLnotConfig());
        addSubPto(lnotSender);
    }

    @Override
    public void init(int maxM, int maxN, int maxNum) throws MpcAbortException {
        setInitInput(maxM, maxN, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        lnotSender.init(maxM, maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void lookupTable(byte[][][] table, int m, int n) throws MpcAbortException {
        setPtoInput(table, m, n);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        LnotSenderOutput lnotSenderOutput = lnotSender.send(num);
        stopWatch.stop();
        long lnotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, lnotTime);

        stopWatch.start();
        prg = PrgFactory.createInstance(envType, byteN);
        IntStream intStream = IntStream.range(0, num);
        intStream = parallel ? intStream.parallel() : intStream;
        List<byte[]> encPayload = intStream
            .mapToObj(i ->
                IntStream.range(0, 1 << m)
                    .mapToObj(j -> {
                        byte[] bytes = lnotSenderOutput.getRb(i, j);
                        return BytesUtils.xor(table[i][j], prg.extendToBytes(bytes));
                    })
                    .collect(Collectors.toList()))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
        stopWatch.stop();
        long evaluationTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, evaluationTime);

        DataPacketHeader encHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SENDS_ENC_ELEMENTS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(encHeader, encPayload));

        logPhaseInfo(PtoState.PTO_END);
    }
}
