package org.ruoyi.service;

import java.util.List;

public interface VectorStoreService {

    void storeEmbeddings(List<String> chunkList, String kid);

    void removeByDocId(String kid,String docId);

    void removeByKid(String kid);

    List<String> getQueryVector(String query, String kid);

    void createSchema(String kid);

    void removeByKidAndFid(String kid, String fid);

}
