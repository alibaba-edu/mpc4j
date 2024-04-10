package edu.alibaba.mpc4j.s2pc.upso.ucpsi.sj23.pdsm;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64Factory;
import edu.alibaba.mpc4j.common.tool.hashbin.MaxBinSizeUtils;
import edu.alibaba.mpc4j.common.tool.hashbin.object.RandomPadHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
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
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.AbstractUcpsiServer;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static edu.alibaba.mpc4j.s2pc.upso.ucpsi.sj23.pdsm.Sj23PdsmUcpsiPtoDesc.PtoStep;
import static edu.alibaba.mpc4j.s2pc.upso.ucpsi.sj23.pdsm.Sj23PdsmUcpsiPtoDesc.getInstance;

/**
 * SJ23 unbalanced circuit PSI server.
 *
 * @author Liqiang Peng
 * @date 2023/7/17
 */
public class Sj23PdsmUcpsiServer<T> extends AbstractUcpsiServer<T> {
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
     * hash keys
     */
    private byte[][] hashKeys;
    /**
     * SJ23 UCPSI params
     */
    private Sj23PdsmUcpsiParams params;
    /**
     * alpha
     */
    private int alpha;
    /**
     * public key
     */
    private byte[] clientPublicKey;
    /**
     * relin keys
     */
    private byte[] clientRelinKeys;
    /**
     * public key
     */
    private byte[] serverPublicKey;
    /**
     * secret key
     */
    private byte[] serverSecretKey;
    /**
     * zp64
     */
    private Zp64 zp64;
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
     * is HE sender
     */
    private boolean isHeSender;
    /**
     * hash byte length
     */
    private int byteL;
    /**
     * shift mask
     */
    private BigInteger shiftMask;

    public Sj23PdsmUcpsiServer(Rpc serverRpc, Party clientParty, Sj23PdsmUcpsiConfig config) {
        super(getInstance(), serverRpc, clientParty, config);
        pdsmReceiver = PdsmFactory.createReceiver(serverRpc, clientParty, config.getPsmConfig());
        addSubPto(pdsmReceiver);
        pdsmSender = PdsmFactory.createSender(serverRpc, clientParty, config.getPsmConfig());
        addSubPto(pdsmSender);
        hashNum = CuckooHashBinFactory.getHashNum(CuckooHashBinType.NO_STASH_PSZ18_3_HASH);
        rpcCount = 0;
        isHeSender = false;
    }

    @Override
    public void init(Set<T> serverElementSet, int maxClientElementSize) throws MpcAbortException {
        setInitInput(serverElementSet, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);
        params = Sj23PdsmUcpsiParams.getParams(serverElementSize, maxClientElementSize);

        stopWatch.start();
        // generate simple hash bin
        byteL = CommonUtils.getByteLength(params.l);
        hashKeys = CommonUtils.generateRandomKeys(hashNum, secureRandom);
        byte[][][] hashBin = generateSimpleHashBin(byteL);
        // server sends hash keys
        List<byte[]> cuckooHashKeyPayload = Arrays.stream(hashKeys).collect(Collectors.toList());
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_HASH_KEYS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(cuckooHashKeyHeader, cuckooHashKeyPayload));
        stopWatch.stop();
        long hashBinTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 4, hashBinTime);

        stopWatch.start();
        // polynomial interpolate
        int approxMaxBinSize = MaxBinSizeUtils.approxMaxBinSize(serverElementSize * hashNum, params.binNum);
        alpha = CommonUtils.getUnitNum(approxMaxBinSize, params.maxPartitionSizePerBin);
        int binSize = alpha * params.maxPartitionSizePerBin;
        zp64 = Zp64Factory.createInstance(envType, params.plainModulus);
        shiftMask = BigInteger.ONE.shiftLeft(params.plainModulusSize).subtract(BigInteger.ONE);
        plaintextList = encodeDatabase(hashBin, binSize);
        stopWatch.stop();
        long encodedTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 4, encodedTime);

        DataPacketHeader clientPublicKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> clientPublicKeysPayload = rpc.receive(clientPublicKeysHeader).getPayload();

        stopWatch.start();
        List<byte[]> serverPublicKeysPayload = serverKeyGen();
        DataPacketHeader serverPublicKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_PUBLIC_KEYS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverPublicKeysHeader, serverPublicKeysPayload));
        stopWatch.stop();
        long keyGenTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 4, keyGenTime);

        stopWatch.start();
        handleClientPublicKeyPayload(clientPublicKeysPayload);
        // initialize pmt
        pdsmSender.init(params.l, params.alphaUpperBound, params.binNum);
        pdsmReceiver.init(params.l, params.alphaUpperBound, params.binNum);
        stopWatch.stop();
        long pmtInitTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 4, 4, pmtInitTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZ2Vector psi() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        // receive query
        DataPacketHeader queryHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), rpcCount++,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> queryPayload = rpc.receive(queryHeader).getPayload();

        stopWatch.start();
        mask = generateMask();
        List<byte[]> responsePayload = computeResponse(queryPayload);
        DataPacketHeader responseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), rpcCount++,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(responseHeader, responsePayload));
        byte[][][] decodeResponse = new byte[params.binNum][][];
        while (alpha > params.alphaUpperBound) {
            alpha = CommonUtils.getUnitNum(alpha, params.maxPartitionSizePerBin);
            if (isHeSender) {
                serverAsSender(decodeResponse);
            } else {
                decodeResponse = serverAsReceiver();
            }
            isHeSender = !isHeSender;
        }
        stopWatch.stop();
        long replyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, replyTime, "Server executes HE-subroutines recursively");

        stopWatch.start();
        // private membership test
        SquareZ2Vector pesmOutput;
        if (isHeSender) {
            pesmOutput = pdsmSender.pdsm(params.l, decodeResponse);
        } else {
            pesmOutput = pdsmReceiver.pdsm(params.l, alpha, mask);
        }
        stopWatch.stop();
        long pmtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, pmtTime, "Server executes private membership test");

        logPhaseInfo(PtoState.PTO_END);
        return pesmOutput;
    }

    /**
     * server as HE - receiver.
     *
     * @return response.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private byte[][][] serverAsReceiver() throws MpcAbortException {
        long[][] query = UpsoUtils.encodeQuery(
            mask, params.itemPerCiphertext, params.ciphertextNum, params.polyModulusDegree, shiftMask,
            params.plainModulus, secureRandom
        );
        List<long[][]> encodedQuery = IntStream.range(0, params.ciphertextNum)
            .mapToObj(i -> UpsoUtils.computePowers(query[i], zp64, params.queryPowers, parallel))
            .collect(Collectors.toCollection(ArrayList::new));
        Stream<long[][]> encodeStream = parallel ? encodedQuery.stream().parallel() : encodedQuery.stream();
        List<byte[]> queryPayload = encodeStream
            .map(i -> Sj23PdsmUcpsiNativeUtils.generateQuery(params.encryptionParams, serverPublicKey, serverSecretKey, i))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
        DataPacketHeader queryHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_QUERY.ordinal(), rpcCount++,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(queryHeader, queryPayload));
        DataPacketHeader responseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_RESPONSE.ordinal(), rpcCount++,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> responsePayload = rpc.receive(responseHeader).getPayload();
        return decodeResponse(responsePayload);
    }

    /**
     * server as HE - sender.
     *
     * @param inputArray input byte array.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private void serverAsSender(byte[][][] inputArray) throws MpcAbortException {
        DataPacketHeader queryHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), rpcCount++,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> queryPayload = rpc.receive(queryHeader).getPayload();
        int binSize = alpha * params.maxPartitionSizePerBin;
        plaintextList = encodeDatabase(inputArray, binSize);
        mask = generateMask();
        List<byte[]> responsePayload = computeResponse(queryPayload);
        DataPacketHeader responseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), rpcCount++,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(responseHeader, responsePayload));
    }

    /**
     * generate simple hash bin.
     *
     * @param byteL byte length.
     * @return simple hash bin.
     */
    private byte[][][] generateSimpleHashBin(int byteL) {
        Hash hash = HashFactory.createInstance(envType, byteL);
        Stream<T> stream = serverElementArrayList.stream();
        stream = parallel ? stream.parallel() : stream;
        List<byte[]> itemList = stream
            .map(ObjectUtils::objectToByteArray)
            .map(hash::digestToBytes)
            .collect(Collectors.toList());
        RandomPadHashBin<byte[]> simpleHashBin = new RandomPadHashBin<>(
            envType, params.binNum, serverElementSize, hashKeys
        );
        simpleHashBin.insertItems(itemList);
        byte[][][] completeHashBins = IntStream.range(0, params.binNum)
            .mapToObj(i -> new ArrayList<>(simpleHashBin.getBin(i)))
            .map(binItemList -> binItemList.stream()
                .map(hashBinEntry -> BytesUtils.clone(hashBinEntry.getItemByteArray()))
                .toArray(byte[][]::new))
            .toArray(byte[][][]::new);
        simpleHashBin.clear();
        return completeHashBins;
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
     * set client public keys.
     *
     * @param clientPublicKeysPayload client public key payload.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private void handleClientPublicKeyPayload(List<byte[]> clientPublicKeysPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(clientPublicKeysPayload.size() == 2);
        clientPublicKey = clientPublicKeysPayload.remove(0);
        clientRelinKeys = clientPublicKeysPayload.remove(0);
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
                clientRelinKeys,
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
                        clientPublicKey,
                        plaintextList.get(i * alpha + j),
                        queryPowers.subList(i * powerDegree.length, (i + 1) * powerDegree.length),
                        maskCoeffList.get(i)))
                    .toArray(byte[][]::new))
            .flatMap(Arrays::stream)
            .collect(Collectors.toList());
    }

    /**
     * client decodes response.
     *
     * @param responsePayload server response.
     * @return decoded response.
     */
    public byte[][][] decodeResponse(List<byte[]> responsePayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(responsePayload.size() == params.ciphertextNum * alpha);
        Stream<byte[]> responseStream = parallel ? responsePayload.stream().parallel() : responsePayload.stream();
        List<long[]> coeffs = responseStream
            .map(i -> Sj23PdsmUcpsiNativeUtils.decodeReply(params.encryptionParams, serverSecretKey, i))
            .collect(Collectors.toCollection(ArrayList::new));
        byte[][][] masks = new byte[params.binNum][alpha][byteL];
        for (int i = 0; i < params.binNum; i++) {
            for (int j = 0; j < alpha; j++) {
                int cipherIndex = i / params.polyModulusDegree;
                int coeffIndex = i % params.polyModulusDegree;
                masks[i][j] = LongUtils.longToByteArray(coeffs.get(cipherIndex * alpha + j)[coeffIndex]);
                BytesUtils.reduceByteArray(masks[i][j], params.l);
            }
        }
        return masks;
    }

    /**
     * generate key pair.
     *
     * @return public keys.
     */
    private List<byte[]> serverKeyGen() {
        List<byte[]> keyPair = Sj23PdsmUcpsiNativeUtils.keyGen(params.encryptionParams);
        List<byte[]> publicKeys = new ArrayList<>();
        this.serverPublicKey = keyPair.get(0);
        byte[] serverRelinKeys = keyPair.get(1);
        this.serverSecretKey = keyPair.get(2);
        publicKeys.add(serverPublicKey);
        publicKeys.add(serverRelinKeys);
        return publicKeys;
    }
}
