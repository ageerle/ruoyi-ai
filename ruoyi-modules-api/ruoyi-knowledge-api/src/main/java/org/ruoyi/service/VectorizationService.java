package org.ruoyi.service;

import java.util.List;

/**
 * 文本向量化
 */
public interface VectorizationService {

    List<List<Double>> batchVectorization(List<String> chunkList, String kid);

    List<Double> singleVectorization(String chunk, String kid);
}
