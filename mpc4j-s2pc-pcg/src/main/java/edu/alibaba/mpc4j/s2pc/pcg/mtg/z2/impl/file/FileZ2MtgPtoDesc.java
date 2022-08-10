package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.impl.file;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * 文件布尔三元组生成协议信息。在创建阶段初始化足够的三元组并存储在文件中，初始化阶段读取文件并写入内存。
 *
 * @author Weiran Liu
 * @date 2022/5/22
 */
public class FileZ2MtgPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int) 3324233002250158029L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "FILE_Z2_MTG";

    /**
     * 单例模式
     */
    private static final FileZ2MtgPtoDesc INSTANCE = new FileZ2MtgPtoDesc();

    /**
     * 私有构造函数
     */
    private FileZ2MtgPtoDesc() {
        // empty
    }

    public static PtoDesc getInstance() {
        return INSTANCE;
    }

    static {
        PtoDescManager.registerPtoDesc(getInstance());
    }

    @Override
    public int getPtoId() {
        return PTO_ID;
    }

    @Override
    public String getPtoName() {
        return PTO_NAME;
    }
}
