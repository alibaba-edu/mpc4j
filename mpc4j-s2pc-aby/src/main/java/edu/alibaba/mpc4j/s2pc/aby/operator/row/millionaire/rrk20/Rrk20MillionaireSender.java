package edu.alibaba.mpc4j.s2pc.aby.operator.row.millionaire.rrk20;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.millionaire.AbstractMillionaireParty;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotSenderOutput;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * RRK+20 Millionaire Protocol Sender.
 *
 * @author Li Peng
 * @date 2023/4/24
 */
public class Rrk20MillionaireSender extends AbstractMillionaireParty {
    /**
     * 1-out-of-n (with n = 2^l) ot sender.
     */
    private final LnotSender lnotSender;
    /**
     * z2 circuit sender.
     */
    private final Z2cParty z2cSender;
    /**
     * bit length of split block
     */
    private final int m;

    public Rrk20MillionaireSender(Z2cParty z2cSender, Party receiverParty, Rrk20MillionaireConfig config) {
        super(Rrk20MillionairePtoDesc.getInstance(), z2cSender.getRpc(), receiverParty, config);
        lnotSender = LnotFactory.createSender(z2cSender.getRpc(), receiverParty, config.getLnotConfig());
        addSubPto(lnotSender);
        this.z2cSender = z2cSender;
        addSubPto(z2cSender);
        this.m = config.getM();
        MathPreconditions.checkPositiveInRangeClosed("m", m, Byte.SIZE);
    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
        setInitInput(maxL, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // q = l / m
        int maxQ = CommonUtils.getUnitNum(maxL, m);
        lnotSender.init(m, maxNum * maxQ);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZ2Vector lt(int l, byte[][] xs) throws MpcAbortException {
        setPtoInput(l, m, xs);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        int[][] partitionInputArray = Rrk20MillionaireUtils.partitionInputArray(inputs, m, q);
        stopWatch.stop();
        long prepareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, prepareTime);

        stopWatch.start();
        SquareZ2Vector[][] shares = iterateSubstrings(partitionInputArray);
        stopWatch.stop();
        long iterateTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, iterateTime);

        stopWatch.start();
        SquareZ2Vector z0 = combine(shares);
        stopWatch.stop();
        long combineTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, combineTime);

        logPhaseInfo(PtoState.PTO_END);

        return z0;
    }


    private SquareZ2Vector[][] iterateSubstrings(int[][] partitionInputArray) throws MpcAbortException {
        // P0 samples random lt_{0,j},eq_{0,j} for all j ∈ [0,q)
        BitVector[] lts = new BitVector[q];
        BitVector[] eqs = new BitVector[q];
        for (int j = 0; j < q; j++) {
            lts[j] = BitVectorFactory.createRandom(BitVectorFactory.BitVectorType.BYTES_BIT_VECTOR, num, secureRandom);
            eqs[j] = BitVectorFactory.createRandom(BitVectorFactory.BitVectorType.BYTES_BIT_VECTOR, num, secureRandom);
        }
        // for j ∈ [0,q) do
        stopWatch.stop();
        stopWatch.reset();

        stopWatch.start();
        for (int j = 0; j < q; j++) {
            final int jFinal = j;
            // P0 & P1 invoke 1-out-of-2^m OT with P0 as sender.
            LnotSenderOutput lnotSenderOutput = lnotSender.send(num);
            // for k ∈ [2^m], P0 sets s_{j,k} ← <lt_{0,j}>_0 ⊕ 1{x_{1,j} < k}
            IntStream intStream = IntStream.range(0, 1 << m);
            intStream = parallel ? intStream.parallel() : intStream;
            List<byte[]> sPayload = intStream
                .mapToObj(k -> {
                    BitVector s = BitVectorFactory.createRandom(BitVectorFactory.BitVectorType.BYTES_BIT_VECTOR, num, secureRandom);
                    for (int index = 0; index < num; index++) {
                        byte[] ri = lnotSenderOutput.getRb(index, k);
                        if (partitionInputArray[jFinal][index] < k) {
                            // x_j < k, s_{j,k} = Rb ⊕ 1
                            s.set(index, (ri[0] % 2) == 0);
                        } else {
                            // x_j >= k, s_{j,k} = Rb
                            s.set(index, (ri[0] % 2) != 0);
                        }
                    }
                    // s_{j,k} ⊕ lts_j
                    s.xori(lts[jFinal]);
                    return s.getBytes();
                })
                .collect(Collectors.toList());
            DataPacketHeader sHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), Rrk20MillionairePtoDesc.PtoStep.SENDER_SENDS_S.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
            );
            extraInfo++;
            rpc.send(DataPacket.fromByteArrayList(sHeader, sPayload));
            // for k ∈ [2^m], P0 sets t_{j,k} ← <eq_{0,j}>_0 ⊕ 1{x_{1,j} == k}
            intStream = IntStream.range(0, 1 << m);
            intStream = parallel ? intStream.parallel() : intStream;
            List<byte[]> tPayload = intStream
                .mapToObj(k -> {
                    BitVector t = BitVectorFactory.createRandom(BitVectorFactory.BitVectorType.BYTES_BIT_VECTOR, num, secureRandom);
                    for (int index = 0; index < num; index++) {
                        byte[] ri = lnotSenderOutput.getRb(index, k);
                        if (partitionInputArray[jFinal][index] == k) {
                            // x_j == k, eq_{j,k} = Rb ⊕ 1
                            t.set(index, (ri[0] % 2) == 0);
                        } else {
                            // x_j == k, e_{j,k} = Rb
                            t.set(index, (ri[0] % 2) != 0);
                        }
                    }
                    // t_{j,k} ⊕ eqs_j
                    t.xori(eqs[jFinal]);
                    return t.getBytes();
                })
                .collect(Collectors.toList());
            DataPacketHeader tHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), Rrk20MillionairePtoDesc.PtoStep.SENDER_SENDS_T.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
            );
            extraInfo++;
            rpc.send(DataPacket.fromByteArrayList(tHeader, tPayload));
        }
        stopWatch.stop();
        long convertTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, convertTime, "test!");

        stopWatch.start();
        SquareZ2Vector[] ltShares = Arrays.stream(lts).map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);
        SquareZ2Vector[] eqShares = Arrays.stream(eqs).map(v -> SquareZ2Vector.create(v, false)).toArray(SquareZ2Vector[]::new);
        return new SquareZ2Vector[][]{ltShares, eqShares};
    }

    private SquareZ2Vector combine(SquareZ2Vector[][] shares) throws MpcAbortException {
        SquareZ2Vector[] lts = shares[0];
        SquareZ2Vector[] eqs = shares[1];
        // tree-based AND
        int logQ = LongUtils.ceilLog2(q);
        int currentNodeNum = q / 2;
        int lastNodeNum = q;
        for (int i = 1; i <= logQ; i++) {
            SquareZ2Vector[] leftLts = new SquareZ2Vector[currentNodeNum];
            SquareZ2Vector[] rightLts = new SquareZ2Vector[currentNodeNum];
            SquareZ2Vector[] leftEqs = new SquareZ2Vector[currentNodeNum];
            SquareZ2Vector[] rightEqs = new SquareZ2Vector[currentNodeNum];
            for (int j = 0; j < currentNodeNum; j++) {
                leftLts[j] = lts[j * 2];
                rightLts[j] = lts[j * 2 + 1];
                leftEqs[j] = eqs[j * 2];
                rightEqs[j] = eqs[j * 2 + 1];
            }
            SquareZ2Vector[] newLts = z2cSender.xor(z2cSender.and(rightLts, leftEqs), leftLts);
            SquareZ2Vector[] newEqs = z2cSender.and(leftEqs, rightEqs);
            if (lastNodeNum % 2 == 1) {
                newLts = Arrays.copyOf(newLts, currentNodeNum + 1);
                newLts[currentNodeNum] = lts[lastNodeNum - 1];
                newEqs = Arrays.copyOf(newEqs, currentNodeNum + 1);
                newEqs[currentNodeNum] = eqs[lastNodeNum - 1];
                currentNodeNum++;
            }
            lastNodeNum = currentNodeNum;
            currentNodeNum = currentNodeNum / 2;
            lts = newLts;
            eqs = newEqs;
        }
        return lts[0];
    }
}
