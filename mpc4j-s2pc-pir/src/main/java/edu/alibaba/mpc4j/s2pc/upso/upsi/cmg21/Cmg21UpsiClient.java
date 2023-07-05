package edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64Factory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfReceiver;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfReceiverOutput;
import edu.alibaba.mpc4j.s2pc.upso.upsi.AbstractUpsiClient;
import edu.alibaba.mpc4j.s2pc.upso.upsi.UpsiParams;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21.Cmg21UpsiPtoDesc.*;

/**
 * CMG21 UPSI client.
 *
 * @author Liqiang Peng
 * @date 2022/5/25
 */
public class Cmg21UpsiClient<T> extends AbstractUpsiClient<T> {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * MP-OPRF receiver
     */
    private final MpOprfReceiver mpOprfReceiver;
    /**
     * UPSI params
     */
    public Cmg21UpsiParams params;
    /**
     * cuckoo hash bin
     */
    private CuckooHashBin<ByteBuffer> cuckooHashBin;
    /**
     * secret key
     */
    public byte[] secretKey;
    /**
     * encryption params
     */
    public byte[] encryptionParams;
    /**
     * public key
     */
    public byte[] publicKey;

    public Cmg21UpsiClient(Rpc clientRpc, Party serverParty, Cmg21UpsiConfig config) {
        super(getInstance(), clientRpc, serverParty, config);
        mpOprfReceiver = OprfFactory.createMpOprfReceiver(clientRpc, serverParty, config.getMpOprfConfig());
        addSubPtos(mpOprfReceiver);
    }

    @Override
    public void init(UpsiParams upsiParams) throws MpcAbortException {
        setInitInput(upsiParams.maxClientElementSize());
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        assert (upsiParams instanceof Cmg21UpsiParams);
        params = (Cmg21UpsiParams) upsiParams;
        mpOprfReceiver.init(params.maxClientElementSize());
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
        params = Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_5535;
        mpOprfReceiver.init(params.maxClientElementSize());
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
        List<byte[]> keyPair = Cmg21UpsiNativeUtils.genEncryptionParameters(
            params.getPolyModulusDegree(), params.getPlainModulus(), params.getCoeffModulusBits()
        );
        List<byte[]> publicKeysPayload = generateKeyPairPayload(keyPair);
        DataPacketHeader publicKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_ENCRYPTION_PARAMS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(publicKeysHeader, publicKeysPayload));
        stopWatch.stop();
        long keyGenTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 5, keyGenTime, "Client generates FHE keys");

        stopWatch.start();
        // generate query
        List<long[][]> encodedQuery = encodeQuery(cuckooHashBin);
        Stream<long[][]> encodeStream = parallel ? encodedQuery.stream().parallel() : encodedQuery.stream();
        List<byte[]> queryPayload = encodeStream
            .map(i -> Cmg21UpsiNativeUtils.generateQuery(encryptionParams, publicKey, secretKey, i))
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
            .map(i -> Cmg21UpsiNativeUtils.decodeReply(encryptionParams, secretKey, i))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * client generates key pair.
     *
     * @param keyPair key pair.
     * @return public keys.
     * @throws MpcAbortException the protocol failure aborts.
     */
    public List<byte[]> generateKeyPairPayload(List<byte[]> keyPair) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(keyPair.size() == 4);
        this.encryptionParams = keyPair.get(0);
        this.publicKey = keyPair.get(2);
        this.secretKey = keyPair.get(3);
        return keyPair.subList(0, 2);
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
            hashKeys = CommonUtils.generateRandomKeys(params.getCuckooHashNum(), secureRandom);
            cuckooHashBin = CuckooHashBinFactory.createCuckooHashBin(
                envType, params.getCuckooHashBinType(), clientElementSize, params.getBinNum(), hashKeys
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
        return IntStream.range(0, clientElementArrayList.size())
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
        int ciphertextNum = params.getBinNum() / (params.getPolyModulusDegree() / params.getItemEncodedSlotSize());
        int itemPerCiphertext = params.getPolyModulusDegree() / params.getItemEncodedSlotSize();
        int partitionCount = decryptedResponse.size() / ciphertextNum;
        for (int i = 0; i < decryptedResponse.size(); i++) {
            List<Integer> matchedItem = new ArrayList<>();
            for (int j = 0; j < params.getItemEncodedSlotSize() * itemPerCiphertext; j++) {
                if (decryptedResponse.get(i)[j] == 0) {
                    matchedItem.add(j);
                }
            }
            for (int j = 0; j < matchedItem.size() - params.getItemEncodedSlotSize() + 1; j++) {
                if (matchedItem.get(j) % params.getItemEncodedSlotSize() == 0) {
                    if (matchedItem.get(j + params.getItemEncodedSlotSize() - 1) - matchedItem.get(j)
                        == params.getItemEncodedSlotSize() - 1) {
                        int hashBinIndex = (matchedItem.get(j) / params.getItemEncodedSlotSize()) +
                            (i / partitionCount) * itemPerCiphertext;
                        intersectionSet.add(oprfMap.get(cuckooHashBin.getHashBinEntry(hashBinIndex).getItem()));
                        j = j + params.getItemEncodedSlotSize() - 1;
                    }
                }
            }
        }
        return intersectionSet;
    }

    /**
     * encode query.
     *
     * @param cuckooHashBin cuckoo hash bin.
     * @return encoded query.
     */
    public List<long[][]> encodeQuery(CuckooHashBin<ByteBuffer> cuckooHashBin) {
        int itemPerCiphertext = params.getPolyModulusDegree() / params.getItemEncodedSlotSize();
        int ciphertextNum = params.getBinNum() / itemPerCiphertext;
        long[][] items = new long[ciphertextNum][params.getPolyModulusDegree()];
        for (int i = 0; i < ciphertextNum; i++) {
            for (int j = 0; j < itemPerCiphertext; j++) {
                long[] item = params.getHashBinEntryEncodedArray(
                    cuckooHashBin.getHashBinEntry(i * itemPerCiphertext + j), true
                );
                System.arraycopy(item, 0, items[i], j * params.getItemEncodedSlotSize(), params.getItemEncodedSlotSize());
            }
            for (int j = itemPerCiphertext * params.getItemEncodedSlotSize(); j < params.getPolyModulusDegree(); j++) {
                items[i][j] = 0;
            }
        }
        return IntStream.range(0, ciphertextNum)
            .mapToObj(i -> computePowers(items[i]))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * compute powers.
     *
     * @param base base.
     * @return powers.
     */
    private long[][] computePowers(long[] base) {
        Zp64 zp64 = Zp64Factory.createInstance(envType, (long) params.getPlainModulus());
        int[] exponents = params.getQueryPowers();
        assert exponents[0] == 1;
        long[][] result = new long[exponents.length][base.length];
        result[0] = base;
        IntStream intStream = IntStream.range(1, exponents.length);
        intStream = parallel ? intStream.parallel() : intStream;
        intStream.forEach(i ->
                IntStream.range(0, base.length).forEach(j -> result[i][j] = zp64.pow(base[j], exponents[i]))
        );
        return result;
    }
}