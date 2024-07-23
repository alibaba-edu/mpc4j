package edu.alibaba.mpc4j.s2pc.upso.upsu.zlp24;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.ecc.EccDokvsFactory;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.ecc.SparseEccDokvs;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;
import edu.alibaba.mpc4j.s2pc.pir.IdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.StdIdxPirFactory;
import edu.alibaba.mpc4j.s2pc.upso.upsu.AbstractUpsuSender;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.upso.upsu.zlp24.Zlp24PkeUpsuPtoDesc.PtoStep;
import static edu.alibaba.mpc4j.s2pc.upso.upsu.zlp24.Zlp24PkeUpsuPtoDesc.getInstance;

/**
 * ZLP24 UPSU sender.
 *
 * @author Liqiang Peng
 * @date 2024/3/17
 */
public class Zlp24PkeUpsuSender extends AbstractUpsuSender {

    /**
     * batch index PIR client
     */
    private final IdxPirClient batchIndexPirClient;
    /**
     * core COT sender
     */
    private final CoreCotSender coreCotSender;
    /**
     * compress encode
     */
    private final boolean compressEncode;
    /**
     * ECC-OVDM type
     */
    private final EccDokvsFactory.EccDokvsType eccDokvsType;
    /**
     * ecc
     */
    private final Ecc ecc;
    /**
     * public key
     */
    private ECPoint y;
    /**
     * Sparse ECC-DOKVS
     */
    private SparseEccDokvs<ByteBuffer> eccDokvs;
    /**
     * DOKVS-encoded KEM
     */
    private ECPoint[] kemEccDokvsStorage;
    /**
     * DOKVS-encoded ciphertext
     */
    private ECPoint[] ctEccDokvsStorage;
    /**
     * retrieval item bit length
     */
    private int bitLength;

    public Zlp24PkeUpsuSender(Rpc senderRpc, Party receiverParty, Zlp24PkeUpsuConfig config) {
        super(getInstance(), senderRpc, receiverParty, config);
        batchIndexPirClient = StdIdxPirFactory.createClient(senderRpc, receiverParty, config.getBatchIndexPirConfig());
        addSubPto(batchIndexPirClient);
        coreCotSender = CoreCotFactory.createSender(senderRpc, receiverParty, config.getCoreCotConfig());
        addSubPto(coreCotSender);
        eccDokvsType = config.getEccDokvsType();
        compressEncode = config.isCompressEncode();
        ecc = EccFactory.createInstance(envType);
    }

    @Override
    public void init(int maxSenderElementSize, int receiverElementSize) throws MpcAbortException {
        setInitInput(maxSenderElementSize, receiverElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        byte[] delta = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        coreCotSender.init(delta);
        stopWatch.stop();
        long initCotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 4, initCotTime);

        stopWatch.start();
        int dokvsHashKeyNum = EccDokvsFactory.getHashKeyNum(eccDokvsType);
        byte[][] dokvsHashKeys = CommonUtils.generateRandomKeys(dokvsHashKeyNum, secureRandom);
        eccDokvs = EccDokvsFactory.createSparseInstance(envType, eccDokvsType, ecc, receiverElementSize, dokvsHashKeys);
        DataPacketHeader keysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_DOKVS_KEYS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(keysHeader, Arrays.stream(dokvsHashKeys).collect(Collectors.toList())));
        stopWatch.stop();
        long initDokvsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 4, initDokvsTime);

        stopWatch.start();
        DataPacketHeader pkHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_PK.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> pkPayload = rpc.receive(pkHeader).getPayload();
        MpcAbortPreconditions.checkArgument(pkPayload.size() == 1);
        if (y != null) {
            ecc.destroyPrecompute(y);
        }
        y = ecc.decode(pkPayload.remove(0));
        ecc.precompute(ecc.getG());
        ecc.precompute(y);
        stopWatch.stop();
        long precomputeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 4, precomputeTime);

        stopWatch.start();
        bitLength = ecc.encode(ecc.getG(), compressEncode).length * Byte.SIZE;
        batchIndexPirClient.init(eccDokvs.sparsePositionRange(), bitLength * 2, dokvsHashKeyNum * maxSenderElementSize);
        stopWatch.stop();
        long initBatchIndexPirTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 4, 4, initBatchIndexPirTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void psu(Set<ByteBuffer> senderElementSet, int elementByteLength)
        throws MpcAbortException {
        setPtoInput(senderElementSet, elementByteLength);
        logPhaseInfo(PtoState.PTO_BEGIN);

        // receive OKVS dense part
        DataPacketHeader denseOkvsHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.RECEIVER_SEND_DENSE_OKVS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> denseOkvsPayload = rpc.receive(denseOkvsHeader).getPayload();
        MpcAbortPreconditions.checkArgument(eccDokvs.densePositionRange() == denseOkvsPayload.size());

        stopWatch.start();
        List<Integer> indices = generateRetrievalIndexList();
        byte[][] okvsSparsePayload = batchIndexPirClient.pir(indices.stream().mapToInt(integer -> integer).toArray());
        MpcAbortPreconditions.checkArgument(indices.size() == okvsSparsePayload.length);
        stopWatch.stop();
        long batchIndexPirTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 4, batchIndexPirTime, "sender executes PIR");

        // recover okvs storage
        stopWatch.start();
        generateOkvsStorage(denseOkvsPayload, okvsSparsePayload, indices);
        stopWatch.stop();
        long generateOkvsStorageTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, generateOkvsStorageTime, "sender generates OKVS storage");

        // re-rand
        stopWatch.start();
        BigInteger[] rs = IntStream.range(0, senderElementSize)
            .mapToObj(index -> ecc.randomZn(secureRandom))
            .toArray(BigInteger[]::new);
        List<byte[]> reRandKemPayload = generateReRandKemPayload(rs);
        DataPacketHeader reRandKemHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_RERAND_KEM.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(reRandKemHeader, reRandKemPayload));
        List<byte[]> reRandCtPayload = generateReRandCtPayload(rs);
        DataPacketHeader reRandCtHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_RERAND_CT.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(reRandCtHeader, reRandCtPayload));
        stopWatch.stop();
        long generateReRandCiphertextsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 4, generateReRandCiphertextsTime, "sender generates re-rand ciphertexts");

        stopWatch.start();
        CotSenderOutput cotSenderOutput = coreCotSender.send(senderElementSize);
        Prg encPrg = PrgFactory.createInstance(envType, elementByteLength);
        IntStream encIntStream = IntStream.range(0, senderElementSize);
        encIntStream = parallel ? encIntStream.parallel() : encIntStream;
        List<byte[]> encPayload = encIntStream
            .mapToObj(index -> {
                // do not need CRHF since we call prg
                byte[] ciphertext = encPrg.extendToBytes(cotSenderOutput.getR0(index));
                BytesUtils.xori(ciphertext, senderElementList.get(index).array());
                return ciphertext;
            })
            .collect(Collectors.toList());
        DataPacketHeader encHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_ENC_ELEMENTS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(encHeader, encPayload));
        stopWatch.stop();
        long encTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 4, encTime, "sender executes COT");

        logPhaseInfo(PtoState.PTO_END);
    }

    /**
     * recover okvs storage.
     *
     * @param okvsDensePayload   okvs dense payload.
     * @param okvsSparsePayload  okvs sparse payload.
     * @param retrievalIndexList retrieval index list.
     */
    private void generateOkvsStorage(List<byte[]> okvsDensePayload, byte[][] okvsSparsePayload,
                                     List<Integer> retrievalIndexList) {
        kemEccDokvsStorage = new ECPoint[eccDokvs.sparsePositionRange() + eccDokvs.densePositionRange()];
        ctEccDokvsStorage = new ECPoint[eccDokvs.sparsePositionRange() + eccDokvs.densePositionRange()];
        int byteLength = CommonUtils.getByteLength(bitLength);
        byte[] item = new byte[byteLength];
        IntStream.range(0, retrievalIndexList.size()).forEach(i -> {
            byte[] sparseItem = okvsSparsePayload[i];
            System.arraycopy(sparseItem, 0, item, 0, byteLength);
            kemEccDokvsStorage[retrievalIndexList.get(i)] = ecc.decode(item);
            System.arraycopy(sparseItem, byteLength, item, 0, byteLength);
            ctEccDokvsStorage[retrievalIndexList.get(i)] = ecc.decode(item);
        });
        IntStream.range(0, eccDokvs.densePositionRange()).forEach(i -> {
            byte[] denseItem = okvsDensePayload.get(i);
            System.arraycopy(denseItem, 0, item, 0, byteLength);
            kemEccDokvsStorage[i + eccDokvs.sparsePositionRange()] = ecc.decode(item);
            System.arraycopy(denseItem, byteLength, item, 0, byteLength);
            ctEccDokvsStorage[i + eccDokvs.sparsePositionRange()] = ecc.decode(item);
        });
    }

    /**
     * generate sparse okvs retrieval index list.
     *
     * @return sparse okvs retrieval index list.
     */
    private List<Integer> generateRetrievalIndexList() {
        return senderElementList.stream()
            .map(eccDokvs::sparsePositions)
            .flatMapToInt(Arrays::stream)
            .distinct()
            .boxed()
            .collect(Collectors.toList());
    }

    /**
     * generate re-rand kem payload.
     *
     * @param rs random elements.
     * @return re-rand kem payload.
     */
    private List<byte[]> generateReRandKemPayload(BigInteger[] rs) {
        IntStream intStream = IntStream.range(0, senderElementSize);
        intStream = parallel ? intStream.parallel() : intStream;
        return intStream
            .mapToObj(i -> {
                ECPoint gr = ecc.multiply(ecc.getG(), rs[i]);
                return eccDokvs.decode(kemEccDokvsStorage, senderElementList.get(i)).add(gr);
            })
            .map(kem -> ecc.encode(kem, compressEncode))
            .collect(Collectors.toList());
    }

    /**
     * generate re-rand ct payload.
     *
     * @param rs random elements.
     * @return re-rand kem payload.
     */
    private List<byte[]> generateReRandCtPayload(BigInteger[] rs) {
        IntStream intStream = IntStream.range(0, senderElementSize);
        intStream = parallel ? intStream.parallel() : intStream;
        return intStream
            .mapToObj(i -> {
                ECPoint yr = ecc.multiply(y, rs[i]);
                return eccDokvs.decode(ctEccDokvsStorage, senderElementList.get(i)).add(yr);
            })
            .map(ct -> ecc.encode(ct, compressEncode))
            .collect(Collectors.toList());
    }
}
