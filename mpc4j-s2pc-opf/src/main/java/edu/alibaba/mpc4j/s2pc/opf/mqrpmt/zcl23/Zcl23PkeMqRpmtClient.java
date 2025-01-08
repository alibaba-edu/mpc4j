package edu.alibaba.mpc4j.s2pc.opf.mqrpmt.zcl23;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.zp.ZpDokvs;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.zp.ZpDokvsFactory;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.zp.ZpDokvsFactory.ZpDokvsType;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.AbstractMqRpmtClient;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.zcl23.Zcl23PkeMqRpmtPtoDesc.PtoStep;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * ZCL23 PKE-mqRPMT client.
 *
 * @author Weiran Liu
 * @date 2024/4/28
 */
public class Zcl23PkeMqRpmtClient extends AbstractMqRpmtClient {
    /**
     * Zp-DOKVS type
     */
    private final ZpDokvsType zpDokvsType;
    /**
     * compress encode
     */
    private final boolean compressEncode;
    /**
     * pipeline size
     */
    private final int pipeSize;
    /**
     * ECC
     */
    private final Ecc ecc;
    /**
     * DOKVS hash keys
     */
    private byte[][] dokvsHashKeys;
    /**
     * indicate ECC point
     */
    private ECPoint s;
    /**
     * secret key
     */
    private BigInteger x;
    /**
     * public key
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

    public Zcl23PkeMqRpmtClient(Rpc clientRpc, Party serverParty, Zcl23PkeMqRpmtConfig config) {
        super(Zcl23PkeMqRpmtPtoDesc.getInstance(), clientRpc, serverParty, config);
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
        // compute secret key and public key
        x = ecc.randomZn(secureRandom);
        y = ecc.multiply(ecc.getG(), x);
        List<byte[]> pkPayload = new LinkedList<>();
        pkPayload.add(ecc.encode(y, compressEncode));
        sendOtherPartyPayload(PtoStep.CLIENT_SEND_PK.ordinal(), pkPayload);
        stopWatch.stop();
        long pkTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, pkTime);

        List<byte[]> keysPayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_DOKVS_KEYS.ordinal());

        stopWatch.start();
        int dokvsHashKeyNum = ZpDokvsFactory.getHashKeyNum(zpDokvsType);
        MpcAbortPreconditions.checkArgument(keysPayload.size() == dokvsHashKeyNum);
        dokvsHashKeys = keysPayload.toArray(new byte[0][]);
        // fix-point multiplication pre-computation
        ecc.precompute(ecc.getG());
        ecc.precompute(y);
        stopWatch.stop();
        long keyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, keyTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public boolean[] mqRpmt(Set<ByteBuffer> clientElementSet, int serverElementSize) throws MpcAbortException {
        setPtoInput(clientElementSet, serverElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        generateDokvsPayload();
        // DOKVS KEM
        sendOtherPartyEqualSizePayload(PtoStep.CLIENT_SEND_DOKVS_KEM.ordinal(), kemDokvsPayload);
        // DOKVS ciphertext
        sendOtherPartyEqualSizePayload(PtoStep.CLIENT_SEND_DOKVS_CT.ordinal(), ctDokvsPayload);
        stopWatch.stop();
        long dokvsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, dokvsTime, "Client generates DOKVS");

        stopWatch.start();
        boolean[] containVector = pipelinePeqt();
        stopWatch.stop();
        long peqtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, peqtTime, "Client runs re-randomized PEQT");

        logPhaseInfo(PtoState.PTO_END);
        return containVector;
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
        // package
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

    private boolean[] pipelinePeqt() throws MpcAbortException {
        boolean[] peqtArray = new boolean[serverElementSize];
        // Pipeline execution
        int pipelineTime = serverElementSize / pipeSize;
        int round;
        for (round = 0; round < pipelineTime; round++) {
            // receive KEM
            List<byte[]> reRandKemPayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_RERAND_KEM.ordinal());
            MpcAbortPreconditions.checkArgument(reRandKemPayload.size() == pipeSize);
            // decode KEM
            Stream<byte[]> reRandKemStream = reRandKemPayload.stream();
            reRandKemStream = parallel ? reRandKemStream.parallel() : reRandKemStream;
            ECPoint[] reRandKemArray = reRandKemStream
                .map(ecc::decode)
                .toArray(ECPoint[]::new);
            // receive ciphertext
            List<byte[]> reRandCtPayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_RERAND_CT.ordinal());
            MpcAbortPreconditions.checkArgument(reRandCtPayload.size() == pipeSize);
            // decode ciphertext
            Stream<byte[]> reRandCtStream = reRandCtPayload.stream();
            reRandCtStream = parallel ? reRandCtStream.parallel() : reRandCtStream;
            ECPoint[] reRandCtArray = reRandCtStream
                .map(ecc::decode)
                .toArray(ECPoint[]::new);
            // decrypt and compare
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
            // receive KEM
            List<byte[]> reRandKemPayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_RERAND_KEM.ordinal());
            MpcAbortPreconditions.checkArgument(reRandKemPayload.size() == remain);
            // decode KEM
            Stream<byte[]> reRandKemStream = reRandKemPayload.stream();
            reRandKemStream = parallel ? reRandKemStream.parallel() : reRandKemStream;
            ECPoint[] rerandKemArray = reRandKemStream
                .map(ecc::decode)
                .toArray(ECPoint[]::new);
            // receive ciphertext
            List<byte[]> reRandCtPayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_RERAND_CT.ordinal());
            MpcAbortPreconditions.checkArgument(reRandCtPayload.size() == remain);
            // decode ciphertext
            Stream<byte[]> reRandCtStream = reRandCtPayload.stream();
            reRandCtStream = parallel ? reRandCtStream.parallel() : reRandCtStream;
            ECPoint[] reRandCtArray = reRandCtStream
                .map(ecc::decode)
                .toArray(ECPoint[]::new);
            // decrypt and compare
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
        return peqtArray;
    }
}
