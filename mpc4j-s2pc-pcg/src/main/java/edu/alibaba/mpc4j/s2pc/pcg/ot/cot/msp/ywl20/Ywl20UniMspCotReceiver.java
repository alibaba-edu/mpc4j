package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.ywl20;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.SimpleIntHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntNoStashCuckooHashBin;
import edu.alibaba.mpc4j.common.tool.hashbin.primitive.cuckoo.IntCuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.AbstractMspCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.ywl20.Ywl20UniMspCotPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotReceiverOutput;

/**
 * YWL20-UNI-MSP-COT协议接收方。
 *
 * @author Weiran Liu
 * @date 2022/01/24
 */
public class Ywl20UniMspCotReceiver extends AbstractMspCotReceiver {
    /**
     * 哈希函数数量
     */
    private static final int HASH_NUM = IntCuckooHashBinFactory.getHashNum(Ywl20UniMspCotUtils.INT_CUCKOO_HASH_BIN_TYPE);
    /**
     * BSP-COT协议接收方
     */
    private final BspCotReceiver bspCotReceiver;
    /**
     * 预计算接收方输出
     */
    private CotReceiverOutput cotReceiverOutput;
    /**
     * 系数索引值数组
     */
    private int[] targetArray;
    /**
     * 哈希密钥
     */
    private byte[][] keys;
    /**
     * 布谷鸟哈希桶
     */
    private IntNoStashCuckooHashBin intNoStashCuckooHashBin;
    /**
     * 哈希桶
     */
    private SimpleIntHashBin intHashBin;
    /**
     * 索引值位置映射
     */
    private ArrayList<Map<Integer, Integer>> positionMaps;
    /**
     * BSP-COT协议接收方输出
     */
    private BspCotReceiverOutput bspCotReceiverOutput;

    public Ywl20UniMspCotReceiver(Rpc senderRpc, Party receiverParty, Ywl20UniMspCotConfig config) {
        super(Ywl20UniMspCotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        bspCotReceiver = BspCotFactory.createReceiver(senderRpc, receiverParty, config.getBspCotConfig());
        bspCotReceiver.addLogLevel();
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        bspCotReceiver.setTaskId(taskId);
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        bspCotReceiver.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        bspCotReceiver.addLogLevel();
    }

    @Override
    public void init(int maxT, int maxNum) throws MpcAbortException {
        setInitInput(maxT, maxNum);
        info("{}{} Recv. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        int maxBinNum = IntCuckooHashBinFactory.getBinNum(Ywl20UniMspCotUtils.INT_CUCKOO_HASH_BIN_TYPE, maxT);
        // 原本应该设置为maxBinSize，但此值不与T成正比，最后考虑直接设置为HASH_NUM * maxN
        bspCotReceiver.init(maxBinNum, HASH_NUM * (maxNum + 1));
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Recv. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public MspCotReceiverOutput receive(int t, int num) throws MpcAbortException {
        setPtoInput(t, num);
        return receive();
    }

    @Override
    public MspCotReceiverOutput receive(int t, int num, CotReceiverOutput preReceiverOutput) throws MpcAbortException {
        setPtoInput(t, num, preReceiverOutput);
        cotReceiverOutput = preReceiverOutput;
        return receive();
    }

    private MspCotReceiverOutput receive() throws MpcAbortException {
        info("{}{} Recv. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        List<byte[]> hashKeysPayload = generateHashKeysPayload();
        DataPacketHeader hashKeysHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(hashKeysHeader, hashKeysPayload));
        int[] targetArray = generateTargetArray();
        stopWatch.stop();
        long cuckooHashBinTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 1/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), cuckooHashBinTime);

        stopWatch.start();
        // R sends (sp-extend, |B_j| + 1, p_j) to F_{SPCOT}
        if (cotReceiverOutput == null) {
            bspCotReceiverOutput = bspCotReceiver.receive(targetArray, intHashBin.maxBinSize() + 1);
        } else {
            bspCotReceiverOutput = bspCotReceiver.receive(targetArray, intHashBin.maxBinSize() + 1, cotReceiverOutput);
            cotReceiverOutput = null;
        }
        stopWatch.stop();
        long bspcotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 2/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), bspcotTime);

        stopWatch.start();
        // 计算输出结果
        MspCotReceiverOutput receiverOutput = generateReceiverOutput();
        stopWatch.stop();
        long outputTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 4/4 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), outputTime);

        info("{}{} Recv. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return receiverOutput;
    }

    private List<byte[]> generateHashKeysPayload() {
        // 生成稀疏数组
        Set<Integer> itemSet = new HashSet<>(t);
        while (itemSet.size() < t) {
            itemSet.add(secureRandom.nextInt(num));
        }
        targetArray = itemSet.stream().mapToInt(item -> item).toArray();
        boolean success = false;
        // 重复插入，直到成功
        while (!success) {
            try {
                keys = CommonUtils.generateRandomKeys(HASH_NUM, secureRandom);
                intNoStashCuckooHashBin = IntCuckooHashBinFactory.createInstance(
                    envType, Ywl20UniMspCotUtils.INT_CUCKOO_HASH_BIN_TYPE, targetArray.length, keys
                );
                // R inserts α_0,...,α_{t − 1} into a Cuckoo hash table T of size m
                intNoStashCuckooHashBin.insertItems(targetArray);
                success = true;
            } catch (ArithmeticException ignored) {

            }
        }
        return Arrays.stream(keys).collect(Collectors.toList());
    }

    private int[] generateTargetArray() {
        // R independently builds m buckets {B_j}_{j ∈ [m]} with B_j = {x ∈ [n] | ∃i ∈ [τ]: h_i(x) = j}
        int m = intNoStashCuckooHashBin.binNum();
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
        // For each j ∈ [m]
        return IntStream.range(0, m)
            .map(binIndex -> {
                int tj = intNoStashCuckooHashBin.getBinEntry(binIndex);
                if (tj < 0) {
                    // R sets p_j = |B_j| + 1 if T[j] = ⊥
                    return intHashBin.maxBinSize();
                } else {
                    // R sets p_j = pos_j(T[j]) otherwise.
                    return positionMaps.get(binIndex).get(tj);
                }
            })
            .toArray();
    }

    private MspCotReceiverOutput generateReceiverOutput() {
        // For each x ∈ [n]
        IntStream nIntStream = IntStream.range(0, num);
        nIntStream = parallel ? nIntStream.parallel() : nIntStream;
        byte[][] rbArray = nIntStream
            .mapToObj(x -> {
                // R computes r[x] = Σ_{i ∈ [τ]} r_{h_i(x)}[pos_{h_i(x)}(x)]
                int[] binIndexes = Arrays.stream(intHashBin.getItemBinIndexes(x)).distinct().toArray();
                byte[] rx = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                for (int binIndex : binIndexes) {
                    BytesUtils.xori(rx, bspCotReceiverOutput.get(binIndex).getRb(positionMaps.get(binIndex).get(x)));
                }
                return rx;
            })
            .toArray(byte[][]::new);
        intHashBin = null;
        positionMaps = null;
        bspCotReceiverOutput = null;
        return MspCotReceiverOutput.create(targetArray, rbArray);
    }
}
