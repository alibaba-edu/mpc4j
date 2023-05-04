package edu.alibaba.mpc4j.sml.opboost;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPto;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.utils.*;
import smile.data.DataFrame;
import smile.data.formula.Formula;
import smile.data.measure.NominalScale;
import smile.data.type.DataTypes;
import smile.data.type.StructField;
import smile.data.type.StructType;
import smile.data.vector.BaseVector;
import smile.data.vector.ByteVector;

import java.util.*;
import java.util.stream.IntStream;

/**
 * OpBoost主机抽象类。
 *
 * @author Weiran Liu
 * @date 2022/6/29
 */
public abstract class AbstractOpBoostHost extends AbstractMultiPartyPto {
    /**
     * 标签值
     */
    protected Formula formula;
    /**
     * 主机数据帧
     */
    private DataFrame hostDataFrame;
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
     * 主机配置项
     */
    private OpBoostHostConfig hostConfig;
    /**
     * 映射：从机 -> 数据格式
     */
    private Map<Party, StructType> slaveSchemaMap;
    /**
     * 映射：列名 -> 从机
     * 协议执行过程中会验证（包括主机和从机）的列名互不相同，从而保证每个列名都只与一个参与方（主机或从机）对应
     */
    protected Map<String, Party> columnSlaveMap;
    /**
     * 总数据，其中：主机数据为原始数据经过LDP处理，从机数据为原始数据经过LDP处理后的排序结果
     */
    protected DataFrame wholeDataFrame;
    /**
     * 完整数据的数据格式
     */
    protected StructType wholeSchema;
    /**
     * 从机排序值切分集合映射
     */
    protected Map<Party, Map<String, Set<Integer>>> slavesSplitSetMap;
    /**
     * 映射：从机 -> 各列所需查找的序号与原值映射列表
     */
    protected Map<Party, Map<String, Map<Integer, Double>>> slavesSplitValueMap;

    protected AbstractOpBoostHost(Rpc hostRpc, Party... slaveParties) {
        super(OpBoostPtoDesc.getInstance(), new OpBoostPtoConfig(), hostRpc, slaveParties);
    }

    /**
     * init the protocol.
     */
    public void init() {
        super.initState();
    }

    protected void setPtoInput(Formula formula, DataFrame hostDataFrame, OpBoostHostConfig hostConfig) {
        checkInitialized();
        // 验证DataFrame与配置参数中的schema相同
        assert hostDataFrame.schema().equals(hostConfig.getSchema());
        this.formula = formula;
        this.hostDataFrame = hostDataFrame;
        rows = hostDataFrame.nrows();
        byteRows = CommonUtils.getByteLength(rows);
        rowOffset = byteRows * Byte.SIZE - rows;
        this.hostConfig = hostConfig;
        extraInfo++;
    }

    protected final void slaveSchemaStep() throws MpcAbortException {
        slaveSchemaMap = new HashMap<>(otherParties().length);
        columnSlaveMap = new HashMap<>(otherParties().length);
        for (Party slaveParty : otherParties()) {
            DataPacketHeader slaveSchemaHeader = new DataPacketHeader(
                encodeTaskId, ptoDesc.getPtoId(), OpBoostPtoDesc.PtoStep.SLAVE_SEND_SCHEMA.ordinal(), extraInfo,
                slaveParty.getPartyId(), ownParty().getPartyId()
            );
            List<byte[]> slaveSchemaPayload = rpc.receive(slaveSchemaHeader).getPayload();
            handleSlaveSchemaPayload(slaveParty, slaveSchemaPayload);
        }
    }

    private void handleSlaveSchemaPayload(Party slaveParty, List<byte[]> slaveSchemaPayload) throws MpcAbortException {
        // 数据帧格式的长度为1
        MpcAbortPreconditions.checkArgument(slaveSchemaPayload.size() == 1);
        // 解析数据格式
        StructType slaveSchema = (StructType) ObjectUtils.byteArrayToSerializableObject(slaveSchemaPayload.remove(0));
        slaveSchemaMap.put(slaveParty, slaveSchema);
        for (StructField structField : slaveSchema.fields()) {
            // 验证数据格式是否为整数类型或字节类型
            MpcAbortPreconditions.checkArgument(
                structField.type.equals(DataTypes.ByteType) || structField.type.equals(DataTypes.IntegerType)
            );
            // 验证度量值是否为空或者为枚举值
            MpcAbortPreconditions.checkArgument(
                structField.measure == null | structField.measure instanceof NominalScale
            );
            // 在列名称 -> 参与方中添加列映射
            columnSlaveMap.put(structField.name, slaveParty);
        }
    }

    protected final void ldpDataFrameStep() {
        wholeDataFrame = OpBoostUtils.ldpDataFrame(hostDataFrame, hostConfig.getLdpConfigMap());
    }

    protected final void slaveDataStep() throws MpcAbortException {
        for (Party slaveParty : otherParties()) {
            DataPacketHeader slaveDataHeader = new DataPacketHeader(
                encodeTaskId, ptoDesc.getPtoId(), OpBoostPtoDesc.PtoStep.SLAVE_SEND_ORDER_DATA_FRAME.ordinal(), extraInfo,
                slaveParty.getPartyId(), ownParty().getPartyId()
            );
            List<byte[]> slaveDataPayload = rpc.receive(slaveDataHeader).getPayload();
            handleSlaveDataPayload(slaveParty, slaveDataPayload);
        }
    }

    private void handleSlaveDataPayload(Party slaveParty, List<byte[]> slaveDataPayload) throws MpcAbortException {
        // 数据的数量为从机数据格式的数量
        StructType slaveSchema = slaveSchemaMap.get(slaveParty);
        MpcAbortPreconditions.checkArgument(slaveDataPayload.size() == slaveSchema.length());
        @SuppressWarnings("rawtypes")
        BaseVector[] slaveBaseVectors = new BaseVector[slaveSchema.length()];
        // 按列循环，提取每一列数据
        for (int columnIndex = 0; columnIndex < slaveSchema.length(); columnIndex++) {
            StructField structField = slaveSchema.field(columnIndex);
            if (structField.measure instanceof NominalScale) {
                // 枚举型数据，从压缩表示转换为正常表示
                byte[] data = slaveDataPayload.remove(0);
                MpcAbortPreconditions.checkArgument(
                    data.length == byteRows && BytesUtils.isReduceByteArray(data, rows)
                );
                byte[] byteData = new byte[rows];
                IntStream.range(0, rows).forEach(row -> {
                    if (BinaryUtils.getBoolean(data, rowOffset + row)) {
                        byteData[row] = 0x01;
                    }
                });
                slaveBaseVectors[columnIndex] = ByteVector.of(structField, byteData);
            } else {
                // 数值型数据，转化并赋值
                int[] data = IntUtils.byteArrayToIntArray(slaveDataPayload.remove(0));
                MpcAbortPreconditions.checkArgument(data.length == rows);
                slaveBaseVectors[columnIndex] = OpBoostUtils.createIntegralVector(structField, data);
            }
        }
        DataFrame slaveDataFrame = DataFrame.of(slaveBaseVectors);
        wholeDataFrame = wholeDataFrame.merge(slaveDataFrame);
    }

    /**
     * 训练模型。
     *
     * @throws MpcAbortException 如果协议执行过程出现异常。
     */
    protected abstract void trainModel() throws MpcAbortException;

    protected final void traverseSplits() {
        // 先构造各列映射表
        wholeSchema = formula.x(wholeDataFrame).schema();
        // 初始化临时变量
        slavesSplitSetMap = new HashMap<>(otherParties().length);
        for (Party slaveParty : otherParties()) {
            StructType slaveSchema = slaveSchemaMap.get(slaveParty);
            Map<String, Set<Integer>> slaveSplitSetMap = new HashMap<>(slaveSchema.length());
            for (StructField structField : slaveSchema.fields()) {
                slaveSplitSetMap.put(structField.name, new HashSet<>());
            }
            slavesSplitSetMap.put(slaveParty, slaveSplitSetMap);
        }
        // 构建排序值 -> 真实值映射表
        slavesSplitValueMap = new HashMap<>(otherParties().length);
    }

    /**
     * 遍历树模型
     */
    protected abstract void traverseTreeModel();

    protected final void updateSplitStep() throws MpcAbortException {
        for (Party slaveParty : otherParties()) {
            List<byte[]> slaveOrderSplitsPayload = generateSlaveOrderSplitsPayload(slaveParty);
            // 发送请求
            DataPacketHeader slaveOrderSplitsHeader = new DataPacketHeader(
                encodeTaskId, ptoDesc.getPtoId(), OpBoostPtoDesc.PtoStep.HOST_SEND_ORDER_SPLIT_NODE.ordinal(), extraInfo,
                ownParty().getPartyId(), slaveParty.getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(slaveOrderSplitsHeader, slaveOrderSplitsPayload));
            // 接收请求
            DataPacketHeader slaveSplitsHeader = new DataPacketHeader(
                encodeTaskId, ptoDesc.getPtoId(), OpBoostPtoDesc.PtoStep.SLAVE_SEND_SPLIT_NODE.ordinal(), extraInfo,
                slaveParty.getPartyId(), ownParty().getPartyId()
            );
            List<byte[]> slaveSplitsPayload = rpc.receive(slaveSplitsHeader).getPayload();
            handleSlaveSplitPayload(slaveParty, slaveSplitsPayload);
        }
    }

    private List<byte[]> generateSlaveOrderSplitsPayload(Party slaveParty) {
        Map<String, Set<Integer>> slaveSplitSetMap = slavesSplitSetMap.get(slaveParty);
        StructType slaveSchema = slaveSchemaMap.get(slaveParty);
        // 向每一个从机构建请求
        List<byte[]> slaveOrderSplitsPayload = new LinkedList<>();
        // 按照从机数据列的顺序构建请求数据包
        for (int columnIndex = 0; columnIndex < slaveSchema.length(); columnIndex++) {
            StructField structField = slaveSchema.field(columnIndex);
            String columnName = structField.name;
            Set<Integer> slaveColumnOrderSplitSet = slaveSplitSetMap.get(columnName);
            if (slaveColumnOrderSplitSet.isEmpty()) {
                // 如果此列不需要请求切分点，则增加一行空列
                slaveOrderSplitsPayload.add(new byte[0]);
            } else {
                // 如果此列需要请求切分点，发送请求后接收返回值
                int[] slaveColumnOrderSplits = slaveColumnOrderSplitSet.stream()
                    .sorted().mapToInt(Integer::intValue).toArray();
                slaveOrderSplitsPayload.add(IntUtils.intArrayToByteArray(slaveColumnOrderSplits));
            }
        }
        return slaveOrderSplitsPayload;
    }

    private void handleSlaveSplitPayload(Party slaveParty, List<byte[]> slaveSplitsPayload) throws MpcAbortException {
        // 接收的数据包长度应该为数据帧列数
        StructType slaveSchema = slaveSchemaMap.get(slaveParty);
        MpcAbortPreconditions.checkArgument(slaveSplitsPayload.size() == slaveSchema.length());
        Map<String, Set<Integer>> slaveSplitSetMap = slavesSplitSetMap.get(slaveParty);
        // 构建切分点-取值映射表
        Map<String, Map<Integer, Double>> slaveSplitValueMap = new HashMap<>(slaveSchema.length());
        for (int columnIndex = 0; columnIndex < slaveSchema.length(); columnIndex++) {
            StructField structField = slaveSchema.field(columnIndex);
            String columnName = structField.name;
            Set<Integer> slaveColumnOrderSplitSet = slaveSplitSetMap.get(columnName);
            byte[] slaveColumnSplitsByteArray = slaveSplitsPayload.remove(0);
            if (slaveColumnOrderSplitSet.isEmpty()) {
                MpcAbortPreconditions.checkArgument(slaveColumnSplitsByteArray.length == 0);
                // 放置空映射
                slaveSplitValueMap.put(columnName, new HashMap<>(0));
            } else {
                double[] slaveColumnSplits = DoubleUtils.byteArrayToDoubleArray(slaveColumnSplitsByteArray);
                int[] slaveColumnOrderSplits = slaveColumnOrderSplitSet.stream()
                    .sorted().mapToInt(Integer::intValue).toArray();
                MpcAbortPreconditions.checkArgument(slaveColumnSplits.length == slaveColumnOrderSplits.length);
                // 放置映射
                Map<Integer, Double> slaveColumnSplitMap = new HashMap<>(slaveColumnOrderSplits.length);
                IntStream.range(0, slaveColumnOrderSplits.length).forEach(index ->
                    slaveColumnSplitMap.put(slaveColumnOrderSplits[index], slaveColumnSplits[index])
                );
                slaveSplitValueMap.put(columnName, slaveColumnSplitMap);
            }
        }
        slavesSplitValueMap.put(slaveParty, slaveSplitValueMap);
    }

    /**
     * 替换切分点
     */
    protected abstract void replaceSplits();
}
