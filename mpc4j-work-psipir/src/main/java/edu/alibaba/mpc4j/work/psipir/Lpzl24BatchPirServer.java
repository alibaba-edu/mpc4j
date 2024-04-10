package edu.alibaba.mpc4j.work.psipir;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteEccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteFullEcc;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.RandomPadHashBin;
import edu.alibaba.mpc4j.common.tool.polynomial.zp64.Zp64Poly;
import edu.alibaba.mpc4j.common.tool.polynomial.zp64.Zp64PolyFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.upso.UpsoUtils;
import edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21.Cmg21UpsiConfig;
import edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21.Cmg21UpsiParams;
import edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21.Cmg21UpsiServer;
import edu.alibaba.mpc4j.work.AbstractBatchPirServer;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static edu.alibaba.mpc4j.work.psipir.Lpzl24BatchPirPtoDesc.PtoStep;

/**
 * PSI-PIR server.
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public class Lpzl24BatchPirServer extends AbstractBatchPirServer {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * UPSI server
     */
    private final Cmg21UpsiServer<ByteBuffer> upsiServer;
    /**
     * hash keys
     */
    private byte[][] hashKeys;
    /**
     * PRF key
     */
    private BigInteger alpha;
    /**
     * encoded database
     */
    private List<List<byte[]>> plaintexts;
    /**
     * max bin size
     */
    private int maxBinSize;
    /**
     * encryption params
     */
    private byte[] encryptionParams;
    /**
     * relinearization keys
     */
    private byte[] relinKeys;
    /**
     * ecc
     */
    private final ByteFullEcc ecc;

    public Lpzl24BatchPirServer(Rpc serverRpc, Party clientParty, Lpzl24BatchPirConfig config) {
        super(Lpzl24BatchPirPtoDesc.getInstance(), serverRpc, clientParty, config);
        upsiServer = new Cmg21UpsiServer<>(serverRpc, clientParty, (Cmg21UpsiConfig) config.getUpsiConfig());
        addSubPto(upsiServer);
        ecc = ByteEccFactory.createFullInstance(envType);
    }

    @Override
    public void init(BitVector database, int maxRetrievalSize) throws MpcAbortException {
        setInitInput(database, maxRetrievalSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        // UPSI params
        Cmg21UpsiParams params = null;
        if (num <= 1 << 20) {
            if (maxRetrievalSize <= 256) {
                params = Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_256;
            } else if (maxRetrievalSize <= 512) {
                params = Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_512_COM;
            } else if (maxRetrievalSize <= 1024) {
                params = Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_1K_COM;
            } else if (maxRetrievalSize <= 2048) {
                params = Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_2K_COM;
            } else if (maxRetrievalSize <= 4096) {
                params = Cmg21UpsiParams.SERVER_1M_CLIENT_MAX_4K_COM;
            } else {
                MpcAbortPreconditions.checkArgument(false, "retrieval size is larger than the upper bound.");
            }
        } else {
            if (maxRetrievalSize <= 1024) {
                params = Cmg21UpsiParams.SERVER_16M_CLIENT_MAX_1024;
            } else if (maxRetrievalSize <= 2048) {
                params = Cmg21UpsiParams.SERVER_16M_CLIENT_MAX_2048;
            } else if (maxRetrievalSize <= 4096) {
                params = Cmg21UpsiParams.SERVER_16M_CLIENT_MAX_4096;
            } else if (maxRetrievalSize <= 11041) {
                params = Cmg21UpsiParams.SERVER_16M_CLIENT_MAX_11041;
            } else {
                MpcAbortPreconditions.checkArgument(false, "retrieval size is larger than the upper bound.");
            }
        }
        assert params != null;
        upsiServer.init(params);
        Zp64Poly zp64Poly = Zp64PolyFactory.createInstance(envType, params.getPlainModulus());
        DataPacketHeader bfvParamsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> bfvKeyPair = rpc.receive(bfvParamsHeader).getPayload();
        MpcAbortPreconditions.checkArgument(bfvKeyPair.size() == 2);
        encryptionParams = bfvKeyPair.remove(0);
        relinKeys = bfvKeyPair.remove(0);

        stopWatch.start();
        hashKeys = CommonUtils.generateRandomKeys(params.getCuckooHashNum(), secureRandom);
        alpha = BigIntegerUtils.randomPositive(ecc.getN(), secureRandom);
        // compute PRF
        List<ByteBuffer> elementPrf = computeElementPrf();
        // complete hash bin
        List<List<HashBinEntry<ByteBuffer>>> hashBin = generateCompleteHashBin(elementPrf);
        // compute coefficients
        List<long[][]> coeffs = UpsoUtils.encodeDatabase(
            zp64Poly, hashBin, maxBinSize, params.getPlainModulus(), params.getMaxPartitionSizePerBin(),
            params.getItemEncodedSlotSize(), params.getItemPerCiphertext(), params.getBinNum(),
            params.getCiphertextNum(), params.getPolyModulusDegree(), parallel
        );
        hashBin.clear();
        IntStream intStream = IntStream.range(0, coeffs.size());
        intStream = parallel ? intStream.parallel() : intStream;
        plaintexts = intStream
            .mapToObj(j -> Lpzl24BatchPirNativeUtils.processDatabase(
                encryptionParams, coeffs.get(j), upsiServer.params.getPsLowDegree()
            ))
            .collect(Collectors.toCollection(ArrayList::new));
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        List<byte[]> cuckooHashKeyPayload = Arrays.stream(hashKeys).collect(Collectors.toList());
        rpc.send(DataPacket.fromByteArrayList(cuckooHashKeyHeader, cuckooHashKeyPayload));
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void pir() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // MP-OPRF
        DataPacketHeader blindHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_BLIND.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> blindPayload = rpc.receive(blindHeader).getPayload();
        List<byte[]> blindPrfPayload = handleBlindPayload(blindPayload);
        DataPacketHeader blindPrfHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SERVER_SEND_BLIND_PRF.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(blindPrfHeader, blindPrfPayload));
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, oprfTime, "Server runs OPRFs");

        DataPacketHeader queryHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> queryPayload = rpc.receive(queryHeader).getPayload();
        MpcAbortPreconditions.checkArgument(
            queryPayload.size() == upsiServer.params.getCiphertextNum() * upsiServer.params.getQueryPowers().length,
            "The size of query is incorrect"
        );

        stopWatch.start();
        int[][] powerDegree = UpsoUtils.computePowerDegree(
            upsiServer.params.getPsLowDegree(), upsiServer.params.getQueryPowers(), upsiServer.params.getMaxPartitionSizePerBin()
        );
        List<byte[]> ciphertextPowers = computeQueryPowers(queryPayload, powerDegree);
        List<byte[]> response = computeResponse(ciphertextPowers, powerDegree);
        DataPacketHeader keywordResponseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(keywordResponseHeader, response));
        stopWatch.stop();
        long replyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, replyTime, "Server generates reply");

        logPhaseInfo(PtoState.PTO_END);
    }

    /**
     * compute element prf.
     *
     * @return element prf.
     */
    private List<ByteBuffer> computeElementPrf() {
        Kdf kdf = KdfFactory.createInstance(envType);
        Prg prg = PrgFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH * 2);
        IntStream intStream = IntStream.range(0, num);
        intStream = parallel ? intStream.parallel() : intStream;
        return intStream
            .mapToObj(i -> ecc.hashToCurve(databaseList.get(i)))
            .map(hash -> ecc.mul(hash, alpha))
            .map(kdf::deriveKey)
            .map(prg::extendToBytes)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * generate complete hash bin.
     *
     * @param elementList element list.
     * @return complete hash bin.
     */
    private List<List<HashBinEntry<ByteBuffer>>> generateCompleteHashBin(List<ByteBuffer> elementList) {
        int binNum = upsiServer.params.getBinNum();
        RandomPadHashBin<ByteBuffer> completeHash = new RandomPadHashBin<>(envType, binNum, num, hashKeys);
        completeHash.insertItems(elementList);
        maxBinSize = IntStream.range(0, binNum).map(completeHash::binSize).max().orElse(0);
        byte[] randomBytes = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(randomBytes);
        HashBinEntry<ByteBuffer> paddingEntry = HashBinEntry.fromEmptyItem(ByteBuffer.wrap(randomBytes));
        List<List<HashBinEntry<ByteBuffer>>> paddingHashBin = new ArrayList<>();
        for (int i = 0; i < binNum; i++) {
            List<HashBinEntry<ByteBuffer>> binItems = new ArrayList<>(completeHash.getBin(i));
            int paddingNum = maxBinSize - completeHash.binSize(i);
            IntStream.range(0, paddingNum).mapToObj(j -> paddingEntry).forEach(binItems::add);
            paddingHashBin.add(binItems);
        }
        return paddingHashBin;
    }

    /**
     * handle blind element.
     *
     * @param blindElements blind element.
     * @return blind element prf.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private List<byte[]> handleBlindPayload(List<byte[]> blindElements) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(blindElements.size() > 0);
        Stream<byte[]> blindStream = blindElements.stream();
        blindStream = parallel ? blindStream.parallel() : blindStream;
        return blindStream
            .map(element -> ecc.mul(element, alpha))
            .collect(Collectors.toList());
    }

    /**
     * compute query powers.
     *
     * @param query       query powers.
     * @param powerDegree power degree.
     * @return query powers.
     */
    private List<byte[]> computeQueryPowers(List<byte[]> query, int[][] powerDegree) {
        IntStream intStream = IntStream.range(0, upsiServer.params.getCiphertextNum());
        intStream = parallel ? intStream.parallel() : intStream;
        return intStream
            .mapToObj(i -> Lpzl24BatchPirNativeUtils.computeEncryptedPowers(
                encryptionParams,
                relinKeys,
                query.subList(
                    i * upsiServer.params.getQueryPowers().length, (i + 1) * upsiServer.params.getQueryPowers().length),
                powerDegree,
                upsiServer.params.getQueryPowers(),
                upsiServer.params.getPsLowDegree())
            )
            .flatMap(Collection::stream)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * compute response.
     *
     * @param clientQuery client query.
     * @return server response.
     */
    private List<byte[]> computeResponse(List<byte[]> clientQuery, int[][] powerDegree) {
        int count = CommonUtils.getUnitNum(maxBinSize, upsiServer.params.getMaxPartitionSizePerBin());
        IntStream intStream = IntStream.range(0, upsiServer.params.getCiphertextNum());
        if (upsiServer.params.getPsLowDegree() > 0) {
            return intStream
                .mapToObj(i ->
                    (parallel ? IntStream.range(0, count).parallel() : IntStream.range(0, count))
                        .mapToObj(j -> Lpzl24BatchPirNativeUtils.optComputeMatches(
                            encryptionParams,
                            relinKeys,
                            plaintexts.get(i * count + j),
                            clientQuery.subList(i * powerDegree.length, (i + 1) * powerDegree.length),
                            upsiServer.params.getPsLowDegree()
                            ))
                        .toArray(byte[][]::new))
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
        } else {
            return intStream
                .mapToObj(i ->
                    (parallel ? IntStream.range(0, count).parallel() : IntStream.range(0, count))
                        .mapToObj(j -> Lpzl24BatchPirNativeUtils.naiveComputeMatches(
                            encryptionParams,
                            plaintexts.get(i * count + j),
                            clientQuery.subList(i * powerDegree.length, (i + 1) * powerDegree.length)
                            ))
                        .toArray(byte[][]::new))
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
        }
    }
}