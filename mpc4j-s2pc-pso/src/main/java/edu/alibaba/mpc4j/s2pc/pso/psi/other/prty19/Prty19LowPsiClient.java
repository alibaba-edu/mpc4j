package edu.alibaba.mpc4j.s2pc.pso.psi.other.prty19;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory.CrhfType;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prp.Prp;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory;
import edu.alibaba.mpc4j.common.structure.filter.Filter;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvs;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.RotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;
import edu.alibaba.mpc4j.s2pc.pso.psi.AbstractPsiClient;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiUtils;
import edu.alibaba.mpc4j.s2pc.pso.psi.other.prty19.Prty19LowPsiPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * PRTY19-PSI (low communication) client.
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/08/17
 */
public class Prty19LowPsiClient<T> extends AbstractPsiClient<T> {
    /**
     * core COT sender
     */
    private final CoreCotSender coreCotSender;
    /**
     * OKVS type
     */
    private final Gf2eDokvsType okvsType;
    /**
     * OKVS key num
     */
    private final int okvsKeyNum;
    /**
     * OKVS keys
     */
    private byte[][] okvsKeys;
    /**
     * ys
     */
    private byte[][] ys;

    public Prty19LowPsiClient(Rpc clientRpc, Party serverParty, Prty19LowPsiConfig config) {
        super(Prty19LowPsiPtoDesc.getInstance(), clientRpc, serverParty, config);
        coreCotSender = CoreCotFactory.createSender(clientRpc, serverParty, config.getCoreCotConfig());
        addSubPto(coreCotSender);
        okvsType = config.getOkvsType();
        okvsKeyNum = Gf2eDokvsFactory.getHashKeyNum(okvsType);
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // init COT
        int maxL = Prty19PsiUtils.getLowL(maxServerElementSize);
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(delta);
        coreCotSender.init(delta, maxL);
        // generate and send OKVS keys
        okvsKeys = CommonUtils.generateRandomKeys(okvsKeyNum, secureRandom);
        List<byte[]> okvsKeyPayload = Arrays.stream(okvsKeys).collect(Collectors.toList());
        DataPacketHeader okvsKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_OKVS_KEY.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(okvsKeyHeader, okvsKeyPayload));
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Set<T> psi(Set<T> clientElementSet, int serverElementSize) throws MpcAbortException {
        setPtoInput(clientElementSet, serverElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // init PEQT hash
        int peqtByteLength = PsiUtils.getSemiHonestPeqtByteLength(serverElementSize, clientElementSize);
        Hash peqtHash = HashFactory.createInstance(envType, peqtByteLength);
        // init Finite field l
        int l = Prty19PsiUtils.getLowL(serverElementSize);
        int byteL = CommonUtils.getByteLength(l);
        int offsetL = byteL * Byte.SIZE - l;
        Prp[] tPrps = IntStream.range(0, l)
            .mapToObj(i -> PrpFactory.createInstance(envType))
            .toArray(Prp[]::new);
        Prp[] uPrps = IntStream.range(0, l)
            .mapToObj(i -> PrpFactory.createInstance(envType))
            .toArray(Prp[]::new);
        // init OKVS
        Gf2eDokvs<ByteBuffer> okvs = Gf2eDokvsFactory.createInstance(envType, okvsType, clientElementSize, l, okvsKeys);
        okvs.setParallelEncode(parallel);
        // init ys
        initClientElements();
        stopWatch.stop();
        long setupTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 4, setupTime, "Client setups parameters");

        stopWatch.start();
        // Alice and Bob invoke l instances of Random OT, Alice receives output q_1, ..., q_l
        CotSenderOutput cotSenderOutput = coreCotSender.send(l);
        RotSenderOutput rotSenderOutput = new RotSenderOutput(envType, CrhfType.MMO, cotSenderOutput);
        // init PRFs
        IntStream.range(0, l).forEach(i -> tPrps[i].setKey(rotSenderOutput.getR0(i)));
        IntStream.range(0, l).forEach(i -> uPrps[i].setKey(rotSenderOutput.getR1(i)));
        stopWatch.stop();
        long rotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, rotTime, "Client runs " + l + " Random OT");

        stopWatch.start();
        // Bob computes R(y) = T(y) ⊕ U(y)
        // where T(y) = F(t_1, y) || F(t_2, y) || ... || F(t_l, y), U(y) = F(u_1, y) || F(u_2, y) || ... || F(u_l, y)
        byte[][] ts = new byte[clientElementSize][];
        IntStream serverElementIntStream = IntStream.range(0, clientElementSize);
        serverElementIntStream = parallel ? serverElementIntStream.parallel() : serverElementIntStream;
        Map<ByteBuffer, byte[]> keyValueMap = serverElementIntStream
            .boxed()
            .collect(Collectors.toMap(
                index -> {
                    byte[] y = ys[index];
                    return ByteBuffer.wrap(y);
                },
                index -> {
                    byte[] y = ys[index];
                    // T(y) = F(t_1, y) || F(t_2, y) || ... || F(t_l, y)
                    ts[index] = new byte[byteL];
                    for (int i = 0; i < l; i++) {
                        boolean ti = (tPrps[i].prp(y)[0] & 0x01) != 0;
                        BinaryUtils.setBoolean(ts[index], offsetL + i, ti);
                    }
                    // U(y) = F(u_1, y) || F(u_2, y) || ... || F(u_l, y)
                    byte[] uy = new byte[byteL];
                    for (int i = 0; i < l; i++) {
                        boolean ui = (uPrps[i].prp(y)[0] & 0x01) != 0;
                        BinaryUtils.setBoolean(uy, offsetL + i, ui);
                    }
                    byte[] ry = BytesUtils.xor(ts[index], uy);
                    ts[index] = peqtHash.digestToBytes(ts[index]);
                    return ry;
                }
            ));
        // Bob computes a polynomial P
        byte[][] storage = okvs.encode(keyValueMap, false);
        List<byte[]> okvsPayload = Arrays.stream(storage).collect(Collectors.toList());
        DataPacketHeader okvsHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.CLIENT_SEND_OKVS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(okvsHeader, okvsPayload));
        ys = null;
        stopWatch.stop();
        long rsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 4, rsTime, "Client computes R(y)");

        DataPacketHeader serverPrfHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_PRFS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> serverPrfsPayload = rpc.receive(serverPrfHeader).getPayload();

        stopWatch.start();
        Filter<byte[]> serverPrfFilter = FilterFactory.load(envType, serverPrfsPayload);
        Set<T> intersection = IntStream.range(0, clientElementSize)
            .mapToObj(index -> {
                T element = clientElementArrayList.get(index);
                return serverPrfFilter.mightContain(ts[index]) ? element : null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        stopWatch.stop();
        long serverPrfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 4, serverPrfTime, "Client computes intersection");

        logPhaseInfo(PtoState.PTO_END);
        return intersection;
    }

    private void initClientElements() {
        ys = new byte[clientElementSize][];
        Prf elementPrf = PrfFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH);
        elementPrf.setKey(new byte[CommonConstants.BLOCK_BYTE_LENGTH]);
        IntStream elementIndexIntStream = IntStream.range(0, clientElementSize);
        elementIndexIntStream = parallel ? elementIndexIntStream.parallel() : elementIndexIntStream;
        elementIndexIntStream.forEach(index -> {
            ys[index] = ObjectUtils.objectToByteArray(clientElementArrayList.get(index));
            ys[index] = elementPrf.getBytes(ys[index]);
        });
    }
}
