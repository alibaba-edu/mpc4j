package edu.alibaba.mpc4j.s2pc.aby.pcg;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyAidPto;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.structure.vector.Zl64Vector;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64Factory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.Z2Triple;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.Zl64Triple;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.ZlTriple;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Trust Dealer implementation.
 *
 * @author Weiran Liu
 * @date 2024/6/28
 */
public class TrustDealer extends AbstractTwoPartyAidPto {
    /**
     * number of protocols that register for a dealer.
     */
    private int count;

    public TrustDealer(Rpc aiderRpc, Party leftParty, Party rightParty) {
        super(TrustDealerPtoDesc.getInstance(), aiderRpc, leftParty, rightParty, new TrustDealerConfig.Builder().build());
        this.count = 0;
    }

    @Override
    public void init() throws MpcAbortException {
        initState();
        logPhaseInfo(PtoState.INIT_BEGIN);
        // empty
        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void aid() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);
        boolean run = true;
        while (run) {
            // receive any packet
            DataPacket thisDataPacket = receiveAnyAidDataPacket();
            DataPacketHeader receivedHeader = thisDataPacket.getHeader();
            TrustDealerPtoStep ptoStep = TrustDealerPtoStep.values()[receivedHeader.getStepId()];
            switch (ptoStep) {
                case REGISTER_QUERY -> {
                    // request register
                    stopWatch.start();
                    handleRegister(thisDataPacket);
                    stopWatch.stop();
                    long registerTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
                    stopWatch.reset();
                    logStepInfo(PtoState.PTO_STEP, 1, 1, registerTime, "Response register");
                }
                case REQUEST_Z2_TRIPLE -> {
                    stopWatch.start();
                    handleZ2Triple(thisDataPacket);
                    stopWatch.stop();
                    long z2TripleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
                    stopWatch.reset();
                    logStepInfo(PtoState.PTO_STEP, 1, 1, z2TripleTime, "Response Z2 triple");
                }
                case REQUEST_ZL_TRIPLE -> {
                    stopWatch.start();
                    handleZlTriple(thisDataPacket);
                    stopWatch.stop();
                    long zlTripleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
                    stopWatch.reset();
                    logStepInfo(PtoState.PTO_STEP, 1, 1, zlTripleTime, "Response Zl triple");
                }
                case REQUEST_ZL64_TRIPLE -> {
                    stopWatch.start();
                    handleZl64Triple(thisDataPacket);
                    stopWatch.stop();
                    long zlTripleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
                    stopWatch.reset();
                    logStepInfo(PtoState.PTO_STEP, 1, 1, zlTripleTime, "Response Zl64 triple");
                }
                case DESTROY_QUERY -> {
                    stopWatch.start();
                    handleDestroy(thisDataPacket);
                    run = (count != 0);
                    stopWatch.stop();
                    long destroyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
                    stopWatch.reset();
                    logStepInfo(PtoState.PTO_STEP, 1, 1, destroyTime, "Destroy request");
                }
                default ->
                    throw new MpcAbortException("Invalid " + TrustDealerPtoStep.class.getSimpleName() + ": " + ptoStep);
            }
        }
        logPhaseInfo(PtoState.PTO_END);
    }

    private void handleRegister(DataPacket thisDataPacket) throws MpcAbortException {
        DataPacketHeader receivedHeader = thisDataPacket.getHeader();
        DataPacket thatDataPacket = receiveAnyAidDataPacket(receivedHeader);
        long encodeTaskId = receivedHeader.getEncodeTaskId();
        MpcAbortPreconditions.checkArgument(thisDataPacket.getPayload().isEmpty());
        MpcAbortPreconditions.checkArgument(thatDataPacket.getPayload().isEmpty());
        count++;
        // response to the left party
        sendLeftPartyAidPayload(encodeTaskId, TrustDealerPtoStep.REGISTER_RESPONSE.ordinal(), new LinkedList<>());
        // response to the right party
        sendRightPartyAidPayload(encodeTaskId, TrustDealerPtoStep.REGISTER_RESPONSE.ordinal(), new LinkedList<>());
    }

    private void handleZ2Triple(DataPacket thisDataPacket) throws MpcAbortException {
        DataPacketHeader receivedHeader = thisDataPacket.getHeader();
        DataPacket thatDataPacket = receiveAnyAidDataPacket(receivedHeader);
        long encodeTaskId = receivedHeader.getEncodeTaskId();
        // parse and check type
        List<byte[]> thisRequestPayload = thisDataPacket.getPayload();
        List<byte[]> thatRequestPayload = thatDataPacket.getPayload();
        MpcAbortPreconditions.checkArgument(thisRequestPayload.size() == 1);
        MpcAbortPreconditions.checkArgument(thatRequestPayload.size() == 1);
        int num = IntUtils.byteArrayToInt(thisRequestPayload.get(0));
        MpcAbortPreconditions.checkArgument(num > 0);
        int thatNum = IntUtils.byteArrayToInt(thatRequestPayload.get(0));
        MpcAbortPreconditions.checkArgument(num == thatNum);
        // generate Z2 triple
        Z2Triple thisTriple = Z2Triple.createRandom(num, secureRandom);
        Z2Triple thatTriple = Z2Triple.createRandom(thisTriple, secureRandom);
        // response to the left party
        List<byte[]> leftResponsePayload = new LinkedList<>();
        leftResponsePayload.add(thisTriple.getA());
        leftResponsePayload.add(thisTriple.getB());
        leftResponsePayload.add(thisTriple.getC());
        sendLeftPartyAidPayload(encodeTaskId, TrustDealerPtoStep.REQUEST_RESPONSE.ordinal(), leftResponsePayload);
        // response to the right party
        List<byte[]> rightResponsePayload = new LinkedList<>();
        rightResponsePayload.add(thatTriple.getA());
        rightResponsePayload.add(thatTriple.getB());
        rightResponsePayload.add(thatTriple.getC());
        sendRightPartyAidPayload(encodeTaskId, TrustDealerPtoStep.REQUEST_RESPONSE.ordinal(), rightResponsePayload);
    }

    private void handleZlTriple(DataPacket thisDataPacket) throws MpcAbortException {
        DataPacketHeader receivedHeader = thisDataPacket.getHeader();
        DataPacket thatRequestDataPacket = receiveAnyAidDataPacket(receivedHeader);
        long encodeTaskId = receivedHeader.getEncodeTaskId();
        // parse and check type
        List<byte[]> thisRequestPayload = thisDataPacket.getPayload();
        List<byte[]> thatRequestPayload = thatRequestDataPacket.getPayload();
        MpcAbortPreconditions.checkArgument(thisRequestPayload.size() == 2);
        MpcAbortPreconditions.checkArgument(thatRequestPayload.size() == 2);
        // read l
        int l = IntUtils.byteArrayToInt(thisRequestPayload.get(0));
        int thatL = IntUtils.byteArrayToInt(thatRequestPayload.get(0));
        MpcAbortPreconditions.checkArgument(l > 0);
        MpcAbortPreconditions.checkArgument(l == thatL);
        // read num
        int num = IntUtils.byteArrayToInt(thisRequestPayload.get(1));
        MpcAbortPreconditions.checkArgument(num > 0);
        int thatNum = IntUtils.byteArrayToInt(thatRequestPayload.get(1));
        MpcAbortPreconditions.checkArgument(num == thatNum);
        // generate Zl triples
        Zl zl = ZlFactory.createInstance(envType, l);
        int byteL = zl.getByteL();
        ZlTriple thisTriple = ZlTriple.createRandom(zl, num, secureRandom);
        ZlTriple thatTriple = ZlTriple.createRandom(thisTriple, secureRandom);
        ZlVector a0Vector = thisTriple.getVectorA();
        ByteBuffer a0ByteBuffer = ByteBuffer.allocate(num * byteL);
        for (int index = 0; index < num; index++) {
            a0ByteBuffer.put(BigIntegerUtils.nonNegBigIntegerToByteArray(a0Vector.getElement(index), byteL));
        }
        ZlVector b0Vector = thisTriple.getVectorB();
        ByteBuffer b0ByteBuffer = ByteBuffer.allocate(num * byteL);
        for (int index = 0; index < num; index++) {
            b0ByteBuffer.put(BigIntegerUtils.nonNegBigIntegerToByteArray(b0Vector.getElement(index), byteL));
        }
        ZlVector c0Vector = thisTriple.getVectorC();
        ByteBuffer c0ByteBuffer = ByteBuffer.allocate(num * byteL);
        for (int index = 0; index < num; index++) {
            c0ByteBuffer.put(BigIntegerUtils.nonNegBigIntegerToByteArray(c0Vector.getElement(index), byteL));
        }
        List<byte[]> leftResponsePayload = new LinkedList<>();
        leftResponsePayload.add(a0ByteBuffer.array());
        leftResponsePayload.add(b0ByteBuffer.array());
        leftResponsePayload.add(c0ByteBuffer.array());
        sendLeftPartyAidPayload(encodeTaskId, TrustDealerPtoStep.REQUEST_RESPONSE.ordinal(), leftResponsePayload);
        // response to the right party
        ZlVector a1Vector = thatTriple.getVectorA();
        ByteBuffer a1ByteBuffer = ByteBuffer.allocate(num * byteL);
        for (int index = 0; index < num; index++) {
            a1ByteBuffer.put(BigIntegerUtils.nonNegBigIntegerToByteArray(a1Vector.getElement(index), byteL));
        }
        ZlVector b1Vector = thatTriple.getVectorB();
        ByteBuffer b1ByteBuffer = ByteBuffer.allocate(num * byteL);
        for (int index = 0; index < num; index++) {
            b1ByteBuffer.put(BigIntegerUtils.nonNegBigIntegerToByteArray(b1Vector.getElement(index), byteL));
        }
        ZlVector c1Vector = thatTriple.getVectorC();
        ByteBuffer c1ByteBuffer = ByteBuffer.allocate(num * byteL);
        for (int index = 0; index < num; index++) {
            c1ByteBuffer.put(BigIntegerUtils.nonNegBigIntegerToByteArray(c1Vector.getElement(index), byteL));
        }
        List<byte[]> rightResponsePayload = new LinkedList<>();
        rightResponsePayload.add(a1ByteBuffer.array());
        rightResponsePayload.add(b1ByteBuffer.array());
        rightResponsePayload.add(c1ByteBuffer.array());
        sendRightPartyAidPayload(encodeTaskId, TrustDealerPtoStep.REQUEST_RESPONSE.ordinal(), rightResponsePayload);
    }

    private void handleZl64Triple(DataPacket thisDataPacket) throws MpcAbortException {
        DataPacketHeader receivedHeader = thisDataPacket.getHeader();
        DataPacket thatRequestDataPacket = receiveAnyAidDataPacket(receivedHeader);
        long encodeTaskId = receivedHeader.getEncodeTaskId();
        // parse and check type
        List<byte[]> thisRequestPayload = thisDataPacket.getPayload();
        List<byte[]> thatRequestPayload = thatRequestDataPacket.getPayload();
        MpcAbortPreconditions.checkArgument(thisRequestPayload.size() == 2);
        MpcAbortPreconditions.checkArgument(thatRequestPayload.size() == 2);
        // read l
        int l = IntUtils.byteArrayToInt(thisRequestPayload.get(0));
        int thatL = IntUtils.byteArrayToInt(thatRequestPayload.get(0));
        MpcAbortPreconditions.checkArgument(l > 0 && l <= Long.SIZE);
        MpcAbortPreconditions.checkArgument(l == thatL);
        // read num
        int num = IntUtils.byteArrayToInt(thisRequestPayload.get(1));
        MpcAbortPreconditions.checkArgument(num > 0);
        int thatNum = IntUtils.byteArrayToInt(thatRequestPayload.get(1));
        MpcAbortPreconditions.checkArgument(num == thatNum);
        // generate Zl64 triples
        Zl64 zl64 = Zl64Factory.createInstance(envType, l);
        int byteL = zl64.getByteL();
        Zl64Triple thisTriple = Zl64Triple.createRandom(zl64, num, secureRandom);
        Zl64Triple thatTriple = Zl64Triple.createRandom(thisTriple, secureRandom);
        Zl64Vector a0Vector = thisTriple.getVectorA();
        ByteBuffer a0ByteBuffer = ByteBuffer.allocate(num * byteL);
        for (int index = 0; index < num; index++) {
            a0ByteBuffer.put(LongUtils.longToFixedByteArray(a0Vector.getElement(index), byteL));
        }
        Zl64Vector b0Vector = thisTriple.getVectorB();
        ByteBuffer b0ByteBuffer = ByteBuffer.allocate(num * byteL);
        for (int index = 0; index < num; index++) {
            b0ByteBuffer.put(LongUtils.longToFixedByteArray(b0Vector.getElement(index), byteL));
        }
        Zl64Vector c0Vector = thisTriple.getVectorC();
        ByteBuffer c0ByteBuffer = ByteBuffer.allocate(num * byteL);
        for (int index = 0; index < num; index++) {
            c0ByteBuffer.put(LongUtils.longToFixedByteArray(c0Vector.getElement(index), byteL));
        }
        List<byte[]> leftResponsePayload = new LinkedList<>();
        leftResponsePayload.add(a0ByteBuffer.array());
        leftResponsePayload.add(b0ByteBuffer.array());
        leftResponsePayload.add(c0ByteBuffer.array());
        sendLeftPartyAidPayload(encodeTaskId, TrustDealerPtoStep.REQUEST_RESPONSE.ordinal(), leftResponsePayload);
        // response to the right party
        Zl64Vector a1Vector = thatTriple.getVectorA();
        ByteBuffer a1ByteBuffer = ByteBuffer.allocate(num * byteL);
        for (int index = 0; index < num; index++) {
            a1ByteBuffer.put(LongUtils.longToFixedByteArray(a1Vector.getElement(index), byteL));
        }
        Zl64Vector b1Vector = thatTriple.getVectorB();
        ByteBuffer b1ByteBuffer = ByteBuffer.allocate(num * byteL);
        for (int index = 0; index < num; index++) {
            b1ByteBuffer.put(LongUtils.longToFixedByteArray(b1Vector.getElement(index), byteL));
        }
        Zl64Vector c1Vector = thatTriple.getVectorC();
        ByteBuffer c1ByteBuffer = ByteBuffer.allocate(num * byteL);
        for (int index = 0; index < num; index++) {
            c1ByteBuffer.put(LongUtils.longToFixedByteArray(c1Vector.getElement(index), byteL));
        }
        List<byte[]> rightResponsePayload = new LinkedList<>();
        rightResponsePayload.add(a1ByteBuffer.array());
        rightResponsePayload.add(b1ByteBuffer.array());
        rightResponsePayload.add(c1ByteBuffer.array());
        sendRightPartyAidPayload(encodeTaskId, TrustDealerPtoStep.REQUEST_RESPONSE.ordinal(), rightResponsePayload);
    }

    private void handleDestroy(DataPacket thisDataPacket) throws MpcAbortException {
        DataPacketHeader receivedHeader = thisDataPacket.getHeader();
        DataPacket thatDataPacket = receiveAnyAidDataPacket(receivedHeader);
        long encodeTaskId = receivedHeader.getEncodeTaskId();
        MpcAbortPreconditions.checkArgument(thisDataPacket.getPayload().isEmpty());
        MpcAbortPreconditions.checkArgument(thatDataPacket.getPayload().isEmpty());
        count--;
        // response to the left party
        sendLeftPartyAidPayload(encodeTaskId, TrustDealerPtoStep.DESTROY_RESPONSE.ordinal(), new LinkedList<>());
        // response to the right party
        sendRightPartyAidPayload(encodeTaskId, TrustDealerPtoStep.DESTROY_RESPONSE.ordinal(), new LinkedList<>());
    }
}
