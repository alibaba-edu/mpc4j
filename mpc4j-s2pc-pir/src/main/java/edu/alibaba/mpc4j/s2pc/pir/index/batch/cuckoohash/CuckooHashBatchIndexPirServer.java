package edu.alibaba.mpc4j.s2pc.pir.index.batch.cuckoohash;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.IntHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.SimpleIntHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntCuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.AbstractBatchIndexPirServer;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirServer;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.pir.index.batch.cuckoohash.CuckooHashBatchIndexPirPtoDesc.PtoStep;

/**
 * cuckoo hash batch index PIR server.
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public class CuckooHashBatchIndexPirServer extends AbstractBatchIndexPirServer {

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
    private final SingleIndexPirServer server;
    /**
     * bin num
     */
    private int binNum;

    public CuckooHashBatchIndexPirServer(Rpc serverRpc, Party clientParty, CuckooHashBatchIndexPirConfig config) {
        super(CuckooHashBatchIndexPirPtoDesc.getInstance(), serverRpc, clientParty, config);
        server = SingleIndexPirFactory.createServer(serverRpc, clientParty, config.getSingleIndexPirConfig());
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
        server.setParallel(parallel);
        server.setDefaultParams();
        server.setPublicKey(publicKeyPayload);
        encodedDatabase = IntStream.range(0, binNum)
            .mapToObj(i -> server.serverSetup(binDatabase[i]))
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
                    return server.generateResponse(
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
        int[] totalIndex = IntStream.range(0, num).toArray();
        IntHashBin intHashBin = new SimpleIntHashBin(envType, binNum, num, hashKeys);
        intHashBin.insertItems(totalIndex);
        int maxBinSize = IntStream.range(0, binNum).map(intHashBin::binSize).max().orElse(0);
        byte[][][] paddingCompleteHashBin = new byte[binNum][maxBinSize][elementByteLength];
        byte[] paddingEntry = BytesUtils.randomByteArray(elementByteLength, elementBitLength, secureRandom);
        for (int i = 0; i < binNum; i++) {
            int size = intHashBin.binSize(i);
            for (int j = 0; j < size; j++) {
                paddingCompleteHashBin[i][j] = database.getBytesData(intHashBin.getBin(i)[j]);
            }
            int paddingNum = maxBinSize - size;
            for (int j = 0; j < paddingNum; j++) {
                paddingCompleteHashBin[i][j + size] = BytesUtils.clone(paddingEntry);
            }
        }
        return IntStream.range(0, binNum)
            .mapToObj(i -> NaiveDatabase.create(elementBitLength, paddingCompleteHashBin[i]))
            .toArray(NaiveDatabase[]::new);
    }
}