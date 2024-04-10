package edu.alibaba.mpc4j.s2pc.aby.operator.agg.hamming.bcp13;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory.CrhfType;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.hamming.AbstractHammingParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.hamming.bcp13.Bcp13ShHammingPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.RotSenderOutput;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * BCP13半诚实安全汉明距离协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/11/23
 */
public class Bcp13ShHammingSender extends AbstractHammingParty {
    /**
     * COT协议发送方
     */
    private final CotSender cotSender;

    public Bcp13ShHammingSender(Rpc senderRpc, Party receiverParty, Bcp13ShHammingConfig config) {
        super(Bcp13ShHammingPtoDesc.getInstance(), senderRpc, receiverParty, config);
        cotSender = CotFactory.createSender(senderRpc, receiverParty, config.getCotConfig());
        addSubPto(cotSender);
    }

    @Override
    public void init(int maxBitNum) throws MpcAbortException {
        setInitInput(maxBitNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // init COT sender
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(delta);
        cotSender.init(delta, maxBitNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void sendHammingDistance(SquareZ2Vector x0) throws MpcAbortException {
        setPtoInput(x0);
        logPhaseInfo(PtoState.PTO_BEGIN, "Sender sends hamming distance");

        int r = executeOtSteps(x0);
        stopWatch.start();
        List<byte[]> rPayload = new LinkedList<>();
        rPayload.add(IntUtils.boundedNonNegIntToByteArray(r, bitNum));
        DataPacketHeader rHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_R.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(rHeader, rPayload));
        stopWatch.stop();
        long rTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, rTime);

        logPhaseInfo(PtoState.PTO_END, "Sender sends hamming distance");
    }

    @Override
    public int receiveHammingDistance(SquareZ2Vector x0) throws MpcAbortException {
        setPtoInput(x0);
        logPhaseInfo(PtoState.PTO_BEGIN, "Sender receives hamming distance");

        int r = executeOtSteps(x0);
        stopWatch.start();
        DataPacketHeader tHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_T.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> tPayload = rpc.receive(tHeader).getPayload();
        MpcAbortPreconditions.checkArgument(tPayload.size() == 1);
        int t = IntUtils.byteArrayToBoundedNonNegInt(tPayload.remove(0), bitNum);
        int hammingDistance = (t - r) % (bitNum + 1);
        hammingDistance = hammingDistance < 0 ? hammingDistance + bitNum + 1 : hammingDistance;
        stopWatch.stop();
        long rTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, rTime);

        logPhaseInfo(PtoState.PTO_END, "Sender receives hamming distance");
        return hammingDistance;
    }

    private int executeOtSteps(SquareZ2Vector x0) throws MpcAbortException {
        stopWatch.start();
        // P_1 generates n random values r_1, ... r_n \in Z_{n + 1} and computes r = Σ_{i = 1}^n t_i
        int[] rs = new int[bitNum];
        IntStream.range(0, bitNum).forEach(index -> rs[index] = secureRandom.nextInt(bitNum + 1));
        int r = 0;
        for (int index = 0; index < bitNum; index++) {
            r = (r + rs[index]) % (bitNum + 1);
        }
        r = r < 0 ? r + bitNum + 1 : r;
        stopWatch.stop();
        long rsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, rsTime);

        stopWatch.start();
        // P_1 and P_2 engage in a OT_1^2, where P_1 acts as the sender, P_1's input is (r_i + x_i, r_i + \neg x_i).
        CotSenderOutput cotSenderOutput = cotSender.send(bitNum);
        RotSenderOutput rotSenderOutput = new RotSenderOutput(envType, CrhfType.MMO, cotSenderOutput);
        int messageByteLength = IntUtils.boundedNonNegIntByteLength(bitNum);
        int offset = CommonUtils.getByteLength(bitNum) * Byte.SIZE - bitNum;
        List<byte[]> senderMessagePayload = IntStream.range(0, bitNum)
            .mapToObj(index -> {
                byte[] key0 = Arrays.copyOf(rotSenderOutput.getR0(index), messageByteLength);
                byte[] key1 = Arrays.copyOf(rotSenderOutput.getR1(index), messageByteLength);
                int rxi = BinaryUtils.getBoolean(x0.getBitVector().getBytes(), index + offset) ? 1 : 0;
                int negRxi = rxi ^ 1;
                rxi = (rxi +  rs[index]) % (bitNum + 1);
                rxi = rxi < 0 ? rxi + bitNum + 1 : rxi;
                negRxi = (negRxi + rs[index]) % (bitNum + 1);
                negRxi = negRxi < 0 ? negRxi + bitNum + 1 : negRxi;
                byte[][] ciphertexts = new byte[2][];
                ciphertexts[0] = IntUtils.boundedNonNegIntToByteArray(rxi, bitNum);
                ciphertexts[1] = IntUtils.boundedNonNegIntToByteArray(negRxi, bitNum);
                BytesUtils.xori(ciphertexts[0], key0);
                BytesUtils.xori(ciphertexts[1], key1);
                return ciphertexts;
            })
            .flatMap(Arrays::stream)
            .collect(Collectors.toList());
        DataPacketHeader senderMessageHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_PAYLOAD.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(senderMessageHeader, senderMessagePayload));
        stopWatch.stop();
        long otTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, otTime);

        return r;
    }
}
