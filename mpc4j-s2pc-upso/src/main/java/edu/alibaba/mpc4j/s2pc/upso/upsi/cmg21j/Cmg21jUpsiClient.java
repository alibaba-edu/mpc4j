package edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21j;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64Factory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.crypto.fhe.*;
import edu.alibaba.mpc4j.crypto.fhe.context.EncryptionParameters;
import edu.alibaba.mpc4j.crypto.fhe.context.SchemeType;
import edu.alibaba.mpc4j.crypto.fhe.context.SealContext;
import edu.alibaba.mpc4j.crypto.fhe.modulus.CoeffModulus;
import edu.alibaba.mpc4j.crypto.fhe.serialization.SealSerializable;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.upso.UpsoUtils;
import edu.alibaba.mpc4j.s2pc.upso.upsi.AbstractUpsiClient;
import edu.alibaba.mpc4j.s2pc.upso.upsi.UpsiParams;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21j.Cmg21jUpsiPtoDesc.PtoStep;
import static edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21j.Cmg21jUpsiPtoDesc.getInstance;

/**
 * CMG21J UPSI client.
 *
 * @author Liqiang Peng
 * @date 2024/2/26
 */
public class Cmg21jUpsiClient<T> extends AbstractUpsiClient<T> {

    /**
     * MP-OPRF receiver
     */
    private final MpOprfReceiver mpOprfReceiver;
    /**
     * UPSI params
     */
    public Cmg21jUpsiParams upsiParams;
    /**
     * cuckoo hash bin
     */
    private CuckooHashBin<ByteBuffer> cuckooHashBin;
    /**
     * encryption parameters
     */
    private EncryptionParameters encryptionParams;
    /**
     * relinearization keys
     */
    private SealSerializable<RelinKeys> relinKeys;
    /**
     * SEAL Context
     */
    private SealContext context;
    /**
     * encryptor
     */
    private Encryptor encryptor;
    /**
     * decryptor
     */
    private Decryptor decryptor;
    /**
     * encoder
     */
    private BatchEncoder encoder;
    /**
     * zp64
     */
    private Zp64 zp64;

    public Cmg21jUpsiClient(Rpc clientRpc, Party serverParty, Cmg21jUpsiConfig config) {
        super(getInstance(), clientRpc, serverParty, config);
        mpOprfReceiver = OprfFactory.createMpOprfReceiver(clientRpc, serverParty, config.getMpOprfConfig());
        addSubPto(mpOprfReceiver);
    }

    @Override
    public void init(UpsiParams upsiParams) throws MpcAbortException {
        setInitInput(upsiParams.maxClientElementSize());
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        assert (upsiParams instanceof Cmg21jUpsiParams);
        this.upsiParams = (Cmg21jUpsiParams) upsiParams;
        mpOprfReceiver.init(this.upsiParams.maxClientElementSize());
        zp64 = Zp64Factory.createInstance(envType, this.upsiParams.getPlainModulus());
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void init(int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        upsiParams = Cmg21jUpsiParams.SERVER_1M_CLIENT_MAX_5535;
        mpOprfReceiver.init(upsiParams.maxClientElementSize());
        zp64 = Zp64Factory.createInstance(envType, this.upsiParams.getPlainModulus());
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Set<T> psi(Set<T> clientElementSet) throws MpcAbortException {
        setPtoInput(clientElementSet);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // MP-OPRF
        List<ByteBuffer> oprfOutputs = oprf(clientElementList);
        Map<ByteBuffer, ByteBuffer> oprfMap = IntStream.range(0, clientElementSize)
            .boxed()
            .collect(Collectors.toMap(oprfOutputs::get, i -> clientElementList.get(i), (a, b) -> b));
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 5, oprfTime, "OPRF");

        stopWatch.start();
        // generate cuckoo hash bin
        byte[][] hashKeys = generateCuckooHashBin(oprfOutputs);
        DataPacketHeader hashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        List<byte[]> hashKeyPayload = Arrays.stream(hashKeys).collect(Collectors.toList());
        rpc.send(DataPacket.fromByteArrayList(hashKeyHeader, hashKeyPayload));
        stopWatch.stop();
        long cuckooHashKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 5, cuckooHashKeyTime, "Client generates cuckoo hash keys");

        stopWatch.start();
        genEncryptionParameters();
        List<byte[]> publicKeysPayload = null;
        try {
            publicKeysPayload = generatePublicKeysPayload();
        } catch (IOException e) {
            e.printStackTrace();
        }
        DataPacketHeader publicKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_ENCRYPTION_PARAMS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        assert publicKeysPayload != null;
        rpc.send(DataPacket.fromByteArrayList(publicKeysHeader, publicKeysPayload));
        stopWatch.stop();
        long keyGenTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 5, keyGenTime, "Client generates FHE keys");

        stopWatch.start();
        // generate query
        List<long[][]> encodedQuery = encodeQuery();
        Stream<long[][]> encodeStream = parallel ? encodedQuery.stream().parallel() : encodedQuery.stream();
        List<byte[]> queryPayload = encodeStream
            .map(this::generateQuery)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
        DataPacketHeader queryHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(queryHeader, queryPayload));
        stopWatch.stop();
        long genQueryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 5, genQueryTime, "Client generates query");

        DataPacketHeader responseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> responsePayload = rpc.receive(responseHeader).getPayload();

        stopWatch.start();
        // decode reply
        List<long[]> decodeResponse = decodeResponse(responsePayload);
        Set<ByteBuffer> intersectionSet = recoverPsiResult(decodeResponse, oprfMap, cuckooHashBin);
        Set<T> result = intersectionSet.stream()
            .map(byteBuffer -> byteArrayObjectMap.get(byteBuffer))
            .collect(Collectors.toSet());
        stopWatch.stop();
        long decodeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 5, 5, decodeTime, "Client decodes response");

        logPhaseInfo(PtoState.PTO_END);
        return result;
    }

    /**
     * client decodes response.
     *
     * @param responsePayload server response.
     * @return decoded response.
     */
    public List<long[]> decodeResponse(List<byte[]> responsePayload) {
        Stream<byte[]> responseStream = parallel ? responsePayload.stream().parallel() : responsePayload.stream();
        return responseStream
            .map(i -> {
                try {
                    Ciphertext response = new Ciphertext();
                    response.load(context, i);
                    int noiseBudget = decryptor.invariantNoiseBudget(response);
                    assert noiseBudget > 0 : "noise budget is 0.";
                    Plaintext decrypted = new Plaintext();
                    long[] coeffs = new long[encoder.slotCount()];
                    decryptor.decrypt(response, decrypted);
                    encoder.decode(decrypted, coeffs);
                    return coeffs;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }).collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * client generates public keys payload.
     *
     * @return public keys.
     * @throws IOException if I/O operations failed.
     */
    public List<byte[]> generatePublicKeysPayload() throws IOException {
        List<byte[]> publicKeysPayload = new ArrayList<>();
        publicKeysPayload.add(encryptionParams.save());
        publicKeysPayload.add(relinKeys.save());
        return publicKeysPayload;
    }

    /**
     * client generates no stash cuckoo hash bin.
     *
     * @param items item list.
     * @return hash keys.
     */
    private byte[][] generateCuckooHashBin(List<ByteBuffer> items) {
        boolean success = false;
        byte[][] hashKeys;
        do {
            hashKeys = CommonUtils.generateRandomKeys(upsiParams.getCuckooHashNum(), secureRandom);
            cuckooHashBin = CuckooHashBinFactory.createCuckooHashBin(
                envType, upsiParams.getCuckooHashBinType(), clientElementSize, upsiParams.getBinNum(), hashKeys
            );
            cuckooHashBin.insertItems(items);
            if (cuckooHashBin.itemNumInStash() == 0) {
                success = true;
            }
        } while (!success);
        cuckooHashBin.insertPaddingItems(botElementByteBuffer);
        return hashKeys;
    }

    /**
     * client executes MP-OPRF protocol.
     *
     * @param clientElementArrayList client element array list.
     * @return MP-OPRF output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private List<ByteBuffer> oprf(List<ByteBuffer> clientElementArrayList) throws MpcAbortException {
        byte[][] oprfReceiverInputs = clientElementArrayList.stream()
            .map(ByteBuffer::array)
            .toArray(byte[][]::new);
        OprfReceiverOutput oprfReceiverOutput = mpOprfReceiver.oprf(oprfReceiverInputs);
        IntStream intStream = IntStream.range(0, clientElementArrayList.size());
        intStream = parallel ? intStream.parallel() : intStream;
        return intStream
            .mapToObj(i -> ByteBuffer.wrap(oprfReceiverOutput.getPrf(i)))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * recover intersection set.
     *
     * @param decryptedResponse decrypted response.
     * @param oprfMap           OPRF map.
     * @param cuckooHashBin     cuckoo hash bin.
     * @return intersection set.
     */
    public Set<ByteBuffer> recoverPsiResult(List<long[]> decryptedResponse, Map<ByteBuffer, ByteBuffer> oprfMap,
                                            CuckooHashBin<ByteBuffer> cuckooHashBin) {
        Set<ByteBuffer> intersectionSet = new HashSet<>();
        int partitionCount = decryptedResponse.size() / upsiParams.getCiphertextNum();
        for (int i = 0; i < decryptedResponse.size(); i++) {
            List<Integer> matchedItem = new ArrayList<>();
            for (int j = 0; j < upsiParams.getItemEncodedSlotSize() * upsiParams.getItemPerCiphertext(); j++) {
                if (decryptedResponse.get(i)[j] == 0) {
                    matchedItem.add(j);
                }
            }
            for (int j = 0; j < matchedItem.size() - upsiParams.getItemEncodedSlotSize() + 1; j++) {
                if (matchedItem.get(j) % upsiParams.getItemEncodedSlotSize() == 0) {
                    if (matchedItem.get(j + upsiParams.getItemEncodedSlotSize() - 1) - matchedItem.get(j)
                        == upsiParams.getItemEncodedSlotSize() - 1) {
                        int hashBinIndex = (matchedItem.get(j) / upsiParams.getItemEncodedSlotSize()) +
                            (i / partitionCount) * upsiParams.getItemPerCiphertext();
                        intersectionSet.add(oprfMap.get(cuckooHashBin.getHashBinEntry(hashBinIndex).getItem()));
                        j = j + upsiParams.getItemEncodedSlotSize() - 1;
                    }
                }
            }
        }
        return intersectionSet;
    }

    /**
     * encode query.
     *
     * @return encoded query.
     */
    private List<long[][]> encodeQuery() {
        long[][] items = new long[upsiParams.getCiphertextNum()][upsiParams.getPolyModulusDegree()];
        for (int i = 0; i < upsiParams.getCiphertextNum(); i++) {
            for (int j = 0; j < upsiParams.getItemPerCiphertext(); j++) {
                long[] item = UpsoUtils.getHashBinEntryEncodedArray(
                    cuckooHashBin.getHashBinEntry(i * upsiParams.getItemPerCiphertext() + j), true,
                    upsiParams.getItemEncodedSlotSize(), upsiParams.getPlainModulus()
                );
                System.arraycopy(
                    item, 0, items[i], j * upsiParams.getItemEncodedSlotSize(), upsiParams.getItemEncodedSlotSize()
                );
            }
        }
        return IntStream.range(0, upsiParams.getCiphertextNum())
            .mapToObj(i -> UpsoUtils.computePowers(items[i], zp64, upsiParams.getQueryPowers(), parallel))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * generate encryption parameters.
     */
    private void genEncryptionParameters() {
        encryptionParams = new EncryptionParameters(SchemeType.BFV);
        encryptionParams.setPolyModulusDegree(upsiParams.getPolyModulusDegree());
        encryptionParams.setPlainModulus(upsiParams.getPlainModulus());
        encryptionParams.setCoeffModulus(
            CoeffModulus.create(upsiParams.getPolyModulusDegree(), upsiParams.getCoeffModulusBits())
        );
        context = new SealContext(encryptionParams);
        assert context.isParametersSet() : "SEAL parameters not valid.";
        assert context.firstContextData().qualifiers().isUsingBatching() : "SEAL parameters do not support batching.";
        assert context.usingKeySwitching() : "SEAL parameters do not support key switching.";
        KeyGenerator keyGen = new KeyGenerator(context);
        SecretKey secretKey = keyGen.secretKey();
        relinKeys = keyGen.createRelinKeys();
        encryptor = new Encryptor(context, secretKey);
        decryptor = new Decryptor(context, secretKey);
        encoder = new BatchEncoder(context);
    }

    /**
     * generates query.
     *
     * @param coeffsArray coefficients array.
     * @return encrypted query.
     */
    private List<byte[]> generateQuery(long[][] coeffsArray) {
        Plaintext[] plaintexts = new Plaintext[coeffsArray.length];
        IntStream intStream = IntStream.range(0, coeffsArray.length);
        intStream = parallel ? intStream.parallel() : intStream;
        return intStream.mapToObj(i -> {
            plaintexts[i] = new Plaintext();
            encoder.encode(coeffsArray[i], plaintexts[i]);
            try {
                return encryptor.encryptSymmetric(plaintexts[i]).save();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }).collect(Collectors.toCollection(ArrayList::new));
    }
}