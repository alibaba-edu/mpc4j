package edu.alibaba.mpc4j.s2pc.aby.operator.row.trunc.zl.rrk20;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.drelu.zl.ZlDreluFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.drelu.zl.ZlDreluParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.trunc.zl.AbstractZlTruncParty;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotSenderOutput;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.aby.operator.row.trunc.zl.rrk20.Rrk20ZlTruncPtoDesc.PtoStep;
import static edu.alibaba.mpc4j.s2pc.aby.operator.row.trunc.zl.rrk20.Rrk20ZlTruncPtoDesc.getInstance;

/**
 * RRK+20 Zl Truncation Sender.
 *
 * @author Liqiang Peng
 * @date 2023/10/2
 */
public class Rrk20ZlTruncSender extends AbstractZlTruncParty {
    /**
     * DReLU sender
     */
    private final ZlDreluParty dreluSender;
    /**
     * 1-out-of-n (with n = 2^l) ot sender.
     */
    private final LnotSender lnotSender;
    /**
     * corr
     */
    private ZlVector corr;

    public Rrk20ZlTruncSender(Rpc senderRpc, Party receiverParty, Rrk20ZlTruncConfig config) {
        super(getInstance(), senderRpc, receiverParty, config);
        dreluSender = ZlDreluFactory.createSender(senderRpc, receiverParty, config.getZlDreluConfig());
        addSubPto(dreluSender);
        lnotSender = LnotFactory.createSender(senderRpc, receiverParty, config.getLnotConfig());
        addSubPto(lnotSender);
    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
        setInitInput(maxL, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        dreluSender.init(maxL, maxNum);
        lnotSender.init(2, maxL * maxNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZlVector trunc(SquareZlVector xi, int s) throws MpcAbortException {
        setPtoInput(xi, s);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // msb(xi)
        SquareZ2Vector msb = getMsbBitVector(xi);
        // DReLU
        SquareZ2Vector drelu = dreluSender.drelu(xi);
        stopWatch.stop();
        long dreluTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, dreluTime);

        stopWatch.start();
        LnotSenderOutput lnotSenderOutput = lnotSender.send(num);
        List<byte[]> sPayload = generateCorrPayload(drelu, lnotSenderOutput, msb);
        DataPacketHeader sHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SENDS_S.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(sHeader, sPayload));
        stopWatch.stop();
        long lnotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, lnotTime);

        stopWatch.start();
        BigInteger[] r1 = IntStream.range(0, num)
            .mapToObj(i -> BigInteger.ONE.shiftLeft(l - s))
            .toArray(BigInteger[]::new);
        ZlVector shift = ZlVector.create(zl, r1);
        ZlVector r = rDiv(xi.getZlVector().getElements(), zl.getRangeBound(), s);
        r.setParallel(parallel);
        r.addi(corr.mul(shift));
        SquareZlVector squareZlVector = SquareZlVector.create(r, false);
        stopWatch.stop();
        long handleOutputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, handleOutputTime);

        logPhaseInfo(PtoState.PTO_END);
        return squareZlVector;
    }

    private SquareZ2Vector getMsbBitVector(SquareZlVector xi) {
        BitVector msbBitVector = BitVectorFactory.createZeros(num);
        IntStream.range(0, num).forEach(i -> {
            BigInteger x = xi.getZlVector().getElement(i);
            msbBitVector.set(i, x.testBit(l - 1));
        });
        return SquareZ2Vector.create(msbBitVector, false);
    }

    private List<byte[]> generateCorrPayload(SquareZ2Vector drelu, LnotSenderOutput lnotSenderOutput, SquareZ2Vector msb) {
        corr = ZlVector.createRandom(zl, num, secureRandom);
        corr.setParallel(parallel);
        ZlVector[] s = new ZlVector[4];
        for (int i = 0; i < 4; i++) {
            BitVector j0, j1, t;
            if (i == 0) {
                j0 = BitVectorFactory.createZeros(num);
                j1 = BitVectorFactory.createZeros(num);
            } else if (i == 1) {
                j0 = BitVectorFactory.createZeros(num);
                j1 = BitVectorFactory.createOnes(num);
            } else if (i == 2) {
                j0 = BitVectorFactory.createOnes(num);
                j1 = BitVectorFactory.createZeros(num);
            } else {
                j0 = BitVectorFactory.createOnes(num);
                j1 = BitVectorFactory.createOnes(num);
            }
            BitVector t1 = drelu.getBitVector().xor(j0).xor(msb.getBitVector());
            BitVector t2 = drelu.getBitVector().xor(j0).xor(j1);
            t = t1.and(t2);
            IntStream intStream = IntStream.range(0, num);
            intStream = parallel ? intStream.parallel() : intStream;
            BigInteger[] sIntArray = intStream.mapToObj(index -> {
                BigInteger c = corr.getElement(index);
                if (t.get(index) & !msb.getBitVector().get(index)) {
                    return zl.sub(zl.neg(c), BigInteger.ONE);
                } else if (t.get(index) & msb.getBitVector().get(index)) {
                    return zl.add(zl.neg(c), BigInteger.ONE);
                } else {
                    return zl.neg(c);
                }
            }).toArray(BigInteger[]::new);
            s[i] = ZlVector.create(zl, sIntArray);
        }
        List<byte[]> corrPayload = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            int finalI = i;
            IntStream intStream = IntStream.range(0, num);
            intStream = parallel ? intStream.parallel() : intStream;
            BigInteger[] randomInts = intStream
                .mapToObj(index -> zl.createRandom(lnotSenderOutput.getRb(index, finalI)))
                .toArray(BigInteger[]::new);
            ZlVector rb = s[i].add(ZlVector.create(zl, randomInts));
            corrPayload.addAll(IntStream.range(0, num)
                .mapToObj(index -> BigIntegerUtils.bigIntegerToByteArray(rb.getElement(index)))
                .collect(Collectors.toList())
            );
        }
        return corrPayload;
    }

    private ZlVector rDiv(BigInteger[] input, BigInteger n, int s) {
        BigInteger nPrime = n.shiftRight(1);
        IntStream intStream = IntStream.range(0, num);
        intStream = parallel ? intStream.parallel() : intStream;
        BigInteger[] shiftElements =  intStream.mapToObj(index -> {
            if (input[index].compareTo(nPrime) < 0) {
                return input[index].shiftRight(s);
            } else {
                return input[index].subtract(n).shiftRight(s).mod(n);
            }
        }).toArray(BigInteger[]::new);
        return ZlVector.create(zl, shiftElements);
    }
}
