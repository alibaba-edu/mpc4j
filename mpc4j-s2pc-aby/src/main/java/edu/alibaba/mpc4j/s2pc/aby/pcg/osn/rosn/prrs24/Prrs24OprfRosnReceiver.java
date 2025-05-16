package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.prrs24;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.galoisfield.Z3ByteField;
import edu.alibaba.mpc4j.common.tool.network.PermutationNetworkUtils;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.AbstractRosnReceiver;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnReceiverOutput;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.prrs24.Prrs24OprfRosnPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.F32SowOprfConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.F32SowOprfFactory;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.F32SowOprfReceiver;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * PRRS24 OPRF Random OSN receiver.
 *
 * @author Weiran Liu
 * @date 2024/6/7
 */
public class Prrs24OprfRosnReceiver extends AbstractRosnReceiver {
    /**
     * (F3, F2)-sowOPRF
     */
    private final F32SowOprfReceiver f32SowOprfReceiver;
    /**
     * Let F : K × X → F be a weak PRF, where F ∈ {0, 1}^w
     */
    private final int w;
    /**
     * Let F : K × X → F be a weak PRF, where X ∈ F_3^n.
     */
    private final int n;
    /**
     * Z3-Field
     */
    private final Z3ByteField z3Field;

    public Prrs24OprfRosnReceiver(Rpc receiverRpc, Party senderParty, Prrs24OprfRosnConfig config) {
        super(Prrs24OprfRosnPtoDesc.getInstance(), receiverRpc, senderParty, config);
        F32SowOprfConfig f32SowOprfConfig = config.getF32SowOprfConfig();
        f32SowOprfReceiver = F32SowOprfFactory.createReceiver(receiverRpc, senderParty, f32SowOprfConfig);
        addSubPto(f32SowOprfReceiver);
        n = f32SowOprfConfig.getInputLength();
        w = f32SowOprfConfig.getOutputByteLength();
        z3Field = new Z3ByteField();
    }

    @Override
    public void init() throws MpcAbortException {
        setInitInput();
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        f32SowOprfReceiver.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public RosnReceiverOutput rosn(int[] pi, int byteLength) throws MpcAbortException {
        setPtoInput(pi, byteLength);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // the receiver samples t ∈ {0, 1}^κ. The receiver sends t to the sender.
        int selfParallelNum = parallel ? ForkJoinPool.getCommonPoolParallelism() : 1;
        byte[][] randomSeeds = BlockUtils.randomBlocks(selfParallelNum, secureRandom);
        sendOtherPartyPayload(PtoStep.RECEIVER_SEND_T.ordinal(), Arrays.stream(randomSeeds).toList());
        // Let x_{i,j} := H(t, i, j) for i ∈ [num], j ∈ [m] where m := ⌈ℓ/w⌉.
        int m = CommonUtils.getUnitNum(byteLength, w);
        byte[][][] xss = new byte[m][num][n];
        // parallel if needed
        if (parallel && num * m > CommonConstants.STATS_BIT_LENGTH * selfParallelNum) {
            int eachLen = (int) Math.ceil(num * 1.0 / selfParallelNum);
            IntStream.range(0, selfParallelNum).parallel().forEach(randIndex -> {
                int startIndex = randIndex * eachLen;
                int endIndex = Math.min(startIndex + eachLen, num);
                SecureRandom h = CommonUtils.createSeedSecureRandom();
                h.setSeed(randomSeeds[randIndex]);
                for (int i = startIndex; i < endIndex; i++) {
                    for (int j = 0; j < m; j++) {
                        for (int k = 0; k < n; k++) {
                            xss[j][i][k] = z3Field.createRandom(h);
                        }
                    }
                }
            });
        } else {
            SecureRandom h = CommonUtils.createSeedSecureRandom();
            h.setSeed(randomSeeds[0]);
            for (int i = 0; i < num; i++) {
                for (int j = 0; j < m; j++) {
                    for (int k = 0; k < n; k++) {
                        xss[j][i][k] = z3Field.createRandom(h);
                    }
                }
            }
        }
        stopWatch.stop();
        long inputsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, inputsTime);

        stopWatch.start();
        // The parties invoke F_Sowprf nm times with the sender inputting k and receiver inputting π(x).
        // The parties receive shares [[y_{i,j}]] where y_{i,j} = F_k(x_{π(i)},j) for i ∈ [n], j ∈ [m].
        byte[][][] pixss = new byte[m][num][n];
        for (int j = 0; j < m; j++) {
            pixss[j] = PermutationNetworkUtils.permutation(pi, xss[j]);
        }
        byte[][] pixs = Arrays.stream(pixss)
            .flatMap(Arrays::stream)
            .toArray(byte[][]::new);
        byte[][] yss = f32SowOprfReceiver.oprf(pixs);
        // The receiver sets C_i := [[y_i]]
        byte[][] cs = new byte[num][];
        for (int i = 0; i < num; i++) {
            ByteBuffer cByteBuffer = ByteBuffer.allocate(m * w);
            for (int j = 0; j < m; j++) {
                cByteBuffer.put(yss[j * num + i]);
            }
            byte[] c = cByteBuffer.array();
            cs[i] = Arrays.copyOf(c, byteLength);
        }
        stopWatch.stop();
        long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, outputTime);

        logPhaseInfo(PtoState.PTO_END);
        return RosnReceiverOutput.create(pi, cs);
    }
}
