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
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64Factory;
import edu.alibaba.mpc4j.common.tool.hashbin.MaxBinSizeUtils;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.NoStashCuckooHashBin;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtParty;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.upso.UpsoUtils;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.AbstractUcpsiClient;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.UcpsiClientOutput;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static edu.alibaba.mpc4j.s2pc.upso.ucpsi.sj23.peqt.Sj23PeqtUcpsiPtoDesc.PtoStep;
import static edu.alibaba.mpc4j.s2pc.upso.ucpsi.sj23.peqt.Sj23PeqtUcpsiPtoDesc.getInstance;

/**
 * SJ23 unbalanced circuit PSI client.
 *
 * @author Liqiang Peng
 * @date 2023/7/17
 */
public class Sj23PeqtUcpsiClient<T> extends AbstractUcpsiClient<T> {
    /**
     * peqt receiver
     */
    private final PeqtParty peqtParty;
    /**
     * cuckoo hash bin type
     */
    private final CuckooHashBinType cuckooHashBinType;
    /**
     * cuckoo hash num
     */
    private final int hashNum;
    /**
     * SJ23 UCPSI params
     */
    private Sj23PeqtUcpsiParams params;
    /**
     * cuckoo hash bin
     */
    private NoStashCuckooHashBin<byte[]> cuckooHashBin;
    /**
     * hash keys
     */
    private byte[][] hashKeys;
    /**
     * secret key
     */
    private byte[] secretKey;
    /**
     * public key
     */
    private byte[] publicKey;
    /**
     * alpha
     */
    private int alpha;

    public Sj23PeqtUcpsiClient(Rpc clientRpc, Party serverParty, Sj23PeqtUcpsiConfig config) {
        super(getInstance(), clientRpc, serverParty, config);
        peqtParty = PeqtFactory.createReceiver(clientRpc, serverParty, config.getPeqtConfig());
        addSubPto(peqtParty);
        cuckooHashBinType = CuckooHashBinType.NO_STASH_PSZ18_3_HASH;
        hashNum = CuckooHashBinFactory.getHashNum(cuckooHashBinType);
    }

    @Override
    public void init(int maxClientElementSize, int serverElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, serverElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);
        params = Sj23PeqtUcpsiParams.getParams(serverElementSize, maxClientElementSize);

        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> cuckooHashKeysPayload = rpc.receive(cuckooHashKeyHeader).getPayload();
        MpcAbortPreconditions.checkArgument(cuckooHashKeysPayload.size() == hashNum);
        hashKeys = cuckooHashKeysPayload.toArray(new byte[0][]);

        stopWatch.start();
        // generate public keys
        int approxMaxBinSize = MaxBinSizeUtils.approxMaxBinSize(serverElementSize * hashNum, params.binNum);
        alpha = CommonUtils.getUnitNum(approxMaxBinSize, params.maxPartitionSizePerBin);
        List<byte[]> publicKeysPayload = keyGen();
        DataPacketHeader clientPublicKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientPublicKeysHeader, publicKeysPayload));
        stopWatch.stop();
        long keyGenTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, keyGenTime);

        stopWatch.start();
        // init peqt and z2pc party
        peqtParty.init(params.l, alpha * params.binNum);
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
        Map<byte[], T> hashObjectMap = generateCuckooHashBin(CommonUtils.getByteLength(params.l));
        stopWatch.stop();
        long binTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 4, binTime, "Client hashes elements");

        stopWatch.start();
        // generate query
        List<byte[]> queryPayload = encodeQuery();
        DataPacketHeader queryHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(queryHeader, queryPayload));
        stopWatch.stop();
        long genQueryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, genQueryTime, "Client generates query");

        DataPacketHeader responseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> responsePayload = rpc.receive(responseHeader).getPayload();

        stopWatch.start();
        // decode reply
        byte[][] decodeResponse = decodeResponse(responsePayload);
        stopWatch.stop();
        long decodeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 4, decodeTime, "Client decodes response");

        stopWatch.start();
        // private equality test
        SquareZ2Vector peqtOutput = peqtParty.peqt(params.l, decodeResponse);
        UcpsiClientOutput<T> clientOutput = handlePeqtOutput(peqtOutput, hashObjectMap);
        stopWatch.stop();
        long peqtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 4, peqtTime, "Client executes peqt time");

        logPhaseInfo(PtoState.PTO_END);
        return clientOutput;
    }

    /**
     * handle peqt output.
     *
     * @param z             peqt output.
     * @param hashObjectMap hash object map.
     * @return ucpsi client output.
     */
    private UcpsiClientOutput<T> handlePeqtOutput(SquareZ2Vector z, Map<byte[], T> hashObjectMap) {
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
        SquareZ2Vector z1 = SquareZ2Vector.create(bitVector, false);
        ArrayList<T> table = IntStream.range(0, params.binNum)
            .mapToObj(batchIndex -> {
                HashBinEntry<byte[]> item = cuckooHashBin.getHashBinEntry(batchIndex);
                if (item.getHashIndex() == HashBinEntry.DUMMY_ITEM_HASH_INDEX) {
                    return null;
                } else {
                    return hashObjectMap.get(item.getItem());
                }
            })
            .collect(Collectors.toCollection(ArrayList::new));
        return new UcpsiClientOutput<>(table, z1);
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
        List<byte[]> itemHashList = stream
            .map(ObjectUtils::objectToByteArray)
            .map(hash::digestToBytes)
            .collect(Collectors.toList());
        Map<byte[], T> hashObjectMap = IntStream.range(0, clientElementSize)
            .boxed()
            .collect(Collectors.toMap(
                itemHashList::get,
                i -> clientElementArrayList.get(i),
                (a, b) -> b,
                () -> new HashMap<>(clientElementSize)
            ));
        cuckooHashBin = CuckooHashBinFactory.createNoStashCuckooHashBin(
            envType, cuckooHashBinType, clientElementSize, params.binNum, hashKeys
        );
        cuckooHashBin.insertItems(itemHashList);
        // padding dummy elements
        cuckooHashBin.insertPaddingItems(secureRandom);
        return hashObjectMap;
    }

    /**
     * generate key pair.
     *
     * @return public keys.
     */
    private List<byte[]> keyGen() {
        List<byte[]> keyPair = Sj23PeqtUcpsiNativeUtils.keyGen(params.encryptionParams);
        List<byte[]> publicKeys = new ArrayList<>();
        this.publicKey = keyPair.get(0);
        byte[] relinKeys = keyPair.get(1);
        secretKey = keyPair.get(2);
        publicKeys.add(publicKey);
        publicKeys.add(relinKeys);
        return publicKeys;
    }

    /**
     * encode query.
     *
     * @return encoded query.
     */
    public List<byte[]> encodeQuery() {
        BigInteger shiftMask = BigInteger.ONE.shiftLeft(params.plainModulusSize).subtract(BigInteger.ONE);
        long[][] query = IntStream.range(0, params.ciphertextNum)
            .mapToObj(i -> IntStream.range(0, params.polyModulusDegree)
                .mapToLong(l -> Math.abs(secureRandom.nextLong()) % params.plainModulus)
                .toArray())
            .toArray(long[][]::new);
        for (int i = 0; i < params.ciphertextNum; i++) {
            for (int j = 0; j < params.itemPerCiphertext; j++) {
                HashBinEntry<byte[]> hashBinEntry = cuckooHashBin.getHashBinEntry(i * params.itemPerCiphertext + j);
                if (hashBinEntry.getHashIndex() != HashBinEntry.DUMMY_ITEM_HASH_INDEX) {
                    long[] item = UpsoUtils.getHashBinEntryEncodedArray(
                        cuckooHashBin.getHashBinEntry(i * params.itemPerCiphertext + j).getItem(), shiftMask,
                        params.itemPerCiphertext, params.plainModulusSize
                    );
                    System.arraycopy(item, 0, query[i], j * params.itemEncodedSlotSize, params.itemEncodedSlotSize);
                }
            }
        }
        Zp64 zp64 = Zp64Factory.createInstance(envType, params.plainModulus);
        List<long[][]> encodedQuery = IntStream.range(0, params.ciphertextNum)
            .mapToObj(i -> UpsoUtils.computePowers(query[i], zp64, params.queryPowers, parallel))
            .collect(Collectors.toCollection(ArrayList::new));
        Stream<long[][]> encodeStream = parallel ? encodedQuery.stream().parallel() : encodedQuery.stream();
        return encodeStream
            .map(i -> Sj23PeqtUcpsiNativeUtils.generateQuery(params.encryptionParams, publicKey, secretKey, i))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    /**
     * client decodes response.
     *
     * @param responsePayload server response.
     * @return decoded response.
     */
    public byte[][] decodeResponse(List<byte[]> responsePayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(responsePayload.size() == params.ciphertextNum * alpha);
        Stream<byte[]> responseStream = parallel ? responsePayload.stream().parallel() : responsePayload.stream();
        List<long[]> coeffs = responseStream
            .map(i -> Sj23PeqtUcpsiNativeUtils.decodeReply(params.encryptionParams, secretKey, i))
            .collect(Collectors.toCollection(ArrayList::new));
        int byteL = CommonUtils.getByteLength(params.l);
        byte[][] masks = new byte[params.binNum * alpha][byteL];
        for (int i = 0; i < params.binNum; i++) {
            for (int j = 0; j < alpha; j++) {
                int cipherIndex = i * params.itemEncodedSlotSize / params.polyModulusDegree;
                int coeffIndex = (i * params.itemEncodedSlotSize) % params.polyModulusDegree;
                long[] item = new long[params.itemEncodedSlotSize];
                System.arraycopy(coeffs.get(cipherIndex * alpha + j), coeffIndex, item, 0, params.itemEncodedSlotSize);
                masks[i * alpha + j] = PirUtils.convertCoeffsToBytes(item, params.plainModulusSize);
                BytesUtils.reduceByteArray(masks[i * alpha + j], params.l);
            }
        }
        return masks;
    }
}
