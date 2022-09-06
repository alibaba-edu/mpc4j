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
 * Beaver91-BC协议客户端。
 *
 * @author Weiran Liu
 * @date 2022/02/14
 */
public class Bea91BcReceiver extends AbstractBcParty {
    /**
     * 布尔三元组生成协议服务端
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
    public void init(int maxRoundNum, int updateNum) throws MpcAbortException {
        setInitInput(maxRoundNum, updateNum);
        info("{}{} Recv. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        z2MtgReceiver.init(maxRoundNum, updateNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Recv. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public BcSquareVector and(BcSquareVector x1, BcSquareVector y1) throws MpcAbortException {
        setAndInput(x1, y1);

        if (x1.isPublic() && y1.isPublic()) {
            // x0和y0为公开导线，服务端和客户端都进行AND运算
            return BcSquareVector.create(BytesUtils.and(x1.getBytes(), y1.getBytes()), num, true);
        } else if (x1.isPublic() || y1.isPublic()) {
            // x0或y0为公开导线，服务端和客户端都进行AND运算
            return BcSquareVector.create(BytesUtils.and(x1.getBytes(), y1.getBytes()), num, false);
        } else {
            // x0和y0为私有导线，执行三元组协议
            andGateNum += num;
            info("{}{} Recv. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

            stopWatch.start();
            Z2Triple z2Triple = z2MtgReceiver.generate(num);
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
            BcSquareVector z1WireGroup = BcSquareVector.create(z1, num, false);
            stopWatch.stop();
            long z1Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            info("{}{} Recv. AND Step 3/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), z1Time);

            info("{}{} Recv. AND end", ptoEndLogPrefix, getPtoDesc().getPtoName());
            return z1WireGroup;
        }
    }

    @Override
    public BcSquareVector xor(BcSquareVector x1, BcSquareVector y1) {
        setXorInput(x1, y1);

        if (x1.isPublic() && y1.isPublic()) {
            // x1和y1为公开导线，服务端和客户端都进行XOR运算
            return BcSquareVector.create(BytesUtils.xor(x1.getBytes(), y1.getBytes()), num, true);
        } else if (x1.isPublic()) {
            // x1为公开导线，y1为私有导线，客户端不做XOR运算，克隆y1
            return BcSquareVector.clone(y1);
        } else if (y1.isPublic()) {
            // x1为私有导线，y1为公开导线，客户端不做XOR运算，克隆x1
            return BcSquareVector.clone(x1);
        } else {
            // x1和y1为私有导线，服务端和客户端都进行XOR运算
            xorGateNum += num;
            info("{}{} Recv. XOR begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

            stopWatch.start();
            BcSquareVector z1 = BcSquareVector.create(BytesUtils.xor(x1.getBytes(), y1.getBytes()), num, false);
            stopWatch.stop();
            long z1Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            info("{}{} Recv. XOR Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), z1Time);

            info("{}{} Recv. XOR end", ptoEndLogPrefix, getPtoDesc().getPtoName());
            return z1;
        }
    }

    @Override
    public BcSquareVector not(BcSquareVector xi) {
        if (xi.isPublic()) {
            // x1为公开导线，客户端对x1进行NOT运算
            return xor(xi, BcSquareVector.createOnes(xi.bitLength()));
        } else {
            // x1为私有导线，客户端对x1不做NOT运算
            return xor(xi, BcSquareVector.createZeros(xi.bitLength()));
        }
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
