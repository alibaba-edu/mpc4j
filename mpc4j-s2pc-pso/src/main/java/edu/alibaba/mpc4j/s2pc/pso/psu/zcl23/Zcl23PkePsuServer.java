package edu.alibaba.mpc4j.s2pc.pso.psu.zcl23;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.ecc.EccDokvs;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.ecc.EccDokvsFactory;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.ecc.EccDokvsFactory.EccDokvsType;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;
import edu.alibaba.mpc4j.s2pc.pso.psu.AbstractPsuServer;
import edu.alibaba.mpc4j.s2pc.pso.psu.zcl23.Zcl23PkePsuPtoDesc.PtoStep;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * ZCL23-PKE-PSU server.
 *
 * @author Weiran Liu
 * @date 2022/02/16
 */
public class Zcl23PkePsuServer extends AbstractPsuServer {
    /**
     * 核COT协议发送方
     */
    private final CoreCotSender coreCotSender;
    /**
     * ECC-DOKVS type
     */
    private final EccDokvsType eccDokvsType;
    /**
     * 是否使用压缩椭圆曲线编码
     */
    private final boolean compressEncode;
    /**
     * 流水线数量
     */
    private final int pipeSize;
    /**
     * 椭圆曲线
     */
    private final Ecc ecc;
    /**
     * DOKVS hash keys
     */
    private byte[][] dokvsHashKeys;
    /**
     * 公钥
     */
    private ECPoint y;
    /**
     * ECC-DOKVS
     */
    private EccDokvs<ByteBuffer> eccDokvs;
    /**
     * ECC-DOKVS KEM storage
     */
    private ECPoint[] kemDokvsStorage;
    /**
     * ECC-DOKVS ciphertext storage
     */
    private ECPoint[] ctDokvsStorage;

    public Zcl23PkePsuServer(Rpc serverRpc, Party clientParty, Zcl23PkePsuConfig config) {
        super(Zcl23PkePsuPtoDesc.getInstance(), serverRpc, clientParty, config);
        coreCotSender = CoreCotFactory.createSender(serverRpc, clientParty, config.getCoreCotConfig());
        addSubPto(coreCotSender);
        eccDokvsType = config.getEccDokvsType();
        compressEncode = config.getCompressEncode();
        pipeSize = config.getPipeSize();
        ecc = EccFactory.createInstance(envType);
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // 初始化各个子协议
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(delta);
        coreCotSender.init(delta, maxServerElementSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 3, initTime);

        stopWatch.start();
        List<byte[]> keysPayload = new LinkedList<>();
        // init DOKVS keys
        int dokvsHashKeyNum = EccDokvsFactory.getHashKeyNum(eccDokvsType);
        dokvsHashKeys = IntStream.range(0, dokvsHashKeyNum)
            .mapToObj(keyIndex -> {
                byte[] key = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                secureRandom.nextBytes(key);
                keysPayload.add(key);
                return key;
            })
            .toArray(byte[][]::new);
        DataPacketHeader keysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_DOKVS_KEYS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(keysHeader, keysPayload));
        stopWatch.stop();
        long keyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 3, keyTime);

        stopWatch.start();
        DataPacketHeader pkHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PK.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> pkPayload = rpc.receive(pkHeader).getPayload();
        MpcAbortPreconditions.checkArgument(pkPayload.size() == 1);
        if (y != null) {
            // 清理之前的预计算结果
            ecc.destroyPrecompute(y);
        }
        y = ecc.decode(pkPayload.remove(0));
        // 预计算
        ecc.precompute(ecc.getG());
        ecc.precompute(y);
        stopWatch.stop();
        long pkTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 3, pkTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void psu(Set<ByteBuffer> serverElementSet, int clientElementSize, int elementByteLength)
        throws MpcAbortException {
        setPtoInput(serverElementSet, clientElementSize, elementByteLength);
        logPhaseInfo(PtoState.PTO_BEGIN);

        // DOKVS KEM
        DataPacketHeader kemDokvsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_DOKVS_KEM.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> kemDokvsPayload = rpc.receive(kemDokvsHeader).getPayload();
        // DOKVS ciphertext
        DataPacketHeader ctDokvsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_DOKVS_CT.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> ctDokvsPayload = rpc.receive(ctDokvsHeader).getPayload();

        stopWatch.start();
        handleDokvsPayload(kemDokvsPayload, ctDokvsPayload);
        stopWatch.stop();
        long dokvsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, dokvsTime, "Server handles DOKVS");

        stopWatch.start();
        pipelineReRand();
        stopWatch.stop();
        long reRandTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, reRandTime, "Server runs re-randomized PEQT");

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
            encodeTaskId, getPtoDesc().getPtoId(), Zcl23PkePsuPtoDesc.PtoStep.SERVER_SEND_ENC_ELEMENTS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(encHeader, encPayload));
        stopWatch.stop();
        long encTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, encTime, "Server handles union");

        logPhaseInfo(PtoState.PTO_END);
    }

    private void handleDokvsPayload(List<byte[]> kemDokvsPayload, List<byte[]> ctDokvsPayload) throws MpcAbortException {
        int eccDokvsM = EccDokvsFactory.getM(eccDokvsType, clientElementSize);
        MpcAbortPreconditions.checkArgument(kemDokvsPayload.size() == eccDokvsM);
        MpcAbortPreconditions.checkArgument(ctDokvsPayload.size() == eccDokvsM);
        // 读取header和payload
        kemDokvsStorage = kemDokvsPayload.stream()
            .map(ecc::decode)
            .toArray(ECPoint[]::new);
        ctDokvsStorage = ctDokvsPayload.stream()
            .map(ecc::decode)
            .toArray(ECPoint[]::new);
        eccDokvs = EccDokvsFactory.createInstance(envType, eccDokvsType, ecc, clientElementSize, dokvsHashKeys);
    }

    private void pipelineReRand() {
        // 生成随机量
        BigInteger[] rs = IntStream.range(0, serverElementSize)
            .mapToObj(index -> ecc.randomZn(secureRandom))
            .toArray(BigInteger[]::new);
        // Pipeline过程，先执行整除倍，最后再循环一遍
        int pipelineTime = serverElementSize / pipeSize;
        int round;
        for (round = 0; round < pipelineTime; round++) {
            int offset = round * pipeSize;
            // 计算KEM
            IntStream kemIntStream = IntStream.range(0, pipeSize);
            kemIntStream = parallel ? kemIntStream.parallel() : kemIntStream;
            List<byte[]> reRandKemPayload = kemIntStream
                .mapToObj(index -> {
                    ECPoint gr = ecc.multiply(ecc.getG(), rs[offset + index]);
                    return eccDokvs.decode(kemDokvsStorage, serverElementArrayList.get(offset + index)).add(gr);
                })
                .map(kem -> ecc.encode(kem, compressEncode))
                .collect(Collectors.toList());
            // 发送KEM
            DataPacketHeader reRandKemHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), Zcl23PkePsuPtoDesc.PtoStep.SERVER_SEND_RERAND_KEM.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(reRandKemHeader, reRandKemPayload));
            // 计算密文
            IntStream ctIntStream = IntStream.range(0, pipeSize);
            ctIntStream = parallel ? ctIntStream.parallel() : ctIntStream;
            List<byte[]> reRandCtPayload = ctIntStream
                .mapToObj(index -> {
                    ECPoint yr = ecc.multiply(y, rs[offset + index]);
                    return eccDokvs.decode(ctDokvsStorage, serverElementArrayList.get(offset + index)).add(yr);
                })
                .map(ct -> ecc.encode(ct, compressEncode))
                .collect(Collectors.toList());
            DataPacketHeader reRandCtHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RERAND_CT.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(reRandCtHeader, reRandCtPayload));
            extraInfo++;
        }
        int remain = serverElementSize - round * pipeSize;
        if (remain > 0) {
            int offset = round * pipeSize;
            // 计算KEM
            IntStream kemIntStream = IntStream.range(0, remain);
            kemIntStream = parallel ? kemIntStream.parallel() : kemIntStream;
            List<byte[]> reRandKemPayload = kemIntStream
                .mapToObj(index -> {
                    ECPoint gr = ecc.multiply(ecc.getG(), rs[offset + index]);
                    return eccDokvs.decode(kemDokvsStorage, serverElementArrayList.get(offset + index)).add(gr);
                })
                .map(kem -> ecc.encode(kem, compressEncode))
                .collect(Collectors.toList());
            // 发送KEM
            DataPacketHeader reRandKemHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RERAND_KEM.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(reRandKemHeader, reRandKemPayload));
            // 计算密文
            IntStream ctIntStream = IntStream.range(0, remain);
            ctIntStream = parallel ? ctIntStream.parallel() : ctIntStream;
            List<byte[]> reRandCtPayload = ctIntStream
                .mapToObj(index -> {
                    ECPoint yr = ecc.multiply(y, rs[offset + index]);
                    return eccDokvs.decode(ctDokvsStorage, serverElementArrayList.get(offset + index)).add(yr);
                })
                .map(ct -> ecc.encode(ct, compressEncode))
                .collect(Collectors.toList());
            // 发送密文
            DataPacketHeader reRandCtHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RERAND_CT.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(reRandCtHeader, reRandCtPayload));
            extraInfo++;
        }
    }
}
