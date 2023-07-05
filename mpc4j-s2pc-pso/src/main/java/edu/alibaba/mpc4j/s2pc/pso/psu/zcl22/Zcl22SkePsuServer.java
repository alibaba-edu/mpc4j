package edu.alibaba.mpc4j.s2pc.pso.psu.zcl22;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrixFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.crypto.matrix.okve.ovdm.gf2e.Gf2eOvdm;
import edu.alibaba.mpc4j.crypto.matrix.okve.ovdm.gf2e.Gf2eOvdmFactory;
import edu.alibaba.mpc4j.crypto.matrix.okve.ovdm.gf2e.Gf2eOvdmFactory.Gf2eOvdmType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;
import edu.alibaba.mpc4j.s2pc.opf.oprp.OprpFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprp.OprpReceiver;
import edu.alibaba.mpc4j.s2pc.opf.oprp.OprpReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pso.psu.AbstractPsuServer;
import edu.alibaba.mpc4j.s2pc.pso.psu.zcl22.Zcl22SkePsuPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * ZCL22-SKE-PSU协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/02/16
 */
public class Zcl22SkePsuServer extends AbstractPsuServer {
    /**
     * Z2 circuit sender
     */
    private final Z2cParty z2cSender;
    /**
     * OPRP协议接收方
     */
    private final OprpReceiver oprpReceiver;
    /**
     * 核COT协议发送方
     */
    private final CoreCotSender coreCotSender;
    /**
     * GF2X-OVDM类型
     */
    private final Gf2eOvdmType gf2eOvdmType;
    /**
     * GF(2^l)-OVDM哈希密钥
     */
    private byte[][] gf2eOvdmHashKeys;
    /**
     * 服务端密文
     */
    private byte[][] senderMessages;

    public Zcl22SkePsuServer(Rpc serverRpc, Party clientParty, Zcl22SkePsuConfig config) {
        super(Zcl22SkePsuPtoDesc.getInstance(), serverRpc, clientParty, config);
        z2cSender = Z2cFactory.createSender(serverRpc, clientParty, config.getZ2cConfig());
        addSubPtos(z2cSender);
        oprpReceiver = OprpFactory.createReceiver(serverRpc, clientParty, config.getOprpConfig());
        addSubPtos(oprpReceiver);
        coreCotSender = CoreCotFactory.createSender(serverRpc, clientParty, config.getCoreCotConfig());
        addSubPtos(coreCotSender);
        gf2eOvdmType = config.getGf2eOvdmType();
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // 涉及三元组部分的初始化
        z2cSender.init(maxServerElementSize * CommonConstants.BLOCK_BIT_LENGTH);
        oprpReceiver.init(maxServerElementSize);
        stopWatch.stop();
        long bcTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 3, bcTime);

        stopWatch.start();
        // 其他部分初始化
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(delta);
        coreCotSender.init(delta, maxServerElementSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 3, initTime);

        stopWatch.start();
        List<byte[]> keysPayload = new LinkedList<>();
        // 初始化Gf2e-OVDM密钥
        int gf2eOvdmHashKeyNum = Gf2eOvdmFactory.getHashNum(gf2eOvdmType);
        gf2eOvdmHashKeys = IntStream.range(0, gf2eOvdmHashKeyNum)
            .mapToObj(keyIndex -> {
                byte[] key = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                secureRandom.nextBytes(key);
                keysPayload.add(key);
                return key;
            })
            .toArray(byte[][]::new);
        DataPacketHeader keysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Zcl22SkePsuPtoDesc.PtoStep.SERVER_SEND_OVDM_KEYS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(keysHeader, keysPayload));
        stopWatch.stop();
        long keyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 3, keyTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void psu(Set<ByteBuffer> serverElementSet, int clientElementSize, int elementByteLength)
        throws MpcAbortException {
        setPtoInput(serverElementSet, clientElementSize, elementByteLength);
        logPhaseInfo(PtoState.PTO_BEGIN);

        DataPacketHeader ovdmHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_OVDM.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> ovdmPayload = rpc.receive(ovdmHeader).getPayload();

        stopWatch.start();
        handleOvdmPayload(ovdmPayload);
        stopWatch.stop();
        long ovdmTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, ovdmTime, "Server handles OVDM");

        stopWatch.start();
        OprpReceiverOutput oprpReceiverOutput = oprpReceiver.oprp(senderMessages);
        // 得到是否为0的信息
        byte[] serverChoiceShares = generatePeqtShares(oprpReceiverOutput);
        List<byte[]> peqtSharesPayload = new LinkedList<>();
        peqtSharesPayload.add(serverChoiceShares);
        DataPacketHeader peqtSharesHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), Zcl22SkePsuPtoDesc.PtoStep.SERVER_SEND_PEQT_SHARES.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(peqtSharesHeader, peqtSharesPayload));
        stopWatch.stop();
        long peqtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, peqtTime, "Server runs PEQT with Boolean circuits");

        stopWatch.start();
        CotSenderOutput cotSenderOutput = coreCotSender.send(serverElementSize);
        Prg encPrg = PrgFactory.createInstance(envType, elementByteLength);
        IntStream encIntStream = IntStream.range(0, serverElementSize);
        encIntStream = parallel ? encIntStream.parallel() : encIntStream;
        List<byte[]> encPayload = encIntStream
            .mapToObj(index -> {
                // do not need CRHF since we call prg
                byte[] ciphertext = encPrg.extendToBytes(cotSenderOutput.getR0(index));
                BytesUtils.xori(ciphertext, serverElementArrayList.get(index).array());
                return ciphertext;
            })
            .collect(Collectors.toList());
        DataPacketHeader encHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_ENC_ELEMENTS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(encHeader, encPayload));
        stopWatch.stop();
        long encTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, encTime, "Server handles union");

        logPhaseInfo(PtoState.PTO_END);
    }

    private void handleOvdmPayload(List<byte[]> ovdmPayload) throws MpcAbortException {
        int gf2eOvdmM = Gf2eOvdmFactory.getM(gf2eOvdmType, clientElementSize);
        MpcAbortPreconditions.checkArgument(ovdmPayload.size() == gf2eOvdmM);
        // 读取OVDM
        byte[][] storages = ovdmPayload.toArray(new byte[0][]);
        Gf2eOvdm<ByteBuffer> gf2eOvdm = Gf2eOvdmFactory.createInstance(
            envType, gf2eOvdmType, CommonConstants.BLOCK_BIT_LENGTH, clientElementSize, gf2eOvdmHashKeys
        );
        IntStream senderMessageIntStream = IntStream.range(0, serverElementSize);
        senderMessageIntStream = parallel ? senderMessageIntStream.parallel() : senderMessageIntStream;
        senderMessages = senderMessageIntStream
            .mapToObj(index -> gf2eOvdm.decode(storages, serverElementArrayList.get(index)))
            .toArray(byte[][]::new);
    }

    private byte[] generatePeqtShares(OprpReceiverOutput oprpReceiverOutput) throws MpcAbortException {
        TransBitMatrix transBitMatrix = TransBitMatrixFactory.createInstance(
            envType, CommonConstants.BLOCK_BIT_LENGTH, serverElementSize, parallel
        );
        for (int i = 0; i < serverElementSize; i++) {
            transBitMatrix.setColumn(i, oprpReceiverOutput.getShare(i));
        }
        TransBitMatrix transposeTransBitMatrix = transBitMatrix.transpose();
        int logSize = LongUtils.ceilLog2(serverElementSize);
        // 服务端结构初始化全为1
        SquareZ2Vector serverPeqtShares = SquareZ2Vector.createOnes(serverElementSize);
        for (int index = 0; index < CommonConstants.BLOCK_BIT_LENGTH - logSize; index++) {
            byte[] bits = transposeTransBitMatrix.getColumn(index);
            SquareZ2Vector notBits = z2cSender.not(SquareZ2Vector.create(serverElementSize, bits, false));
            serverPeqtShares = z2cSender.and(serverPeqtShares, notBits);
        }
        return serverPeqtShares.getBitVector().getBytes();
    }
}
