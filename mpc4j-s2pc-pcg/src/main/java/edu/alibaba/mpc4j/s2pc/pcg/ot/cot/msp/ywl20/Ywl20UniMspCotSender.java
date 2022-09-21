package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.ywl20;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.SimpleIntHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntCuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.AbstractMspCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotSenderOutput;

/**
 * YWL20-UNI-MSP-COT协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/01/22
 */
public class Ywl20UniMspCotSender extends AbstractMspCotSender {
    /**
     * 哈希函数数量
     */
    private static final int HASH_NUM = IntCuckooHashBinFactory.getHashNum(Ywl20UniMspCotUtils.INT_CUCKOO_HASH_BIN_TYPE);
    /**
     * BSP-COT协议发送方
     */
    private final BspCotSender bspCotSender;
    /**
     * 预计算发送方输出
     */
    private CotSenderOutput cotSenderOutput;
    /**
     * 桶数量
     */
    private int m;
    /**
     * 哈希桶
     */
    private SimpleIntHashBin intHashBin;
    /**
     * 索引值位置映射
     */
    private ArrayList<Map<Integer, Integer>> positionMaps;
    /**
     * BSP-COT协议发送方输出
     */
    private BspCotSenderOutput bspCotSenderOutput;

    public Ywl20UniMspCotSender(Rpc senderRpc, Party receiverParty, Ywl20UniMspCotConfig config) {
        super(Ywl20UniMspCotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        bspCotSender = BspCotFactory.createSender(senderRpc, receiverParty, config.getBspCotConfig());
        bspCotSender.addLogLevel();
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        bspCotSender.setTaskId(taskId);
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        bspCotSender.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        bspCotSender.addLogLevel();
    }

    @Override
    public void init(byte[] delta, int maxT, int maxNum) throws MpcAbortException {
        setInitInput(delta, maxT, maxNum);
        info("{}{} Send. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        int maxBinNum = IntCuckooHashBinFactory.getBinNum(Ywl20UniMspCotUtils.INT_CUCKOO_HASH_BIN_TYPE, maxT);
        // 原本应该设置为maxBinSize，但此值不与T成正比，最后考虑直接设置为HASH_NUM * maxN
        bspCotSender.init(delta, maxBinNum, HASH_NUM * (maxNum + 1));
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Send. Init end", ptoBeginLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public MspCotSenderOutput send(int t, int num) throws MpcAbortException {
        setPtoInput(t, num);
        return send();
    }

    @Override
    public MspCotSenderOutput send(int t, int num, CotSenderOutput preSenderOutput) throws MpcAbortException {
        setPtoInput(t, num, preSenderOutput);
        cotSenderOutput = preSenderOutput;
        return send();
    }

    private MspCotSenderOutput send() throws MpcAbortException {
        info("{}{} Send. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        // 接收哈希桶密钥，设置哈希桶
        DataPacketHeader hashKeysHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Ywl20UniMspCotPtoDesc.PtoStep.RECEIVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> hashKeysPayload = rpc.receive(hashKeysHeader).getPayload();
        handleHashKeysPayload(hashKeysPayload);
        stopWatch.stop();
        long hashBinTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 1/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), hashBinTime);

        stopWatch.start();
        // S sends (sp-extend, |B_j| + 1) to F_{SPCOT}
        if (cotSenderOutput == null) {
            bspCotSenderOutput = bspCotSender.send(m, intHashBin.maxBinSize() + 1);
        } else {
            bspCotSenderOutput = bspCotSender.send(m, intHashBin.maxBinSize() + 1, cotSenderOutput);
            cotSenderOutput = null;
        }
        stopWatch.stop();
        long bspcotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 2/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), bspcotTime);

        stopWatch.start();
        // 计算输出结果
        MspCotSenderOutput senderOutput = generateSenderOutput();
        stopWatch.stop();
        long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 3/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), outputTime);

        info("{}{} Send. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return senderOutput;
    }

    private void handleHashKeysPayload(List<byte[]> hashKeysPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(hashKeysPayload.size() == HASH_NUM);
        // S independently builds m buckets {B_j}_{j ∈ [m]} with B_j = {x ∈ [n] | ∃i ∈ [τ]: h_i(x) = j}
        m = IntCuckooHashBinFactory.getBinNum(Ywl20UniMspCotUtils.INT_CUCKOO_HASH_BIN_TYPE, t);
        byte[][] keys = hashKeysPayload.toArray(new byte[0][]);
        // Initialize m empty buckets {B_j}_{j ∈ [m]}.
        intHashBin = new SimpleIntHashBin(envType, m, num, keys);
        // For each x ∈ [n], i ∈ [τ], compute j := h_i(x) and add x into bucket B_j.
        int[] xs = IntStream.range(0, num).toArray();
        intHashBin.insertItems(xs);
        positionMaps = IntStream.range(0, m)
            .mapToObj(binIndex -> {
                // Sort all values in each bucket in an increasing order.
                int[] bin = Arrays.stream(intHashBin.getBin(binIndex))
                    .filter(item -> item >= 0)
                    .distinct()
                    .sorted()
                    .toArray();
                // Define a function pos_j : B_j → [|Bj|] to map a value into its position in the j-th bucket B_j.
                Map<Integer, Integer> positionMap = new HashMap<>(bin.length);
                for (int position = 0; position < bin.length; position++) {
                    positionMap.put(bin[position], position);
                }
                return positionMap;
            })
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private MspCotSenderOutput generateSenderOutput() {
        // For each x ∈ [n]
        IntStream nIntStream = IntStream.range(0, num);
        nIntStream = parallel ? nIntStream.parallel() : nIntStream;
        byte[][] r0Array = nIntStream
            .mapToObj(x -> {
                // S computes s[x] = Σ_{i ∈ [τ]} s_{h_i(x)}[pos_{h_i(x)}(x)]
                int[] binIndexes = Arrays.stream(intHashBin.getItemBinIndexes(x)).distinct().toArray();
                byte[] sx = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                for (int binIndex : binIndexes) {
                    BytesUtils.xori(sx, bspCotSenderOutput.get(binIndex).getR0(positionMaps.get(binIndex).get(x)));
                }
                return sx;
            })
            .toArray(byte[][]::new);
        intHashBin = null;
        positionMaps = null;
        bspCotSenderOutput = null;
        return MspCotSenderOutput.create(delta, r0Array);
    }
}
