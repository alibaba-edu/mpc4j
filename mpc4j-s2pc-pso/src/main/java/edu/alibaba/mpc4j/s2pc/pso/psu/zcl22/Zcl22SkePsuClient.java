package edu.alibaba.mpc4j.s2pc.pso.psu.zcl22;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrixFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prp.Prp;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory;
import edu.alibaba.mpc4j.common.tool.okve.ovdm.gf2e.Gf2eOvdm;
import edu.alibaba.mpc4j.common.tool.okve.ovdm.gf2e.Gf2eOvdmFactory;
import edu.alibaba.mpc4j.common.tool.okve.ovdm.gf2e.Gf2eOvdmFactory.Gf2eOvdmType;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcParty;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.opf.oprp.OprpFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprp.OprpSender;
import edu.alibaba.mpc4j.s2pc.opf.oprp.OprpSenderOutput;
import edu.alibaba.mpc4j.s2pc.pso.psu.AbstractPsuClient;
import edu.alibaba.mpc4j.s2pc.pso.psu.zcl22.Zcl22SkePsuPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * ZCL22-SKE-PSU协议接收方。
 *
 * @author Weiran Liu
 * @date 2022/02/16
 */
public class Zcl22SkePsuClient extends AbstractPsuClient {
    /**
     * BC协议接收方
     */
    private final BcParty bcReceiver;
    /**
     * OPRP协议发送方
     */
    private final OprpSender oprpSender;
    /**
     * 核COT协议接收方
     */
    private final CoreCotReceiver coreCotReceiver;
    /**
     * GF2E-OVDM类型
     */
    private final Gf2eOvdmType gf2eOvdmType;
    /**
     * GF(2^l)-OVDM哈希密钥
     */
    private byte[][] gf2eOvdmHashKeys;
    /**
     * PRP密钥
     */
    private byte[] prpKey;
    /**
     * PRP
     */
    private Prp prp;

    public Zcl22SkePsuClient(Rpc clientRpc, Party serverParty, Zcl22SkePsuConfig config) {
        super(Zcl22SkePsuPtoDesc.getInstance(), clientRpc, serverParty, config);
        bcReceiver = BcFactory.createReceiver(clientRpc, serverParty, config.getBcConfig());
        addSubPtos(bcReceiver);
        oprpSender = OprpFactory.createSender(clientRpc, serverParty, config.getOprpConfig());
        addSubPtos(oprpSender);
        coreCotReceiver = CoreCotFactory.createReceiver(clientRpc, serverParty, config.getCoreCotConfig());
        addSubPtos(coreCotReceiver);
        gf2eOvdmType = config.getGf2eOvdmType();
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // 涉及三元组部分的初始化
        bcReceiver.init(maxServerElementSize, maxServerElementSize * CommonConstants.BLOCK_BIT_LENGTH);
        oprpSender.init(maxServerElementSize);
        stopWatch.stop();
        long bcTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 3, bcTime);

        stopWatch.start();
        // 其他部分初始化
        coreCotReceiver.init(maxServerElementSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 3, initTime);

        stopWatch.start();
        DataPacketHeader keysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_OVDM_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> keysPayload = rpc.receive(keysHeader).getPayload();
        int gf2eOvdmHashKeyNum = Gf2eOvdmFactory.getHashNum(gf2eOvdmType);
        MpcAbortPreconditions.checkArgument(keysPayload.size() == gf2eOvdmHashKeyNum);
        // 初始化Gf2e-OVDM密钥;
        gf2eOvdmHashKeys = keysPayload.toArray(new byte[0][]);
        // 初始化PRP
        prpKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(prpKey);
        prp = PrpFactory.createInstance(oprpSender.getPrpType());
        prp.setKey(prpKey);
        stopWatch.stop();
        long keyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 3, keyTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Set<ByteBuffer> psu(Set<ByteBuffer> clientElementSet, int serverElementSize, int elementByteLength)
        throws MpcAbortException {
        setPtoInput(clientElementSet, serverElementSize, elementByteLength);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        List<byte[]> ovdmPayload = generateOvdmPayload();
        DataPacketHeader ovdmHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_OVDM.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(ovdmHeader, ovdmPayload));
        stopWatch.stop();
        long ovdmTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, ovdmTime, "Client generates OVDM");

        stopWatch.start();
        OprpSenderOutput oprpSenderOutput = oprpSender.oprp(prpKey, serverElementSize);
        // 得到是否为0的信息
        byte[] peqtArray = generatePeqtShares(oprpSenderOutput);
        DataPacketHeader peqtSharesHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_PEQT_SHARES.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> peqtSharesPayload = rpc.receive(peqtSharesHeader).getPayload();
        MpcAbortPreconditions.checkArgument(peqtSharesPayload.size() == 1);
        byte[] serverPeqtShares = peqtSharesPayload.remove(0);
        BytesUtils.xori(peqtArray, serverPeqtShares);
        boolean[] choiceArray = BinaryUtils.byteArrayToBinary(peqtArray, serverElementSize);
        stopWatch.stop();
        long peqtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, peqtTime, "Server runs PEQT with Boolean circuits");

        stopWatch.start();
        CotReceiverOutput cotReceiverOutput = coreCotReceiver.receive(choiceArray);
        DataPacketHeader encHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Zcl22SkePsuPtoDesc.PtoStep.SERVER_SEND_ENC_ELEMENTS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> encPayload = rpc.receive(encHeader).getPayload();
        MpcAbortPreconditions.checkArgument(encPayload.size() == serverElementSize);
        ArrayList<byte[]> encArrayList = new ArrayList<>(encPayload);
        // Y \cup Z
        Prg encPrg = PrgFactory.createInstance(envType, elementByteLength);
        IntStream decIntStream = IntStream.range(0, serverElementSize);
        decIntStream = parallel ? decIntStream.parallel() : decIntStream;
        Set<ByteBuffer> union = decIntStream
            .mapToObj(index -> {
                if (choiceArray[index]) {
                    return botElementByteBuffer;
                } else {
                    // do not need CRHF since we call prg
                    byte[] message = encPrg.extendToBytes(cotReceiverOutput.getRb(index));
                    BytesUtils.xori(message, encArrayList.get(index));
                    return ByteBuffer.wrap(message);
                }
            })
            .collect(Collectors.toSet());
        union.addAll(clientElementSet);
        union.remove(botElementByteBuffer);

        stopWatch.stop();
        long unionTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, unionTime, "Client handles union");

        logPhaseInfo(PtoState.PTO_END);
        return union;
    }

    private List<byte[]> generateOvdmPayload() {
        Gf2eOvdm<ByteBuffer> gf2eOvdm = Gf2eOvdmFactory.createInstance(
            envType, gf2eOvdmType, CommonConstants.BLOCK_BIT_LENGTH, clientElementSize, gf2eOvdmHashKeys
        );
        // 加密
        IntStream receiverElementIndexStream = IntStream.range(0, clientElementSize);
        receiverElementIndexStream = parallel ? receiverElementIndexStream.parallel() : receiverElementIndexStream;
        Map<ByteBuffer, byte[]> keyValueMap = receiverElementIndexStream
            .boxed()
            .collect(Collectors.toMap(
                index -> clientElementArrayList.get(index),
                index -> {
                    byte[] indexBlock = ByteBuffer.allocate(CommonConstants.BLOCK_BYTE_LENGTH)
                        .putInt(CommonConstants.BLOCK_BYTE_LENGTH - Integer.BYTES, index).array();
                    return oprpSender.isInvPrp() ? prp.prp(indexBlock) : prp.invPrp(indexBlock);
                }
            ));
        return Arrays.stream(gf2eOvdm.encode(keyValueMap)).collect(Collectors.toList());
    }

    private byte[] generatePeqtShares(OprpSenderOutput oprpSenderOutput) throws MpcAbortException {
        TransBitMatrix transBitMatrix = TransBitMatrixFactory.createInstance(
            envType, CommonConstants.BLOCK_BIT_LENGTH, serverElementSize, parallel
        );
        for (int i = 0; i < serverElementSize; i++) {
            transBitMatrix.setColumn(i, oprpSenderOutput.getShare(i));
        }
        TransBitMatrix transposeTransBitMatrix = transBitMatrix.transpose();
        int logSize = LongUtils.ceilLog2(serverElementSize);
        // 客户端初始化全为0
        SquareZ2Vector clientPeqtShares = SquareZ2Vector.createZeros(serverElementSize);
        for (int index = 0; index < CommonConstants.BLOCK_BIT_LENGTH - logSize; index++) {
            byte[] bits = transposeTransBitMatrix.getColumn(index);
            SquareZ2Vector notBits = bcReceiver.not(SquareZ2Vector.create(serverElementSize, bits, false));
            clientPeqtShares = bcReceiver.and(clientPeqtShares, notBits);
        }
        return clientPeqtShares.getBitVector().getBytes();
    }
}
