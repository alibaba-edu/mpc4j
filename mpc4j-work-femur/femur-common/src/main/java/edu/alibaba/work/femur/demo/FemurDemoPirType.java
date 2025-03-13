package edu.alibaba.work.femur.demo;

/**
 * Femur PIR type.
 *
 * @author Weiran Liu
 * @date 2024/12/3
 */
public enum FemurDemoPirType {
    /**
     * Naive with memory database, where server directly returns entries in the range queried by the client.
     */
    NAIVE_MEMORY,
    /**
     * SealPIR with memory database, where server and client run SealPIR in the range queried by the client.
     */
    SEAL_MEMORY,
    /**
     * Naive with Redis
     */
    NAIVE_REDIS,
    /**
     * SealPIR with Redis
     */
    SEAL_REDIS,
}
