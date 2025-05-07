package org.ruoyi.service;

import java.util.List;

/**
 * @author ageer
 * 向量库管理
 */
public interface VectorStoreService {

    void storeEmbeddings(List<String> chunkList, String kid,String docId,List<String> fids);

    void removeByDocId(String kid,String docId);

    void removeByKid(String kid);

    List<String> getQueryVector(String query, String kid);

    void createSchema(String kid);

    void removeByKidAndFid(String kid, String fid);

}
