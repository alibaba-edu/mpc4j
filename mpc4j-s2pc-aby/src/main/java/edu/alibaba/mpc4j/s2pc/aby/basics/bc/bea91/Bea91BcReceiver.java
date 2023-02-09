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
 * Beaver91-BC协议接收方。
 *
 * @author Weiran Liu
 * @date 2022/02/14
 */
public class Bea91BcReceiver extends AbstractBcParty {
    /**
     * 布尔三元组生成协议接收方
     */
    private final Z2MtgParty z2MtgReceiver;

    public Bea91BcReceiver(Rpc receiverRpc, Party senderParty, Bea91BcConfig config) {
        super(Bea91BcPtoDesc.getInstance(), receiverRpc, senderParty, config);
        z2MtgReceiver = Z2MtgFactory.createReceiver(receiverRpc, senderParty, config.getZ2MtgConfig());
        z2MtgReceiver.addLogLevel();
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        z2MtgReceiver.setTaskId(taskId);
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        z2MtgReceiver.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        z2MtgReceiver.addLogLevel();
    }

    @Override
    public void init(int maxRoundBitNum, int updateBitNum) throws MpcAbortException {
        setInitInput(maxRoundBitNum, updateBitNum);
        info("{}{} Recv. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        z2MtgReceiver.init(maxRoundBitNum, updateBitNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Recv. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public SquareSbitVector shareOwn(BitVector x) {
        setShareOwnInput(x);
        info("{}{} Recv. share (Recv.) begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        BitVector x1BitVector = BitVectorFactory.createRandom(bitNum, secureRandom);
        BitVector x0BitVector = x.xor(x1BitVector);
        List<byte[]> x0Payload = Collections.singletonList(x0BitVector.getBytes());
        DataPacketHeader x0Header = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Bea91BcPtoDesc.PtoStep.RECEIVER_SEND_INPUT_SHARE.ordinal(), inputBitNum,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(x0Header, x0Payload));
        stopWatch.stop();
        long shareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. share (Recv.) Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), shareTime);

        info("{}{} Recv. share (Recv.) end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return SquareSbitVector.create(x1BitVector, false);
    }

    @Override
    public SquareSbitVector shareOther(int bitNum) throws MpcAbortException {
        setShareOtherInput(bitNum);
        info("{}{} Recv. share (Send.) begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        DataPacketHeader x1Header = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Bea91BcPtoDesc.PtoStep.SENDER_SEND_INPUT_SHARE.ordinal(), inputBitNum,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> x1Payload = rpc.receive(x1Header).getPayload();
        MpcAbortPreconditions.checkArgument(x1Payload.size() == 1);
        BitVector x1BitVector = BitVectorFactory.create(bitNum, x1Payload.get(0));
        stopWatch.stop();
        long shareTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. share (Send.) Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), shareTime);

        info("{}{} Recv. share (Send.) end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return SquareSbitVector.create(x1BitVector, false);
    }

    @Override
    public SquareSbitVector and(SquareSbitVector x1, SquareSbitVector y1) throws MpcAbortException {
        setAndInput(x1, y1);

        if (x1.isPlain() && y1.isPlain()) {
            // x1和y1为明文比特向量，发送方和接收方都执行AND运算
            return x1.and(y1);
        } else if (x1.isPlain() || y1.isPlain()) {
            // x1或y1为明文比特向量，发送方和接收方都执行AND运算
            return x1.and(y1);
        } else {
            // x1和y1为密文比特向量，执行AND协议
            andGateNum += bitNum;
            info("{}{} Recv. AND begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

            stopWatch.start();
            Z2Triple z2Triple = z2MtgReceiver.generate(bitNum);
            stopWatch.stop();
            long z2MtgTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            info("{}{} Recv. AND Step 1/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), z2MtgTime);

            stopWatch.start();
            byte[] a1 = z2Triple.getA();
            byte[] b1 = z2Triple.getB();
            byte[] c1 = z2Triple.getC();
            // e1 = x1 ⊕ a1
            byte[] e1 = BytesUtils.xor(x1.getBytes(), a1);
            // f1 = y1 ⊕ b1
            byte[] f1 = BytesUtils.xor(y1.getBytes(), b1);
            List<byte[]> e1f1Payload = new LinkedList<>();
            e1f1Payload.add(e1);
            e1f1Payload.add(f1);
            DataPacketHeader e1f1Header = new DataPacketHeader(
                taskId, getPtoDesc().getPtoId(), Bea91BcPtoDesc.PtoStep.RECEIVER_SEND_E1_F1.ordinal(), andGateNum,
                ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(e1f1Header, e1f1Payload));
            stopWatch.stop();
            long e1f1Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            info("{}{} Recv. AND Step 2/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), e1f1Time);

            stopWatch.start();
            DataPacketHeader e0f0Header = new DataPacketHeader(
                taskId, getPtoDesc().getPtoId(), Bea91BcPtoDesc.PtoStep.SENDER_SEND_E0_F0.ordinal(), andGateNum,
                otherParty().getPartyId(), ownParty().getPartyId()
            );
            List<byte[]> e0f0Payload = rpc.receive(e0f0Header).getPayload();
            MpcAbortPreconditions.checkArgument(e0f0Payload.size() == 2);
            byte[] e0 = e0f0Payload.remove(0);
            byte[] f0 = e0f0Payload.remove(0);
            // e = (e0 ⊕ e1)
            byte[] z1 = BytesUtils.xor(e0, e1);
            // f = (f0 ⊕ f1)
            byte[] f = BytesUtils.xor(f0, f1);
            // z1 = (e ☉ b1) ⊕ (f ☉ a1) ⊕ c1 ⊕ (e ☉ f)
            byte[] ef = BytesUtils.and(z1, f);
            BytesUtils.andi(z1, b1);
            BytesUtils.andi(f, a1);
            BytesUtils.xori(z1, f);
            BytesUtils.xori(z1, c1);
            BytesUtils.xori(z1, ef);
            SquareSbitVector z1ShareBitVector = SquareSbitVector.create(bitNum, z1, false);
            stopWatch.stop();
            long z1Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            info("{}{} Recv. AND Step 3/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), z1Time);

            info("{}{} Recv. AND end", ptoEndLogPrefix, getPtoDesc().getPtoName());
            return z1ShareBitVector;
        }
    }

    @Override
    public SquareSbitVector xor(SquareSbitVector x1, SquareSbitVector y1) {
        setXorInput(x1, y1);

        if (x1.isPlain() && y1.isPlain()) {
            // x1和y1为明文比特向量，发送方和接收方都执行XOR运算
            return x1.xor(y1, true);
        } else if (x1.isPlain()) {
            // x1为明文比特向量，y1为密文比特向量，接收方不执行XOR运算，复制y1
            return y1.copy();
        } else if (y1.isPlain()) {
            // x1为密文比特向量，y1为明文比特向量，接收方不执行XOR运算，克隆x1
            return x1.copy();
        } else {
            // x1和y1为密文比特向量，发送方和接收方都执行XOR运算
            xorGateNum += bitNum;
            info("{}{} Recv. XOR begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

            stopWatch.start();
            SquareSbitVector z1ShareBitVector = x1.xor(y1, false);
            stopWatch.stop();
            long z1Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            info("{}{} Recv. XOR Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), z1Time);

            info("{}{} Recv. XOR end", ptoEndLogPrefix, getPtoDesc().getPtoName());
            return z1ShareBitVector;
        }
    }

    @Override
    public BitVector revealOwn(SquareSbitVector x1) throws MpcAbortException {
        setRevealOwnInput(x1);
        if (x1.isPlain()) {
            return x1.getBitVector();
        } else {
            outputBitNum += bitNum;
            info("{}{} Recv. reveal (Recv.) begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

            stopWatch.start();
            DataPacketHeader x0Header = new DataPacketHeader(
                taskId, getPtoDesc().getPtoId(), Bea91BcPtoDesc.PtoStep.SENDER_SEND_OUTPUT_SHARE.ordinal(), outputBitNum,
                otherParty().getPartyId(), ownParty().getPartyId()
            );
            List<byte[]> x0Payload = rpc.receive(x0Header).getPayload();
            MpcAbortPreconditions.checkArgument(x0Payload.size() == 1);
            SquareSbitVector x0 = SquareSbitVector.create(bitNum, x0Payload.get(0), true);
            stopWatch.stop();
            long revealTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            info("{}{} Recv. reveal (Recv.) Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), revealTime);

            info("{}{} Recv. reveal (Recv.) end", ptoEndLogPrefix, getPtoDesc().getPtoName());
            return x0.xor(x1, false).getBitVector();
        }
    }

    @Override
    public void revealOther(SquareSbitVector x1) {
        setRevealOtherInput(x1);
        if (!x1.isPlain()) {
            outputBitNum += bitNum;
            info("{}{} Recv. reveal (Send.) begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

            stopWatch.start();
            List<byte[]> x1Payload = Collections.singletonList(x1.getBytes());
            DataPacketHeader x1Header = new DataPacketHeader(
                taskId, getPtoDesc().getPtoId(), Bea91BcPtoDesc.PtoStep.RECEIVER_SEND_SHARE_OUTPUT.ordinal(), outputBitNum,
                ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(x1Header, x1Payload));
            stopWatch.stop();
            long revealTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            info("{}{} Recv. reveal (Send.) Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), revealTime);

            info("{}{} Recv. reveal (Send.) end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        }
    }
}
