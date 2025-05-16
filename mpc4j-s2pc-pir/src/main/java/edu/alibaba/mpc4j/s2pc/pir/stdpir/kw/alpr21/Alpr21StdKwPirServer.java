package edu.alibaba.mpc4j.s2pc.pir.stdpir.kw.alpr21;

import com.google.common.primitives.Bytes;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBin;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.PbcableStdIdxPirServer;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.StdIdxPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.kw.AbstractStdKwPirServer;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import static edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.createEnforceNoStashCuckooHashBin;
import static edu.alibaba.mpc4j.s2pc.pir.stdpir.kw.alpr21.Alpr21StdKwPirPtoDesc.PtoStep;
import static edu.alibaba.mpc4j.s2pc.pir.stdpir.kw.alpr21.Alpr21StdKwPirPtoDesc.getInstance;

/**
 * ALPR21 standard keyword PIR server.
 *
 * @author Liqiang Peng
 * @date 2023/6/16
 */
public class Alpr21StdKwPirServer<T> extends AbstractStdKwPirServer<T> {
    /**
     * ALPR21 standard KS PIR params
     */
    private final Alpr21StdKwPirParams params;
    /**
     * index PIR server
     */
    private final PbcableStdIdxPirServer pirServer;
    /**
     * cuckoo hash bin type
     */
    private final CuckooHashBinType cuckooHashBinType;
    /**
     * prf key
     */
    private byte[] prfKey;
    /**
     * hash keys
     */
    private byte[][] hashKeys;

    public Alpr21StdKwPirServer(Rpc serverRpc, Party clientParty, Alpr21StdKwPirConfig config) {
        super(getInstance(), serverRpc, clientParty, config);
        pirServer = StdIdxPirFactory.createPbcableServer(serverRpc, clientParty, config.getPbcableStdIdxPirConfig());
        addSubPto(pirServer);
        cuckooHashBinType = config.getCuckooHashBinType();
        params = config.getParams();
    }

    @Override
    public void init(Map<T, byte[]> keyValueMap, int l, int maxBatchNum) throws MpcAbortException {
        setInitInput(keyValueMap, l, maxBatchNum);
        logPhaseInfo(PtoState.INIT_BEGIN);
        params.setMaxRetrievalSize(maxBatchNum);

        // compute keyword prf
        stopWatch.start();
        ArrayList<T> keysList = new ArrayList<>(keyValueMap.keySet());
        List<ByteBuffer> keysPrf = computeKeywordPrf(keysList);
        Map<ByteBuffer, byte[]> prfLabelMap = IntStream.range(0, n)
            .boxed()
            .collect(
                Collectors.toMap(keysPrf::get, i -> keyValueMap.get(keysList.get(i)), (a, b) -> b)
            );
        sendOtherPartyPayload(PtoStep.SERVER_SEND_PRF_KEY.ordinal(), Collections.singletonList(prfKey));
        stopWatch.stop();
        long prfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, prfTime);

        // init index pir
        stopWatch.start();
        NaiveDatabase database = generateCuckooHashBin(keysPrf, prfLabelMap);
        sendOtherPartyPayload(
            PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), Arrays.stream(hashKeys).collect(Collectors.toList())
        );
        pirServer.init(database, hashKeys.length * maxBatchNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void pir(int batchNum) throws MpcAbortException {
        setPtoInput(batchNum);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        pirServer.pir(batchNum * hashKeys.length);
        stopWatch.stop();
        long replyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, replyTime, "Server generates reply");

        logPhaseInfo(PtoState.PTO_END);
    }

    private List<ByteBuffer> computeKeywordPrf(ArrayList<T> keys) {
        Prf prf = PrfFactory.createInstance(envType, params.keywordPrfByteLength);
        prfKey = BlockUtils.randomBlock(secureRandom);
        prf.setKey(prfKey);
        Stream<T> keywordStream = parallel ? keys.stream().parallel() : keys.stream();
        return keywordStream
            .map(ObjectUtils::objectToByteArray)
            .map(prf::getBytes)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toList());
    }

    private NaiveDatabase generateCuckooHashBin(List<ByteBuffer> keywordPrf, Map<ByteBuffer, byte[]> prfLabelMap) {
        byte[] botElementByteArray = new byte[params.keywordPrfByteLength];
        Arrays.fill(botElementByteArray, (byte) 0xFF);
        botByteBuffer = ByteBuffer.wrap(botElementByteArray);
        CuckooHashBin<ByteBuffer> cuckooHashBin = createEnforceNoStashCuckooHashBin(
            envType, cuckooHashBinType, n, keywordPrf, secureRandom
        );
        hashKeys = cuckooHashBin.getHashKeys();
        cuckooHashBin.insertPaddingItems(botByteBuffer);
        byte[][] cuckooHashBinItems = new byte[cuckooHashBin.binNum()][];
        for (int i = 0; i < cuckooHashBin.binNum(); i++) {
            ByteBuffer item = cuckooHashBin.getHashBinEntry(i).getItem();
            byte[] value = new byte[byteL];
            if (prfLabelMap.get(item) != null) {
                value = prfLabelMap.get(item);
            } else {
                secureRandom.nextBytes(value);
            }
            cuckooHashBinItems[i] = Bytes.concat(BytesUtils.clone(item.array(), 0, params.truncationByteLength), value);
        }
        return NaiveDatabase.create((params.truncationByteLength + byteL) * Byte.SIZE, cuckooHashBinItems);
    }
}