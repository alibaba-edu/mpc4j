package edu.alibaba.mpc4j.s3pc.abb3.basic;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyPto;
import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPto;
import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.structure.vector.LongVector;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Abstract three-party protocol for abb3.
 *
 * @author Feng Han
 * @date 2024/01/18
 */
public abstract class AbstractAbbThreePartyPto extends AbstractThreePartyPto {
    /**
     * index of message that this party sends to or receives from his left party
     */
    protected int indexSendToLeft, indexReceiveFromLeft;
    /**
     * index of message that this party sends to or receives from his right party
     */
    protected int indexSendToRight, indexReceiveFromRight;
    protected AbstractAbbThreePartyPto(PtoDesc ptoDesc, Rpc rpc, Party leftParty, Party rightParty, MultiPartyPtoConfig config) {
        super(ptoDesc, rpc, leftParty, rightParty, config);
        indexSendToLeft = 0;
        indexReceiveFromLeft = 0;
        indexSendToRight = 0;
        indexReceiveFromRight = 0;
    }

    protected AbstractAbbThreePartyPto(PtoDesc ptoDesc, Rpc rpc, MultiPartyPtoConfig config) {
        super(ptoDesc, rpc,
            rpc.getParty((rpc.ownParty().getPartyId() + 2) % 3),
            rpc.getParty((rpc.ownParty().getPartyId() + 1) % 3), config);
        indexSendToLeft = 0;
        indexReceiveFromLeft = 0;
        indexSendToRight = 0;
        indexReceiveFromRight = 0;
    }

    protected void addMultiSubPto(MultiPartyPto... subPtos){
        for (MultiPartyPto pto : subPtos){
            addSubPto(pto);
        }
    }


    protected void logStepInfo(PtoState ptoState, String funcName, int stepIndex, int totalStepIndex, long time) {
        assert stepIndex >= 0 && stepIndex <= totalStepIndex
            : "step index must be in range [0, " + totalStepIndex + "]: " + stepIndex;
        switch (ptoState) {
            case INIT_STEP:
                info("{}{}-{} {}:  init Step {}/{} ({}ms)",
                    ptoStepLogPrefix, getPtoDesc().getPtoName(), funcName, ownParty().getPartyName(),
                    stepIndex, totalStepIndex, time
                );
                break;
            case PTO_STEP:
                info("{}{}-{} {} Step {}/{} ({}ms)",
                    ptoStepLogPrefix, getPtoDesc().getPtoName(), funcName, ownParty().getPartyName(),
                    stepIndex, totalStepIndex, time
                );
                break;
            default:
                throw new IllegalStateException("Invalid " + PtoState.class.getSimpleName() + ": " + ptoState);
        }
    }

    protected void logStepInfo(PtoState ptoState, String funcName, int stepIndex, int totalStepIndex, long time, String description) {
        assert stepIndex >= 0 && stepIndex <= totalStepIndex
            : "step index must be in range [0, " + totalStepIndex + "]: " + stepIndex;
        switch (ptoState) {
            case INIT_STEP:
                info("{}{}-{} {} init Step {}/{} ({}ms): {}",
                    ptoStepLogPrefix, getPtoDesc().getPtoName(), funcName, ownParty().getPartyName(),
                    stepIndex, totalStepIndex, time, description
                );
                break;
            case PTO_STEP:
                info("{}{}-{} {} Step {}/{} ({}ms): {}",
                    ptoStepLogPrefix, getPtoDesc().getPtoName(), funcName, ownParty().getPartyName(),
                    stepIndex, totalStepIndex, time, description
                );
                break;
            default:
                throw new IllegalStateException("Invalid " + PtoState.class.getSimpleName() + ": " + ptoState);
        }
    }

    /**
     * reset stopWatch and return time
     */
    protected long resetAndGetTime(){
        stopWatch.stop();
        long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        return time;
    }

    protected List<byte[]> receive(int stepId, Party fromParty){
//        LOGGER.info("{} waiting for msg from {} step id:{}, info:{}, ptoID:{}", rpc.ownParty().getPartyId(), fromParty.getPartyId(), stepId, fromParty.equals(leftParty()) ? indexReceiveFromLeft : indexReceiveFromRight, getPtoDesc().getPtoId());
        int info = fromParty.equals(leftParty()) ? indexReceiveFromLeft++ : indexReceiveFromRight++;
        DataPacketHeader header = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), stepId, info,
            fromParty.getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> data = rpc.receive(header).getPayload();
//        LOGGER.info("{} received msg from {} step id:{}, info:{}, ptoID:{}", rpc.ownParty().getPartyId(), fromParty.getPartyId(), stepId, fromParty.equals(leftParty()) ? indexReceiveFromLeft - 1 : indexReceiveFromRight - 1, getPtoDesc().getPtoId());
        return data;
    }
    protected void send(int stepId, Party toParty, List<byte[]> data){
//        LOGGER.info("{} send msg to {} step id:{}, info:{}, ptoID:{}", rpc.ownParty().getPartyId(), toParty.getPartyId(), stepId, toParty.equals(leftParty()) ? indexSendToLeft : indexSendToRight, getPtoDesc().getPtoId());
        int info = toParty.equals(leftParty()) ? indexSendToLeft++ : indexSendToRight++;
        DataPacketHeader header = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), stepId, info,
            ownParty().getPartyId(), toParty.getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(header, data));
    }

    protected BitVector[] receiveBitVectors(int stepId, Party fromParty, int[] bitNums){
        List<byte[]> tmp = receive(stepId, fromParty);
        MathPreconditions.checkEqual("bitNums.length", "tmp.size()", bitNums.length, tmp.size());
        return IntStream.range(0, bitNums.length).mapToObj(i ->
            BitVectorFactory.create(bitNums[i], tmp.get(i))).toArray(BitVector[]::new);
    }
    protected void sendBitVectors(int stepId, Party toParty, BitVector... data){
        send(stepId, toParty, Arrays.stream(data).map(BitVector::getBytes).collect(Collectors.toList()));
    }

    protected long[][] receiveLong(int stepId, Party fromParty){
        List<byte[]> data = receive(stepId, fromParty);
        return data.stream().map(LongUtils::byteArrayToLongArray).toArray(long[][]::new);
    }
    protected void sendLong(int stepId, Party toParty, long[]... data){
        int maxArrayLen = Integer.MAX_VALUE>>3;
        for(long[] x : data){
            MathPreconditions.checkGreaterOrEqual("(Integer.MAX_VALUE>>3) >= x.length", maxArrayLen, x.length);
        }
        List<byte[]> sendData = Arrays.stream(data).map(LongUtils::longArrayToByteArray).collect(Collectors.toList());
        send(stepId, toParty, sendData);
    }

    protected int[][] receiveInt(int stepId, Party fromParty){
        List<byte[]> data = receive(stepId, fromParty);
        return data.stream().map(IntUtils::byteArrayToIntArray).toArray(int[][]::new);
    }

    protected void sendInt(int stepId, Party toParty, int[]... data){
        int maxArrayLen = Integer.MAX_VALUE>>3;
        for(int[] x : data){
            MathPreconditions.checkGreaterOrEqual("(Integer.MAX_VALUE>>3) >= x.length", maxArrayLen, x.length);
        }
        List<byte[]> sendData = Arrays.stream(data).map(IntUtils::intArrayToByteArray).collect(Collectors.toList());
        send(stepId, toParty, sendData);
    }

    protected LongVector[] receiveLongVectors(int stepId, Party fromParty){
        List<byte[]> data = receive(stepId, fromParty);
        return data.stream().map(x -> LongVector.create(LongUtils.byteArrayToLongArray(x))).toArray(LongVector[]::new);
    }

    protected void sendLongVectors(int stepId, Party toParty, LongVector... data){
        int maxArrayLen = Integer.MAX_VALUE>>3;
        List<byte[]> sendData = Arrays.stream(data).map(x -> {
            MathPreconditions.checkGreaterOrEqual("(Integer.MAX_VALUE>>3) >= x.length", maxArrayLen, x.getNum());
            return LongUtils.longArrayToByteArray(x.getElements());
        }).collect(Collectors.toList());
        send(stepId, toParty, sendData);
    }
}
