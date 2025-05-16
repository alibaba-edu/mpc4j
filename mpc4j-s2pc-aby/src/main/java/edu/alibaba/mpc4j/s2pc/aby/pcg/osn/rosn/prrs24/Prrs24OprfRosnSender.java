package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.prrs24;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.galoisfield.Z3ByteField;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.AbstractRosnSender;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnSenderOutput;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.prrs24.Prrs24OprfRosnPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.F32SowOprfConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.F32SowOprfFactory;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.F32SowOprfSender;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * PRRS24 OPRF Random OSN sender.
 *
 * @author Weiran Liu
 * @date 2024/6/7
 */
public class Prrs24OprfRosnSender extends AbstractRosnSender {
    /**
     * (F3, F2)-sowOPRF
     */
    private final F32SowOprfSender f32SowOprfSender;
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

    public Prrs24OprfRosnSender(Rpc senderRpc, Party receiverParty, Prrs24OprfRosnConfig config) {
        super(Prrs24OprfRosnPtoDesc.getInstance(), senderRpc, receiverParty, config);
        F32SowOprfConfig f32SowOprfConfig = config.getF32SowOprfConfig();
        f32SowOprfSender = F32SowOprfFactory.createSender(senderRpc, receiverParty, f32SowOprfConfig);
        addSubPto(f32SowOprfSender);
        n = f32SowOprfConfig.getInputLength();
        w = f32SowOprfConfig.getOutputByteLength();
        z3Field = new Z3ByteField();
    }

    @Override
    public void init() throws MpcAbortException {
        setInitInput();
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        f32SowOprfSender.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public RosnSenderOutput rosn(int num, int byteLength) throws MpcAbortException {
        setPtoInput(num, byteLength);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // the receiver samples t ∈ {0, 1}^κ. The receiver sends t to the sender.
        List<byte[]> tPayload = receiveOtherPartyPayload(PtoStep.RECEIVER_SEND_T.ordinal());
        int seedNum = tPayload.size();
        // Let x_{i,j} := H(t, i, j) for i ∈ [num], j ∈ [m] where m := ⌈ℓ/w⌉.
        int m = CommonUtils.getUnitNum(byteLength, w);
        byte[][][] xss = new byte[m][num][n];
        // parallel if needed
        if (seedNum > 1 && num * m > CommonConstants.STATS_BIT_LENGTH * seedNum) {
            int eachLen = (int) Math.ceil(num * 1.0 / tPayload.size());
            IntStream intStream = parallel ? IntStream.range(0, seedNum).parallel() : IntStream.range(0, seedNum);
            intStream.forEach(randIndex -> {
                int startIndex = randIndex * eachLen;
                int endIndex = Math.min(startIndex + eachLen, num);
                SecureRandom h = CommonUtils.createSeedSecureRandom();
                h.setSeed(tPayload.get(randIndex));
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
            h.setSeed(tPayload.get(0));
            for (int i = 0; i < num; i++) {
                for (int j = 0; j < m; j++) {
                    for (int k = 0; k < n; k++) {
                        xss[j][i][k] = z3Field.createRandom(h);
                    }
                }
            }
        }
        // The sender computes A_{i,j} := F_k(x_{i,j}) for i ∈ [n], j ∈ [m].
        byte[][] as = new byte[num][];
        IntStream batchIntStream = IntStream.range(0, num);
        batchIntStream = parallel ? batchIntStream.parallel() : batchIntStream;
        batchIntStream.forEach(i -> {
            ByteBuffer aByteBuffer = ByteBuffer.allocate(m * w);
            for (int j = 0; j < m; j++) {
                aByteBuffer.put(f32SowOprfSender.prf(xss[j][i]));
            }
            byte[] a = aByteBuffer.array();
            as[i] = Arrays.copyOf(a, byteLength);
        });
        stopWatch.stop();
        long inputsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, inputsTime);

        stopWatch.start();
        // The parties invoke F_Sowprf nm times with the sender inputting k and receiver inputting π(x).
        // The parties receive shares [[y_{i,j}]] where y_{i,j} = F_k(x_{π(i)},j) for i ∈ [n], j ∈ [m].
        byte[][] yss = f32SowOprfSender.oprf(m * num);
        // the sender sets B_i := [[y_i]].
        byte[][] bs = new byte[num][];
        for (int i = 0; i < num; i++) {
            ByteBuffer bByteBuffer = ByteBuffer.allocate(m * w);
            for (int j = 0; j < m; j++) {
                bByteBuffer.put(yss[j * num + i]);
            }
            byte[] b = bByteBuffer.array();
            bs[i] = Arrays.copyOf(b, byteLength);
        }
        stopWatch.stop();
        long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, outputTime);

        logPhaseInfo(PtoState.PTO_END);
        return RosnSenderOutput.create(as, bs);
    }
}
