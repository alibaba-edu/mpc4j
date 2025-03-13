package edu.alibaba.femur.service.server;

import gnu.trove.map.TLongObjectMap;

/**
 * @author Weiran Liu
 * @date 2024/12/11
 */
public interface FemurPirServerBoot {

    /**
     * start service
     */
    void start();

    void stop();

    void init(int n, int l) ;

    void setDatabase(TLongObjectMap<byte[]> keyValueDatabase);

    void updateValue(Long key, byte[] value);

    void reset();

    FemurPirServerProxy getPirServerProxy();
}
