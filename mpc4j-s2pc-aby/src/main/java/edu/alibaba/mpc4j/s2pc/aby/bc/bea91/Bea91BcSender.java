package edu.alibaba.mpc4j.s2pc.aby.bc.bea91;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.aby.bc.AbstractBcParty;
import edu.alibaba.mpc4j.s2pc.aby.bc.BcSquareVector;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2Triple;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgFactory;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgParty;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Beaver91-BC协议服务端。
 *
 * @author Weiran Liu
 * @date 2022/02/14
 */
public class Bea91BcSender extends AbstractBcParty {
    /**
     * 布尔三元组生成协议服务端
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
    public void init(int maxRoundNum, int updateNum) throws MpcAbortException {
        setInitInput(maxRoundNum, updateNum);
        info("{}{} Send. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        z2MtgSender.init(maxRoundNum, updateNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Send. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public BcSquareVector and(BcSquareVector xi, BcSquareVector yi) throws MpcAbortException {
        setAndInput(xi, yi);

        if (xi.isPublic() && yi.isPublic()) {
            // x0和y0为公开导线，服务端和客户端都进行AND运算
            return BcSquareVector.create(BytesUtils.and(xi.getBytes(), yi.getBytes()), num, true);
        } else if (xi.isPublic() || yi.isPublic()) {
            // x0或y0为公开导线，服务端和客户端都进行AND运算
            return BcSquareVector.create(BytesUtils.and(xi.getBytes(), yi.getBytes()), num, false);
        } else {
            // x0和y0为私有导线，执行三元组协议
            andGateNum += num;
            info("{}{} Send. AND begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

            stopWatch.start();
            Z2Triple z2Triple = z2MtgSender.generate(num);
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
            byte[] e0 = BytesUtils.xor(xi.getBytes(), a0);
            // f0 = y0 ⊕ b0
            byte[] f0 = BytesUtils.xor(yi.getBytes(), b0);
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
            BcSquareVector z0WireGroup = BcSquareVector.create(z0, num, false);
            stopWatch.stop();
            long z0Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            info("{}{} Send. AND Step 3/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), z0Time);

            info("{}{} Send. AND end", ptoEndLogPrefix, getPtoDesc().getPtoName());
            return z0WireGroup;
        }
    }

    @Override
    public BcSquareVector xor(BcSquareVector xi, BcSquareVector yi) {
        setXorInput(xi, yi);
        if (xi.isPublic() && yi.isPublic()) {
            // x0和y0为公开导线，服务端和客户端都进行XOR运算
            return BcSquareVector.create(BytesUtils.xor(xi.getBytes(), yi.getBytes()), num, true);
        } else if (xi.isPublic() || yi.isPublic()) {
            // x0或y0为公开导线，服务端进行XOR运算，客户端不执行XOR运算
            return BcSquareVector.create(BytesUtils.xor(xi.getBytes(), yi.getBytes()), num, false);
        } else {
            // x0和y0为私有导线，服务端和客户端都进行XOR运算
            xorGateNum += num;
            info("{}{} Send. XOR begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

            stopWatch.start();
            BcSquareVector z0 = BcSquareVector.create(BytesUtils.xor(xi.getBytes(), yi.getBytes()), num, false);
            stopWatch.stop();
            long z0Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            info("{}{} Send. XOR Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), z0Time);

            info("{}{} Send. XOR end", ptoEndLogPrefix, getPtoDesc().getPtoName());
            return z0;
        }
    }

    @Override
    public BcSquareVector not(BcSquareVector xi) {
        return xor(xi, BcSquareVector.createOnes(xi.bitLength()));
    }

    @Override
    public long andGateNum(boolean reset) {
        long result = andGateNum;
        andGateNum = reset ? 0L : andGateNum;
        return result;
    }

    @Override
    public long xorGateNum(boolean reset) {
        long result = xorGateNum;
        xorGateNum = reset ? 0L : xorGateNum;
        return result;
    }
}
