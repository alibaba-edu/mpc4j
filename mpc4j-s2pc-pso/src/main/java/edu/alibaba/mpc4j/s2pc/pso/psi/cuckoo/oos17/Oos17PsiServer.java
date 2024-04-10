package edu.alibaba.mpc4j.s2pc.pso.psi.cuckoo.oos17;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.structure.filter.Filter;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lcot.LcotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pso.psi.AbstractPsiServer;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiUtils;
import edu.alibaba.mpc4j.s2pc.pso.psi.cuckoo.oos17.Oos17PsiPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * OOS17-PSI server.
 *
 * @author Weiran Liu
 * @date 2023/9/18
 */
public class Oos17PsiServer<T> extends AbstractPsiServer<T> {
    /**
     * LCOT sender
     */
    private final LcotSender lcotSender;
    /**
     * cuckoo hash bin type
     */
    private final CuckooHashBinType cuckooHashBinType;
    /**
     * cuckoo hash num
     */
    private final int cuckooHashNum;
    /**
     * filter type
     */
    private final FilterType filterType;
    /**
     * bin num
     */
    private int binNum;
    /**
     * bin hashes
     */
    private Prf[] binHashes;
    /**
     * h1: {0, 1}^* → {0, 1}^l
     */
    private Hash h1;

    public Oos17PsiServer(Rpc serverRpc, Party clientParty, Oos17PsiConfig config) {
        super(Oos17PsiPtoDesc.getInstance(), serverRpc, clientParty, config);
        lcotSender = LcotFactory.createSender(serverRpc, clientParty, config.getLcotConfig());
        addSubPto(lcotSender);
        cuckooHashBinType = config.getCuckooHashBinType();
        cuckooHashNum = CuckooHashBinFactory.getHashNum(cuckooHashBinType);
        filterType = config.getFilterType();
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int maxBinNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, maxClientElementSize);
        int maxStashNum = CuckooHashBinFactory.getStashSize(cuckooHashBinType, maxClientElementSize);
        int byteL = PsiUtils.getSemiHonestPeqtByteLength(maxServerElementSize, maxClientElementSize);
        h1 = HashFactory.createInstance(envType, byteL);
        int l = byteL * Byte.SIZE;
        // init cuckoo hash keys
        DataPacketHeader cuckooHashKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> cuckooHashKeysPayload = rpc.receive(cuckooHashKeysHeader).getPayload();
        MpcAbortPreconditions.checkArgument(cuckooHashKeysPayload.size() == cuckooHashNum);
        byte[][] cuckooHashKeys = cuckooHashKeysPayload.toArray(new byte[0][]);
        binHashes = IntStream.range(0, cuckooHashNum)
            .mapToObj(hashIndex -> {
                Prf prf = PrfFactory.createInstance(envType, Integer.BYTES);
                prf.setKey(cuckooHashKeys[hashIndex]);
                return prf;
            })
            .toArray(Prf[]::new);
        // init LCOT
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(delta);
        lcotSender.init(l, maxBinNum + maxStashNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void psi(Set<T> serverElementSet, int clientElementSize) throws MpcAbortException {
        setPtoInput(serverElementSet, clientElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        binNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, clientElementSize);
        int stashSize = CuckooHashBinFactory.getStashSize(cuckooHashBinType, clientElementSize);
        stopWatch.stop();
        long setupTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, setupTime, "Server inits tools");

        stopWatch.start();
        LcotSenderOutput lcotSenderOutput = lcotSender.send(binNum + stashSize);
        stopWatch.stop();
        long lcotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, lcotTime, "Server runs LCOT");

        stopWatch.start();
        for (int hashIndex = 0; hashIndex < cuckooHashNum; hashIndex++) {
            List<byte[]> serverBinPrfPayload = generateBinPrfPayload(hashIndex, lcotSenderOutput);
            DataPacketHeader serverBinPrfHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_BIN_PRFS.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(serverBinPrfHeader, serverBinPrfPayload));
            extraInfo++;
        }
        for (int stashIndex = 0; stashIndex < stashSize; stashIndex++) {
            List<byte[]> serverStashPrfPayload = generateStashPrfPayload(stashIndex, lcotSenderOutput);
            DataPacketHeader serverStashPrfHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_STASH_PRFS.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(serverStashPrfHeader, serverStashPrfPayload));
            extraInfo++;
        }
        stopWatch.stop();
        long serverPrfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, serverPrfTime, "Server computes PRFs");

        logPhaseInfo(PtoState.PTO_END);
    }

    private List<byte[]> generateBinPrfPayload(int hashIndex, LcotSenderOutput lcotSenderOutput) {
        Stream<T> serverElementStream = serverElementArrayList.stream();
        serverElementStream = parallel ? serverElementStream.parallel() : serverElementStream;
        List<byte[]> binPrfs = serverElementStream
            .map(x -> getBinPrf(x, hashIndex, lcotSenderOutput))
            .collect(Collectors.toList());
        Collections.shuffle(binPrfs, secureRandom);
        Filter<byte[]> binPrfFilter = FilterFactory.load(envType, filterType, serverElementSize, secureRandom);
        binPrfs.forEach(binPrfFilter::put);
        return binPrfFilter.save();
    }


    private byte[] getBinPrf(T x, int hashIndex, LcotSenderOutput lcotSenderOutput) {
        byte[] xBytes = ObjectUtils.objectToByteArray(x);
        int binIndex = binHashes[hashIndex].getInteger(xBytes, binNum);
        // OPRF(x || hashIndex)
        byte[] hx = ByteBuffer.allocate(xBytes.length + Integer.BYTES)
            .put(xBytes)
            .putInt(hashIndex)
            .array();
        hx = h1.digestToBytes(hx);
        byte[] prf = lcotSenderOutput.getRb(binIndex, hx);
        prf = h1.digestToBytes(prf);
        return prf;
    }

    private List<byte[]> generateStashPrfPayload(int stashIndex, LcotSenderOutput lcotSenderOutput) {
        Stream<T> serverElementStream = serverElementArrayList.stream();
        serverElementStream = parallel ? serverElementStream.parallel() : serverElementStream;
        List<byte[]> serverStashPrfList = serverElementStream
            .map(x -> getStashPrf(x, stashIndex, lcotSenderOutput))
            .collect(Collectors.toList());
        Collections.shuffle(serverStashPrfList, secureRandom);
        Filter<byte[]> stashPrfFilter = FilterFactory.load(envType, filterType, serverElementSize, secureRandom);
        serverStashPrfList.forEach(stashPrfFilter::put);
        return stashPrfFilter.save();
    }

    private byte[] getStashPrf(T x, int stashIndex, LcotSenderOutput lcotSenderOutput) {
        byte[] xBytes = ObjectUtils.objectToByteArray(x);
        // OPRF(x || hashIndex)
        byte[] hx = ByteBuffer.allocate(xBytes.length + Integer.BYTES)
            .put(xBytes)
            .putInt(binNum + stashIndex)
            .array();
        hx = h1.digestToBytes(hx);
        byte[] prf = lcotSenderOutput.getRb(binNum + stashIndex, hx);
        prf = h1.digestToBytes(prf);
        return prf;
    }
}
