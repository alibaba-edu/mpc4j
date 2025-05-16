package edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.sp.gyw23;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.Crhf;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory.CrhfType;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.sp.AbstractSpCdpprfSender;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.sp.SpCdpprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.cdpprf.sp.gyw23.Gyw23SpCdpprfPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotSender;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * GWY23-SP-CDPPRF sender.
 *
 * @author Weiran Liu
 * @date 2024/4/22
 */
public class Gyw23SpCdpprfSender extends AbstractSpCdpprfSender {
    /**
     * core COT
     */
    private final CoreCotSender coreCotSender;
    /**
     * pre-compute COT
     */
    private final PreCotSender preCotSender;
    /**
     * hash that satisfies circular correlation robustness
     */
    private final Crhf hash;
    /**
     * tree depth
     */
    private int h;
    /**
     * COT sender output
     */
    private CotSenderOutput cotSenderOutput;
    /**
     * the GGM tree. It contains (l + 1)-level keys. The i-th level contains 2^i keys.
     */
    private ArrayList<byte[][]> ggmTree;
    /**
     * K_0^i = ⊕_{j ∈ [2^{i - 1}]} s_{2j}^i
     */
    private byte[][] k0s;

    public Gyw23SpCdpprfSender(Rpc senderRpc, Party receiverParty, Gyw23SpCdpprfConfig config) {
        super(Gyw23SpCdpprfPtoDesc.getInstance(), senderRpc, receiverParty, config);
        coreCotSender = CoreCotFactory.createSender(senderRpc, receiverParty, config.getCoreCotConfig());
        addSubPto(coreCotSender);
        preCotSender = PreCotFactory.createSender(senderRpc, receiverParty, config.getPreCotConfig());
        addSubPto(preCotSender);
        hash = CrhfFactory.createInstance(envType, CrhfType.MMO);
    }

    @Override
    public void init(byte[] delta) throws MpcAbortException {
        setInitInput(delta);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        coreCotSender.init(delta);
        preCotSender.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SpCdpprfSenderOutput puncture(int num) throws MpcAbortException {
        setPtoInput(num);
        return puncture();
    }

    @Override
    public SpCdpprfSenderOutput puncture(int num, CotSenderOutput preSenderOutput) throws MpcAbortException {
        setPtoInput(num, preSenderOutput);
        cotSenderOutput = preSenderOutput;
        return puncture();
    }

    private SpCdpprfSenderOutput puncture() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        byte[] actualDelta;
        h = LongUtils.ceilLog2(num, 1);
        // P_0 send (extend, h) to F_COT, which returns (K[r_1], ..., K[r_n]) ∈ {{0,1}^κ}^h to P_0
        if (cotSenderOutput == null) {
            cotSenderOutput = coreCotSender.send(cotNum);
            actualDelta = delta;
        } else {
            cotSenderOutput.reduce(cotNum);
            actualDelta = cotSenderOutput.getDelta();
        }
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, cotTime);

        SpCdpprfSenderOutput senderOutput;
        if (num == 1) {
            assert cotNum == 1 && h == 1;
            stopWatch.start();
            cotSenderOutput = preCotSender.send(cotSenderOutput);
            stopWatch.stop();
            long updateCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 2, 3, updateCotTime);

            stopWatch.start();
            senderOutput = new SpCdpprfSenderOutput(actualDelta, new byte[][]{actualDelta});
            cotSenderOutput = null;
            stopWatch.stop();
            long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 3, 3, outputTime);
        } else {
            stopWatch.start();
            cotSenderOutput = preCotSender.send(cotSenderOutput);
            stopWatch.stop();
            long updateCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 2, 3, updateCotTime);

            stopWatch.start();
            // P_0 computes the GGM tree
            generateGgmTree(actualDelta);
            List<byte[]> correlationPayload = generateCorrelationPayload();
            DataPacketHeader correlationDataPacketHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_CORRELATION.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(correlationDataPacketHeader, correlationPayload));
            senderOutput = generateSenderOutput(actualDelta);
            stopWatch.stop();
            long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 3, 3, outputTime);
        }
        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }

    private void generateGgmTree(byte[] actualDelta) {
        k0s = new byte[h][];
        ggmTree = new ArrayList<>(h + 1);
        // treat Δ as the root node
        ggmTree.add(new byte[][]{actualDelta});
        // X_1^0 = k
        byte[][] level1 = BlockUtils.zeroBlocks(2);
        secureRandom.nextBytes(level1[0]);
        // X_1^1 = Δ - k
        level1[1] = BlockUtils.xor(actualDelta, level1[0]);
        // the first level should use randomness
        ggmTree.add(level1);
        // For i ∈ {1,...,h}, j ∈ [2^{i − 1}], do X_i^{2j} = H(X_{i - 1}^j), X_i^{2j + 1} = X_{i - 1}^j - X_i^{2j}
        for (int i = 2; i <= h; i++) {
            byte[][] previousLowLevel = ggmTree.get(i - 1);
            byte[][] currentLevel = new byte[1 << i][];
            for (int j = 0; j < (1 << (i - 1)); j++) {
                // X_i^{2j} = H(X_{i - 1}^j)
                currentLevel[2 * j] = hash.hash(previousLowLevel[j]);
                currentLevel[2 * j + 1] = BlockUtils.xor(previousLowLevel[j], currentLevel[2 * j]);
            }
            ggmTree.add(currentLevel);
        }
        // For each i ∈ {1,...,h}, do K_i^0 = ⊕_{j ∈ [2^{i - 1}]} X_i^{2j}
        for (int i = 1; i <= h; i++) {
            int hIndex = i - 1;
            byte[][] currentLevel = ggmTree.get(i);
            // K_i^0 = ⊕_{j ∈ [2^{i - 1}]} X_i^{2j}
            k0s[hIndex] = BlockUtils.zeroBlock();
            for (int j = 0; j < (1 << (i - 1)); j++) {
                BlockUtils.xori(k0s[hIndex], currentLevel[2 * j]);
            }
        }
    }

    private List<byte[]> generateCorrelationPayload() {
        List<byte[]> correlationPayload = IntStream.range(0, h)
            .mapToObj(hIndex -> {
                // S sends C_i = K_0^i ⊕ K[r_i]
                byte[] ci = cotSenderOutput.getR0(hIndex);
                BlockUtils.xori(ci, k0s[hIndex]);
                return ci;
            })
            .collect(Collectors.toList());
        k0s = null;
        cotSenderOutput = null;
        return correlationPayload;
    }

    private SpCdpprfSenderOutput generateSenderOutput(byte[] actualDelta) {
        byte[][] r0Array = ggmTree.get(h);
        if (num < (1 << h)) {
            byte[][] reduceR0Array = new byte[num][];
            System.arraycopy(r0Array, 0, reduceR0Array, 0, num);
            r0Array = reduceR0Array;
        }
        ggmTree = null;
        return new SpCdpprfSenderOutput(actualDelta, r0Array);
    }
}
