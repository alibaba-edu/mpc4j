package edu.alibaba.mpc4j.s2pc.opf.mqrpmt.zcl23;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.ecc.EccDokvs;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.ecc.EccDokvsFactory;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.ecc.EccDokvsFactory.EccDokvsType;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.AbstractMqRpmtServer;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.zcl23.Zcl23PkeMqRpmtPtoDesc.PtoStep;
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
 * ZCL23 PKE-mqRPMT server.
 *
 * @author Weiran Liu
 * @date 2024/4/28
 */
public class Zcl23PkeMqRpmtServer extends AbstractMqRpmtServer {
    /**
     * ECC-DOKVS type
     */
    private final EccDokvsType eccDokvsType;
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
     * byte of encoded result of ecc
     */
    private final int eccEncodeByteLen;
    /**
     * DOKVS hash keys
     */
    private byte[][] dokvsHashKeys;
    /**
     * public key
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

    public Zcl23PkeMqRpmtServer(Rpc serverRpc, Party clientParty, Zcl23PkeMqRpmtConfig config) {
        super(Zcl23PkeMqRpmtPtoDesc.getInstance(), serverRpc, clientParty, config);
        eccDokvsType = config.getEccDokvsType();
        compressEncode = config.getCompressEncode();
        pipeSize = config.getPipeSize();
        ecc = EccFactory.createInstance(envType);
        eccEncodeByteLen = ecc.getEncodeByteLen(compressEncode);
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

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
        sendOtherPartyPayload(PtoStep.SERVER_SEND_DOKVS_KEYS.ordinal(), keysPayload);
        stopWatch.stop();
        long keyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, keyTime);

        List<byte[]> pkPayload = receiveOtherPartyPayload(PtoStep.CLIENT_SEND_PK.ordinal());

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(pkPayload.size() == 1);
        if (y != null) {
            ecc.destroyPrecompute(y);
        }
        y = ecc.decode(pkPayload.remove(0));
        // fix-point multiplication pre-computation
        ecc.precompute(ecc.getG());
        ecc.precompute(y);
        stopWatch.stop();
        long pkTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, pkTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public ByteBuffer[] mqRpmt(Set<ByteBuffer> serverElementSet, int clientElementSize) throws MpcAbortException {
        setPtoInput(serverElementSet, clientElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        // DOKVS KEM
        List<byte[]> kemDokvsPayload = receiveOtherPartyEqualSizePayload(PtoStep.CLIENT_SEND_DOKVS_KEM.ordinal(), clientElementSize, eccEncodeByteLen);

        // DOKVS ciphertext
        List<byte[]> ctDokvsPayload = receiveOtherPartyEqualSizePayload(PtoStep.CLIENT_SEND_DOKVS_CT.ordinal(), clientElementSize, eccEncodeByteLen);

        stopWatch.start();
        handleDokvsPayload(kemDokvsPayload, ctDokvsPayload);
        stopWatch.stop();
        long dokvsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, dokvsTime, "Server handles DOKVS");

        stopWatch.start();
        pipelineReRand();
        stopWatch.stop();
        long reRandTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, reRandTime, "Server runs re-randomized PEQT");

        logPhaseInfo(PtoState.PTO_END);
        return serverElementArrayList.toArray(new ByteBuffer[0]);
    }

    private void handleDokvsPayload(List<byte[]> kemDokvsPayload, List<byte[]> ctDokvsPayload) throws MpcAbortException {
        int eccDokvsM = EccDokvsFactory.getM(eccDokvsType, clientElementSize);
        MpcAbortPreconditions.checkArgument(kemDokvsPayload.size() == eccDokvsM);
        MpcAbortPreconditions.checkArgument(ctDokvsPayload.size() == eccDokvsM);
        kemDokvsStorage = kemDokvsPayload.stream()
            .map(ecc::decode)
            .toArray(ECPoint[]::new);
        ctDokvsStorage = ctDokvsPayload.stream()
            .map(ecc::decode)
            .toArray(ECPoint[]::new);
        eccDokvs = EccDokvsFactory.createInstance(envType, eccDokvsType, ecc, clientElementSize, dokvsHashKeys);
    }

    private void pipelineReRand() {
        // generate randomness
        BigInteger[] rs = IntStream.range(0, serverElementSize)
            .mapToObj(index -> ecc.randomZn(secureRandom))
            .toArray(BigInteger[]::new);
        // Pipeline execution
        int pipelineTime = serverElementSize / pipeSize;
        int round;
        for (round = 0; round < pipelineTime; round++) {
            int offset = round * pipeSize;
            // compute KEM
            IntStream kemIntStream = IntStream.range(0, pipeSize);
            kemIntStream = parallel ? kemIntStream.parallel() : kemIntStream;
            List<byte[]> reRandKemPayload = kemIntStream
                .mapToObj(index -> {
                    ECPoint gr = ecc.multiply(ecc.getG(), rs[offset + index]);
                    return eccDokvs.decode(kemDokvsStorage, serverElementArrayList.get(offset + index)).add(gr);
                })
                .map(kem -> ecc.encode(kem, compressEncode))
                .collect(Collectors.toList());
            // send KEM
            sendOtherPartyPayload(PtoStep.SERVER_SEND_RERAND_KEM.ordinal(), reRandKemPayload);
            // compute ciphertext
            IntStream ctIntStream = IntStream.range(0, pipeSize);
            ctIntStream = parallel ? ctIntStream.parallel() : ctIntStream;
            List<byte[]> reRandCtPayload = ctIntStream
                .mapToObj(index -> {
                    ECPoint yr = ecc.multiply(y, rs[offset + index]);
                    return eccDokvs.decode(ctDokvsStorage, serverElementArrayList.get(offset + index)).add(yr);
                })
                .map(ct -> ecc.encode(ct, compressEncode))
                .collect(Collectors.toList());
            // send ciphertext
            sendOtherPartyPayload(PtoStep.SERVER_SEND_RERAND_CT.ordinal(), reRandCtPayload);
            extraInfo++;
        }
        int remain = serverElementSize - round * pipeSize;
        if (remain > 0) {
            int offset = round * pipeSize;
            // compute KEM
            IntStream kemIntStream = IntStream.range(0, remain);
            kemIntStream = parallel ? kemIntStream.parallel() : kemIntStream;
            List<byte[]> reRandKemPayload = kemIntStream
                .mapToObj(index -> {
                    ECPoint gr = ecc.multiply(ecc.getG(), rs[offset + index]);
                    return eccDokvs.decode(kemDokvsStorage, serverElementArrayList.get(offset + index)).add(gr);
                })
                .map(kem -> ecc.encode(kem, compressEncode))
                .collect(Collectors.toList());
            // send KEM
            sendOtherPartyPayload(PtoStep.SERVER_SEND_RERAND_KEM.ordinal(), reRandKemPayload);
            // compute ciphertext
            IntStream ctIntStream = IntStream.range(0, remain);
            ctIntStream = parallel ? ctIntStream.parallel() : ctIntStream;
            List<byte[]> reRandCtPayload = ctIntStream
                .mapToObj(index -> {
                    ECPoint yr = ecc.multiply(y, rs[offset + index]);
                    return eccDokvs.decode(ctDokvsStorage, serverElementArrayList.get(offset + index)).add(yr);
                })
                .map(ct -> ecc.encode(ct, compressEncode))
                .collect(Collectors.toList());
            // send ciphertext
            sendOtherPartyPayload(PtoStep.SERVER_SEND_RERAND_CT.ordinal(), reRandCtPayload);
            extraInfo++;
        }
    }
}
