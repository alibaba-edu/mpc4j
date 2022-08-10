package edu.alibaba.mpc4j.s2pc.pso.psu.zcl22;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.Crhf;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory.CrhfType;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.okve.ovdm.zp.ZpOvdm;
import edu.alibaba.mpc4j.common.tool.okve.ovdm.zp.ZpOvdmFactory;
import edu.alibaba.mpc4j.common.tool.okve.ovdm.zp.ZpOvdmFactory.ZpOvdmType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pso.psu.AbstractPsuClient;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * ZCL22-PKE-PSU协议接收方。
 *
 * @author Weiran Liu
 * @date 2022/02/16
 */
public class Zcl22PkePsuClient extends AbstractPsuClient {
    /**
     * 核COT协议接收方
     */
    private final CoreCotReceiver coreCotReceiver;
    /**
     * Zp-OVDM类型
     */
    private final ZpOvdmType zpOvdmType;
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
     * 抗关联哈希函数
     */
    private final Crhf crhf;
    /**
     * ECC-OVDM哈希密钥
     */
    private byte[][] zpOvdmHashKeys;
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
     * OVDM密文
     */
    private List<byte[]> kemOvdmPayload;
    /**
     * OVDM负载
     */
    private List<byte[]> ctOvdmPayload;
    /**
     * preq结果矩阵
     */
    private boolean[] peqtArray;

    public Zcl22PkePsuClient(Rpc clientRpc, Party serverParty, Zcl22PkePsuConfig config) {
        super(Zcl22PkePsuPtoDesc.getInstance(), clientRpc, serverParty, config);
        coreCotReceiver = CoreCotFactory.createReceiver(clientRpc, serverParty, config.getCoreCotConfig());
        coreCotReceiver.addLogLevel();
        zpOvdmType = config.getZpOvdmType();
        compressEncode = config.getCompressEncode();
        pipeSize = config.getPipeSize();
        ecc = EccFactory.createInstance(getEnvType());
        crhf = CrhfFactory.createInstance(getEnvType(), CrhfType.MMO);
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        coreCotReceiver.setTaskId(taskId);
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        coreCotReceiver.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        coreCotReceiver.addLogLevel();
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        info("{}{} Client Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // 初始化各个子协议
        coreCotReceiver.init(maxServerElementSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Init Step 1/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        stopWatch.start();
        // 计算公私钥
        x = ecc.randomZn(secureRandom);
        y = ecc.multiply(ecc.getG(), x);
        List<byte[]> pkPayload = new LinkedList<>();
        pkPayload.add(ecc.encode(y, compressEncode));
        DataPacketHeader pkHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Zcl22PkePsuPtoDesc.PtoStep.CLIENT_SEND_PK.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(pkHeader, pkPayload));
        stopWatch.stop();
        long pkTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Init Step 2/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), pkTime);

        stopWatch.start();
        DataPacketHeader keysHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Zcl22PkePsuPtoDesc.PtoStep.SERVER_SEND_OVDM_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> keysPayload = rpc.receive(keysHeader).getPayload();
        int zpOvdmHashKeyNum = ZpOvdmFactory.getHashNum(zpOvdmType);
        MpcAbortPreconditions.checkArgument(keysPayload.size() == zpOvdmHashKeyNum);
        zpOvdmHashKeys = keysPayload.toArray(new byte[0][]);
        // 预计算
        ecc.precompute(ecc.getG());
        ecc.precompute(y);
        stopWatch.stop();
        long keyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Init Step 3/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), keyTime);

        initialized = true;
        info("{}{} Client Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public Set<ByteBuffer> psu(Set<ByteBuffer> clientElementSet, int serverElementSize, int elementByteLength)
        throws MpcAbortException {
        setPtoInput(clientElementSet, serverElementSize, elementByteLength);
        info("{}{} Client begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        generateOvdmPayload();
        stopWatch.stop();
        // 发送密文header
        DataPacketHeader kemOvdmHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Zcl22PkePsuPtoDesc.PtoStep.CLIENT_SEND_OVDM_KEM.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(kemOvdmHeader, kemOvdmPayload));
        // 发送密文payload
        DataPacketHeader ctOvdmHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Zcl22PkePsuPtoDesc.PtoStep.CLIENT_SEND_OVDM_CT.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(ctOvdmHeader, ctOvdmPayload));
        long ovdmTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 1/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), ovdmTime);

        stopWatch.start();
        pipelinePeqt();
        stopWatch.stop();
        long peqtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Client Step 2/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), peqtTime);

        stopWatch.start();
        CotReceiverOutput cotReceiverOutput = coreCotReceiver.receive(peqtArray);
        DataPacketHeader encHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Zcl22PkePsuPtoDesc.PtoStep.SERVER_SEND_ENC_ELEMENTS.ordinal(), extraInfo,
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
                    byte[] key = cotReceiverOutput.getRb(index);
                    key = crhf.hash(key);
                    byte[] message = encPrg.extendToBytes(key);
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
        info("{}{} Client Step 3/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), unionTime);

        info("{}{} Client end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return union;
    }

    private void generateOvdmPayload() {
        BigInteger exp = ecc.randomZn(secureRandom);
        s = ecc.multiply(y, exp);

        ZpOvdm<ByteBuffer> zpOvdm = ZpOvdmFactory.createInstance(
            envType, zpOvdmType, ecc.getN(), clientElementSize, zpOvdmHashKeys
        );
        BigInteger[] rs = ecc.randomZn(clientElementSize, secureRandom);
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
        BigInteger[] kemZpOvdmStorage = zpOvdm.encode(headerMap);
        BigInteger[] ctZpOvdmStorage = zpOvdm.encode(payloadMap);
        // 打包
        Stream<BigInteger> kemOvdmStream = Arrays.stream(kemZpOvdmStorage);
        kemOvdmStream = parallel ? kemOvdmStream.parallel() : kemOvdmStream;
        kemOvdmPayload = kemOvdmStream
            .map(r -> ecc.multiply(ecc.getG(), r))
            .map(kem -> ecc.encode(kem, compressEncode))
            .collect(Collectors.toList());
        Stream<BigInteger> ctOvdmStream = Arrays.stream(ctZpOvdmStorage);
        ctOvdmStream = parallel ? ctOvdmStream.parallel() : ctOvdmStream;
        ctOvdmPayload = ctOvdmStream
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
                taskId, getPtoDesc().getPtoId(), Zcl22PkePsuPtoDesc.PtoStep.SERVER_SEND_RERAND_KEM.ordinal(), extraInfo,
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
                taskId, getPtoDesc().getPtoId(), Zcl22PkePsuPtoDesc.PtoStep.SERVER_SEND_RERAND_CT.ordinal(), extraInfo,
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
                taskId, getPtoDesc().getPtoId(), Zcl22PkePsuPtoDesc.PtoStep.SERVER_SEND_RERAND_KEM.ordinal(), extraInfo,
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
                taskId, getPtoDesc().getPtoId(), Zcl22PkePsuPtoDesc.PtoStep.SERVER_SEND_RERAND_CT.ordinal(), extraInfo,
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
