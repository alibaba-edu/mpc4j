package edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.cgs22;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.AbstractPeqtParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.cgs22.Cgs22PeqtPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotSenderOutput;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * CGS22 private equality test sender.
 *
 * @author Weiran Liu
 * @date 2023/4/14
 */
public class Cgs22PeqtSender extends AbstractPeqtParty {
    /**
     * Z2 circuit sender
     */
    private final Z2cParty Z2cSender;
    /**
     * LNOT sender
     */
    private final LnotSender lnotSender;

    public Cgs22PeqtSender(Rpc senderRpc, Party receiverParty, Cgs22PeqtConfig config) {
        super(Cgs22PeqtPtoDesc.getInstance(), senderRpc, receiverParty, config);
        Z2cSender = Z2cFactory.createSender(senderRpc, receiverParty, config.getZ2cConfig());
        addSubPto(Z2cSender);
        lnotSender = LnotFactory.createSender(senderRpc, receiverParty, config.getLnotConfig());
        addSubPto(lnotSender);
    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
        setInitInput(maxL, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // q = l / m, where m = 4
        int maxByteL = CommonUtils.getByteLength(maxL);
        int maxQ = maxByteL * 2;
        Z2cSender.init(maxNum * (maxQ - 1));
        lnotSender.init(4, maxNum * maxQ);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZ2Vector peqt(int l, byte[][] xs) throws MpcAbortException {
        setPtoInput(l, xs);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // q = l/4
        int q = byteL * 2;
        int[][] partitionInputArray = partitionInputArray(q);
        stopWatch.stop();
        long prepareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, prepareTime);

        stopWatch.start();
        // P0 samples eq_{0,j} for all j ∈ [0,q)
        BitVector[] eqs = new BitVector[q];
        for (int j = 0; j < q; j++) {
            eqs[j] = BitVectorFactory.createZeros(num);
        }
        // for j ∈ [0,q) do
        for (int j = 0; j < q; j++) {
            final int jFinal = j;
            // P0 & P1 invoke 1-out-of-2^4 OT with P0 as sender.
            LnotSenderOutput lnotSenderOutput = lnotSender.send(num);
            // for v ∈ [2^4], P0 sets e_{j,v} ← <eq_{0,j}>_0 ⊕ 1{x_{1,j} = v}
            IntStream vIntStream = IntStream.range(0, 1 << 4);
            vIntStream = parallel ? vIntStream.parallel() : vIntStream;
            List<byte[]> evsPayload = vIntStream
                .mapToObj(v -> {
                    BitVector ev = BitVectorFactory.createRandom(num, secureRandom);
                    for (int index = 0; index < num; index++) {
                        byte[] ri = lnotSenderOutput.getRb(index, v);
                        if (v == partitionInputArray[index][jFinal]) {
                            // x_j == v, e_{j,v} = Rb ⊕ 1
                            ev.set(index, (ri[0] % 2) == 0);
                        } else {
                            // x_j != v, e_{j,v} = Rb
                            ev.set(index, (ri[0] % 2) != 0);
                        }
                    }
                    // e_{j,v} ⊕ eqs_j
                    ev.xori(eqs[jFinal]);
                    return ev.getBytes();
                })
                .collect(Collectors.toList());
            DataPacketHeader evsHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_EVS.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
            );
            extraInfo++;
            rpc.send(DataPacket.fromByteArrayList(evsHeader, evsPayload));
        }
        stopWatch.stop();
        long lnotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, lnotTime);

        stopWatch.start();
        SquareZ2Vector z0 = combine(eqs, q);
        stopWatch.stop();
        long bitwiseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, bitwiseTime);

        logPhaseInfo(PtoState.PTO_END);
        return z0;
    }

    private int[][] partitionInputArray(int q) {
        // P0 parses each of its input element as x_{q-1} || ... || x_{0}, where x_j ∈ {0,1}^4 for all j ∈ [0,q).
        int[][] partitionInputArray = new int[num][q];
        IntStream.range(0, num).forEach(index -> {
            byte[] x = inputs[index];
            for (int lIndex = 0; lIndex < byteL; lIndex++) {
                byte lIndexByte = x[lIndex];
                // the left part
                partitionInputArray[index][lIndex * 2] = ((lIndexByte & 0xFF) >> 4);
                // the right part
                partitionInputArray[index][lIndex * 2 + 1] = (lIndexByte & 0x0F);
            }
        });
        return partitionInputArray;
    }

    private SquareZ2Vector combine(BitVector[] eqs, int q) throws MpcAbortException {
        SquareZ2Vector[] eqs0 = new SquareZ2Vector[q];
        for (int j = 0; j < q; j++) {
            eqs0[j] = SquareZ2Vector.create(eqs[j], false);
        }
        int logQ = LongUtils.ceilLog2(q);
        // for t = 1 to log(q) do
        for (int t = 1; t <= logQ; t++) {
            // P0 invokes F_AND with inputs <eq_{t-1,2j}_0 and <eq_{t-1,2j+1}_0 to learn output <eq_{t,j}>_0
            int nodeNum = eqs0.length / 2;
            SquareZ2Vector[] eqsx0 = new SquareZ2Vector[nodeNum];
            SquareZ2Vector[] eqsy0 = new SquareZ2Vector[nodeNum];
            for (int i = 0; i < nodeNum; i++) {
                eqsx0[i] = eqs0[i * 2];
                eqsy0[i] = eqs0[i * 2 + 1];
            }
            SquareZ2Vector[] eqsz0 = Z2cSender.and(eqsx0, eqsy0);
            if (eqs0.length % 2 == 1) {
                eqsz0 = Arrays.copyOf(eqsz0, nodeNum + 1);
                eqsz0[nodeNum] = eqs0[eqs0.length - 1];
            }
            eqs0 = eqsz0;
        }
        return eqs0[0];
    }
}
