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
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvs;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.RotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotReceiver;
import edu.alibaba.mpc4j.s2pc.pso.psi.AbstractPsiServer;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiUtils;
import edu.alibaba.mpc4j.s2pc.pso.psi.other.prty19.Prty19LowPsiPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * PRTY19-PSI (low communication) server.
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/08/17
 */
public class Prty19LowPsiServer<T> extends AbstractPsiServer<T> {
    /**
     * core COT receiver
     */
    private final CoreCotReceiver coreCotReceiver;
    /**
     * OKVS type
     */
    private final Gf2eDokvsType okvsType;
    /**
     * OKVS key num
     */
    private final int okvsKeyNum;
    /**
     * filter type
     */
    private final FilterType filterType;
    /**
     * OKVS keys
     */
    private byte[][] okvsKeys;
    /**
     * xs
     */
    private byte[][] xs;

    public Prty19LowPsiServer(Rpc serverRpc, Party clientParty, Prty19LowPsiConfig config) {
        super(Prty19LowPsiPtoDesc.getInstance(), serverRpc, clientParty, config);
        coreCotReceiver = CoreCotFactory.createReceiver(serverRpc, clientParty, config.getCoreCotConfig());
        addSubPto(coreCotReceiver);
        okvsType = config.getOkvsType();
        okvsKeyNum = Gf2eDokvsFactory.getHashKeyNum(okvsType);
        filterType = config.getFilterType();
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // init COT
        int maxL = Prty19PsiUtils.getLowL(maxServerElementSize);
        coreCotReceiver.init(maxL);
        // receive OKVS keys
        DataPacketHeader okvsKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_OKVS_KEY.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> okvsKeyPayload = rpc.receive(okvsKeyHeader).getPayload();
        MpcAbortPreconditions.checkArgument(okvsKeyPayload.size() == okvsKeyNum);
        okvsKeys = okvsKeyPayload.toArray(new byte[0][]);
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
        // init PEQT hash
        int peqtByteLength = PsiUtils.getSemiHonestPeqtByteLength(serverElementSize, clientElementSize);
        Hash peqtHash = HashFactory.createInstance(envType, peqtByteLength);
        // init Finite field l
        int l = Prty19PsiUtils.getLowL(serverElementSize);
        int byteL = CommonUtils.getByteLength(l);
        int offsetL = byteL * Byte.SIZE - l;
        Prp[] qPrps = IntStream.range(0, l)
            .mapToObj(i -> PrpFactory.createInstance(envType))
            .toArray(Prp[]::new);
        boolean[] s = new boolean[l];
        IntStream.range(0, l).forEach(i -> s[i] = secureRandom.nextBoolean());
        // Δ = s_1 || ... || s_l
        byte[] delta = BinaryUtils.binaryToRoundByteArray(s);
        // init OKVS
        Gf2eDokvs<ByteBuffer> okvs = Gf2eDokvsFactory.createInstance(envType, okvsType, clientElementSize, l, okvsKeys);
        okvs.setParallelEncode(parallel);
        // init xs
        initServerElements();
        stopWatch.stop();
        long setupTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 4, setupTime, "Server setups parameters");

        stopWatch.start();
        // Alice and Bob invoke l instances of Random OT, Alice receives output q_1, ..., q_l
        CotReceiverOutput cotReceiverOutput = coreCotReceiver.receive(s);
        RotReceiverOutput rotReceiverOutput = new RotReceiverOutput(envType, CrhfType.MMO, cotReceiverOutput);
        // init PRFs
        IntStream.range(0, l).forEach(i -> qPrps[i].setKey(rotReceiverOutput.getRb(i)));
        stopWatch.stop();
        long rotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, rotTime, "Server runs " + l + " Random OT");

        stopWatch.start();
        // Alice computes Q(x) = F(q_1, x) || F(q_2, x) || ... || F(q_l, x)
        IntStream serverElementIntStream = IntStream.range(0, serverElementSize);
        serverElementIntStream = parallel ? serverElementIntStream.parallel() : serverElementIntStream;
        byte[][] qs = serverElementIntStream
            .mapToObj(index -> {
                byte[] x = xs[index];
                byte[] qx = new byte[byteL];
                for (int i = 0; i < l; i++) {
                    boolean qi = (qPrps[i].prp(x)[0] & 0x01) != 0;
                    BinaryUtils.setBoolean(qx, offsetL + i, qi);
                }
                return qx;
            })
            .toArray(byte[][]::new);
        stopWatch.stop();
        long qsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 4, qsTime, "Server computes Q(x)");

        DataPacketHeader okvsHeader = new DataPacketHeader(
            this.encodeTaskId, ptoDesc.getPtoId(), PtoStep.CLIENT_SEND_OKVS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> okvsPayload = rpc.receive(okvsHeader).getPayload();

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(okvsPayload.size() == Gf2eDokvsFactory.getM(envType, okvsType, clientElementSize));
        byte[][] storage = okvsPayload.toArray(new byte[0][]);
        // Server computes O = {H(Q(x) ⊕ s · P(x)) | x ∈ X}
        serverElementIntStream = IntStream.range(0, serverElementSize);
        serverElementIntStream = parallel ? serverElementIntStream.parallel() : serverElementIntStream;
        List<byte[]> serverElementPrfs = serverElementIntStream
            .mapToObj(index -> {
                byte[] x = xs[index];
                // P(x)
                byte[] prf = okvs.decode(storage, ByteBuffer.wrap(x));
                // s · P(x)
                BytesUtils.andi(prf, delta);
                // Q(x) ⊕ s · P(x)
                BytesUtils.xori(prf, qs[index]);
                return peqtHash.digestToBytes(prf);
            })
            .collect(Collectors.toList());
        // randomly permuted
        Collections.shuffle(serverElementPrfs, secureRandom);
        // create filter
        Filter<byte[]> filter = FilterFactory.load(envType, filterType, serverElementSize, secureRandom);
        serverElementPrfs.forEach(filter::put);
        List<byte[]> serverPrfsPayload = filter.save();
        DataPacketHeader serverPrfsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_PRFS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverPrfsHeader, serverPrfsPayload));
        xs = null;
        stopWatch.stop();
        long serverPrfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 4, serverPrfTime, "Server computes PRF");

        logPhaseInfo(PtoState.PTO_END);
    }

    private void initServerElements() {
        xs = new byte[serverElementSize][];
        Prf elementPrf = PrfFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH);
        elementPrf.setKey(new byte[CommonConstants.BLOCK_BYTE_LENGTH]);
        IntStream elementIndexIntStream = IntStream.range(0, serverElementSize);
        elementIndexIntStream = parallel ? elementIndexIntStream.parallel() : elementIndexIntStream;
        elementIndexIntStream.forEach(index -> {
            xs[index] = ObjectUtils.objectToByteArray(serverElementArrayList.get(index));
            xs[index] = elementPrf.getBytes(xs[index]);
        });
    }
}
