package edu.alibaba.mpc4j.s2pc.pso.psu.zcl23;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.zp.ZpDokvs;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.zp.ZpDokvsFactory;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.zp.ZpDokvsFactory.ZpDokvsType;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pso.psu.AbstractPsuClient;
import edu.alibaba.mpc4j.s2pc.pso.psu.zcl23.Zcl23PkePsuPtoDesc.PtoStep;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * ZCL23-PKE-PSU client.
 *
 * @author Weiran Liu
 * @date 2022/02/16
 */
public class Zcl23PkePsuClient extends AbstractPsuClient {
    /**
     * 核COT协议接收方
     */
    private final CoreCotReceiver coreCotReceiver;
    /**
     * Zp-DOKVS type
     */
    private final ZpDokvsType zpDokvsType;
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
     * 索引点
     */
    private ECPoint s;
    /**
     * 私钥
     */
    private BigInteger x;
    /**
     * 公钥
     */
    private ECPoint y;
    /**
     * DOKVS-encoded KEM
     */
    private List<byte[]> kemDokvsPayload;
    /**
     * DOKVS-encoded ciphertext
     */
    private List<byte[]> ctDokvsPayload;
    /**
     * PEQT array
     */
    private boolean[] peqtArray;

    public Zcl23PkePsuClient(Rpc clientRpc, Party serverParty, Zcl23PkePsuConfig config) {
        super(Zcl23PkePsuPtoDesc.getInstance(), clientRpc, serverParty, config);
        coreCotReceiver = CoreCotFactory.createReceiver(clientRpc, serverParty, config.getCoreCotConfig());
        addSubPto(coreCotReceiver);
        zpDokvsType = config.getZpDokvsType();
        compressEncode = config.getCompressEncode();
        pipeSize = config.getPipeSize();
        ecc = EccFactory.createInstance(envType);
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // 初始化各个子协议
        coreCotReceiver.init(maxServerElementSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 3, initTime);

        stopWatch.start();
        // 计算公私钥
        x = ecc.randomZn(secureRandom);
        y = ecc.multiply(ecc.getG(), x);
        List<byte[]> pkPayload = new LinkedList<>();
        pkPayload.add(ecc.encode(y, compressEncode));
        DataPacketHeader pkHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PK.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(pkHeader, pkPayload));
        stopWatch.stop();
        long pkTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 3, pkTime);

        stopWatch.start();
        DataPacketHeader keysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_DOKVS_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> keysPayload = rpc.receive(keysHeader).getPayload();
        int dokvsHashKeyNum = ZpDokvsFactory.getHashKeyNum(zpDokvsType);
        MpcAbortPreconditions.checkArgument(keysPayload.size() == dokvsHashKeyNum);
        dokvsHashKeys = keysPayload.toArray(new byte[0][]);
        // 预计算
        ecc.precompute(ecc.getG());
        ecc.precompute(y);
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
        generateDokvsPayload();
        stopWatch.stop();
        // DOKVS KEM
        DataPacketHeader kemDokvsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_DOKVS_KEM.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(kemDokvsHeader, kemDokvsPayload));
        // DOKVS ciphertext
        DataPacketHeader ctDokvsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_DOKVS_CT.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(ctDokvsHeader, ctDokvsPayload));
        long dokvsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, dokvsTime, "Client generates DOKVS");

        stopWatch.start();
        pipelinePeqt();
        stopWatch.stop();
        long peqtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, peqtTime, "Client runs re-randomized PEQT");

        stopWatch.start();
        CotReceiverOutput cotReceiverOutput = coreCotReceiver.receive(peqtArray);
        DataPacketHeader encHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_ENC_ELEMENTS.ordinal(), extraInfo,
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
                if (peqtArray[index]) {
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

    private void generateDokvsPayload() {
        BigInteger exp = ecc.randomZn(secureRandom);
        s = ecc.multiply(y, exp);

        ZpDokvs<ByteBuffer> zpDokvs = ZpDokvsFactory.createInstance(
            envType, zpDokvsType, ecc.getN(), clientElementSize, dokvsHashKeys
        );
        BigInteger[] rs = IntStream.range(0, clientElementSize)
            .mapToObj(index -> ecc.randomZn(secureRandom))
            .toArray(BigInteger[]::new);
        Map<ByteBuffer, BigInteger> headerMap = IntStream.range(0, clientElementSize)
            .boxed()
            .collect(Collectors.toMap(
                index -> clientElementArrayList.get(index),
                index -> rs[index]
            ));
        Map<ByteBuffer, BigInteger> payloadMap = IntStream.range(0, clientElementSize)
            .boxed()
            .collect(Collectors.toMap(
                index -> clientElementArrayList.get(index),
                index -> rs[index].add(exp).mod(ecc.getN())
            ));
        BigInteger[] kemZpDokvsStorage = zpDokvs.encode(headerMap, true);
        BigInteger[] ctZpDokvsStorage = zpDokvs.encode(payloadMap, true);
        // 打包
        Stream<BigInteger> kemDokvsStream = Arrays.stream(kemZpDokvsStorage);
        kemDokvsStream = parallel ? kemDokvsStream.parallel() : kemDokvsStream;
        kemDokvsPayload = kemDokvsStream
            .map(r -> ecc.multiply(ecc.getG(), r))
            .map(kem -> ecc.encode(kem, compressEncode))
            .collect(Collectors.toList());
        Stream<BigInteger> ctDokvsStream = Arrays.stream(ctZpDokvsStorage);
        ctDokvsStream = parallel ? ctDokvsStream.parallel() : ctDokvsStream;
        ctDokvsPayload = ctDokvsStream
            .map(r -> ecc.multiply(y, r))
            .map(ct -> ecc.encode(ct, compressEncode))
            .collect(Collectors.toList());
    }

    private void pipelinePeqt() throws MpcAbortException {
        peqtArray = new boolean[serverElementSize];
        // Pipeline过程，先执行整除倍，最后再循环一遍
        int pipelineTime = serverElementSize / pipeSize;
        int round;
        for (round = 0; round < pipelineTime; round++) {
            // 接收KEM
            DataPacketHeader reRandKemHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), Zcl23PkePsuPtoDesc.PtoStep.SERVER_SEND_RERAND_KEM.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
            );
            List<byte[]> reRandKemPayload = rpc.receive(reRandKemHeader).getPayload();
            MpcAbortPreconditions.checkArgument(reRandKemPayload.size() == pipeSize);
            // 解码密文
            Stream<byte[]> reRandKemStream = reRandKemPayload.stream();
            reRandKemStream = parallel ? reRandKemStream.parallel() : reRandKemStream;
            ECPoint[] reRandKemArray = reRandKemStream
                .map(ecc::decode)
                .toArray(ECPoint[]::new);
            // 接收密文
            DataPacketHeader reRandCtHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), Zcl23PkePsuPtoDesc.PtoStep.SERVER_SEND_RERAND_CT.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
            );
            List<byte[]> reRandCtPayload = rpc.receive(reRandCtHeader).getPayload();
            MpcAbortPreconditions.checkArgument(reRandCtPayload.size() == pipeSize);
            // 解码密文
            Stream<byte[]> reRandCtStream = reRandCtPayload.stream();
            reRandCtStream = parallel ? reRandCtStream.parallel() : reRandCtStream;
            ECPoint[] reRandCtArray = reRandCtStream
                .map(ecc::decode)
                .toArray(ECPoint[]::new);
            // 解密并比较
            int offset = round * pipeSize;
            IntStream decIntStream = IntStream.range(0, pipeSize);
            decIntStream = parallel ? decIntStream.parallel() : decIntStream;
            decIntStream.forEach(index -> {
                ECPoint yr = ecc.multiply(reRandKemArray[index], x);
                ECPoint sStar = reRandCtArray[index].subtract(yr);
                peqtArray[offset + index] = s.equals(sStar);
            });
            extraInfo++;
        }
        int remain = serverElementSize - round * pipeSize;
        if (remain > 0) {
            // 接收KEM
            DataPacketHeader reRandKemHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), Zcl23PkePsuPtoDesc.PtoStep.SERVER_SEND_RERAND_KEM.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
            );
            List<byte[]> reRandKemPayload = rpc.receive(reRandKemHeader).getPayload();
            MpcAbortPreconditions.checkArgument(reRandKemPayload.size() == remain);
            // 解码密文
            Stream<byte[]> reRandKemStream = reRandKemPayload.stream();
            reRandKemStream = parallel ? reRandKemStream.parallel() : reRandKemStream;
            ECPoint[] rerandKemArray = reRandKemStream
                .map(ecc::decode)
                .toArray(ECPoint[]::new);
            // 接收密文
            DataPacketHeader reRandCtHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), Zcl23PkePsuPtoDesc.PtoStep.SERVER_SEND_RERAND_CT.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
            );
            List<byte[]> reRandCtPayload = rpc.receive(reRandCtHeader).getPayload();
            MpcAbortPreconditions.checkArgument(reRandCtPayload.size() == remain);
            // 解码密文
            Stream<byte[]> reRandCtStream = reRandCtPayload.stream();
            reRandCtStream = parallel ? reRandCtStream.parallel() : reRandCtStream;
            ECPoint[] reRandCtArray = reRandCtStream
                .map(ecc::decode)
                .toArray(ECPoint[]::new);
            // 解密并比较
            int offset = round * pipeSize;
            IntStream decIntStream = IntStream.range(0, remain);
            decIntStream = parallel ? decIntStream.parallel() : decIntStream;
            decIntStream.forEach(index -> {
                ECPoint yr = ecc.multiply(rerandKemArray[index], x);
                ECPoint sStar = reRandCtArray[index].subtract(yr);
                peqtArray[offset + index] = s.equals(sStar);
            });
            extraInfo++;
        }
    }
}
