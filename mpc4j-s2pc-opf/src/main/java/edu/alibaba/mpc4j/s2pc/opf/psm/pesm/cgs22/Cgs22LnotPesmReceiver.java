package edu.alibaba.mpc4j.s2pc.opf.psm.pesm.cgs22;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.opf.psm.pesm.AbstractPesmReceiver;
import edu.alibaba.mpc4j.s2pc.opf.psm.pesm.cgs22.Cgs22LnotPesmPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotReceiverOutput;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * CGS22 1-out-of-n (with n = 2^l) OT based PESM receiver.
 *
 * @author Weiran Liu
 * @date 2023/4/16
 */
public class Cgs22LnotPesmReceiver extends AbstractPesmReceiver {
    /**
     * Boolean circuit receiver
     */
    private final Z2cParty bcReceiver;
    /**
     * LNOT receiver
     */
    private final LnotReceiver lnotReceiver;

    public Cgs22LnotPesmReceiver(Rpc senderRpc, Party receiverParty, Cgs22LnotPesmConfig config) {
        super(Cgs22LnotPesmPtoDesc.getInstance(), senderRpc, receiverParty, config);
        bcReceiver = Z2cFactory.createReceiver(senderRpc, receiverParty, config.getZ2cConfig());
        addSubPto(bcReceiver);
        lnotReceiver = LnotFactory.createReceiver(senderRpc, receiverParty, config.getLnotConfig());
        addSubPto(lnotReceiver);
    }

    @Override
    public void init(int maxL, int maxD, int maxNum) throws MpcAbortException {
        setInitInput(maxL, maxD, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // q = l / m, where m = 4
        int maxByteL = CommonUtils.getByteLength(maxL);
        int maxQ = maxByteL * 2;
        bcReceiver.init(maxNum * (maxQ - 1) * maxD);
        lnotReceiver.init(4, maxNum * maxQ);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZ2Vector pesm(int l, int d, byte[][] inputArray) throws MpcAbortException {
        setPtoInput(l, d, inputArray);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // q = l/4.
        int q = byteL * 2;
        int[][] partitionInputArray = partitionInputArray(q);
        stopWatch.stop();
        long prepareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, prepareTime);

        stopWatch.start();
        // P1 creates all-zero eq_{0,1,j} || ... || eq_{0,d,j} for all j ∈ [0,q)
        BitVector[][] eqArrays = new BitVector[d][q];
        for (int i = 0; i < d; i++) {
            for (int j = 0; j < q; j++) {
                eqArrays[i][j] = BitVectorFactory.createZeros(num);
            }
        }
        // for j ∈ [0,q) do
        for (int j = 0; j < q; j++) {
            // P0 & P1 invoke 1-out-of-2^4 OT with P1 as receiver.
            LnotReceiverOutput lnotReceiverOutput = lnotReceiver.receive(partitionInputArray[j]);
            DataPacketHeader evsHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_EV_ARRAYS.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
            );
            // for v ∈ [2^4], P1 receives e_{0,1,j}_1 || ... || e_{0,d,j}_1
            List<byte[]> evsPayload = rpc.receive(evsHeader).getPayload();
            extraInfo++;
            MpcAbortPreconditions.checkArgument(evsPayload.size() == (1 << 4) * d);
            BitVector[] evArrays = evsPayload.stream()
                .map(ev -> BitVectorFactory.create(num, ev))
                .toArray(BitVector[]::new);
            for (int index = 0; index < num; index++) {
                int v = lnotReceiverOutput.getChoice(index);
                byte[] rv = lnotReceiverOutput.getRb(index);
                for (int i = 0; i < d; i++) {
                    eqArrays[i][j].set(index, evArrays[v * d + i].get(index) ^ ((rv[0] % 2) != 0));
                }

            }
        }
        stopWatch.stop();
        long lnotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, lnotTime);

        stopWatch.start();
        SquareZ2Vector z1 = combine(eqArrays, q);
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
            byte[] y = inputArray[index];
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

    private SquareZ2Vector combine(BitVector[][] eqArrays, int q) throws MpcAbortException {
        SquareZ2Vector[][] eqArrays1 = new SquareZ2Vector[d][q];
        for (int i = 0; i < d; i++) {
            for (int j = 0; j < q; j++) {
                eqArrays1[i][j] = SquareZ2Vector.create(eqArrays[i][j], false);
            }
        }
        int logQ = LongUtils.ceilLog2(q);
        // for i ∈ [d] do
        for (int i = 0; i < d; i++) {
            // for t = 1 to log(q) do
            for (int t = 1; t <= logQ; t++) {
                // P1 invokes F_AND with inputs <eq_{t-1,i,2j}_1 and <eq_{t-1,i,2j+1}_1 to learn output <eq_{t,i,j}>_1
                int nodeNum = eqArrays1[i].length / 2;
                SquareZ2Vector[] eqsx1 = new SquareZ2Vector[nodeNum];
                SquareZ2Vector[] eqsy1 = new SquareZ2Vector[nodeNum];
                for (int k = 0; k < nodeNum; k++) {
                    eqsx1[k] = eqArrays1[i][k * 2];
                    eqsy1[k] = eqArrays1[i][k * 2 + 1];
                }
                SquareZ2Vector[] eqsz1 = bcReceiver.and(eqsx1, eqsy1);
                if (eqArrays1[i].length % 2 == 1) {
                    eqsz1 = Arrays.copyOf(eqsz1, nodeNum + 1);
                    eqsz1[nodeNum] = eqArrays1[i][eqArrays1[i].length - 1];
                }
                eqArrays1[i] = eqsz1;
            }
        }
        // P1 computes eq_{log(q),1,0}_1 ⊕ ... ⊕ eq_{log(q),d,0}_1
        SquareZ2Vector z1 = SquareZ2Vector.createZeros(num);
        for (int i = 0; i < d; i++) {
            z1 = bcReceiver.xor(z1, eqArrays1[i][0]);
        }
        return z1;
    }
}
