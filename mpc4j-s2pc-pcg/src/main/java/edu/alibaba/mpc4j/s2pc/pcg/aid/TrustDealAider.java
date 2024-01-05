package edu.alibaba.mpc4j.s2pc.pcg.aid;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractThreePartyPto;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;
import edu.alibaba.mpc4j.s2pc.pcg.aid.TrustDealPtoDesc.AidPtoStep;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * trust deal aider.
 *
 * @author Weiran Liu
 * @date 2023/5/19
 */
public class TrustDealAider extends AbstractThreePartyPto {
    /**
     * encoded task ID -> param
     */
    private final Map<Long, Object> encodeTaskIdParamMap;
    /**
     * encode task ID -> type
     */
    private final Map<Long, TrustDealType> encodeTaskIdTypeMap;

    public TrustDealAider(Rpc aiderRpc, Party leftParty, Party rightParty) {
        super(TrustDealPtoDesc.getInstance(), aiderRpc, leftParty, rightParty, new TrustDealConfig.Builder().build());
        encodeTaskIdParamMap = new HashMap<>(1);
        encodeTaskIdTypeMap = new HashMap<>(1);
    }

    /**
     * Inits the protocol.
     *
     * @throws MpcAbortException the protocol failure aborts.
     */
    public void init() throws MpcAbortException {
        initState();
        logPhaseInfo(PtoState.INIT_BEGIN);
        // empty
        logPhaseInfo(PtoState.INIT_END);
    }

    /**
     * Executes the protocol.
     *
     * @throws MpcAbortException the protocol failure aborts.
     */
    public void aid() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);
        boolean run = true;
        while (run) {
            // receive any packet
            DataPacket dataPacket = rpc.receiveAny();
            DataPacketHeader header = dataPacket.getHeader();
            // verify protocol ID
            MpcAbortPreconditions.checkArgument(header.getPtoId() == getPtoDesc().getPtoId());
            AidPtoStep aidPtoStep = AidPtoStep.values()[header.getStepId()];
            switch (aidPtoStep) {
                case INIT_QUERY:
                    stopWatch.start();
                    initResponse(dataPacket);
                    stopWatch.stop();
                    long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
                    stopWatch.reset();
                    logStepInfo(PtoState.PTO_STEP, 1, 3, initTime);
                    break;
                case REQUEST_QUERY:
                    stopWatch.start();
                    requestResponse(dataPacket);
                    stopWatch.stop();
                    long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
                    stopWatch.reset();
                    logStepInfo(PtoState.PTO_STEP, 2, 3, responseTime);
                    break;
                case DESTROY_QUERY:
                    stopWatch.start();
                    destroyResponse(dataPacket);
                    stopWatch.stop();
                    long destroyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
                    stopWatch.reset();
                    logStepInfo(PtoState.PTO_STEP, 3, 3, destroyTime);
                    break;
                default:
                    throw new MpcAbortException("Invalid " + AidPtoStep.class.getSimpleName() + ": " + aidPtoStep);
            }
            run = (encodeTaskIdParamMap.size() != 0);
        }
        logPhaseInfo(PtoState.PTO_END);
    }

    private void initResponse(DataPacket thisInitDataPacket) throws MpcAbortException {
        DataPacketHeader thisInitHeader = thisInitDataPacket.getHeader();
        long initEncodeTaskId = thisInitHeader.getEncodeTaskId();
        int thisId = thisInitHeader.getSenderId();
        int thatId = (thisId == leftParty().getPartyId() ? rightParty().getPartyId() : leftParty().getPartyId());
        long initExtraInfo = thisInitHeader.getExtraInfo();
        // check no-exist of encode task ID
        MpcAbortPreconditions.checkArgument(!encodeTaskIdParamMap.containsKey(initEncodeTaskId));
        MpcAbortPreconditions.checkArgument(!encodeTaskIdParamMap.containsKey(initEncodeTaskId));
        // receive init query from that party
        DataPacketHeader thatInitHeader = new DataPacketHeader(
            initEncodeTaskId, getPtoDesc().getPtoId(), AidPtoStep.INIT_QUERY.ordinal(), initExtraInfo,
            thatId, ownParty().getPartyId()
        );
        DataPacket thatInitDataPacket = rpc.receive(thatInitHeader);
        // read the config
        List<byte[]> thisInitPayload = thisInitDataPacket.getPayload();
        List<byte[]> thatInitPayload = thatInitDataPacket.getPayload();
        MpcAbortPreconditions.checkArgument(thisInitPayload.size() >= 1);
        MpcAbortPreconditions.checkArgument(thatInitPayload.size() >= 1);
        int thisTypeIndex = IntUtils.byteArrayToInt(thisInitPayload.get(0));
        int thatTypeIndex = IntUtils.byteArrayToInt(thatInitPayload.get(0));
        MpcAbortPreconditions.checkArgument(thisTypeIndex == thatTypeIndex);
        TrustDealType trustDealType = TrustDealType.values()[thatTypeIndex];
        encodeTaskIdTypeMap.put(initEncodeTaskId, trustDealType);
        switch (trustDealType) {
            case Z2_TRIPLE:
                // Z2 triple, no config
                encodeTaskIdParamMap.put(initEncodeTaskId, new Object());
                break;
            case ZL_TRIPLE:
                // Zl triple, read l
                MpcAbortPreconditions.checkArgument(thisInitPayload.size() == 2);
                MpcAbortPreconditions.checkArgument(thatInitPayload.size() == 2);
                int thisL = IntUtils.byteArrayToInt(thisInitPayload.get(1));
                int thatL = IntUtils.byteArrayToInt(thatInitPayload.get(1));
                MpcAbortPreconditions.checkArgument(thisL == thatL);
                encodeTaskIdParamMap.put(initEncodeTaskId, ZlFactory.createInstance(envType, thisL));
                break;
            default:
                throw new MpcAbortException("Invalid " + TrustDealType.class.getSimpleName() + ": " + trustDealType.name());
        }
        // response to the left party
        DataPacketHeader leftResponseHeader = new DataPacketHeader(
            initEncodeTaskId, getPtoDesc().getPtoId(), AidPtoStep.INIT_RESPONSE.ordinal(), initExtraInfo,
            ownParty().getPartyId(), leftParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(leftResponseHeader, new LinkedList<>()));
        // response to the right party
        DataPacketHeader rightResponseHeader = new DataPacketHeader(
            initEncodeTaskId, getPtoDesc().getPtoId(), AidPtoStep.INIT_RESPONSE.ordinal(), initExtraInfo,
            ownParty().getPartyId(), rightParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(rightResponseHeader, new LinkedList<>()));
    }

    private void requestResponse(DataPacket thisRequestDataPacket) throws MpcAbortException {
        DataPacketHeader thisRequestHeader = thisRequestDataPacket.getHeader();
        long requestEncodeTaskId = thisRequestHeader.getEncodeTaskId();
        int thisId = thisRequestHeader.getSenderId();
        int thatId = (thisId == leftParty().getPartyId() ? rightParty().getPartyId() : leftParty().getPartyId());
        long requestExtraInfo = thisRequestHeader.getExtraInfo();
        // check encode task ID
        MpcAbortPreconditions.checkArgument(encodeTaskIdParamMap.containsKey(requestEncodeTaskId));
        // receive request query from that party
        DataPacketHeader thatRequestHeader = new DataPacketHeader(
            requestEncodeTaskId, getPtoDesc().getPtoId(), AidPtoStep.REQUEST_QUERY.ordinal(), requestExtraInfo,
            thatId, ownParty().getPartyId()
        );
        DataPacket thatRequestDataPacket = rpc.receive(thatRequestHeader);
        // parse and check type
        List<byte[]> thisRequestPayload = thisRequestDataPacket.getPayload();
        List<byte[]> thatRequestPayload = thatRequestDataPacket.getPayload();
        MpcAbortPreconditions.checkArgument(thisRequestPayload.size() == 1);
        MpcAbortPreconditions.checkArgument(thatRequestPayload.size() == 1);
        // read type
        TrustDealType trustDealType = encodeTaskIdTypeMap.get(requestEncodeTaskId);
        // read num
        int thisNum = IntUtils.byteArrayToInt(thisRequestPayload.get(0));
        int thatNum = IntUtils.byteArrayToInt(thatRequestPayload.get(0));
        MpcAbortPreconditions.checkArgument(thisNum == thatNum);
        MpcAbortPreconditions.checkArgument(thisNum > 0);
        switch (trustDealType) {
            case Z2_TRIPLE:
                z2TripleResponse(requestEncodeTaskId, requestExtraInfo, thisNum);
                break;
            case ZL_TRIPLE:
                zlTripleResponse(requestEncodeTaskId, requestExtraInfo, thisNum);
                break;
            default:
                throw new MpcAbortException("Invalid " + TrustDealType.class.getSimpleName() + ": " + trustDealType.name());
        }
    }

    private void z2TripleResponse(long requestEncodeTaskId, long requestExtraInfo, int num) {
        // generate Z2 triple
        BitVector aVector = BitVectorFactory.createRandom(num, secureRandom);
        BitVector bVector = BitVectorFactory.createRandom(num, secureRandom);
        BitVector cVector = aVector.and(bVector);
        BitVector a0Vector = BitVectorFactory.createRandom(num, secureRandom);
        BitVector a1Vector = aVector.xor(a0Vector);
        BitVector b0Vector = BitVectorFactory.createRandom(num, secureRandom);
        BitVector b1Vector = bVector.xor(b0Vector);
        BitVector c0Vector = BitVectorFactory.createRandom(num, secureRandom);
        BitVector c1Vector = cVector.xor(c0Vector);
        // response to the left party
        List<byte[]> leftResponsePayload = new LinkedList<>();
        leftResponsePayload.add(a0Vector.getBytes());
        leftResponsePayload.add(b0Vector.getBytes());
        leftResponsePayload.add(c0Vector.getBytes());
        DataPacketHeader leftResponseHeader = new DataPacketHeader(
            requestEncodeTaskId, getPtoDesc().getPtoId(), AidPtoStep.REQUEST_RESPONSE.ordinal(), requestExtraInfo,
            ownParty().getPartyId(), leftParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(leftResponseHeader, leftResponsePayload));
        // response to the right party
        List<byte[]> rightResponsePayload = new LinkedList<>();
        rightResponsePayload.add(a1Vector.getBytes());
        rightResponsePayload.add(b1Vector.getBytes());
        rightResponsePayload.add(c1Vector.getBytes());
        DataPacketHeader rightResponseHeader = new DataPacketHeader(
            requestEncodeTaskId, getPtoDesc().getPtoId(), AidPtoStep.REQUEST_RESPONSE.ordinal(), requestExtraInfo,
            ownParty().getPartyId(), rightParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(rightResponseHeader, rightResponsePayload));
    }

    private void zlTripleResponse(long requestEncodeTaskId, long requestExtraInfo, int num) {
        // generate Zl triple
        Zl zl = (Zl) encodeTaskIdParamMap.get(requestEncodeTaskId);
        int byteL = zl.getByteL();
        ZlVector aVector = ZlVector.createRandom(zl, num, secureRandom);
        ZlVector bVector = ZlVector.createRandom(zl, num, secureRandom);
        ZlVector cVector = aVector.mul(bVector);
        ZlVector a0Vector = ZlVector.createRandom(zl, num, secureRandom);
        ZlVector a1Vector = aVector.sub(a0Vector);
        ZlVector b0Vector = ZlVector.createRandom(zl, num, secureRandom);
        ZlVector b1Vector = bVector.sub(b0Vector);
        ZlVector c0Vector = ZlVector.createRandom(zl, num, secureRandom);
        ZlVector c1Vector = cVector.sub(c0Vector);
        // response to the left party
        ByteBuffer a0ByteBuffer = ByteBuffer.allocate(num * byteL);
        for (int index = 0; index < num; index++) {
            a0ByteBuffer.put(BigIntegerUtils.nonNegBigIntegerToByteArray(a0Vector.getElement(index), byteL));
        }
        ByteBuffer b0ByteBuffer = ByteBuffer.allocate(num * byteL);
        for (int index = 0; index < num; index++) {
            b0ByteBuffer.put(BigIntegerUtils.nonNegBigIntegerToByteArray(b0Vector.getElement(index), byteL));
        }
        ByteBuffer c0ByteBuffer = ByteBuffer.allocate(num * byteL);
        for (int index = 0; index < num; index++) {
            c0ByteBuffer.put(BigIntegerUtils.nonNegBigIntegerToByteArray(c0Vector.getElement(index), byteL));
        }
        List<byte[]> leftResponsePayload = new LinkedList<>();
        leftResponsePayload.add(a0ByteBuffer.array());
        leftResponsePayload.add(b0ByteBuffer.array());
        leftResponsePayload.add(c0ByteBuffer.array());
        DataPacketHeader leftResponseHeader = new DataPacketHeader(
            requestEncodeTaskId, getPtoDesc().getPtoId(), AidPtoStep.REQUEST_RESPONSE.ordinal(), requestExtraInfo,
            ownParty().getPartyId(), leftParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(leftResponseHeader, leftResponsePayload));
        // response to the right party
        ByteBuffer a1ByteBuffer = ByteBuffer.allocate(num * byteL);
        for (int index = 0; index < num; index++) {
            a1ByteBuffer.put(BigIntegerUtils.nonNegBigIntegerToByteArray(a1Vector.getElement(index), byteL));
        }
        ByteBuffer b1ByteBuffer = ByteBuffer.allocate(num * byteL);
        for (int index = 0; index < num; index++) {
            b1ByteBuffer.put(BigIntegerUtils.nonNegBigIntegerToByteArray(b1Vector.getElement(index), byteL));
        }
        ByteBuffer c1ByteBuffer = ByteBuffer.allocate(num * byteL);
        for (int index = 0; index < num; index++) {
            c1ByteBuffer.put(BigIntegerUtils.nonNegBigIntegerToByteArray(c1Vector.getElement(index), byteL));
        }
        List<byte[]> rightResponsePayload = new LinkedList<>();
        rightResponsePayload.add(a1ByteBuffer.array());
        rightResponsePayload.add(b1ByteBuffer.array());
        rightResponsePayload.add(c1ByteBuffer.array());
        DataPacketHeader rightResponseHeader = new DataPacketHeader(
            requestEncodeTaskId, getPtoDesc().getPtoId(), AidPtoStep.REQUEST_RESPONSE.ordinal(), requestExtraInfo,
            ownParty().getPartyId(), rightParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(rightResponseHeader, rightResponsePayload));
    }

    private void destroyResponse(DataPacket thisDataPacket) throws MpcAbortException {
        DataPacketHeader thisHeader = thisDataPacket.getHeader();
        long destroyEncodeTaskId = thisHeader.getEncodeTaskId();
        int thisId = thisHeader.getSenderId();
        int thatId = (thisId == leftParty().getPartyId() ? rightParty().getPartyId() : leftParty().getPartyId());
        long destroyExtraInfo = thisHeader.getExtraInfo();
        // check encode task ID
        MpcAbortPreconditions.checkArgument(encodeTaskIdParamMap.containsKey(destroyEncodeTaskId));
        MpcAbortPreconditions.checkArgument(encodeTaskIdTypeMap.containsKey(destroyEncodeTaskId));
        // receive destroy query from that party
        DataPacketHeader thatHeader = new DataPacketHeader(
            destroyEncodeTaskId, getPtoDesc().getPtoId(), AidPtoStep.DESTROY_QUERY.ordinal(), destroyExtraInfo,
            thatId, ownParty().getPartyId()
        );
        DataPacket thatDataPacket = rpc.receive(thatHeader);
        MpcAbortPreconditions.checkArgument(thisDataPacket.getPayload().size() == 0);
        MpcAbortPreconditions.checkArgument(thatDataPacket.getPayload().size() == 0);
        // remove encode task ID from the set
        encodeTaskIdParamMap.remove(destroyEncodeTaskId);
        encodeTaskIdTypeMap.remove(destroyEncodeTaskId);
        // response to the left party
        DataPacketHeader leftResponseHeader = new DataPacketHeader(
            destroyEncodeTaskId, getPtoDesc().getPtoId(), AidPtoStep.DESTROY_RESPONSE.ordinal(), destroyExtraInfo,
            ownParty().getPartyId(), leftParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(leftResponseHeader, new LinkedList<>()));
        // response to the right party
        DataPacketHeader rightResponseHeader = new DataPacketHeader(
            destroyEncodeTaskId, getPtoDesc().getPtoId(), AidPtoStep.DESTROY_RESPONSE.ordinal(), destroyExtraInfo,
            ownParty().getPartyId(), rightParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(rightResponseHeader, new LinkedList<>()));
    }
}
