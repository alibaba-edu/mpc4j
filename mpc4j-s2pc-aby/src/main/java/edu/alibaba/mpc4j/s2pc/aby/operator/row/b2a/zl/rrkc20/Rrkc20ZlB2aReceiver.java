package edu.alibaba.mpc4j.s2pc.aby.operator.row.b2a.zl.rrkc20;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.b2a.zl.AbstractZlB2aParty;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.RotReceiverOutput;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.aby.operator.row.b2a.zl.rrkc20.Rrkc20ZlB2aPtoDesc.*;

/**
 * RRKC20 Zl boolean to arithmetic protocol receiver.
 *
 * @author Liqiang Peng
 * @date 2024/6/4
 */
public class Rrkc20ZlB2aReceiver extends AbstractZlB2aParty {
    /**
     * cot receiver.
     */
    private final CotReceiver cotReceiver;

    public Rrkc20ZlB2aReceiver(Rpc receiverRpc, Party senderParty, Rrkc20ZlB2aConfig config) {
        super(getInstance(), receiverRpc, senderParty, config);
        cotReceiver = CotFactory.createReceiver(receiverRpc, senderParty, config.getCotConfig());
        addSubPto(cotReceiver);
    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
        setInitInput(maxL, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        cotReceiver.init(maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZlVector b2a(MpcZ2Vector yi, Zl zl) throws MpcAbortException {
        setPtoInput(yi, zl);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        boolean[] ys = BinaryUtils.byteArrayToBinary(yi.getBitVector().getBytes(), num);
        CotReceiverOutput cotReceiverOutput = cotReceiver.receive(ys);
        RotReceiverOutput rotReceiverOutput = new RotReceiverOutput(envType, CrhfFactory.CrhfType.MMO, cotReceiverOutput);
        DataPacketHeader senderMessageHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SENDS_ENC_ELEMENTS.ordinal(), extraInfo++,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> senderMessagePayload = rpc.receive(senderMessageHeader).getPayload();
        MpcAbortPreconditions.checkArgument(senderMessagePayload.size() == num * 2);
        byte[][] senderMessageFlattenArray = senderMessagePayload.toArray(new byte[0][]);
        int messageByteLength = IntUtils.boundedNonNegIntByteLength(num);
        IntStream intStream = IntStream.range(0, num);
        intStream = parallel ? intStream.parallel() : intStream;
        int[] rs = intStream
            .map(index -> {
                byte[] keyi = Arrays.copyOf(rotReceiverOutput.getRb(index), messageByteLength);
                byte[] choiceCiphertext = ys[index] ?
                    senderMessageFlattenArray[index * 2 + 1] : senderMessageFlattenArray[index * 2];
                BytesUtils.xori(choiceCiphertext, keyi);
                return IntUtils.fixedByteArrayToNonNegInt(choiceCiphertext);
            })
            .toArray();
        BigInteger[] as = (parallel ? IntStream.range(0, num).parallel() : IntStream.range(0, num))
            .mapToObj(i -> BigInteger.valueOf((yi.getBitVector().get(i) ? 1 : 0) - rs[i] * 2L).mod(zl.getRangeBound()))
            .toArray(BigInteger[]::new);
        stopWatch.stop();
        long b2aTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, b2aTime);

        logPhaseInfo(PtoState.PTO_END);
        return SquareZlVector.create(zl, as, false);
    }
}
