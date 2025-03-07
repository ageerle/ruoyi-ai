package org.ruoyi.knowledge.service;

import java.util.List;

public interface EmbeddingService {

    void storeEmbeddings(List<String> chunkList, String kid, String docId,List<String> fidList);

    void removeByDocId(String kid,String docId);

    void removeByKid(String kid);

    List<Double> getQueryVector(String query, String kid);

    void createSchema(String kid);

    void removeByKidAndFid(String kid, String fid);

    void saveFragment(String kid, String docId, String fid, String content);
}
