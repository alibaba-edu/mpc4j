package edu.alibaba.mpc4j.common.rpc.impl.file;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * 文件通信管理器。
 *
 * @author Weiran Liu
 * @date 2021/12/17
 */
public class FileRpcManager implements RpcManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileRpcManager.class);
    /**
     * 参与方数量
     */
    private final int partyNum;
    /**
     * 参与方集合
     */
    private final Set<FileParty> filePartySet;
    /**
     * 所有参与方RPC
     */
    private final Map<Integer, FileRpc> fileRpcMap;

    /**
     * 初始化文件通信管理器。
     *
     * @param partyNum 参与方数量。
     */
    public FileRpcManager(int partyNum) {
        Preconditions.checkArgument(partyNum > 1, "Number of parties must be greater than 1");
        this.partyNum = partyNum;
        // 初始化所有参与方
        filePartySet = new HashSet<>(partyNum);
        IntStream.range(0, partyNum).forEach(partyId -> {
            FileParty fileParty = new FileParty(partyId, getPartyName(partyId), "." + File.separator);
            filePartySet.add(fileParty);
        });
        // 初始化所有参与方的内存通信
        fileRpcMap = new HashMap<>(partyNum);
        for (FileParty fileParty : filePartySet) {
            FileRpc fileRpc = new FileRpc(fileParty, filePartySet);
            fileRpcMap.put(fileRpc.ownParty().getPartyId(), fileRpc);
            LOGGER.debug("Add file party: {}", fileParty);
        }
    }

    @Override
    public Rpc getRpc(int partyId) {
        Preconditions.checkArgument(
            partyId >= 0 && partyId < partyNum, "Party ID must be in range [0, %s)", partyNum
        );
        return fileRpcMap.get(partyId);
    }

    private String getPartyName(int partyId) {
        return "P_" + (partyId + 1);
    }

    @Override
    public int getPartyNum() {
        return partyNum;
    }

    @Override
    public Set<Party> getPartySet() {
        return new HashSet<>(filePartySet);
    }
}
