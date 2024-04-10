package edu.alibaba.mpc4j.s2pc.pjc.pmid.zcl22;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvs;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.s2pc.opf.oprf.*;
import edu.alibaba.mpc4j.s2pc.pjc.pmid.AbstractPmidClient;
import edu.alibaba.mpc4j.s2pc.pjc.pmid.PmidPartyOutput;
import edu.alibaba.mpc4j.s2pc.pjc.pmid.PmidUtils;
import edu.alibaba.mpc4j.s2pc.pjc.pmid.zcl22.Zcl22MpPmidPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuClient;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory;
import edu.alibaba.mpc4j.common.tool.utils.*;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * 多点ZCL22PMID协议客户端。
 *
 * @author Weiran Liu
 * @date 2022/5/10
 */
public class Zcl22MpPmidClient<T> extends AbstractPmidClient<T> {
    /**
     * 多点OPRF接收方
     */
    private final MpOprfReceiver mpOprfReceiver;
    /**
     * 多点OPRF发送方
     */
    private final MpOprfSender mpOprfSender;
    /**
     * PSU客户端
     */
    private final PsuClient psuClient;
    /**
     * σ的OKVS类型
     */
    private final Gf2eDokvsType sigmaOkvsType;
    /**
     * 客户端σ的OKVS密钥
     */
    private byte[][] clientSigmaOkvsHashKeys;
    /**
     * 服务端σ的OKVS密钥
     */
    private byte[][] serverSigmaOkvsHashKeys;
    /**
     * PMID字节长度
     */
    private int pmidByteLength;
    /**
     * PMID映射函数
     */
    private Hash pmidMap;
    /**
     * σ的OKVS值字节长度
     */
    private int sigmaOkvsValueByteLength;
    /**
     * σ的OKVS值映射函数
     */
    private Hash sigmaOkvsValueMap;
    /**
     * {F_{k_A}(y) | y ∈ Y'}
     */
    private MpOprfReceiverOutput kaMpOprfOutput;
    /**
     * k_B
     */
    private MpOprfSenderOutput kbMpOprfKey;
    /**
     * 客户端元素字节数组
     */
    private byte[][] clientElementByteArrays;
    /**
     * dy
     */
    private int[] dyArray;

    public Zcl22MpPmidClient(Rpc serverRpc, Party clientParty, Zcl22MpPmidConfig config) {
        super(Zcl22MpPmidPtoDesc.getInstance(), serverRpc, clientParty, config);
        mpOprfReceiver = OprfFactory.createMpOprfReceiver(serverRpc, clientParty, config.getMpOprfConfig());
        addSubPto(mpOprfReceiver);
        mpOprfSender = OprfFactory.createMpOprfSender(serverRpc, clientParty, config.getMpOprfConfig());
        addSubPto(mpOprfSender);
        psuClient = PsuFactory.createClient(serverRpc, clientParty, config.getPsuConfig());
        addSubPto(psuClient);
        sigmaOkvsType = config.getSigmaOkvsType();
    }

    @Override
    public void init(int maxClientSetSize, int maxClientU, int maxServerSetSize, int maxServerU) throws MpcAbortException {
        setInitInput(maxClientSetSize, maxClientU, maxServerSetSize, maxServerU);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        mpOprfReceiver.init(maxClientSetSize);
        mpOprfSender.init(maxServerSetSize);
        psuClient.init(maxClientSetSize * maxServerU * maxClientU, maxServerSetSize * maxServerU * maxClientU);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 3, initTime);

        stopWatch.start();
        List<byte[]> clientKeysPayload = new LinkedList<>();
        int okvsHashKeyNum = Gf2eDokvsFactory.getHashKeyNum(sigmaOkvsType);
        // 客户端OKVS密钥，由客户端生成
        clientSigmaOkvsHashKeys = IntStream.range(0, okvsHashKeyNum)
            .mapToObj(keyIndex -> {
                byte[] clientSigmaOkvsHashKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                secureRandom.nextBytes(clientSigmaOkvsHashKey);
                clientKeysPayload.add(clientSigmaOkvsHashKey);
                return clientSigmaOkvsHashKey;
            })
            .toArray(byte[][]::new);
        DataPacketHeader clientKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_KEYS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientKeysHeader, clientKeysPayload));
        stopWatch.stop();
        long clientKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 3, clientKeyTime);

        stopWatch.start();
        DataPacketHeader serverKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> serverKeysPayload = rpc.receive(serverKeysHeader).getPayload();
        MpcAbortPreconditions.checkArgument(serverKeysPayload.size() == okvsHashKeyNum);
        serverSigmaOkvsHashKeys = serverKeysPayload.toArray(new byte[0][]);
        stopWatch.stop();
        long serverKeyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 3, serverKeyTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public PmidPartyOutput<T> pmid(Set<T> clientElementSet, int serverSetSize) throws MpcAbortException {
        setPtoInput(clientElementSet, serverSetSize);
        return pmid();
    }

    @Override
    public PmidPartyOutput<T> pmid(Map<T, Integer> clientElementMap, int serverSetSize) throws MpcAbortException {
        setPtoInput(clientElementMap, serverSetSize);
        return pmid();
    }

    @Override
    public PmidPartyOutput<T> pmid(Set<T> clientElementSet, int serverSetSize, int serverU) throws MpcAbortException {
        setPtoInput(clientElementSet, serverSetSize, serverU);
        return pmid();
    }

    @Override
    public PmidPartyOutput<T> pmid(Map<T, Integer> clientElementMap, int serverSetSize, int serverU) throws MpcAbortException {
        setPtoInput(clientElementMap, serverSetSize, serverU);
        return pmid();
    }

    private PmidPartyOutput<T> pmid() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        initVariables();
        oprf();
        stopWatch.stop();
        long oprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 4, oprfTime);

        stopWatch.start();
        if (serverU == 1 && clientU == 1) {
            serverEmptySigmaOkvs();
        } else if (serverU == 1) {
            clientSigmaOkvs();
            serverEmptySigmaOkvs();
        } else if (clientU == 1) {
            serverSigmaOkvs();
        } else {
            clientSigmaOkvs();
            serverSigmaOkvs();
        }
        stopWatch.stop();
        long sigmaOkvsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, sigmaOkvsTime);

        stopWatch.start();
        // Bob computes id_{y_i}^(j)
        Map<ByteBuffer, T> clientPmidMap = generateClientPmidMap();
        stopWatch.stop();
        long pmidMapTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 4, pmidMapTime);

        stopWatch.start();
        Set<ByteBuffer> pmidSet = union(clientPmidMap);
        stopWatch.stop();
        long unionTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 4, unionTime);

        logPhaseInfo(PtoState.PTO_END);
        return new PmidPartyOutput<>(pmidByteLength, pmidSet, clientPmidMap);
    }

    private void initVariables() {
        pmidByteLength = PmidUtils.getPmidByteLength(serverSetSize, serverU, clientSetSize, clientU);
        pmidMap = HashFactory.createInstance(envType, pmidByteLength);
        sigmaOkvsValueByteLength = Zcl22PmidUtils.getSigmaOkvsValueByteLength(
            serverSetSize, serverU, clientSetSize, clientU
        );
        sigmaOkvsValueMap = HashFactory.createInstance(envType, sigmaOkvsValueByteLength);
        clientElementByteArrays = clientElementArrayList.stream()
            .map(ObjectUtils::objectToByteArray)
            .toArray(byte[][]::new);
    }

    private void oprf() throws MpcAbortException {
        // Alice and Bob invoke the OPRF functionality F_{oprf}.
        // Bob acts as receiver with input Y ′ and receives {F_{k_A}(y) | y ∈ Y'}.
        kaMpOprfOutput = mpOprfReceiver.oprf(clientElementByteArrays);
        // Alice and Bob invoke another OPRF functionality F_{oprf}.
        // Bob acts as sender and receives a PRF key k_B
        kbMpOprfKey = mpOprfSender.oprf(serverSetSize);
    }

    private void clientSigmaOkvs() {
        // Bob defines q_{y_i}^B = H(F_{k_B}(y_i) || 0) for i ∈ [n]
        Stream<byte[]> clientElementByteArrayStream = Arrays.stream(clientElementByteArrays);
        clientElementByteArrayStream = parallel ? clientElementByteArrayStream.parallel() : clientElementByteArrayStream;
        // q_{y_i}^B = H(F_{k_B}(y_i) || 0) for i ∈ [m]
        final byte[][] qykbArray = clientElementByteArrayStream
            .map(y -> {
                byte[] fykb = kbMpOprfKey.getPrf(y);
                return ByteBuffer.allocate(fykb.length + Integer.BYTES).put(fykb).putInt(0).array();
            })
            .map(sigmaOkvsValueMap::digestToBytes)
            .toArray(byte[][]::new);
        // Bob computes an σ-OKVS D^B
        IntStream clientElementIndexStream = IntStream.range(0, clientSetSize);
        clientElementIndexStream = parallel ? clientElementIndexStream.parallel() : clientElementIndexStream;
        Map<ByteBuffer, byte[]> clientSigmaOkvsKeyValueMap = clientElementIndexStream
            .boxed()
            .collect(Collectors.toMap(
                index -> {
                    byte[] y = clientElementByteArrays[index];
                    return ByteBuffer.wrap(sigmaOkvsValueMap.digestToBytes(y));
                },
                index -> {
                    T y = clientElementArrayList.get(index);
                    int uy = clientElementMap.get(y);
                    byte[] dy;
                    if (uy == 1) {
                        // if u_j = 1, Bob selects a random c_j ← {0, 1}^σ
                        dy = new byte[sigmaOkvsValueByteLength];
                        secureRandom.nextBytes(dy);
                    } else {
                        // else defines c_j = u_j
                        dy = IntUtils.nonNegIntToFixedByteArray(uy, sigmaOkvsValueByteLength);
                    }
                    BytesUtils.xori(dy, qykbArray[index]);
                    return dy;
                }
            ));
        Gf2eDokvs<ByteBuffer> clientSigmaOkvs = Gf2eDokvsFactory.createInstance(
            envType, sigmaOkvsType, clientSetSize, sigmaOkvsValueByteLength * Byte.SIZE, clientSigmaOkvsHashKeys
        );
        // σ的OKVS编码可以并行处理
        clientSigmaOkvs.setParallelEncode(parallel);
        byte[][] clientSigmaOkvsStorage = clientSigmaOkvs.encode(clientSigmaOkvsKeyValueMap, false);
        List<byte[]> clientSigmaOkvsPayload = Arrays.stream(clientSigmaOkvsStorage).collect(Collectors.toList());
        DataPacketHeader clientSigmaOkvsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_SIGMA_OKVS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientSigmaOkvsHeader, clientSigmaOkvsPayload));
    }

    private void serverEmptySigmaOkvs() {
        // 服务端没有重数，设置dy = 1
        dyArray = new int[clientSetSize];
        Arrays.fill(dyArray, 1);
    }

    private void serverSigmaOkvs() throws MpcAbortException {
        // q_{y_i}^A = H(F_{k_A}(x_i) || 0) for i ∈ [n]
        IntStream clientElementIndexStream = IntStream.range(0, clientSetSize);
        clientElementIndexStream = parallel ? clientElementIndexStream.parallel() : clientElementIndexStream;
        final byte[][] qykaArray = clientElementIndexStream
            .mapToObj(index -> {
                byte[] fyka = kaMpOprfOutput.getPrf(index);
                return ByteBuffer.allocate(fyka.length + Integer.BYTES).put(fyka).putInt(0).array();
            })
            .map(sigmaOkvsValueMap::digestToBytes)
            .toArray(byte[][]::new);
        // Bob receives σ-OKVS D^A
        DataPacketHeader serverSigmaOkvsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_SIGMA_OKVS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> serverSigmaOkvsPayload = rpc.receive(serverSigmaOkvsHeader).getPayload();
        int serverSigmaOkvsM = Gf2eDokvsFactory.getM(envType, sigmaOkvsType, serverSetSize);
        MpcAbortPreconditions.checkArgument(serverSigmaOkvsPayload.size() == serverSigmaOkvsM);
        // 读取服务端σ的OKVS
        byte[][] serverSigmaOkvsStorage = serverSigmaOkvsPayload.toArray(new byte[0][]);
        Gf2eDokvs<ByteBuffer> serverSigmaOkvs = Gf2eDokvsFactory.createInstance(
            envType, sigmaOkvsType, serverSetSize, sigmaOkvsValueByteLength * Byte.SIZE, serverSigmaOkvsHashKeys
        );
        dyArray = new int[clientSetSize];
        BigInteger serverBigIntegerU = BigInteger.valueOf(serverU);
        clientElementIndexStream = IntStream.range(0, clientSetSize);
        clientElementIndexStream = parallel ? clientElementIndexStream.parallel() : clientElementIndexStream;
        clientElementIndexStream.forEach(index -> {
            // Bob computes d_y = q_{y_i}^A ⊕ Decode_H(D, y_i) for i ∈ [n].
            byte[] y = clientElementByteArrays[index];
            ByteBuffer yi = ByteBuffer.wrap(sigmaOkvsValueMap.digestToBytes(y));
            byte[] dyBytes = serverSigmaOkvs.decode(serverSigmaOkvsStorage, yi);
            BytesUtils.xori(dyBytes, qykaArray[index]);
            BigInteger dyBigInteger = BigIntegerUtils.byteArrayToNonNegBigInteger(dyBytes);
            if (BigIntegerUtils.lessOrEqual(dyBigInteger, serverBigIntegerU) && BigIntegerUtils.greater(dyBigInteger, BigInteger.ONE)) {
                // If 1 < d_i ≤ serverU, set d_i = d_i
                dyArray[index] = dyBigInteger.intValue();
            } else {
                // else, set d_i = 1
                dyArray[index] = 1;
            }
        });
    }

    private Map<ByteBuffer, T> generateClientPmidMap() {
        // 构建客户端PmidMap
        Map<ByteBuffer, T> clientPmidMap = new ConcurrentHashMap<>(clientSetSize * serverU * clientU);
        IntStream clientElementIndexStream = IntStream.range(0, clientSetSize);
        clientElementIndexStream = parallel ? clientElementIndexStream.parallel() : clientElementIndexStream;
        clientElementIndexStream.forEach(index -> {
            T y = clientElementArrayList.get(index);
            byte[] fyka = kaMpOprfOutput.getPrf(index);
            byte[] fykb = kbMpOprfKey.getPrf(clientElementByteArrays[index]);
            for (int j = 1; j <= clientElementMap.get(y) * dyArray[index]; j++) {
                byte[] extendPmid0 = ByteBuffer.allocate(fyka.length + Integer.BYTES)
                    .put(fyka)
                    .put(IntUtils.intToByteArray(j))
                    .array();
                extendPmid0 = pmidMap.digestToBytes(extendPmid0);
                byte[] extendPmid1 = ByteBuffer.allocate(fykb.length + Integer.BYTES)
                    .put(fykb)
                    .put(IntUtils.intToByteArray(j))
                    .array();
                extendPmid1 = pmidMap.digestToBytes(extendPmid1);
                BytesUtils.xori(extendPmid0, extendPmid1);
                clientPmidMap.put(ByteBuffer.wrap(extendPmid0), y);
            }
        });
        clientElementByteArrays = null;
        kaMpOprfOutput = null;
        kbMpOprfKey = null;
        dyArray = null;
        return clientPmidMap;
    }

    private Set<ByteBuffer> union(Map<ByteBuffer, T> clientPmidMap) throws MpcAbortException {
        // 双方同步对方PSU的元素数量
        List<byte[]> clientPsuSetSizePayload = new LinkedList<>();
        clientPsuSetSizePayload.add(IntUtils.intToByteArray(clientPmidMap.size()));
        DataPacketHeader clientPsuSetSizeHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PSU_SET_SIZE.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientPsuSetSizeHeader, clientPsuSetSizePayload));

        DataPacketHeader serverPsuSetSizeHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_PSU_SET_SIZE.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> serverPsuSetSizePayload = rpc.receive(serverPsuSetSizeHeader).getPayload();
        MpcAbortPreconditions.checkArgument(serverPsuSetSizePayload.size() == 1);
        int serverPsuSetSize = IntUtils.byteArrayToInt(serverPsuSetSizePayload.remove(0));
        // Alice and Bob invoke the PSU functionality F_{psu}. Bob acts as receiver with input ID_y and receives the union
        Set<ByteBuffer> pmidSet = psuClient.psu(clientPmidMap.keySet(), serverPsuSetSize, pmidByteLength);
        // Bob sends union
        List<byte[]> unionPayload = pmidSet.stream().map(ByteBuffer::array).collect(Collectors.toList());
        DataPacketHeader unionHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_UNION.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(unionHeader, unionPayload));
        return pmidSet;
    }
}
