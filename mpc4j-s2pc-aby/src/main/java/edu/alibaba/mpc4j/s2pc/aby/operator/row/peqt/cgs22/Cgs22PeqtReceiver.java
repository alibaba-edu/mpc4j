package edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.cgs22;

import edu.alibaba.mpc4j.common.rpc.*;
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
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotReceiverOutput;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * CGS22 private equality test receiver.
 *
 * @author Weiran Liu
 * @date 2023/4/14
 */
public class Cgs22PeqtReceiver extends AbstractPeqtParty {
    /**
     * Z2 circuit receiver
     */
    private final Z2cParty z2cReceiver;
    /**
     * LNOT receiver
     */
    private final LnotReceiver lnotReceiver;

    public Cgs22PeqtReceiver(Rpc senderRpc, Party receiverParty, Cgs22PeqtConfig config) {
        super(Cgs22PeqtPtoDesc.getInstance(), senderRpc, receiverParty, config);
        z2cReceiver = Z2cFactory.createReceiver(senderRpc, receiverParty, config.getZ2cConfig());
        addSubPto(z2cReceiver);
        lnotReceiver = LnotFactory.createReceiver(senderRpc, receiverParty, config.getLnotConfig());
        addSubPto(lnotReceiver);
    }

    @Override
    public void init(int maxL, int maxNum) throws MpcAbortException {
        setInitInput(maxL, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // q = l / m, where m = 4
        int maxByteL = CommonUtils.getByteLength(maxL);
        int maxQ = maxByteL * 2;
        z2cReceiver.init(maxNum * (maxQ - 1));
        lnotReceiver.init(4, maxNum * maxQ);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZ2Vector peqt(int l, byte[][] ys) throws MpcAbortException {
        setPtoInput(l, ys);
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
        // P1 creates all-zero eq_{0,j} for all j ∈ [0,q)
        BitVector[] eqs = new BitVector[q];
        for (int j = 0; j < q; j++) {
            eqs[j] = BitVectorFactory.createZeros(num);
        }
        // for j ∈ [0, q) do
        for (int j = 0; j < q; j++) {
            // P0 & P1 invoke 1-out-of-2^4 OT with P1 as receiver.
            LnotReceiverOutput lnotReceiverOutput = lnotReceiver.receive(partitionInputArray[j]);
            DataPacketHeader evsHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_EVS.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
            );
            // for v ∈ [2^4], P1 receives e_{0,j}_1
            List<byte[]> evsPayload = rpc.receive(evsHeader).getPayload();
            extraInfo++;
            MpcAbortPreconditions.checkArgument(evsPayload.size() == 1 << 4);
            BitVector[] evs = evsPayload.stream()
                .map(ev -> BitVectorFactory.create(num, ev))
                .toArray(BitVector[]::new);
            for (int index = 0; index < num; index++) {
                int v = lnotReceiverOutput.getChoice(index);
                byte[] rv = lnotReceiverOutput.getRb(index);
                eqs[j].set(index, evs[v].get(index) ^ ((rv[0] % 2) != 0));
            }
        }
        stopWatch.stop();
        long lnotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, lnotTime);

        stopWatch.start();
        SquareZ2Vector z1 = combine(eqs, q);
        stopWatch.stop();
        long bitwiseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, bitwiseTime);

        logPhaseInfo(PtoState.PTO_END);
        return z1;
    }

    private int[][] partitionInputArray(int q) {
        // P1 parses each of its input element as y_{q-1} || ... || y_{0}, where y_j ∈ {0,1}^4 for all j ∈ [0,q).
        int[][] partitionInputArray = new int[q][num];
        IntStream.range(0, num).forEach(index -> {
            byte[] y = inputs[index];
            for (int lIndex = 0; lIndex < byteL; lIndex++) {
                byte lIndexByte = y[lIndex];
                // the left part
                partitionInputArray[lIndex * 2][index] = ((lIndexByte & 0xFF) >> 4);
                // the right part
                partitionInputArray[lIndex * 2 + 1][index] = (lIndexByte & 0x0F);
            }
        });
        return partitionInputArray;
    }

    private SquareZ2Vector combine(BitVector[] eqs, int q) throws MpcAbortException {
        SquareZ2Vector[] eqs1 = new SquareZ2Vector[q];
        for (int j = 0; j < q; j++) {
            eqs1[j] = SquareZ2Vector.create(eqs[j], false);
        }
        int logQ = LongUtils.ceilLog2(q);
        // for t = 1 to log(q) do
        for (int t = 1; t <= logQ; t++) {
            // P1 invokes F_AND with inputs <eq_{t-1,2j}_1 and <eq_{t-1,2j+1}_1 to learn output <eq_{t,j}>_1
            int nodeNum = eqs1.length / 2;
            SquareZ2Vector[] eqsx1 = new SquareZ2Vector[nodeNum];
            SquareZ2Vector[] eqsy1 = new SquareZ2Vector[nodeNum];
            for (int i = 0; i < nodeNum; i++) {
                eqsx1[i] = eqs1[i * 2];
                eqsy1[i] = eqs1[i * 2 + 1];
            }
            SquareZ2Vector[] eqsz1 = z2cReceiver.and(eqsx1, eqsy1);
            if (eqs1.length % 2 == 1) {
                eqsz1 = Arrays.copyOf(eqsz1, nodeNum + 1);
                eqsz1[nodeNum] = eqs1[eqs1.length - 1];
            }
            eqs1 = eqsz1;
        }
        return eqs1[0];
    }
}
