package edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.ywl20;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.Crhf;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.AbstractSpRdpprfSender;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.SpRdpprfSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.dpprf.rdpprf.sp.ywl20.Ywl20SpRdpprfPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotSender;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * YWL20-SP-RDPPRF sender.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public class Ywl20SpRdpprfSender extends AbstractSpRdpprfSender {
    /**
     * core COT sender
     */
    private final CoreCotSender coreCotSender;
    /**
     * pre-compute COT sender
     */
    private final PreCotSender preCotSender;
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
    /**
     * K_1^i = ⊕_{j ∈ [2^{i - 1}]} s_{2j + 1}^i
     */
    private byte[][] k1s;

    public Ywl20SpRdpprfSender(Rpc senderRpc, Party receiverParty, Ywl20SpRdpprfConfig config) {
        super(Ywl20SpRdpprfPtoDesc.getInstance(), senderRpc, receiverParty, config);
        coreCotSender = CoreCotFactory.createSender(senderRpc, receiverParty, config.getCoreCotConfig());
        addSubPto(coreCotSender);
        preCotSender = PreCotFactory.createSender(senderRpc, receiverParty, config.getPreCotConfig());
        addSubPto(preCotSender);
    }

    @Override
    public void init() throws MpcAbortException {
        setInitInput();
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        byte[] delta = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        coreCotSender.init(delta);
        preCotSender.init();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SpRdpprfSenderOutput puncture(int num) throws MpcAbortException {
        setPtoInput(num);
        return puncture();
    }

    @Override
    public SpRdpprfSenderOutput puncture(int num, CotSenderOutput preSenderOutput) throws MpcAbortException {
        setPtoInput(num, preSenderOutput);
        cotSenderOutput = preSenderOutput;
        return puncture();
    }

    private SpRdpprfSenderOutput puncture() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // S send (extend, h) to F_COT, which returns q_i ∈ {0,1}^κ to S
        if (cotSenderOutput == null) {
            cotSenderOutput = coreCotSender.send(logNum);
        } else {
            cotSenderOutput.reduce(logNum);
            // use pre-computed COT to correct the choice bits
            cotSenderOutput = preCotSender.send(cotSenderOutput);
        }
        stopWatch.stop();
        long cotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, cotTime);

        stopWatch.start();
        generatePprfKeys();
        stopWatch.stop();
        long keyGenTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, keyGenTime);

        stopWatch.start();
        List<byte[]> messagePayload = generateMessagePayload();
        DataPacketHeader messageHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_MESSAGE.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(messageHeader, messagePayload));
        SpRdpprfSenderOutput senderOutput = generateSenderOutput();
        stopWatch.stop();
        long messageTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, messageTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }

    private void generatePprfKeys() {
        k0s = new byte[logNum][];
        k1s = new byte[logNum][];
        Prg prg = PrgFactory.createInstance(envType, 2 * CommonConstants.BLOCK_BYTE_LENGTH);
        ggmTree = new ArrayList<>(logNum + 1);
        // S picks a random s_0^0 ∈ {0, 1}^κ
        byte[][] s0 = new byte[1][CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(s0[0]);
        // adds s0 into level-0 GGM tree
        ggmTree.add(s0);
        // For each i ∈ {1,...,h}, j ∈ [2^{i − 1}], S computes (s_{2j}^i, s_{2j + 1}^i) = G(s_j^{i - 1})
        for (int i = 1; i <= logNum; i++) {
            byte[][] lowLevelSeeds = ggmTree.get(i - 1);
            byte[][] currentLevelSeeds = new byte[1 << i][];
            for (int j = 0; j < (1 << (i - 1)); j++) {
                byte[] extendSeeds = prg.extendToBytes(lowLevelSeeds[j]);
                currentLevelSeeds[2 * j] = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                System.arraycopy(
                    extendSeeds, 0,
                    currentLevelSeeds[2 * j], 0,
                    CommonConstants.BLOCK_BYTE_LENGTH
                );
                currentLevelSeeds[2 * j + 1] = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                System.arraycopy(
                    extendSeeds, CommonConstants.BLOCK_BYTE_LENGTH,
                    currentLevelSeeds[2 * j + 1], 0,
                    CommonConstants.BLOCK_BYTE_LENGTH
                );
            }
            ggmTree.add(currentLevelSeeds);
        }
        // For each i ∈ {1,..., h}
        for (int i = 1; i <= logNum; i++) {
            int hIndex = i - 1;
            byte[][] currentLevelSeeds = ggmTree.get(i);
            // S then computes K_0^i = ⊕_{j ∈ [2^{i - 1}]} s_{2j}^i
            k0s[hIndex] = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            for (int j = 0; j < (1 << (i - 1)); j++) {
                BytesUtils.xori(k0s[hIndex], currentLevelSeeds[2 * j]);
            }
            // and K_1^i = ⊕_{j ∈ [2^{i - 1}]} s_{2j + 1}^i
            k1s[hIndex] = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            for (int j = 0; j < (1 << (i - 1)); j++) {
                BytesUtils.xori(k1s[hIndex], currentLevelSeeds[2 * j + 1]);
            }
        }
    }

    private List<byte[]> generateMessagePayload() {
        Crhf crhf = CrhfFactory.createInstance(envType, CrhfFactory.CrhfType.MMO);
        IntStream hIntStream = IntStream.range(0, logNum);
        hIntStream = parallel ? hIntStream.parallel() : hIntStream;
        List<byte[]> messagePayload = hIntStream
            .mapToObj(hIndex -> {
                // S sends M_0^i = K_0^i ⊕ H(q_i, i || l)
                byte[] message0 = cotSenderOutput.getR0(hIndex);
                message0 = crhf.hash(message0);
                BytesUtils.xori(message0, k0s[hIndex]);
                // and M_1^i = K_1^i ⊕ H(q_i ⊕ \not ∆, i || l)
                byte[] message1 = cotSenderOutput.getR1(hIndex);
                message1 = crhf.hash(message1);
                BytesUtils.xori(message1, k1s[hIndex]);
                return new byte[][]{message0, message1};
            })
            .flatMap(Arrays::stream)
            .collect(Collectors.toList());
        k0s = null;
        k1s = null;
        cotSenderOutput = null;
        return messagePayload;
    }

    private SpRdpprfSenderOutput generateSenderOutput() {
        byte[][] v0Array = ggmTree.get(logNum);
        if (num < (1 << logNum)) {
            byte[][] reducePrfKeys = new byte[num][];
            System.arraycopy(v0Array, 0, reducePrfKeys, 0, num);
            v0Array = reducePrfKeys;
        }
        ggmTree = null;
        return new SpRdpprfSenderOutput(v0Array);
    }
}
