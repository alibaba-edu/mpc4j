package edu.alibaba.mpc4j.s2pc.pso.psu.zcl23;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvs;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrixFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prp.Prp;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.opf.oprp.OprpFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprp.OprpSender;
import edu.alibaba.mpc4j.s2pc.opf.oprp.OprpSenderOutput;
import edu.alibaba.mpc4j.s2pc.pso.psu.AbstractPsuClient;
import edu.alibaba.mpc4j.s2pc.pso.psu.zcl23.Zcl23SkePsuPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * ZCL23-SKE-PSU client.
 *
 * @author Weiran Liu
 * @date 2022/02/16
 */
public class Zcl23SkePsuClient extends AbstractPsuClient {
    /**
     * Z2 circuit receiver
     */
    private final Z2cParty z2cReceiver;
    /**
     * OPRP协议发送方
     */
    private final OprpSender oprpSender;
    /**
     * 核COT协议接收方
     */
    private final CoreCotReceiver coreCotReceiver;
    /**
     * GF2E-DOKVS type
     */
    private final Gf2eDokvsType dokvsType;
    /**
     * GF2E-DOKVS hash keys
     */
    private byte[][] dokvsHashKeys;
    /**
     * PRP密钥
     */
    private byte[] prpKey;
    /**
     * PRP
     */
    private Prp prp;

    public Zcl23SkePsuClient(Rpc clientRpc, Party serverParty, Zcl23SkePsuConfig config) {
        super(Zcl23SkePsuPtoDesc.getInstance(), clientRpc, serverParty, config);
        z2cReceiver = Z2cFactory.createReceiver(clientRpc, serverParty, config.getZ2cConfig());
        addSubPto(z2cReceiver);
        oprpSender = OprpFactory.createSender(clientRpc, serverParty, config.getOprpConfig());
        addSubPto(oprpSender);
        coreCotReceiver = CoreCotFactory.createReceiver(clientRpc, serverParty, config.getCoreCotConfig());
        addSubPto(coreCotReceiver);
        dokvsType = config.getGf2eDokvsType();
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // 涉及三元组部分的初始化
        z2cReceiver.init((long) maxServerElementSize * CommonConstants.BLOCK_BIT_LENGTH);
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
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_DOKVS_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> keysPayload = rpc.receive(keysHeader).getPayload();
        int dokvsHashKeyNum = Gf2eDokvsFactory.getHashKeyNum(dokvsType);
        MpcAbortPreconditions.checkArgument(keysPayload.size() == dokvsHashKeyNum);
        // init DOKVS hash keys
        dokvsHashKeys = keysPayload.toArray(new byte[0][]);
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
        List<byte[]> dokvsPayload = generateDokvsPayload();
        DataPacketHeader dokvsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_DOKVS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(dokvsHeader, dokvsPayload));
        stopWatch.stop();
        long dokvsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, dokvsTime, "Client generates DOKVS");

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
            encodeTaskId, getPtoDesc().getPtoId(), Zcl23SkePsuPtoDesc.PtoStep.SERVER_SEND_ENC_ELEMENTS.ordinal(), extraInfo,
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

    private List<byte[]> generateDokvsPayload() {
        Gf2eDokvs<ByteBuffer> dokvs = Gf2eDokvsFactory.createInstance(
            envType, dokvsType, clientElementSize, CommonConstants.BLOCK_BIT_LENGTH, dokvsHashKeys
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
        return Arrays.stream(dokvs.encode(keyValueMap, true)).collect(Collectors.toList());
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
            SquareZ2Vector notBits = z2cReceiver.not(SquareZ2Vector.create(serverElementSize, bits, false));
            clientPeqtShares = z2cReceiver.and(clientPeqtShares, notBits);
        }
        return clientPeqtShares.getBitVector().getBytes();
    }
}
