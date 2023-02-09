package edu.alibaba.mpc4j.s2pc.aby.basics.bc.bea91;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.AbstractBcParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareSbitVector;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgFactory;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgParty;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2Triple;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Beaver91-BC协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/02/14
 */
public class Bea91BcSender extends AbstractBcParty {
    /**
     * 布尔三元组生成协议发送方
     */
    private final Z2MtgParty z2MtgSender;

    public Bea91BcSender(Rpc senderRpc, Party receiverParty, Bea91BcConfig config) {
        super(Bea91BcPtoDesc.getInstance(), senderRpc, receiverParty, config);
        z2MtgSender = Z2MtgFactory.createSender(senderRpc, receiverParty, config.getZ2MtgConfig());
        z2MtgSender.addLogLevel();
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        z2MtgSender.setTaskId(taskId);
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        z2MtgSender.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        z2MtgSender.addLogLevel();
    }

    @Override
    public void init(int maxRoundBitNum, int updateBitNum) throws MpcAbortException {
        setInitInput(maxRoundBitNum, updateBitNum);
        info("{}{} Send. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        z2MtgSender.init(maxRoundBitNum, updateBitNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Send. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public SquareSbitVector shareOwn(BitVector x) {
        setShareOwnInput(x);
        info("{}{} Send. share (Send.) begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        BitVector x0BitVector = BitVectorFactory.createRandom(bitNum, secureRandom);
        BitVector x1BitVector = x.xor(x0BitVector);
        List<byte[]> x1Payload = Collections.singletonList(x1BitVector.getBytes());
        DataPacketHeader x1Header = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Bea91BcPtoDesc.PtoStep.SENDER_SEND_INPUT_SHARE.ordinal(), inputBitNum,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(x1Header, x1Payload));
        stopWatch.stop();
        long shareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. share (Send.) Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), shareTime);

        info("{}{} Send. share (Send.) end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return SquareSbitVector.create(x0BitVector, false);
    }

    @Override
    public SquareSbitVector shareOther(int bitNum) throws MpcAbortException {
        setShareOtherInput(bitNum);
        info("{}{} Send. share (Recv.) begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        DataPacketHeader x0Header = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Bea91BcPtoDesc.PtoStep.RECEIVER_SEND_INPUT_SHARE.ordinal(), inputBitNum,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> x0Payload = rpc.receive(x0Header).getPayload();
        MpcAbortPreconditions.checkArgument(x0Payload.size() == 1);
        BitVector x0BitVector = BitVectorFactory.create(bitNum, x0Payload.get(0));
        stopWatch.stop();
        long shareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. share (Recv.) Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), shareTime);

        info("{}{} Send. share (Recv.) end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return SquareSbitVector.create(x0BitVector, false);
    }

    @Override
    public SquareSbitVector and(SquareSbitVector x0, SquareSbitVector y0) throws MpcAbortException {
        setAndInput(x0, y0);
        if (x0.isPlain() && y0.isPlain()) {
            // x0和y0为明文比特向量，发送方和接收方都执行AND运算
            return x0.and(y0);
        } else if (x0.isPlain() || y0.isPlain()) {
            // x0或y0为明文比特向量，发送方和接收方都执行AND运算
            return x0.and(y0);
        } else {
            // x0和y0为密文比特向量，执行AND协议
            andGateNum += bitNum;
            info("{}{} Send. AND begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

            stopWatch.start();
            Z2Triple z2Triple = z2MtgSender.generate(bitNum);
            stopWatch.stop();
            long z2MtgTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            info("{}{} Send. AND Step 1/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), z2MtgTime);

            // 计算e0和f0
            stopWatch.start();
            byte[] a0 = z2Triple.getA();
            byte[] b0 = z2Triple.getB();
            byte[] c0 = z2Triple.getC();
            // e0 = x0 ⊕ a0
            byte[] e0 = BytesUtils.xor(x0.getBytes(), a0);
            // f0 = y0 ⊕ b0
            byte[] f0 = BytesUtils.xor(y0.getBytes(), b0);
            List<byte[]> e0f0Payload = new LinkedList<>();
            e0f0Payload.add(e0);
            e0f0Payload.add(f0);
            DataPacketHeader e0f0Header = new DataPacketHeader(
                taskId, getPtoDesc().getPtoId(), Bea91BcPtoDesc.PtoStep.SENDER_SEND_E0_F0.ordinal(), andGateNum,
                ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(e0f0Header, e0f0Payload));
            stopWatch.stop();
            long e0f0Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            info("{}{} Send. AND Step 2/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), e0f0Time);

            stopWatch.start();
            DataPacketHeader e1f1Header = new DataPacketHeader(
                taskId, getPtoDesc().getPtoId(), Bea91BcPtoDesc.PtoStep.RECEIVER_SEND_E1_F1.ordinal(), andGateNum,
                otherParty().getPartyId(), ownParty().getPartyId()
            );
            List<byte[]> e1f1Payload = rpc.receive(e1f1Header).getPayload();
            MpcAbortPreconditions.checkArgument(e1f1Payload.size() == 2);
            byte[] e1 = e1f1Payload.remove(0);
            byte[] f1 = e1f1Payload.remove(0);
            // e = (e0 ⊕ e1)
            byte[] z0 = BytesUtils.xor(e0, e1);
            // f = (f0 ⊕ f1)
            byte[] f = BytesUtils.xor(f0, f1);
            // z0 = (e ☉ b0) ⊕ (f ☉ a0) ⊕ c0
            BytesUtils.andi(z0, b0);
            BytesUtils.andi(f, a0);
            BytesUtils.xori(z0, f);
            BytesUtils.xori(z0, c0);
            SquareSbitVector z0ShareBitVector = SquareSbitVector.create(bitNum, z0, false);
            stopWatch.stop();
            long z0Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            info("{}{} Send. AND Step 3/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), z0Time);

            info("{}{} Send. AND end", ptoEndLogPrefix, getPtoDesc().getPtoName());
            return z0ShareBitVector;
        }
    }

    @Override
    public SquareSbitVector xor(SquareSbitVector x0, SquareSbitVector y0) {
        setXorInput(x0, y0);
        if (x0.isPlain() && y0.isPlain()) {
            // x0和y0为明文比特向量，发送方和接收方都执行XOR运算
            return x0.xor(y0, true);
        } else if (x0.isPlain() || y0.isPlain()) {
            // x0或y0为明文比特向量，发送方进行XOR运算，接收方不执行XOR运算
            return x0.xor(y0, false);
        } else {
            // x0和y0为密文比特向量，发送方和接收方都执行XOR运算
            xorGateNum += bitNum;
            info("{}{} Send. XOR begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());
            stopWatch.start();
            SquareSbitVector z0ShareBitVector = x0.xor(y0, false);
            stopWatch.stop();
            long z0Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            info("{}{} Send. XOR Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), z0Time);

            info("{}{} Send. XOR end", ptoEndLogPrefix, getPtoDesc().getPtoName());
            return z0ShareBitVector;
        }
    }

    @Override
    public BitVector revealOwn(SquareSbitVector x0) throws MpcAbortException {
        setRevealOwnInput(x0);
        if (x0.isPlain()) {
            return x0.getBitVector();
        } else {
            outputBitNum += bitNum;
            info("{}{} Send. reveal (Send.) begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

            stopWatch.start();
            DataPacketHeader x1Header = new DataPacketHeader(
                taskId, getPtoDesc().getPtoId(), Bea91BcPtoDesc.PtoStep.RECEIVER_SEND_SHARE_OUTPUT.ordinal(), outputBitNum,
                otherParty().getPartyId(), ownParty().getPartyId()
            );
            List<byte[]> x1Payload = rpc.receive(x1Header).getPayload();
            MpcAbortPreconditions.checkArgument(x1Payload.size() == 1);
            SquareSbitVector x1 = SquareSbitVector.create(bitNum, x1Payload.get(0), true);
            stopWatch.stop();
            long revealTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            info("{}{} Send. reveal (Send.) Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), revealTime);

            info("{}{} Send. reveal (Send.) end", ptoEndLogPrefix, getPtoDesc().getPtoName());
            return x0.xor(x1, false).getBitVector();
        }
    }

    @Override
    public void revealOther(SquareSbitVector x0) {
        setRevealOtherInput(x0);
        if (!x0.isPlain()) {
            outputBitNum += bitNum;
            info("{}{} Send. reveal (Recv.) begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

            stopWatch.start();
            List<byte[]> x0Payload = Collections.singletonList(x0.getBytes());
            DataPacketHeader x0Header = new DataPacketHeader(
                taskId, getPtoDesc().getPtoId(), Bea91BcPtoDesc.PtoStep.SENDER_SEND_OUTPUT_SHARE.ordinal(), outputBitNum,
                ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(x0Header, x0Payload));
            stopWatch.stop();
            long revealTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            info("{}{} Send. reveal (Recv.) Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), revealTime);

            info("{}{} Send. reveal (Recv.) end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        }
    }
}
