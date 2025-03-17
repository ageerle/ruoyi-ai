from flask import Flask, request, jsonify
from sentence_transformers import SentenceTransformer
from sklearn.metrics.pairwise import cosine_similarity
import json

app = Flask(__name__)

# 创建一个全局的模型缓存字典
model_cache = {}

# 分割文本块
def split_text(text, block_size, overlap_chars, delimiter):
    chunks = text.split(delimiter)
    text_blocks = []
    current_block = ""

    for chunk in chunks:
        if len(current_block) + len(chunk) + 1 <= block_size:
            if current_block:
                current_block += " " + chunk
            else:
                current_block = chunk
        else:
            text_blocks.append(current_block)
            current_block = chunk
    if current_block:
        text_blocks.append(current_block)

    overlap_blocks = []
    for i in range(len(text_blocks)):
        if i > 0:
            overlap_block = text_blocks[i - 1][-overlap_chars:] + text_blocks[i]
            overlap_blocks.append(overlap_block)
        overlap_blocks.append(text_blocks[i])

    return overlap_blocks

# 文本向量化
def vectorize_text_blocks(text_blocks, model):
    return model.encode(text_blocks)

# 文本检索
def retrieve_top_k(query, knowledge_base, k, block_size, overlap_chars, delimiter, model):
    # 将知识库拆分为文本块
    text_blocks = split_text(knowledge_base, block_size, overlap_chars, delimiter)
    # 向量化文本块
    knowledge_vectors = vectorize_text_blocks(text_blocks, model)
    # 向量化查询文本
    query_vector = model.encode([query]).reshape(1, -1)
    # 计算相似度
    similarities = cosine_similarity(query_vector, knowledge_vectors)
    # 获取相似度最高的 k 个文本块的索引
    top_k_indices = similarities[0].argsort()[-k:][::-1]

    # 返回文本块和它们的向量
    top_k_texts = [text_blocks[i] for i in top_k_indices]
    top_k_embeddings = [knowledge_vectors[i] for i in top_k_indices]

    return top_k_texts, top_k_embeddings

@app.route('/vectorize', methods=['POST'])
def vectorize_text():
    # 从请求中获取 JSON 数据
    data = request.json
    print(f"Received request data: {data}")  # 调试输出请求数据

    text_list = data.get("text", [])
    model_name = data.get("model_name", "msmarco-distilbert-base-tas-b")  # 默认模型

    delimiter = data.get("delimiter", "\n")  # 默认分隔符
    k = int(data.get("k", 3))  # 默认检索条数
    block_size = int(data.get("block_size", 500))  # 默认文本块大小
    overlap_chars = int(data.get("overlap_chars", 50))  # 默认重叠字符数

    if not text_list:
        return jsonify({"error": "Text is required."}), 400

    # 检查模型是否已经加载
    if model_name not in model_cache:
        try:
            model = SentenceTransformer(model_name)
            model_cache[model_name] = model  # 缓存模型
        except Exception as e:
            return jsonify({"error": f"Failed to load model: {e}"}), 500

    model = model_cache[model_name]

    top_k_texts_all = []
    top_k_embeddings_all = []

    # 如果只有一个查询文本
    if len(text_list) == 1:
        top_k_texts, top_k_embeddings = retrieve_top_k(text_list[0], text_list[0], k, block_size, overlap_chars, delimiter, model)
        top_k_texts_all.append(top_k_texts)
        top_k_embeddings_all.append(top_k_embeddings)
    elif len(text_list) > 1:
        # 如果多个查询文本，依次处理
        for query in text_list:
            top_k_texts, top_k_embeddings = retrieve_top_k(query, text_list[0], k, block_size, overlap_chars, delimiter, model)
            top_k_texts_all.append(top_k_texts)
            top_k_embeddings_all.append(top_k_embeddings)

    # 将嵌入向量（ndarray）转换为可序列化的列表
    top_k_embeddings_all = [[embedding.tolist() for embedding in embeddings] for embeddings in top_k_embeddings_all]

    print(f"Top K texts: {top_k_texts_all}")  # 打印检索到的文本
    print(f"Top K embeddings: {top_k_embeddings_all}")  # 打印检索到的向量

    # 返回 JSON 格式的数据
    return jsonify({

        "topKEmbeddings": top_k_embeddings_all  # 返回嵌入向量
    })

if __name__ == '__main__':
    app.run(host="0.0.0.0", port=5000, debug=True)
