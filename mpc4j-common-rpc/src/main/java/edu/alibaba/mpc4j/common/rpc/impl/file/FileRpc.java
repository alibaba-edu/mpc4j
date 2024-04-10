package edu.alibaba.mpc4j.common.rpc.impl.file;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.impl.file.FilePtoDesc.StepEnum;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 文件通信机制。
 *
 * @author Weiran Liu
 * @date 2021/12/17
 */
public class FileRpc implements Rpc {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileRpc.class);
    /**
     * each file contains 7 fields: taskId, ptoId, stepId, extraInfo, senderId, receiverId, suffix.
     */
    private static final int FILE_NAME_SPLIT_NUM = 7;
    /**
     * 读取文件单位等待时间
     */
    private static final int DEFAULT_READ_WAIT_MILLI_SECOND = 1;
    /**
     * 删除文件单位等待时间
     */
    private static final int DEFAULT_DELETE_WAIT_MILLI_SECOND = 10;
    /**
     * 文件名分隔符
     */
    private static final String FILE_NAME_SEPARATOR = "_";
    /**
     * 文件写入完毕的状态文件后缀
     */
    private static final String FILE_STATUS_SUFFIX = "STATUS";
    /**
     * 传输内容本身的负载文件后缀
     */
    private static final String FILE_PAYLOAD_SUFFIX = "PAYLOAD";
    /**
     * 参与方ID映射
     */
    private final HashMap<Integer, FileParty> partyIdHashMap;
    /**
     * 自己的参与方信息
     */
    private final FileParty ownParty;
    /**
     * Own party's ID
     */
    private final int ownPartyId;
    /**
     * 数据包数量
     */
    private long dataPacketNum;
    /**
     * 负载字节长度
     */
    private long payloadByteLength;
    /**
     * 发送字节长度
     */
    private long sendByteLength;

    /**
     * 构建文件RPC。
     *
     * @param ownParty 自己的参与方信息。
     * @param partySet 参与方集合。
     */
    public FileRpc(FileParty ownParty, Set<FileParty> partySet) {
        // 所有参与方的数量必须大于1
        Preconditions.checkArgument(partySet.size() > 1, "Party set size must be greater than 1");
        // 参与方自身必须在所有参与方之中
        Preconditions.checkArgument(partySet.contains(ownParty), "Party set must contain own party");
        this.ownParty = ownParty;
        ownPartyId = ownParty.getPartyId();
        // 按照参与方索引值，将参与方信息插入到ID映射中
        partyIdHashMap = new HashMap<>();
        partySet.forEach(partySpec -> partyIdHashMap.put(partySpec.getPartyId(), partySpec));
        dataPacketNum = 0;
        payloadByteLength = 0;
        sendByteLength = 0;
    }

    @Override
    public Party ownParty() {
        return ownParty;
    }

    @Override
    public Set<Party> getPartySet() {
        return partyIdHashMap.keySet().stream().map(partyIdHashMap::get).collect(Collectors.toSet());
    }

    @Override
    public Party getParty(int partyId) {
        assert (partyIdHashMap.containsKey(partyId));
        return partyIdHashMap.get(partyId);
    }

    @Override
    public void connect() {
        partyIdHashMap.keySet().stream().sorted().forEach(otherPartyId -> {
            if (otherPartyId != ownPartyId) {
                LOGGER.debug(
                    "{} successfully make connection with {}",
                    partyIdHashMap.get(ownPartyId), partyIdHashMap.get(otherPartyId)
                );
            }
        });
        LOGGER.info("{} connected", ownParty);
    }

    @Override
    public void send(DataPacket dataPacket) {
        DataPacketHeader header = dataPacket.getHeader();
        Preconditions.checkArgument(
            ownPartyId == header.getSenderId(), "Sender ID must be %s", ownPartyId
        );
        Preconditions.checkArgument(
            partyIdHashMap.containsKey(header.getReceiverId()),
            "Party set does not contain Receiver ID = %s", header.getReceiverId()
        );
        String receiverFilePath = partyIdHashMap.get(header.getReceiverId()).getPartyFilePath();
        List<byte[]> payload = dataPacket.getPayload();
        try {
            String payloadFileName = getPayloadFileName(header);
            String statusFileName = getStatusFileName(header);
            // 在写入之前必然没有状态文件
            File statusFile = new File(receiverFilePath + File.separator + statusFileName);
            if (statusFile.exists()) {
                throw new IllegalStateException("File " + statusFile.getName() + " already exists.");
            }
            // 如果写入之前发现了负载文件，则先删除
            File payloadFile = new File(receiverFilePath + File.separator + payloadFileName);
            if (payloadFile.exists()) {
                boolean deleted = payloadFile.delete();
                if (!deleted) {
                    throw new IllegalStateException("File " + payloadFile.getName() + " exists and cannot delete.");
                }
            }
            // 写入数据包并统计发送数据量
            FileWriter payloadFileWriter = new FileWriter(payloadFile);
            PrintWriter payloadPrintWriter = new PrintWriter(payloadFileWriter, true);
            for (byte[] byteArray : payload) {
                payloadByteLength += byteArray.length;
                String payloadString = Base64.getEncoder().encodeToString(byteArray);
                sendByteLength += payloadString.getBytes(StandardCharsets.UTF_8).length;
                payloadPrintWriter.println(payloadString);
            }
            payloadPrintWriter.close();
            dataPacketNum++;
            FileWriter statusFileWriter = new FileWriter(statusFile);
            PrintWriter statusPrintWriter = new PrintWriter(statusFileWriter, true);
            statusPrintWriter.println(FILE_STATUS_SUFFIX);
            statusPrintWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("Unknown IOException for receiver file path: " + receiverFilePath);
        }
    }

    @Override
    public DataPacket receive(DataPacketHeader header) {
        Preconditions.checkArgument(
            ownPartyId == header.getReceiverId(), "Receiver ID must be %s", ownPartyId
        );
        Preconditions.checkArgument(
            partyIdHashMap.containsKey(header.getSenderId()),
            "Party set does not contain Sender ID = %s", header.getSenderId()
        );
        String ownFilePath = ownParty.getPartyFilePath();
        // 收取数据
        try {
            String payloadFileName = getPayloadFileName(header);
            String statusFileName = getStatusFileName(header);
            File statusFile = new File(ownFilePath + File.separator + statusFileName);
            File payloadFile = new File(ownFilePath + File.separator + payloadFileName);
            while (!statusFile.exists() || !payloadFile.exists()) {
                //noinspection BusyWait
                Thread.sleep(DEFAULT_READ_WAIT_MILLI_SECOND);
            }
            // 如果状态文件存在，则负载文件一定已经写入完毕，要先删除状态文件
            boolean deleted = statusFile.delete();
            // @风笛验证后指出，在Windows环境下删除不是一瞬间完成的，因此要等一小段时间保证删除完毕
            Thread.sleep(DEFAULT_DELETE_WAIT_MILLI_SECOND);
            if (!deleted) {
                throw new IllegalStateException("Cannot delete file: " + statusFile.getName());
            }
            // 开始读取
            List<byte[]> byteArrayData = new LinkedList<>();
            InputStreamReader inputStreamReader = new InputStreamReader(
                new FileInputStream(payloadFile), StandardCharsets.UTF_8
            );
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String payloadString;
            while ((payloadString = bufferedReader.readLine()) != null) {
                // 包含该行内容的字符串，不包含任何行终止符，如果已到达流末尾，则返回null
                byte[] byteArray = Base64.getDecoder().decode(payloadString);
                byteArrayData.add(byteArray);
            }
            // Fix Issue #5, close() should be called before "deleted = payloadFile.delete()"
            bufferedReader.close();
            // 删除负载文件
            deleted = payloadFile.delete();
            Thread.sleep(DEFAULT_DELETE_WAIT_MILLI_SECOND);
            if (!deleted) {
                throw new IllegalStateException("Cannot delete file: " + payloadFile.getName());
            }
            return DataPacket.fromByteArrayList(header, byteArrayData);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            throw new IllegalStateException("Unknown IOException for receiver file path: " + ownFilePath);
        }
    }

    @Override
    public DataPacket receiveAny() {
        DataPacketHeader[] receivedDataPacketHeaders;
        while ((receivedDataPacketHeaders = getReceivedDataPacketHeaders()).length == 0) {
            try {
                //noinspection BusyWait
                Thread.sleep(DEFAULT_READ_WAIT_MILLI_SECOND);
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new IllegalStateException("Unknown IOException for receiver");
            }
        }
        return receive(receivedDataPacketHeaders[0]);
    }

    @Override
    public long getPayloadByteLength() {
        return payloadByteLength;
    }

    @Override
    public long getSendByteLength() {
        return sendByteLength;
    }

    @Override
    public long getSendDataPacketNum() {
        return dataPacketNum;
    }

    @Override
    public void reset() {
        payloadByteLength = 0;
        sendByteLength = 0;
        dataPacketNum = 0;
    }

    @Override
    public void synchronize() {
        // 对参与方进行排序，所有在自己之前的自己作为client、所有在自己之后的自己作为server
        partyIdHashMap.keySet().stream().sorted().forEach(otherPartyId -> {
            if (otherPartyId < ownPartyId) {
                // 如果对方排序比自己小，则自己是client，需要给对方发送同步信息
                DataPacketHeader clientSynchronizeHeader = new DataPacketHeader(
                    Long.MAX_VALUE - ownPartyId, FilePtoDesc.getInstance().getPtoId(), StepEnum.CLIENT_SYNCHRONIZE.ordinal(),
                    ownPartyId, otherPartyId
                );
                send(DataPacket.fromByteArrayList(clientSynchronizeHeader, new LinkedList<>()));
                // 获得对方的回复
                DataPacketHeader serverSynchronizeHeader = new DataPacketHeader(
                    Long.MAX_VALUE - otherPartyId, FilePtoDesc.getInstance().getPtoId(), StepEnum.SERVER_SYNCHRONIZE.ordinal(),
                    otherPartyId, ownPartyId
                );
                receive(serverSynchronizeHeader);
            } else if (otherPartyId > ownPartyId) {
                // 如果对方排序比自己大，则自己是server
                DataPacketHeader clientSynchronizeHeader = new DataPacketHeader(
                    Long.MAX_VALUE - otherPartyId, FilePtoDesc.getInstance().getPtoId(), StepEnum.CLIENT_SYNCHRONIZE.ordinal(),
                    otherPartyId, ownPartyId
                );
                receive(clientSynchronizeHeader);
                DataPacketHeader serverSynchronizeHeader = new DataPacketHeader(
                    Long.MAX_VALUE - ownPartyId, FilePtoDesc.getInstance().getPtoId(), StepEnum.SERVER_SYNCHRONIZE.ordinal(),
                    ownPartyId, otherPartyId
                );
                send(DataPacket.fromByteArrayList(serverSynchronizeHeader, new LinkedList<>()));
            }
        });
        LOGGER.info("{} synchronized", ownParty);
    }

    private String getPayloadFileName(DataPacketHeader header) {
        // testId_PtoId_StepId_extraInfo_senderId_receiverId_PAYLOAD
        return header.getEncodeTaskId()
            + FILE_NAME_SEPARATOR + header.getPtoId()
            + FILE_NAME_SEPARATOR + header.getStepId()
            + FILE_NAME_SEPARATOR + header.getExtraInfo()
            + FILE_NAME_SEPARATOR + header.getSenderId()
            + FILE_NAME_SEPARATOR + header.getReceiverId()
            + FILE_NAME_SEPARATOR + FILE_PAYLOAD_SUFFIX;
    }

    private String getStatusFileName(DataPacketHeader header) {
        // testId_PtoId_StepId_extraInfo_senderId_receiverId_STATUS
        return header.getEncodeTaskId()
            + FILE_NAME_SEPARATOR + header.getPtoId()
            + FILE_NAME_SEPARATOR + header.getStepId()
            + FILE_NAME_SEPARATOR + header.getExtraInfo()
            + FILE_NAME_SEPARATOR + header.getSenderId()
            + FILE_NAME_SEPARATOR + header.getReceiverId()
            + FILE_NAME_SEPARATOR + FILE_STATUS_SUFFIX;
    }

    private DataPacketHeader[] getReceivedDataPacketHeaders() {
        // read all status file
        File ownFilePath = new File(ownParty.getPartyFilePath());
        File[] files = ownFilePath.listFiles();
        Objects.requireNonNull(files, ownFilePath + " is not a dictionary");
        return Arrays.stream(files)
            .map(File::getName)
            .map(fileName -> fileName.split(FILE_NAME_SEPARATOR))
            // valid file name
            .filter(splitFileName -> splitFileName.length == FILE_NAME_SPLIT_NUM)
            // given sender and receiver
            .filter(splitFileName ->
                splitFileName[FILE_NAME_SPLIT_NUM - 1].equals(FILE_STATUS_SUFFIX)
                && Integer.parseInt(splitFileName[5]) == ownPartyId
            )
            .map(splitFileName -> {
                long taskId = Long.parseLong(splitFileName[0]);
                int ptoId = Integer.parseInt(splitFileName[1]);
                int stepId = Integer.parseInt(splitFileName[2]);
                int senderId = Integer.parseInt(splitFileName[4]);
                long extraInfo = Long.parseLong(splitFileName[3]);
                return new DataPacketHeader(taskId, ptoId, stepId, extraInfo, senderId, ownPartyId);
            })
            .filter(header -> header.getReceiverId() == ownPartyId)
            .toArray(DataPacketHeader[]::new);
    }



    @Override
    public void disconnect() {
        LOGGER.info("{} disconnected", ownParty);
    }
}
