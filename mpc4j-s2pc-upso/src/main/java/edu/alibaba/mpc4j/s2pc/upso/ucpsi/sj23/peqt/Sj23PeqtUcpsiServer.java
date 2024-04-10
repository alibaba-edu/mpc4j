package edu.alibaba.mpc4j.s2pc.upso.ucpsi.sj23.peqt;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrixFactory;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.MaxBinSizeUtils;
import edu.alibaba.mpc4j.common.tool.hashbin.object.RandomPadHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.polynomial.zp64.Zp64Poly;
import edu.alibaba.mpc4j.common.tool.polynomial.zp64.Zp64PolyFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtParty;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.upso.UpsoUtils;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.AbstractUcpsiServer;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.sj23.peqt.Sj23PeqtUcpsiPtoDesc.PtoStep;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * SJ23 unbalanced circuit PSI server.
 *
 * @author Liqiang Peng
 * @date 2023/7/17
 */
public class Sj23PeqtUcpsiServer<T> extends AbstractUcpsiServer<T> {
    /**
     * peqt sender
     */
    private final PeqtParty peqtParty;
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
    private Sj23PeqtUcpsiParams params;
    /**
     * alpha
     */
    private int alpha;
    /**
     * public key
     */
    private byte[] publicKey;
    /**
     * relin keys
     */
    private byte[] relinKeys;
    /**
     * plaintext list
     */
    private List<long[][]> plaintextList;
    /**
     * maks coeffs
     */
    private List<long[]> maskCoeffList;

    public Sj23PeqtUcpsiServer(Rpc serverRpc, Party clientParty, Sj23PeqtUcpsiConfig config) {
        super(Sj23PeqtUcpsiPtoDesc.getInstance(), serverRpc, clientParty, config);
        peqtParty = PeqtFactory.createSender(serverRpc, clientParty, config.getPeqtConfig());
        addSubPto(peqtParty);
        hashNum = CuckooHashBinFactory.getHashNum(CuckooHashBinType.NO_STASH_PSZ18_3_HASH);
    }

    @Override
    public void init(Set<T> serverElementSet, int maxClientElementSize) throws MpcAbortException {
        setInitInput(serverElementSet, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);
        params = Sj23PeqtUcpsiParams.getParams(serverElementSize, maxClientElementSize);

        stopWatch.start();
        // generate simple hash bin
        hashKeys = CommonUtils.generateRandomKeys(hashNum, secureRandom);
        List<byte[][]> hashBins = generateSimpleHashBin(CommonUtils.getByteLength(params.l));
        // max bin size
        int approxMaxBinSize = MaxBinSizeUtils.approxMaxBinSize(serverElementSize * hashNum, params.binNum);
        alpha = CommonUtils.getUnitNum(approxMaxBinSize, params.maxPartitionSizePerBin);
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
        logStepInfo(PtoState.INIT_STEP, 1, 3, hashBinTime);

        stopWatch.start();
        // polynomial interpolate
        plaintextList = encodeDatabase(hashBins);
        stopWatch.stop();
        long encodedTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 3, encodedTime);

        DataPacketHeader clientPublicKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> clientPublicKeysPayload = rpc.receive(clientPublicKeysHeader).getPayload();

        stopWatch.start();
        // handle client public keys
        handleClientPublicKeyPayload(clientPublicKeysPayload);
        // initialize peqt
        peqtParty.init(params.l, alpha * params.binNum);
        stopWatch.stop();
        long peqtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 4, 4, peqtTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public SquareZ2Vector psi() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        // receive query
        DataPacketHeader queryHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> queryPayload = rpc.receive(queryHeader).getPayload();

        stopWatch.start();
        byte[][] masks = generateMask();
        List<byte[]> responsePayload = computeResponse(queryPayload);
        stopWatch.stop();
        long replyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        DataPacketHeader responseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(responseHeader, responsePayload));
        logStepInfo(PtoState.PTO_STEP, 1, 2, replyTime, "Server generates reply");

        stopWatch.start();
        // private equality test
        SquareZ2Vector peqtOutput = peqtParty.peqt(params.l, masks);
        SquareZ2Vector z0 = handlePeqtOutput(peqtOutput);
        stopWatch.stop();
        long peqtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, peqtTime, "Server executes peqt");

        logPhaseInfo(PtoState.PTO_END);
        return z0;
    }

    /**
     * handle peqt output.
     *
     * @param z peqt output.
     * @return ucpsi output.
     */
    private SquareZ2Vector handlePeqtOutput(SquareZ2Vector z) {
        SquareZ2Vector[] binVector = IntStream.range(0, params.binNum)
            .mapToObj(i -> z.split(alpha))
            .toArray(SquareZ2Vector[]::new);
        TransBitMatrix matrix = TransBitMatrixFactory.createInstance(envType, alpha, params.binNum, parallel);
        IntStream.range(0, params.binNum).forEach(i -> matrix.setColumn(i, binVector[i].getBitVector().getBytes()));
        TransBitMatrix transpose = matrix.transpose();
        BitVector bitVector = BitVectorFactory.createZeros(params.binNum);
        for (int i = 0; i < alpha; i++) {
            BitVector temp = BitVectorFactory.create(params.binNum, transpose.getColumn(i));
            bitVector = bitVector.xor(temp);
        }
        return SquareZ2Vector.create(bitVector, false);
    }

    /**
     * generate masks.
     *
     * @return masks.
     */
    private byte[][] generateMask() {
        maskCoeffList = new ArrayList<>();
        for (int i = 0; i < params.ciphertextNum; i++) {
            for (int j = 0; j < alpha; j++) {
                long[] r = IntStream.range(0, params.polyModulusDegree)
                    .mapToLong(l -> Math.abs(secureRandom.nextLong()) % params.plainModulus)
                    .toArray();
                maskCoeffList.add(r);
            }
        }
        int byteL = CommonUtils.getByteLength(params.l);
        byte[][] masks = new byte[params.binNum * alpha][byteL];
        for (int i = 0; i < params.binNum; i++) {
            int cipherIndex = i * params.itemEncodedSlotSize / params.polyModulusDegree;
            int coeffIndex = (i * params.itemEncodedSlotSize) % params.polyModulusDegree;
            for (int j = 0; j < alpha; j++) {
                long[] item = new long[params.itemEncodedSlotSize];
                System.arraycopy(maskCoeffList.get(cipherIndex * alpha + j), coeffIndex, item, 0, params.itemEncodedSlotSize);
                masks[i * alpha + j] = PirUtils.convertCoeffsToBytes(item, params.plainModulusSize);
                BytesUtils.reduceByteArray(masks[i * alpha + j], params.l);
            }
        }
        return masks;
    }

    /**
     * generate simple hash bin.
     *
     * @param byteL hash byte length.
     * @return simple hash bin.
     */
    private List<byte[][]> generateSimpleHashBin(int byteL) {
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
        List<byte[][]> completeHashBins = IntStream.range(0, params.binNum)
            .mapToObj(i -> new ArrayList<>(simpleHashBin.getBin(i)))
            .map(binItemList -> binItemList.stream()
                .map(hashBinEntry -> BytesUtils.clone(hashBinEntry.getItemByteArray()))
                .toArray(byte[][]::new))
            .collect(Collectors.toList());
        simpleHashBin.clear();
        return completeHashBins;
    }

    /**
     * encode database.
     *
     * @param hashBins hash bin.
     * @return encoded database.
     */
    public List<long[][]> encodeDatabase(List<byte[][]> hashBins) {
        int binSize = alpha * params.maxPartitionSizePerBin;
        Zp64Poly zp64Poly = Zp64PolyFactory.createInstance(envType, params.plainModulus);
        // we will split the hash table into partitions
        long[][] encodedItemArray = new long[params.binNum * params.itemEncodedSlotSize][binSize];
        BigInteger shiftMask = BigInteger.ONE.shiftLeft(params.plainModulusSize).subtract(BigInteger.ONE);
        for (int i = 0; i < params.binNum; i++) {
            for (int j = 0; j < hashBins.get(i).length; j++) {
                long[] item = UpsoUtils.getHashBinEntryEncodedArray(
                    hashBins.get(i)[j], shiftMask, params.itemEncodedSlotSize, params.plainModulusSize
                );
                for (int l = 0; l < params.itemEncodedSlotSize; l++) {
                    encodedItemArray[i * params.itemEncodedSlotSize + l][j] = item[l];
                }
            }
            // padding dummy elements
            for (int j = 0; j < binSize - hashBins.get(i).length; j++) {
                long[] item = IntStream.range(0, params.itemEncodedSlotSize)
                    .mapToLong(l -> Math.abs(secureRandom.nextLong()) % params.plainModulus)
                    .toArray();
                for (int l = 0; l < params.itemEncodedSlotSize; l++) {
                    encodedItemArray[i * params.itemEncodedSlotSize + l][j + hashBins.get(i).length] = item[l];
                }
            }
        }
        return UpsoUtils.rootInterpolate(
            encodedItemArray, params.itemPerCiphertext, params.ciphertextNum, alpha, params.maxPartitionSizePerBin,
            params.polyModulusDegree, params.itemEncodedSlotSize, zp64Poly, parallel
        );
    }

    /**
     * set public keys.
     *
     * @param clientPublicKeysPayload client public keys payload.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private void handleClientPublicKeyPayload(List<byte[]> clientPublicKeysPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(clientPublicKeysPayload.size() == 2);
        publicKey = clientPublicKeysPayload.remove(0);
        relinKeys = clientPublicKeysPayload.remove(0);
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
        int[][] powerDegree = UpsoUtils.computePowerDegree(
            params.psLowDegree, params.queryPowers, params.maxPartitionSizePerBin
        );
        IntStream intStream = IntStream.range(0, params.ciphertextNum);
        intStream = parallel ? intStream.parallel() : intStream;
        List<byte[]> queryPowers = intStream
            .mapToObj(i -> Sj23PeqtUcpsiNativeUtils.computeEncryptedPowers(
                params.encryptionParams,
                relinKeys,
                queryList.subList(i * params.queryPowers.length, (i + 1) * params.queryPowers.length),
                powerDegree,
                params.queryPowers,
                params.psLowDegree)
            )
            .flatMap(Collection::stream)
            .collect(Collectors.toCollection(ArrayList::new));
        if (params.psLowDegree > 0) {
            return IntStream.range(0, params.ciphertextNum)
                .mapToObj(i ->
                    (parallel ? IntStream.range(0, alpha).parallel() : IntStream.range(0, alpha))
                        .mapToObj(j -> Sj23PeqtUcpsiNativeUtils.optComputeMatches(
                            params.encryptionParams,
                            publicKey,
                            relinKeys,
                            plaintextList.get(i * alpha + j),
                            queryPowers.subList(i * powerDegree.length, (i + 1) * powerDegree.length),
                            params.psLowDegree,
                            maskCoeffList.get(i * alpha + j))
                        )
                        .toArray(byte[][]::new))
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
        } else if (params.psLowDegree == 0) {
            return IntStream.range(0, params.ciphertextNum)
                .mapToObj(i ->
                    (parallel ? IntStream.range(0, alpha).parallel() : IntStream.range(0, alpha))
                        .mapToObj(j -> Sj23PeqtUcpsiNativeUtils.naiveComputeMatches(
                                params.encryptionParams,
                                publicKey,
                                plaintextList.get(i * alpha + j),
                                queryPowers.subList(i * powerDegree.length, (i + 1) * powerDegree.length),
                                maskCoeffList.get(i * alpha + j)
                            )
                        )
                        .toArray(byte[][]::new))
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
        } else {
            throw new MpcAbortException("ps_low_degree is incorrect");
        }
    }
}
