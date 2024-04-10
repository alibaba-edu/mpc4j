package edu.alibaba.mpc4j.s2pc.opf.psm.pesm.cgs22;

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
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.opf.psm.pesm.AbstractPesmSender;
import edu.alibaba.mpc4j.s2pc.opf.psm.pesm.cgs22.Cgs22LnotPesmPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lnot.LnotSenderOutput;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * CGS22 1-out-of-n (with n = 2^l) OT based PESM sender.
 *
 * @author Weiran Liu
 * @date 2023/4/16
 */
public class Cgs22LnotPesmSender extends AbstractPesmSender {
    /**
     * Z2 circuit sender
     */
    private final Z2cParty z2cSender;
    /**
     * LNOT sender
     */
    private final LnotSender lnotSender;

    public Cgs22LnotPesmSender(Rpc senderRpc, Party receiverParty, Cgs22LnotPesmConfig config) {
        super(Cgs22LnotPesmPtoDesc.getInstance(), senderRpc, receiverParty, config);
        z2cSender = Z2cFactory.createSender(senderRpc, receiverParty, config.getZ2cConfig());
        addSubPto(z2cSender);
        lnotSender = LnotFactory.createSender(senderRpc, receiverParty, config.getLnotConfig());
        addSubPto(lnotSender);
    }

    @Override
    public void init(int maxL, int maxD, int maxNum) throws MpcAbortException {
        setInitInput(maxL, maxD, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // q = l / m, where m = 4
        int maxByteL = CommonUtils.getByteLength(maxL);
        int maxQ = maxByteL * 2;
        z2cSender.init(maxNum * (maxQ - 1) * maxD);
        lnotSender.init(4, maxNum * maxQ);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZ2Vector pesm(int l, byte[][][] inputArrays) throws MpcAbortException {
        setPtoInput(l, inputArrays);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // q = l/4
        int q = byteL * 2;
        int[][][] partitionInputArrays = partitionInputArrays(q);
        stopWatch.stop();
        long prepareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, prepareTime);

        stopWatch.start();
        // P0 samples eq_{0,i,j} for all i ∈ [1,d], j ∈ [0,q)
        BitVector[][] eqArrays = new BitVector[d][q];
        for (int i = 0; i < d; i++) {
            for (int j = 0; j < q; j++) {
                eqArrays[i][j] = BitVectorFactory.createZeros(num);
            }
        }

        // for j ∈ [0, q) do
        for (int j = 0; j < q; j++) {
            final int jFinal = j;
            // P0 & P1 invoke 1-out-of-2^4 OT with P0 as sender.
            LnotSenderOutput lnotSenderOutput = lnotSender.send(num);
            // for v ∈ [2^4], P0 sets e_{j,v} ← <eq_{0,1,j}>_0 ⊕ 1{x_{1,j} = v} || ... || <eq_{0,d,j}>_0 ⊕ 1{x_{d,j} = v}
            IntStream vIntStream = IntStream.range(0, 1 << 4);
            vIntStream = parallel ? vIntStream.parallel() : vIntStream;
            List<byte[]> evArraysPayload = vIntStream
                .mapToObj(v -> {
                    // P0 samples <eq_{0,i,j>_0 ← {0,1}, ∀i ∈ [d].
                    BitVector[] evArray = new BitVector[d];
                    for (int i = 0; i < d; i++) {
                        evArray[i] = BitVectorFactory.createRandom(num, secureRandom);
                    }
                    for (int index = 0; index < num; index++) {
                        byte[] ri = lnotSenderOutput.getRb(index, v);
                        for (int i = 0; i < d; i++) {
                            if (v == partitionInputArrays[index][i][jFinal]) {
                                // x_j == v, e_{j,v} = Rb ⊕ 1
                                evArray[i].set(index, (ri[0] % 2) == 0);
                            } else {
                                // x_j != v, e_{j,v} = Rb
                                evArray[i].set(index, (ri[0] % 2) != 0);
                            }
                        }
                    }
                    for (int i = 0; i < d; i++) {
                        // e_{j,v} ⊕ eqs_j
                        evArray[i].xori(eqArrays[i][jFinal]);
                    }
                    return evArray;
                })
                .flatMap(Arrays::stream)
                .map(BitVector::getBytes)
                .collect(Collectors.toList());
            DataPacketHeader evArraysHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_EV_ARRAYS.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
            );
            extraInfo++;
            rpc.send(DataPacket.fromByteArrayList(evArraysHeader, evArraysPayload));
        }
        stopWatch.stop();
        long lnotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, lnotTime);

        stopWatch.start();
        SquareZ2Vector z0 = combine(eqArrays, q);
        stopWatch.stop();
        long bitwiseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, bitwiseTime);

        logPhaseInfo(PtoState.PTO_END);
        return z0;
    }

    private int[][][] partitionInputArrays(int q) {
        // P0 parses each of its input element as x_{i,q-1} || ... || x_{i,0},
        // where x_j ∈ {0,1}^4 for all i ∈ [1,d] and j ∈ [0,q).
        int[][][] partitionInputArrays = new int[num][d][q];
        IntStream.range(0, num).forEach(index -> {
            byte[][] inputArray = inputArrays[index];
            for (int i = 0; i < d; i++) {
                byte[] input = inputArray[i];
                for (int lIndex = 0; lIndex < byteL; lIndex++) {
                    byte lIndexByte = input[lIndex];
                    // the left part
                    partitionInputArrays[index][i][lIndex * 2] = ((lIndexByte & 0xFF) >> 4);
                    // the right part
                    partitionInputArrays[index][i][lIndex * 2 + 1] = (lIndexByte & 0x0F);
                }
            }
        });
        return partitionInputArrays;
    }

    private SquareZ2Vector combine(BitVector[][] eqArrays, int q) throws MpcAbortException {
        SquareZ2Vector[][] eqArrays0 = new SquareZ2Vector[d][q];
        for (int i = 0; i < d; i++) {
            for (int j = 0; j < q; j++) {
                eqArrays0[i][j] = SquareZ2Vector.create(eqArrays[i][j], false);
            }
        }
        int logQ = LongUtils.ceilLog2(q);
        // for i ∈ [d] do
        for (int i = 0; i < d; i++) {
            // for t = 1 to log(q) do
            for (int t = 1; t <= logQ; t++) {
                // P0 invokes F_AND with inputs <eq_{t-1,i,2j}_0 and <eq_{t-1,i,2j+1}_0 to learn output <eq_{t,i,j}>_0
                int nodeNum = eqArrays0[i].length / 2;
                SquareZ2Vector[] eqsx0 = new SquareZ2Vector[nodeNum];
                SquareZ2Vector[] eqsy0 = new SquareZ2Vector[nodeNum];
                for (int k = 0; k < nodeNum; k++) {
                    eqsx0[k] = eqArrays0[i][k * 2];
                    eqsy0[k] = eqArrays0[i][k * 2 + 1];
                }
                SquareZ2Vector[] eqsz0 = z2cSender.and(eqsx0, eqsy0);
                if (eqArrays0[i].length % 2 == 1) {
                    eqsz0 = Arrays.copyOf(eqsz0, nodeNum + 1);
                    eqsz0[nodeNum] = eqArrays0[i][eqArrays0[i].length - 1];
                }
                eqArrays0[i] = eqsz0;
            }
        }
        // P1 computes eq_{log(q),1,0}_0 ⊕ ... ⊕ eq_{log(q),d,0}_0
        SquareZ2Vector z0 = SquareZ2Vector.createZeros(num);
        for (int i = 0; i < d; i++) {
            z0 = z2cSender.xor(z0, eqArrays0[i][0]);
        }
        return z0;
    }
}
