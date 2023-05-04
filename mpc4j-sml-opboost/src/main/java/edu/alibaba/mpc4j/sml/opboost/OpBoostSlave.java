package edu.alibaba.mpc4j.sml.opboost;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPto;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.utils.RankUtils;
import edu.alibaba.mpc4j.common.tool.utils.*;
import edu.alibaba.mpc4j.sml.opboost.OpBoostPtoDesc.PtoStep;
import smile.data.DataFrame;
import smile.data.measure.NominalScale;
import smile.data.type.DataTypes;
import smile.data.type.StructField;
import smile.data.type.StructType;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * OpBoost从机。
 *
 * @author Weiran Liu
 * @date 2021/10/08
 */
public class OpBoostSlave extends AbstractMultiPartyPto {
    /**
     * 从机数据帧
     */
    private DataFrame slaveDataFrame;
    /**
     * 数据总行数
     */
    private int rows;
    /**
     * 数据总行数的字节长度
     */
    private int byteRows;
    /**
     * 偏移量
     */
    private int rowOffset;
    /**
     * 从机LDP数据帧
     */
    private DataFrame slaveLdpDataFrame;
    /**
     * 映射：列名 -> 排序值与真实值的映射
     */
    private Map<String, Map<Integer, Double>> slaveSplitValueMap;

    public OpBoostSlave(Rpc slaveRpc, Party hostParty) {
        super(OpBoostPtoDesc.getInstance(), new OpBoostPtoConfig(), slaveRpc, hostParty);
    }

    /**
     * init the protocol.
     */
    public void init() {
        super.initState();
    }

    public void fit(DataFrame slaveDataFrame, OpBoostSlaveConfig slaveConfig) throws MpcAbortException {
        setPtoInput(slaveDataFrame, slaveConfig);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        List<byte[]> slaveSchemaPayload = generateSlaveSchemaPayload();
        DataPacketHeader slaveSchemaHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), OpBoostPtoDesc.PtoStep.SLAVE_SEND_SCHEMA.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParties()[0].getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(slaveSchemaHeader, slaveSchemaPayload));
        stopWatch.stop();
        long slaveSchemaTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 5, slaveSchemaTime);

        stopWatch.start();
        slaveLdpDataFrame = OpBoostUtils.ldpDataFrame(slaveDataFrame, slaveConfig.getLdpConfigMap());
        stopWatch.stop();
        long ldpTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 5, ldpTime);

        stopWatch.start();
        List<byte[]> slaveDataPayload = generateSlaveDataPayload();
        DataPacketHeader slaveDataHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SLAVE_SEND_ORDER_DATA_FRAME.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParties()[0].getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(slaveDataHeader, slaveDataPayload));
        stopWatch.stop();
        long slaveDataTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 5, slaveDataTime);

        stopWatch.start();
        DataPacketHeader slaveOrderSplitsHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.HOST_SEND_ORDER_SPLIT_NODE.ordinal(), extraInfo,
            otherParties()[0].getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> slaveOrderSplitsPayload = rpc.receive(slaveOrderSplitsHeader).getPayload();
        stopWatch.stop();
        long orderSplitsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 5, orderSplitsTime);

        stopWatch.start();
        List<byte[]> slaveSplitsPayload = generateSplitsNodes(slaveOrderSplitsPayload);
        DataPacketHeader slaveSplitsHeader = new DataPacketHeader(
            encodeTaskId, ptoDesc.getPtoId(), PtoStep.SLAVE_SEND_SPLIT_NODE.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParties()[0].getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(slaveSplitsHeader, slaveSplitsPayload));
        stopWatch.stop();
        long splitNodeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 5, 5, splitNodeTime);

        logPhaseInfo(PtoState.PTO_END);
    }

    private void setPtoInput(DataFrame slaveDataFrame, OpBoostSlaveConfig slaveConfig) {
        checkInitialized();
        // 验证DataFrame与配置参数中的schema相同
        assert slaveDataFrame.schema().equals(slaveConfig.getSchema());
        this.slaveDataFrame = slaveDataFrame;
        rows = slaveDataFrame.nrows();
        byteRows = CommonUtils.getByteLength(rows);
        rowOffset = byteRows * Byte.SIZE - rows;
        extraInfo++;
    }

    private List<byte[]> generateSlaveSchemaPayload() {
        // 构建数据格式
        List<StructField> structFieldList = Arrays.stream(slaveDataFrame.schema().fields())
            .map(structField -> {
                if (structField.measure instanceof NominalScale) {
                    // 枚举类数据，保留measure，数据类型为ByteType
                    return new StructField(structField.name, DataTypes.ByteType, structField.measure);
                } else {
                    // 非枚举类数据，数据类型为IntegerType
                    return new StructField(structField.name, DataTypes.IntegerType);
                }
            }).collect(Collectors.toList());
        StructType slaveSchema = DataTypes.struct(structFieldList);
        List<byte[]> slaveSchemaPayload = new LinkedList<>();
        slaveSchemaPayload.add(ObjectUtils.objectToByteArray(slaveSchema));

        return slaveSchemaPayload;
    }

    private List<byte[]> generateSlaveDataPayload() {
        StructType slaveSchema = slaveLdpDataFrame.schema();
        slaveSplitValueMap = new HashMap<>(slaveSchema.length());
        List<byte[]> slaveDataPayload = new LinkedList<>();
        for (int columnIndex = 0; columnIndex < slaveSchema.length(); columnIndex++) {
            StructField structField = slaveSchema.field(columnIndex);
            if (structField.measure instanceof NominalScale) {
                // 枚举类数据，不用排序，压缩通信
                Map<Integer, Double> slaveColumnTableMap = new HashMap<>(0);
                slaveSplitValueMap.put(structField.name, slaveColumnTableMap);
                int[] intData = slaveLdpDataFrame.column(columnIndex).toIntArray();
                byte[] byteData = new byte[byteRows];
                IntStream.range(0, rows).forEach(row -> {
                    Preconditions.checkArgument(
                        intData[row] == 0 || intData[row] == 1,
                        "Column %s, row %s: Nominal data must be 0 or 1: %s",
                        structField.name, row, intData[row]
                    );
                    if (intData[row] == 1) {
                        BinaryUtils.setBoolean(byteData, rowOffset + row, true);
                    }
                });
                slaveDataPayload.add(byteData);
            } else {
                // 非枚举类数据，需要排序
                double[] doubleData = slaveLdpDataFrame.column(columnIndex).toDoubleArray();
                int[] intData = RankUtils.denseRank(doubleData);
                Map<Integer, Double> slaveColumnTableMap = new HashMap<>(rows);
                IntStream.range(0, rows).forEach(row ->
                    slaveColumnTableMap.put(intData[row], doubleData[row])
                );
                slaveSplitValueMap.put(structField.name, slaveColumnTableMap);
                byte[] byteData = IntUtils.intArrayToByteArray(intData);
                slaveDataPayload.add(byteData);
            }
        }
        return slaveDataPayload;
    }

    private List<byte[]> generateSplitsNodes(List<byte[]> slaveOrderSplitsPayload) throws MpcAbortException {
        StructType slaveSchema = slaveLdpDataFrame.schema();
        // 数据的数量为从机数据格式的数量
        MpcAbortPreconditions.checkArgument(slaveOrderSplitsPayload.size() == slaveSchema.length());
        List<byte[]> slaveSplitsPayload = new LinkedList<>();
        // 按照从机数据列的顺序构建请求
        for (int columnIndex = 0; columnIndex < slaveSchema.length(); columnIndex++) {
            byte[] slaveOrderSplitsByteArray = slaveOrderSplitsPayload.remove(0);
            if (slaveOrderSplitsByteArray.length == 0) {
                // 如果此列没有数据，则添加空数据
                slaveSplitsPayload.add(new byte[0]);
            } else {
                StructField structField = slaveSchema.field(columnIndex);
                String columnName = structField.name;
                int[] slaveColumnOrderSplits = IntUtils.byteArrayToIntArray(slaveOrderSplitsByteArray);
                Map<Integer, Double> slaveColumnTableMap = slaveSplitValueMap.get(columnName);
                double[] slaveColumnSplits = new double[slaveColumnOrderSplits.length];
                IntStream.range(0, slaveColumnOrderSplits.length).forEach(index -> {
                    if (slaveColumnTableMap.containsKey(slaveColumnOrderSplits[index])) {
                        slaveColumnSplits[index] = slaveColumnTableMap.get(slaveColumnOrderSplits[index]);
                    } else {
                        // 实验中确实出现切分点超过最大值或者小于最小值的情况
                        int min = slaveColumnTableMap.keySet().stream()
                            .mapToInt(value -> value)
                            .min().orElse(0);
                        int max = slaveColumnTableMap.keySet().stream()
                            .mapToInt(value -> value)
                            .max().orElse(slaveDataFrame.nrows());
                        slaveColumnSplits[index] = slaveColumnOrderSplits[index] < min ? min : max;
                    }
                });
                slaveSplitsPayload.add(DoubleUtils.doubleArrayToByteArray(slaveColumnSplits));
            }
        }
        return slaveSplitsPayload;
    }
}
