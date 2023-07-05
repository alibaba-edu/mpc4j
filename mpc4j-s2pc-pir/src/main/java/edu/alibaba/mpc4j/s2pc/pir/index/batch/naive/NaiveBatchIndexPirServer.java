package edu.alibaba.mpc4j.s2pc.pir.index.batch.naive;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.hashbin.object.RandomPadHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntCuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.crypto.matrix.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.AbstractBatchIndexPirServer;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirServer;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.pir.index.batch.naive.NaiveBatchIndexPirPtoDesc.PtoStep;

/**
 * naive batch index PIR server.
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public class NaiveBatchIndexPirServer extends AbstractBatchIndexPirServer {

    /**
     * plaintext in NTT form
     */
    private List<List<byte[][]>> encodedDatabase;
    /**
     * cuckoo hash bin type
     */
    private final IntCuckooHashBinFactory.IntCuckooHashBinType cuckooHashBinType;
    /**
     * single index PIR server
     */
    private final SingleIndexPirServer singleIndexPirServer;
    /**
     * bin num
     */
    private int binNum;

    public NaiveBatchIndexPirServer(Rpc serverRpc, Party clientParty, NaiveBatchIndexPirConfig config) {
        super(NaiveBatchIndexPirPtoDesc.getInstance(), serverRpc, clientParty, config);
        singleIndexPirServer = SingleIndexPirFactory.createServer(serverRpc, clientParty, config.getSingleIndexPirConfig());
        addSubPtos(singleIndexPirServer);
        cuckooHashBinType = config.getCuckooHashBinType();
    }

    @Override
    public void init(NaiveDatabase database, int maxRetrievalSize) throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);
        setInitInput(database, maxRetrievalSize);

        stopWatch.start();
        // generate simple hash bin
        int hashNum = IntCuckooHashBinFactory.getHashNum(cuckooHashBinType);
        binNum = IntCuckooHashBinFactory.getBinNum(cuckooHashBinType, maxRetrievalSize);
        byte[][] hashKeys = CommonUtils.generateRandomKeys(hashNum, secureRandom);
        NaiveDatabase[] binDatabase = generateSimpleHashBin(database, hashKeys);
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        List<byte[]> cuckooHashKeyPayload = Arrays.stream(hashKeys).collect(Collectors.toList());
        rpc.send(DataPacket.fromByteArrayList(cuckooHashKeyHeader, cuckooHashKeyPayload));
        stopWatch.stop();
        long hashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, hashTime);

        DataPacketHeader clientPublicKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> publicKeyPayload = rpc.receive(clientPublicKeysHeader).getPayload();

        stopWatch.start();
        // init single index PIR server
        singleIndexPirServer.setPublicKey(publicKeyPayload);
        encodedDatabase = IntStream.range(0, binNum)
            .mapToObj(i -> singleIndexPirServer.serverSetup(binDatabase[i]))
            .collect(Collectors.toList());
        stopWatch.stop();
        long initIndexPirTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, initIndexPirTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void pir() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        // receive query
        DataPacketHeader clientQueryHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> clientQueryPayload = rpc.receive(clientQueryHeader).getPayload();
        MpcAbortPreconditions.checkArgument(clientQueryPayload.size() % binNum == 0);

        // generate response for each bucket
        stopWatch.start();
        int binQuerySize = clientQueryPayload.size() / binNum;
        List<byte[]> responsePayload = IntStream.range(0, binNum)
            .mapToObj(i -> {
                try {
                    return singleIndexPirServer.generateResponse(
                        clientQueryPayload.subList(i * binQuerySize, (i + 1) * binQuerySize), encodedDatabase.get(i));
                } catch (MpcAbortException e) {
                    e.printStackTrace();
                }
                return null;
            })
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
        DataPacketHeader responseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(responseHeader, responsePayload));
        stopWatch.stop();
        long genResponseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, genResponseTime, "Server generates response");

        logPhaseInfo(PtoState.PTO_END);
    }

    /**
     * generate simple hash bin.
     *
     * @param hashKeys hash keys.
     * @param database database.
     * @return bin database.
     */
    private NaiveDatabase[] generateSimpleHashBin(NaiveDatabase database, byte[][] hashKeys) {
        List<Integer> totalIndexList = IntStream.range(0, num)
            .boxed()
            .collect(Collectors.toCollection(() -> new ArrayList<>(num)));
        RandomPadHashBin<Integer> completeHash = new RandomPadHashBin<>(envType, binNum, num, hashKeys);
        completeHash.insertItems(totalIndexList);
        int maxBinSize = completeHash.binSize(0);
        for (int i = 1; i < binNum; i++) {
            if (completeHash.binSize(i) > maxBinSize) {
                maxBinSize = completeHash.binSize(i);
            }
        }
        byte[][][] paddingCompleteHashBin = new byte[binNum][maxBinSize][elementByteLength];
        byte[] paddingEntry = BytesUtils.randomByteArray(elementByteLength, elementBitLength, secureRandom);
        for (int i = 0; i < binNum; i++) {
            List<HashBinEntry<Integer>> binItems = new ArrayList<>(completeHash.getBin(i));
            for (int j = 0; j < binItems.size(); j++) {
                paddingCompleteHashBin[i][j] =
                    database.getBytesData(IntUtils.byteArrayToInt(binItems.get(j).getItemByteArray()));
            }
            int paddingNum = maxBinSize - binItems.size();
            for (int j = 0; j < paddingNum; j++) {
                paddingCompleteHashBin[i][j + binItems.size()] = BytesUtils.clone(paddingEntry);
            }
        }
        return IntStream.range(0, binNum)
            .mapToObj(i -> NaiveDatabase.create(elementBitLength, paddingCompleteHashBin[i]))
            .toArray(NaiveDatabase[]::new);
    }
}