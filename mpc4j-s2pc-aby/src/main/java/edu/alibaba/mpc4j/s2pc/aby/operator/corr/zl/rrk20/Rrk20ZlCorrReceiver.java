package edu.alibaba.mpc4j.s2pc.aby.operator.corr.zl.rrk20;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.operator.corr.zl.AbstractZlCorrParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.drelu.zl.ZlDreluFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.drelu.zl.ZlDreluParty;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotReceiverOutput;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.aby.operator.corr.zl.rrk20.Rrk20ZlCorrPtoDesc.*;

/**
 * RRK+20 Zl Corr Receiver.
 *
 * @author Liqiang Peng
 * @date 2023/10/2
 */
public class Rrk20ZlCorrReceiver extends AbstractZlCorrParty {
    /**
     * DReLU receiver
     */
    private final ZlDreluParty dreluReceiver;
    /**
     * most significant bit.
     */
    private SquareZ2Vector msb;
    /**
     * 1-out-of-n (with n = 2^l) ot receiver.
     */
    private final LnotReceiver lnotReceiver;

    public Rrk20ZlCorrReceiver(Rpc receiverRpc, Party senderParty, Rrk20ZlCorrConfig config) {
        super(getInstance(), receiverRpc, senderParty, config);
        dreluReceiver = ZlDreluFactory.createReceiver(receiverRpc, senderParty, config.getZlDreluConfig());
        addSubPto(dreluReceiver);
        lnotReceiver = LnotFactory.createReceiver(receiverRpc, senderParty, config.getLnotConfig());
        addSubPto(lnotReceiver);
    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
        setInitInput(maxL, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        dreluReceiver.init(maxL, maxNum);
        lnotReceiver.init(2, maxL * maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZlVector corr(SquareZlVector xi) throws MpcAbortException {
        setPtoInput(xi);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        getMsbBitVector(xi);
        SquareZ2Vector drelu = dreluReceiver.drelu(xi);
        SquareZ2Vector oneZ2Vector = SquareZ2Vector.createOnes(num);
        drelu.getBitVector().xori(oneZ2Vector.getBitVector());
        stopWatch.stop();
        long prepareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, prepareTime);

        stopWatch.start();
        int[] choice = IntStream.range(0, num)
            .map(i -> (drelu.getBitVector().get(i) ? 1 : 0) * 2 + (msb.getBitVector().get(i) ? 1 : 0))
            .toArray();
        LnotReceiverOutput lnotReceiverOutput = lnotReceiver.receive(choice);
        stopWatch.stop();
        long lnotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, lnotTime);

        DataPacketHeader sHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SENDS_S.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> sPayload = rpc.receive(sHeader).getPayload();
        MpcAbortPreconditions.checkArgument(sPayload.size() == 4 * num);

        stopWatch.start();
        ZlVector corr = handleCorrPayload(sPayload, lnotReceiverOutput);
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, ptoTime);

        logPhaseInfo(PtoState.PTO_END);
        return SquareZlVector.create(corr, false);
    }

    private void getMsbBitVector(SquareZlVector xi) {
        BitVector msbBitVector = BitVectorFactory.createZeros(num);
        IntStream.range(0, num).forEach(i -> {
            BigInteger x = xi.getZlVector().getElement(i);
            msbBitVector.set(i, x.testBit(l - 1));
        });
        msb = SquareZ2Vector.create(msbBitVector, false);
    }

    private ZlVector handleCorrPayload(List<byte[]> sList, LnotReceiverOutput lnotReceiverOutput) {
        byte[][] sArray = sList.toArray(new byte[0][]);
        IntStream intStream = IntStream.range(0, num);
        intStream = parallel ? intStream.parallel() : intStream;
        BigInteger[] t = intStream
            .mapToObj(index -> {
                byte[] rv = lnotReceiverOutput.getRb(index);
                return zl.createRandom(rv);
            }).toArray(BigInteger[]::new);
        BigInteger[] rb = IntStream.range(0, num)
            .mapToObj(index -> {
                int v = lnotReceiverOutput.getChoice(index);
                return BigIntegerUtils.byteArrayToBigInteger(sArray[v * num + index]);
            }).toArray(BigInteger[]::new);
        return ZlVector.create(zl, rb).sub(ZlVector.create(zl, t));
    }
}
