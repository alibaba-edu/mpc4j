package edu.alibaba.mpc4j.s2pc.upso.upsu.zlp24;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.zp.SparseZpDokvs;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.zp.ZpDokvsFactory;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.BatchIndexPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.BatchIndexPirServer;
import edu.alibaba.mpc4j.s2pc.upso.upsu.AbstractUpsuReceiver;
import edu.alibaba.mpc4j.s2pc.upso.upsu.UpsuReceiverOutput;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.upso.upsu.zlp24.Zlp24PkeUpsuPtoDesc.PtoStep;
import static edu.alibaba.mpc4j.s2pc.upso.upsu.zlp24.Zlp24PkeUpsuPtoDesc.getInstance;

/**
 * ZLP24 UPSU receiver.
 *
 * @author Liqiang Peng
 * @date 2024/3/18
 */
public class Zlp24PkeUpsuReceiver extends AbstractUpsuReceiver {

    /**
     * batch index PIR server
     */
    private final BatchIndexPirServer batchIndexPirServer;
    /**
     * core COT receiver
     */
    private final CoreCotReceiver coreCotReceiver;
    /**
     * Zp-DOKVS type
     */
    private final ZpDokvsFactory.ZpDokvsType zpDokvsType;
    /**
     * compress encode
     */
    private final boolean compressEncode;
    /**
     * ecc
     */
    private final Ecc ecc;
    /**
     * secret key
     */
    private BigInteger x;
    /**
     * public key
     */
    private ECPoint y;
    /**
     * index point
     */
    private ECPoint s;
    /**
     * DOKVS-encoded KEM
     */
    private BigInteger[] kemZpDokvsStorage;
    /**
     * DOKVS-encoded ciphertext
     */
    private BigInteger[] ctZpDokvsStorage;
    /**
     * sparse Zp dokvs
     */
    private SparseZpDokvs<ByteBuffer> zpDokvs;
    /**
     * Zp OKVS dense payload
     */
    private List<byte[]> okvsDensePayload;

    public Zlp24PkeUpsuReceiver(Rpc receiverRpc, Party senderParty, Zlp24PkeUpsuConfig config) {
        super(getInstance(), receiverRpc, senderParty, config);
        batchIndexPirServer = BatchIndexPirFactory.createServer(receiverRpc, senderParty, config.getBatchIndexPirConfig());
        addSubPto(batchIndexPirServer);
        coreCotReceiver = CoreCotFactory.createReceiver(receiverRpc, senderParty, config.getCoreCotConfig());
        addSubPto(coreCotReceiver);
        zpDokvsType = config.getZpDokvsType();
        compressEncode = config.isCompressEncode();
        ecc = EccFactory.createInstance(envType);
    }

    @Override
    public void init(Set<ByteBuffer> receiverElementSet, int maxSenderElementSize, int elementByteLength)
        throws MpcAbortException {
        setInitInput(receiverElementSet, maxSenderElementSize, elementByteLength);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        coreCotReceiver.init(maxSenderElementSize);
        stopWatch.stop();
        long initCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 4, initCotTime);

        DataPacketHeader keysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_DOKVS_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> keysPayload = rpc.receive(keysHeader).getPayload();
        int dokvsHashKeyNum = ZpDokvsFactory.getHashKeyNum(zpDokvsType);
        MpcAbortPreconditions.checkArgument(keysPayload.size() == dokvsHashKeyNum);
        byte[][] dokvsHashKeys = keysPayload.toArray(new byte[0][]);

        stopWatch.start();
        zpDokvs = ZpDokvsFactory.createSparseInstance(envType, zpDokvsType, ecc.getN(), receiverElementSize, dokvsHashKeys);
        stopWatch.stop();
        long initDokvsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 4, initDokvsTime);

        stopWatch.start();
        x = ecc.randomZn(secureRandom);
        y = ecc.multiply(ecc.getG(), x);
        List<byte[]> pkPayload = new LinkedList<>();
        pkPayload.add(ecc.encode(y, compressEncode));
        DataPacketHeader pkHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_PK.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(pkHeader, pkPayload));
        ecc.precompute(ecc.getG());
        ecc.precompute(y);
        stopWatch.stop();
        long precomputeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 4, precomputeTime);

        stopWatch.start();
        int bitLength = ecc.encode(ecc.getG(), compressEncode).length * Byte.SIZE;
        NaiveDatabase database = generateOkvsSparsePayload(bitLength);
        okvsDensePayload = generateOkvsDensePayload(bitLength);
        batchIndexPirServer.init(database, dokvsHashKeyNum * maxSenderElementSize);
        stopWatch.stop();
        long initBatchIndexPirTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 4, 4, initBatchIndexPirTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public UpsuReceiverOutput psu(int senderElementSize)
        throws MpcAbortException {
        setPtoInput(senderElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        // send OKVS dense part
        DataPacketHeader denseOkvsHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.RECEIVER_SEND_DENSE_OKVS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(denseOkvsHeader, okvsDensePayload));

        stopWatch.start();
        batchIndexPirServer.pir();
        stopWatch.stop();
        long batchIndexPirTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, batchIndexPirTime, "receiver executes PIR");

        // receive re-rand payload
        DataPacketHeader reRandKemHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_RERAND_KEM.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> reRandKemPayload = rpc.receive(reRandKemHeader).getPayload();
        MpcAbortPreconditions.checkArgument(reRandKemPayload.size() == senderElementSize);
        DataPacketHeader reRandCtHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_RERAND_CT.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> reRandCtPayload = rpc.receive(reRandCtHeader).getPayload();
        MpcAbortPreconditions.checkArgument(reRandCtPayload.size() == senderElementSize);

        stopWatch.start();
        boolean[] peqtArray = decryptReRand(reRandKemPayload, reRandCtPayload);
        int intersectionSetSize = (int) IntStream.range(0, peqtArray.length).filter(i -> peqtArray[i]).count();
        stopWatch.stop();
        long decryptReRandTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, decryptReRandTime, "receiver decrypts re-rand ciphertexts");

        stopWatch.start();
        CotReceiverOutput cotReceiverOutput = coreCotReceiver.receive(peqtArray);
        DataPacketHeader encHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_ENC_ELEMENTS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> encPayload = rpc.receive(encHeader).getPayload();
        MpcAbortPreconditions.checkArgument(encPayload.size() == senderElementSize);
        ArrayList<byte[]> encArrayList = new ArrayList<>(encPayload);
        Prg encPrg = PrgFactory.createInstance(envType, elementByteLength);
        IntStream decIntStream = IntStream.range(0, senderElementSize);
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
        union.remove(botElementByteBuffer);
        union.addAll(receiverElementList);
        stopWatch.stop();
        long unionTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, unionTime, "receiver handles union");

        logPhaseInfo(PtoState.PTO_END);
        return new UpsuReceiverOutput(union, intersectionSetSize);
    }

    /**
     * generate Zp OKVS sparse payload.
     *
     * @param bitLength bit length.
     * @return naive database.
     */
    private NaiveDatabase generateOkvsSparsePayload(int bitLength) {
        BigInteger exp = ecc.randomZn(secureRandom);
        s = ecc.multiply(y, exp);
        BigInteger[] rs = IntStream.range(0, receiverElementSize)
            .mapToObj(index -> ecc.randomZn(secureRandom))
            .toArray(BigInteger[]::new);
        Map<ByteBuffer, BigInteger> headerMap = IntStream.range(0, receiverElementSize)
            .boxed()
            .collect(Collectors.toMap(
                index -> receiverElementList.get(index),
                index -> rs[index]
            ));
        IntStream intStream = IntStream.range(0, receiverElementSize);
        intStream = parallel ? intStream.parallel() : intStream;
        Map<ByteBuffer, BigInteger> payloadMap = intStream
            .boxed()
            .collect(Collectors.toMap(
                index -> receiverElementList.get(index),
                index -> rs[index].add(exp).mod(ecc.getN())
            ));
        kemZpDokvsStorage = zpDokvs.encode(headerMap, true);
        ctZpDokvsStorage = zpDokvs.encode(payloadMap, true);
        int sparsePositionRange = zpDokvs.sparsePositionRange();
        byte[][] database = new byte[sparsePositionRange][];
        intStream = IntStream.range(0, sparsePositionRange);
        intStream = parallel ? intStream.parallel() : intStream;
        intStream.forEach(i -> {
            ECPoint kem = ecc.multiply(ecc.getG(), kemZpDokvsStorage[i]);
            ECPoint ct = ecc.multiply(y, ctZpDokvsStorage[i]);
            ByteBuffer byteBuffer = ByteBuffer.allocate(2 * CommonUtils.getByteLength(bitLength))
                .put(ecc.encode(kem, compressEncode))
                .put(ecc.encode(ct, compressEncode));
            database[i] = byteBuffer.array();
        });
        return NaiveDatabase.create(2 * bitLength, database);
    }

    /**
     * generate Zp OKVS dense payload.
     *
     * @param bitLength bit length.
     * @return Zp OKVS dense payload.
     */
    private List<byte[]> generateOkvsDensePayload(int bitLength) {
        int sparsePositionRange = zpDokvs.sparsePositionRange();
        int densePositionRange = zpDokvs.densePositionRange();
        IntStream intStream = IntStream.range(0, densePositionRange);
        intStream = parallel ? intStream.parallel() : intStream;
        return intStream.mapToObj(i -> {
            ECPoint kem = ecc.multiply(ecc.getG(), kemZpDokvsStorage[i + sparsePositionRange]);
            ECPoint ct = ecc.multiply(y, ctZpDokvsStorage[i + sparsePositionRange]);
            ByteBuffer byteBuffer = ByteBuffer.allocate(2 * CommonUtils.getByteLength(bitLength))
                .put(ecc.encode(kem, compressEncode))
                .put(ecc.encode(ct, compressEncode));
            return byteBuffer.array();
        }).collect(Collectors.toList());
    }

    /**
     * decrypt re-rand ciphertexts.
     *
     * @param reRandKemPayload re-rand kem payload.
     * @param reRandCtPayload  re-rand ct payload.
     * @return peqt array.
     */
    private boolean[] decryptReRand(List<byte[]> reRandKemPayload, List<byte[]> reRandCtPayload) {
        boolean[] peqtArray = new boolean[senderElementSize];
        IntStream intStream = IntStream.range(0, senderElementSize);
        intStream = parallel ? intStream.parallel() : intStream;
        intStream.forEach(i -> {
            ECPoint kem = ecc.decode(reRandKemPayload.get(i));
            ECPoint ct = ecc.decode(reRandCtPayload.get(i));
            ECPoint yr = ecc.multiply(kem, x);
            ECPoint sStar = ct.subtract(yr);
            peqtArray[i] = s.equals(sStar);
        });
        return peqtArray;
    }
}