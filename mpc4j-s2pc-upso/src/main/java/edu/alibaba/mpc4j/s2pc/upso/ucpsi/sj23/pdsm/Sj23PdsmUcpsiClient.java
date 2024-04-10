package edu.alibaba.mpc4j.s2pc.upso.ucpsi.sj23.pdsm;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64Factory;
import edu.alibaba.mpc4j.common.tool.hashbin.MaxBinSizeUtils;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.NoStashCuckooHashBin;
import edu.alibaba.mpc4j.common.tool.polynomial.zp64.Zp64Poly;
import edu.alibaba.mpc4j.common.tool.polynomial.zp64.Zp64PolyFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.opf.psm.pdsm.PdsmFactory;
import edu.alibaba.mpc4j.s2pc.opf.psm.pdsm.PdsmReceiver;
import edu.alibaba.mpc4j.s2pc.opf.psm.pdsm.PdsmSender;
import edu.alibaba.mpc4j.s2pc.upso.UpsoUtils;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.AbstractUcpsiClient;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.UcpsiClientOutput;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static edu.alibaba.mpc4j.s2pc.upso.ucpsi.sj23.pdsm.Sj23PdsmUcpsiPtoDesc.PtoStep;
import static edu.alibaba.mpc4j.s2pc.upso.ucpsi.sj23.pdsm.Sj23PdsmUcpsiPtoDesc.getInstance;

/**
 * SJ23 unbalanced circuit PSI client.
 *
 * @author Liqiang Peng
 * @date 2023/7/21
 */
public class Sj23PdsmUcpsiClient<T> extends AbstractUcpsiClient<T> {
    /**
     * private set membership receiver
     */
    private final PdsmReceiver pdsmReceiver;
    /**
     * private set membership sender
     */
    private final PdsmSender pdsmSender;
    /**
     * cuckoo hash num
     */
    private final int hashNum;
    /**
     * cuckoo hash bin type
     */
    private final CuckooHashBinType cuckooHashBinType;
    /**
     * hash keys
     */
    private byte[][] hashKeys;
    /**
     * SJ23 UCPSI params
     */
    private Sj23PdsmUcpsiParams params;
    /**
     * cuckoo hash bin
     */
    private NoStashCuckooHashBin<byte[]> cuckooHashBin;
    /**
     * alpha
     */
    private int alpha;
    /**
     * public key
     */
    private byte[] clientPublicKey;
    /**
     * secret key
     */
    private byte[] clientSecretKey;
    /**
     * public key
     */
    private byte[] serverPublicKey;
    /**
     * relin keys
     */
    private byte[] serverRelinKeys;
    /**
     * plaintext list
     */
    private List<long[][]> plaintextList;
    /**
     * maks coeffs
     */
    private List<long[]> maskCoeffList;
    /**
     * rpc count
     */
    private long rpcCount;
    /**
     * mask byte
     */
    private byte[][] mask;
    /**
     * is HE receiver
     */
    private boolean isHeReceiver;
    /**
     * zp64
     */
    private Zp64 zp64;
    /**
     * hash byte length
     */
    private int byteL;
    /**
     * shift mask
     */
    private BigInteger shiftMask;

    public Sj23PdsmUcpsiClient(Rpc clientRpc, Party serverParty, Sj23PdsmUcpsiConfig config) {
        super(getInstance(), clientRpc, serverParty, config);
        pdsmSender = PdsmFactory.createSender(clientRpc, serverParty, config.getPsmConfig());
        addSubPto(pdsmSender);
        pdsmReceiver = PdsmFactory.createReceiver(clientRpc, serverParty, config.getPsmConfig());
        addSubPto(pdsmReceiver);
        cuckooHashBinType = CuckooHashBinType.NO_STASH_PSZ18_3_HASH;
        hashNum = CuckooHashBinFactory.getHashNum(cuckooHashBinType);
        rpcCount = 0;
        isHeReceiver = false;
    }

    @Override
    public void init(int maxClientElementSize, int serverElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, serverElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);
        params = Sj23PdsmUcpsiParams.getParams(serverElementSize, maxClientElementSize);

        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> cuckooHashKeysPayload = rpc.receive(cuckooHashKeyHeader).getPayload();
        MpcAbortPreconditions.checkArgument(cuckooHashKeysPayload.size() == hashNum);
        hashKeys = cuckooHashKeysPayload.toArray(new byte[0][]);

        stopWatch.start();
        // generate public keys
        List<byte[]> clientPublicKeysPayload = clientKeyGen();
        DataPacketHeader clientPublicKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientPublicKeysHeader, clientPublicKeysPayload));
        stopWatch.stop();
        long keyGenTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, keyGenTime);

        DataPacketHeader serverPublicKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_PUBLIC_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> serverPublicKeysPayload = rpc.receive(serverPublicKeysHeader).getPayload();

        stopWatch.start();
        // handle server public keys
        handleServerPublicKeyPayload(serverPublicKeysPayload);
        // initialize pmt
        int approxMaxBinSize = MaxBinSizeUtils.approxMaxBinSize(serverElementSize * hashNum, params.binNum);
        alpha = CommonUtils.getUnitNum(approxMaxBinSize, params.maxPartitionSizePerBin);
        shiftMask = BigInteger.ONE.shiftLeft(params.plainModulusSize).subtract(BigInteger.ONE);
        byteL = CommonUtils.getByteLength(params.l);
        zp64 = Zp64Factory.createInstance(envType, params.plainModulus);
        pdsmReceiver.init(params.l, params.alphaUpperBound, params.binNum);
        pdsmSender.init(params.l, params.alphaUpperBound, params.binNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public UcpsiClientOutput<T> psi(Set<T> clientElementSet) throws MpcAbortException {
        setPtoInput(clientElementSet);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        Map<byte[], T> hashObjectMap = generateCuckooHashBin(byteL);
        stopWatch.stop();
        long binTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 4, binTime, "Client generates cuckoo hash bin.");

        stopWatch.start();
        List<byte[]> queryPayload = generateQuery();
        DataPacketHeader queryHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), rpcCount++,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(queryHeader, queryPayload));
        stopWatch.stop();
        long genQueryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, genQueryTime, "Client generates query.");

        DataPacketHeader responseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), rpcCount++,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> responsePayload = rpc.receive(responseHeader).getPayload();

        stopWatch.start();
        byte[][][] response = decodeResponse(responsePayload);
        while (alpha > params.alphaUpperBound) {
            alpha = CommonUtils.getUnitNum(alpha, params.maxPartitionSizePerBin);
            if (isHeReceiver) {
                response = clientAsReceiver();
            } else {
                clientAsSender(response);
            }
            isHeReceiver = !isHeReceiver;
        }
        stopWatch.stop();
        long decodeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 4, decodeTime, "Client executes HE-subroutines recursively");

        stopWatch.start();
        // private membership test
        SquareZ2Vector pemtOutput;
        if (isHeReceiver) {
            pemtOutput = pdsmReceiver.pdsm(params.l, alpha, mask);
        } else {
            pemtOutput = pdsmSender.pdsm(params.l, response);
        }
        ArrayList<T> table = IntStream.range(0, params.binNum)
            .mapToObj(batchIndex -> {
                HashBinEntry<byte[]> item = cuckooHashBin.getHashBinEntry(batchIndex);
                return item.getHashIndex() == HashBinEntry.DUMMY_ITEM_HASH_INDEX ?
                    null : hashObjectMap.get(item.getItem());
            })
            .collect(Collectors.toCollection(ArrayList::new));
        UcpsiClientOutput<T> clientOutput = new UcpsiClientOutput<>(table, pemtOutput);
        stopWatch.stop();
        long pmtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 4, pmtTime, "Client executes private membership test");

        logPhaseInfo(PtoState.PTO_END);
        return clientOutput;
    }

    /**
     * generate query.
     *
     * @return client query.
     */
    private List<byte[]> generateQuery() {
        long[][] query = UpsoUtils.encodeQuery(
            cuckooHashBin, params.itemPerCiphertext, params.ciphertextNum, params.polyModulusDegree, shiftMask
        );
        List<long[][]> encodedQuery = IntStream.range(0, params.ciphertextNum)
            .mapToObj(i -> UpsoUtils.computePowers(query[i], zp64, params.queryPowers, parallel))
            .collect(Collectors.toCollection(ArrayList::new));
        Stream<long[][]> queryStream = parallel ? encodedQuery.stream().parallel() : encodedQuery.stream();
        return queryStream
            .map(i -> Sj23PdsmUcpsiNativeUtils.generateQuery(params.encryptionParams, clientPublicKey, clientSecretKey, i))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    /**
     * set server public keys.
     *
     * @param serverPublicKeysPayload server public key payload.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private void handleServerPublicKeyPayload(List<byte[]> serverPublicKeysPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(serverPublicKeysPayload.size() == 2);
        serverPublicKey = serverPublicKeysPayload.remove(0);
        serverRelinKeys = serverPublicKeysPayload.remove(0);
    }

    /**
     * client as HE - sender.
     *
     * @param inputArray input byte array.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private void clientAsSender(byte[][][] inputArray) throws MpcAbortException {
        DataPacketHeader queryHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_QUERY.ordinal(), rpcCount++,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> queryPayload = rpc.receive(queryHeader).getPayload();
        int binSize = alpha * params.maxPartitionSizePerBin;
        plaintextList = encodeDatabase(inputArray, binSize);
        mask = generateMask();
        List<byte[]> responsePayload = computeResponse(queryPayload);
        DataPacketHeader responseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_RESPONSE.ordinal(), rpcCount++,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(responseHeader, responsePayload));
    }

    /**
     * client as HE - receiver.
     *
     * @return response.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private byte[][][] clientAsReceiver() throws MpcAbortException {
        long[][] query = UpsoUtils.encodeQuery(
            mask, params.itemPerCiphertext, params.ciphertextNum, params.polyModulusDegree, shiftMask,
            params.plainModulus, secureRandom
        );
        List<long[][]> encodedQuery = IntStream.range(0, params.ciphertextNum)
            .mapToObj(i -> UpsoUtils.computePowers(query[i], zp64, params.queryPowers, parallel))
            .collect(Collectors.toCollection(ArrayList::new));
        Stream<long[][]> encodeStream = parallel ? encodedQuery.stream().parallel() : encodedQuery.stream();
        List<byte[]> queryPayload = encodeStream
            .map(i -> Sj23PdsmUcpsiNativeUtils.generateQuery(params.encryptionParams, clientPublicKey, clientSecretKey, i))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
        DataPacketHeader queryHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), rpcCount++,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(queryHeader, queryPayload));
        DataPacketHeader responseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), rpcCount++,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> responsePayload = rpc.receive(responseHeader).getPayload();
        return decodeResponse(responsePayload);
    }

    /**
     * server generate response.
     *
     * @param queryList query list.
     * @return server response.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private List<byte[]> computeResponse(List<byte[]> queryList) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(
            queryList.size() == params.ciphertextNum * params.queryPowers.length, "The size of query is incorrect"
        );
        int[][] powerDegree = UpsoUtils.computePowerDegree(params.queryPowers, params.maxPartitionSizePerBin);
        IntStream intStream = IntStream.range(0, params.ciphertextNum);
        intStream = parallel ? intStream.parallel() : intStream;
        List<byte[]> queryPowers = intStream
            .mapToObj(i -> Sj23PdsmUcpsiNativeUtils.computeEncryptedPowers(
                params.encryptionParams,
                serverRelinKeys,
                queryList.subList(i * params.queryPowers.length, (i + 1) * params.queryPowers.length),
                powerDegree,
                params.queryPowers,
                0))
            .flatMap(Collection::stream)
            .collect(Collectors.toCollection(ArrayList::new));
        return IntStream.range(0, params.ciphertextNum)
            .mapToObj(i ->
                (parallel ? IntStream.range(0, alpha).parallel() : IntStream.range(0, alpha))
                    .mapToObj(j -> Sj23PdsmUcpsiNativeUtils.naiveComputeMatches(
                        params.encryptionParams,
                        serverPublicKey,
                        plaintextList.get(i * alpha + j),
                        queryPowers.subList(i * powerDegree.length, (i + 1) * powerDegree.length),
                        maskCoeffList.get(i))
                    )
                    .toArray(byte[][]::new))
            .flatMap(Arrays::stream)
            .collect(Collectors.toList());
    }

    /**
     * generate key pair.
     *
     * @return public keys.
     */
    private List<byte[]> clientKeyGen() {
        List<byte[]> keyPair = Sj23PdsmUcpsiNativeUtils.keyGen(params.encryptionParams);
        List<byte[]> publicKeys = new ArrayList<>();
        this.clientPublicKey = keyPair.get(0);
        byte[] clientRelinKeys = keyPair.get(1);
        this.clientSecretKey = keyPair.get(2);
        publicKeys.add(clientPublicKey);
        publicKeys.add(clientRelinKeys);
        return publicKeys;
    }

    /**
     * generate cuckoo hash bin.
     *
     * @param byteL hash byte length.
     * @return hash object map.
     */
    private Map<byte[], T> generateCuckooHashBin(int byteL) {
        Hash hash = HashFactory.createInstance(envType, byteL);
        Stream<T> stream = clientElementArrayList.stream();
        stream = parallel ? stream.parallel() : stream;
        List<byte[]> itemHash = stream
            .map(ObjectUtils::objectToByteArray)
            .map(hash::digestToBytes)
            .collect(Collectors.toList());
        Map<byte[], T> hashObjectMap = IntStream.range(0, clientElementSize)
            .boxed()
            .collect(Collectors.toMap(
                itemHash::get, i -> clientElementArrayList.get(i), (a, b) -> b, () -> new HashMap<>(clientElementSize)
            ));
        cuckooHashBin = CuckooHashBinFactory.createNoStashCuckooHashBin(
            envType, cuckooHashBinType, clientElementSize, params.binNum, hashKeys
        );
        cuckooHashBin.insertItems(itemHash);
        // padding dummy elements
        cuckooHashBin.insertPaddingItems(secureRandom);
        return hashObjectMap;
    }

    /**
     * client decodes response.
     *
     * @param responsePayload server response.
     * @return decoded response.
     */
    private byte[][][] decodeResponse(List<byte[]> responsePayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(responsePayload.size() == params.ciphertextNum * alpha);
        Stream<byte[]> responseStream = parallel ? responsePayload.stream().parallel() : responsePayload.stream();
        List<long[]> coeffs = responseStream
            .map(i -> Sj23PdsmUcpsiNativeUtils.decodeReply(params.encryptionParams, clientSecretKey, i))
            .collect(Collectors.toCollection(ArrayList::new));
        byte[][][] response = new byte[params.binNum][alpha][byteL];
        for (int i = 0; i < params.binNum; i++) {
            for (int j = 0; j < alpha; j++) {
                int cipherIndex = i / params.polyModulusDegree;
                int coeffIndex = i % params.polyModulusDegree;
                response[i][j] = LongUtils.longToByteArray(coeffs.get(cipherIndex * alpha + j)[coeffIndex]);
                BytesUtils.reduceByteArray(response[i][j], params.l);
            }
        }
        return response;
    }

    /**
     * encode database.
     *
     * @param hashBins hash bin.
     * @return encoded database.
     */
    private List<long[][]> encodeDatabase(byte[][][] hashBins, int binSize) {
        Zp64Poly zp64Poly = Zp64PolyFactory.createInstance(envType, params.plainModulus);
        long[][] encodedItems = UpsoUtils.encodeDatabase(
            hashBins, binSize, params.binNum, shiftMask, params.plainModulus, secureRandom
        );
        // for each bucket, compute the coefficients of the polynomial f(x) = \prod_{y in bucket} (x - y)
        return UpsoUtils.rootInterpolate(
            encodedItems, params.itemPerCiphertext, params.ciphertextNum, alpha, params.maxPartitionSizePerBin,
            params.polyModulusDegree, 1, zp64Poly, parallel
        );
    }

    /**
     * generate masks.
     *
     * @return masks.
     */
    private byte[][] generateMask() {
        maskCoeffList = new ArrayList<>();
        for (int i = 0; i < params.ciphertextNum; i++) {
            long[] r = IntStream.range(0, params.polyModulusDegree)
                .mapToLong(l -> Math.abs(secureRandom.nextLong()) % params.plainModulus)
                .toArray();
            maskCoeffList.add(r);
        }
        byte[][] masks = new byte[params.binNum][byteL];
        for (int i = 0; i < params.binNum; i++) {
            int cipherIndex = i / params.polyModulusDegree;
            int coeffIndex = i % params.polyModulusDegree;
            masks[i] = LongUtils.longToByteArray(maskCoeffList.get(cipherIndex)[coeffIndex]);
            BytesUtils.reduceByteArray(masks[i], params.l);
        }
        return masks;
    }
}
