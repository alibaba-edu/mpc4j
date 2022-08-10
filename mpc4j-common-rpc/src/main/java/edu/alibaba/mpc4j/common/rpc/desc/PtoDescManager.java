package edu.alibaba.mpc4j.common.rpc.desc;

import java.util.HashMap;
import java.util.Map;

/**
 * 协议管理器，防止协议的ID发生冲突。
 *
 * @author Weiran Liu
 * @date 2021/12/09
 */
public class PtoDescManager {
    /**
     * 协议ID映射
     */
    private static final Map<Integer, PtoDesc> PTO_DESC_MAP = new HashMap<>();
    /**
     * 协议姓名映射
     */
    private static final Map<String, PtoDesc> PTO_NAME_MAP = new HashMap<>();

    /**
     * 私有构造函数。
     */
    private PtoDescManager() {
        // empty
    }

    /**
     * 注册协议。
     *
     * @param ptoDesc 协议描述。
     */
    public static void registerPtoDesc(PtoDesc ptoDesc) {
        assert !PTO_DESC_MAP.containsKey(ptoDesc.getPtoId())
            : "Existing PtoDesc contains new PtoID, please change to another PtoID: " + ptoDesc.getPtoId();
        PTO_DESC_MAP.put(ptoDesc.getPtoId(), ptoDesc);
        assert !PTO_NAME_MAP.containsKey(ptoDesc.getPtoName())
        : "Existing PtoDesc contains new PtoName, please change to another PtoName: " + ptoDesc.getPtoName();
        PTO_NAME_MAP.put(ptoDesc.getPtoName(), ptoDesc);
    }
}
