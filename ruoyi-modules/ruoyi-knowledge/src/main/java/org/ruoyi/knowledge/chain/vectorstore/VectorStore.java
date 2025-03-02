package org.ruoyi.knowledge.chain.vectorstore;

import java.util.List;

/**
 * 向量存储
 */
public interface VectorStore {
    void storeEmbeddings(List<String> chunkList,List<List<Double>> vectorList, String kid, String docId,List<String> fidList);
    void removeByDocId(String kid,String docId);
    void removeByKid(String kid);
    List<String> nearest(List<Double> queryVector,String kid);
    List<String> nearest(String query,String kid);

    void newSchema(String kid);

    void removeByKidAndFid(String kid, String fid);
}
