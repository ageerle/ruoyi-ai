package org.ruoyi.service.knowledge.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.enums.KnowledgeAttachStatus;
import org.ruoyi.common.core.domain.dto.OssDTO;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.core.service.OssService;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.SpringUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.domain.bo.knowledge.KnowledgeAttachBo;
import org.ruoyi.domain.bo.knowledge.KnowledgeInfoUploadBo;
import org.ruoyi.domain.bo.vector.StoreEmbeddingBo;
import org.ruoyi.domain.entity.knowledge.KnowledgeAttach;
import org.ruoyi.domain.entity.knowledge.KnowledgeFragment;
import org.ruoyi.domain.vo.knowledge.DocFragmentCountVo;
import org.ruoyi.domain.vo.knowledge.KnowledgeAttachVo;
import org.ruoyi.domain.vo.knowledge.KnowledgeInfoVo;
import org.ruoyi.domain.vo.knowledge.KnowledgeReparseVo;
import org.ruoyi.factory.ResourceLoaderFactory;
import org.ruoyi.mapper.knowledge.KnowledgeAttachMapper;
import org.ruoyi.mapper.knowledge.KnowledgeFragmentMapper;
import org.ruoyi.service.knowledge.IKnowledgeAttachService;
import org.ruoyi.service.knowledge.IKnowledgeInfoService;
import org.ruoyi.service.knowledge.ResourceLoader;
import org.ruoyi.service.knowledge.DocumentSplitConfig;
import org.ruoyi.service.vector.VectorStoreService;
import org.ruoyi.service.retrieval.KnowledgeRetrievalService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识库附件Service业务层处理
 *
 * @author ageerle
 * @date 2025-12-17
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class KnowledgeAttachServiceImpl implements IKnowledgeAttachService {

    private final KnowledgeAttachMapper baseMapper;
    private final IKnowledgeInfoService knowledgeInfoService;
    private final KnowledgeFragmentMapper knowledgeFragmentMapper;
    private final IChatModelService chatModelService;
    private final ResourceLoaderFactory resourceLoaderFactory;
    private final VectorStoreService vectorStoreService;
    private final OssService ossService;
    private final KnowledgeRetrievalService knowledgeRetrievalService;

    @Override
    public KnowledgeAttachVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public TableDataInfo<KnowledgeAttachVo> queryPageList(KnowledgeAttachBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<KnowledgeAttach> lqw = buildQueryWrapper(bo);
        Page<KnowledgeAttachVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        fillFragmentCount(result.getRecords());
        return TableDataInfo.build(result);
    }

    @Override
    public List<KnowledgeAttachVo> queryList(KnowledgeAttachBo bo) {
        LambdaQueryWrapper<KnowledgeAttach> lqw = buildQueryWrapper(bo);
        List<KnowledgeAttachVo> list = baseMapper.selectVoList(lqw);
        fillFragmentCount(list);
        return list;
    }

    private void fillFragmentCount(List<KnowledgeAttachVo> records) {
        if (records == null || records.isEmpty()) return;
        List<String> docIds = records.stream()
            .map(KnowledgeAttachVo::getDocId)
            .filter(StringUtils::isNotBlank)
            .distinct()
            .collect(Collectors.toList());
        if (docIds.isEmpty()) return;
        List<DocFragmentCountVo> countList = knowledgeFragmentMapper.selectFragmentCountByDocIds(docIds);
        Map<String, Integer> countMap = countList.stream()
            .collect(Collectors.toMap(DocFragmentCountVo::getDocId, DocFragmentCountVo::getFragmentCount, (k1, k2) -> k1));
        for (KnowledgeAttachVo vo : records) {
            vo.setFragmentCount(countMap.getOrDefault(vo.getDocId(), 0));
        }
    }

    private LambdaQueryWrapper<KnowledgeAttach> buildQueryWrapper(KnowledgeAttachBo bo) {
        LambdaQueryWrapper<KnowledgeAttach> lqw = Wrappers.lambdaQuery();
        lqw.orderByAsc(KnowledgeAttach::getId);
        lqw.eq(bo.getKnowledgeId() != null, KnowledgeAttach::getKnowledgeId, bo.getKnowledgeId());
        lqw.like(StringUtils.isNotBlank(bo.getName()), KnowledgeAttach::getName, bo.getName());
        lqw.eq(StringUtils.isNotBlank(bo.getType()), KnowledgeAttach::getType, bo.getType());
        lqw.eq(bo.getOssId() != null, KnowledgeAttach::getOssId, bo.getOssId());
        return lqw;
    }

    @Override
    public Boolean insertByBo(KnowledgeAttachBo bo) {
        KnowledgeAttach add = MapstructUtils.convert(bo, KnowledgeAttach.class);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(KnowledgeAttachBo bo) {
        KnowledgeAttach update = MapstructUtils.convert(bo, KnowledgeAttach.class);
        return baseMapper.updateById(update) > 0;
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        // 删除附件前，同步清理其片段记录与向量库中的向量
        List<KnowledgeAttach> attaches = baseMapper.selectByIds(ids);
        for (KnowledgeAttach attach : attaches) {
            String docId = attach.getDocId();
            String kid = String.valueOf(attach.getKnowledgeId());
            vectorStoreService.removeByDocId(docId, kid);
            knowledgeFragmentMapper.delete(
                Wrappers.<KnowledgeFragment>lambdaQuery().eq(KnowledgeFragment::getDocId, docId));
            if (attach.getOssId() != null) {
                ossService.deleteFile(attach.getOssId());
            }
            knowledgeRetrievalService.invalidateKnowledge(kid);
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    @Override
    public void upload(KnowledgeInfoUploadBo bo) {
        MultipartFile file = bo.getFile();
        final String fileHash;
        try (InputStream input = file.getInputStream()) {
            fileHash = DigestUtil.sha256Hex(input);
        } catch (Exception e) {
            throw new ServiceException("计算文件摘要失败", e);
        }
        boolean duplicate = baseMapper.exists(Wrappers.<KnowledgeAttach>lambdaQuery()
            .eq(KnowledgeAttach::getKnowledgeId, bo.getKnowledgeId())
            .eq(KnowledgeAttach::getFileHash, fileHash));
        if (duplicate) {
            throw new ServiceException("该文件已上传，请勿重复提交");
        }
        OssDTO ossDTO = ossService.uploadFile(file);

        KnowledgeAttach knowledgeAttach = new KnowledgeAttach();
        knowledgeAttach.setKnowledgeId(bo.getKnowledgeId());
        knowledgeAttach.setOssId(ossDTO.getOssId());
        knowledgeAttach.setDocId(RandomUtil.randomString(10));
        knowledgeAttach.setFileHash(fileHash);
        knowledgeAttach.setName(ossDTO.getOriginalName());
        knowledgeAttach.setType(ossDTO.getFileSuffix());
        knowledgeAttach.setStatus(KnowledgeAttachStatus.WAITING.getCode()); // 待解析

        baseMapper.insert(knowledgeAttach);

        if (Boolean.TRUE.equals(bo.getAutoParse())) {
            // 通过 SpringUtils 获取代理对象，确保 @Async 生效
            SpringUtils.getBean(IKnowledgeAttachService.class).parse(knowledgeAttach.getId());
        }
    }

    @Async("knowledgeParseExecutor")
    @Override
    public void parse(Long id) {
        KnowledgeAttach attach = baseMapper.selectById(id);
        if (attach == null || KnowledgeAttachStatus.PARSING.getCode().equals(attach.getStatus())) {
            return;
        }

        int claimed = baseMapper.update(null, Wrappers.<KnowledgeAttach>lambdaUpdate()
            .set(KnowledgeAttach::getStatus, KnowledgeAttachStatus.PARSING.getCode())
            .set(KnowledgeAttach::getRemark, null)
            .eq(KnowledgeAttach::getId, id)
            .ne(KnowledgeAttach::getStatus, KnowledgeAttachStatus.PARSING.getCode()));
        if (claimed == 0) return;

        try {
            attach.setStatus(KnowledgeAttachStatus.PARSING.getCode()); // 解析中
            baseMapper.updateById(attach);

            log.info("开始解析知识库文档... id: {}, docId: {}", id, attach.getDocId());

            Long knowledgeId = attach.getKnowledgeId();
            String docId = attach.getDocId();
            KnowledgeInfoVo knowledgeInfoVo = knowledgeInfoService.queryById(knowledgeId);
            if (knowledgeInfoVo == null) {
                throw new ServiceException("知识库不存在: " + knowledgeId);
            }
            int blockSize = knowledgeInfoVo.getTextBlockSize() == null
                ? DocumentSplitConfig.DEFAULT_BLOCK_SIZE : knowledgeInfoVo.getTextBlockSize().intValue();
            int overlap = knowledgeInfoVo.getOverlapChar() == null
                ? DocumentSplitConfig.DEFAULT_OVERLAP : knowledgeInfoVo.getOverlapChar().intValue();
            DocumentSplitConfig splitConfig = new DocumentSplitConfig(
                knowledgeInfoVo.getSeparator(), blockSize, overlap, attach.getType());

            // 获取文件信息并下载
            List<OssDTO> ossDTOs = ossService.selectByIds(String.valueOf(attach.getOssId()));
            if (ossDTOs == null || ossDTOs.isEmpty()) {
                throw new RuntimeException("未找到对应的 OSS 文件信息");
            }
            OssDTO ossDTO = ossDTOs.get(0);
            String content;
            ResourceLoader resourceLoader = resourceLoaderFactory.getLoaderByFileType(attach.getType());
            try (InputStream inputStream = new URL(ossDTO.getUrl()).openStream()) {
                content = resourceLoader.getContent(inputStream);
            }
            List<String> chunkList = resourceLoader.getChunkList(content, splitConfig);

            if (CollUtil.isEmpty(chunkList)) {
                throw new RuntimeException("文档分片结果为空，请检查文档内容或分片器是否支持该文件类型");
            }

            // 重新解析前先清理旧的向量数据，避免向量重复累积
            List<String> fids = new ArrayList<>();
            List<KnowledgeFragment> knowledgeFragmentList = new ArrayList<>();
            for (int i = 0; i < chunkList.size(); i++) {
                String fid = RandomUtil.randomString(10);
                fids.add(fid);
                KnowledgeFragment knowledgeFragment = new KnowledgeFragment();
                knowledgeFragment.setKnowledgeId(knowledgeId);
                knowledgeFragment.setDocId(docId);
                knowledgeFragment.setFid(fid);
                knowledgeFragment.setIdx(i);
                knowledgeFragment.setContent(chunkList.get(i));
                knowledgeFragment.setCreateTime(new Date());
                knowledgeFragmentList.add(knowledgeFragment);
            }
            ChatModelVo chatModelVo = chatModelService.selectModelByName(knowledgeInfoVo.getEmbeddingModel());

            StoreEmbeddingBo storeEmbeddingBo = new StoreEmbeddingBo();
            storeEmbeddingBo.setKid(String.valueOf(knowledgeId));
            storeEmbeddingBo.setDocId(docId);
            storeEmbeddingBo.setFids(fids);
            storeEmbeddingBo.setChunkList(chunkList);
            storeEmbeddingBo.setVectorStoreName(knowledgeInfoVo.getVectorModel());
            storeEmbeddingBo.setEmbeddingModelName(knowledgeInfoVo.getEmbeddingModel());
            storeEmbeddingBo.setApiKey(chatModelVo.getApiKey());
            storeEmbeddingBo.setBaseUrl(chatModelVo.getApiHost());
            try {
                // 写入新向量前，先按 docId 清理该文档的旧向量：
                // 历史数据的片段 fid 为迁移脚本回填的 MD5 值，与向量库中实际存储的 fid 不一致，
                // 按 fid 删除无法命中旧向量，会导致重复向量累积；按 docId 清理对三种向量库均一致有效。
                vectorStoreService.removeByDocId(docId, String.valueOf(knowledgeId));
                vectorStoreService.storeEmbeddings(storeEmbeddingBo);
            } catch (Exception vectorError) {
                for (String newFid : fids) {
                    try {
                        vectorStoreService.removeByFid(newFid, String.valueOf(knowledgeId));
                    } catch (Exception cleanupError) {
                        log.error("补偿删除新向量失败, kid={}, fid={}", knowledgeId, newFid, cleanupError);
                    }
                }
                throw vectorError;
            }

            knowledgeFragmentMapper.delete(Wrappers.<KnowledgeFragment>lambdaQuery().eq(KnowledgeFragment::getDocId, docId));
            knowledgeFragmentMapper.insertBatch(knowledgeFragmentList);
            knowledgeRetrievalService.invalidateKnowledge(String.valueOf(knowledgeId));

            attach.setStatus(KnowledgeAttachStatus.COMPLETED.getCode()); // 已完成
            baseMapper.updateById(attach);
            log.info("知识库文档解析、向量化并入库成功！id: {}", id);
        } catch (Exception e) {
            log.error("解析文档失败！id: {}, error: {}", id, e.getMessage(), e);
            attach.setStatus(KnowledgeAttachStatus.FAILED.getCode()); // 失败
            attach.setRemark(StringUtils.substring(e.getMessage(), 0, 255)); // 保存错误原因，截取防止溢出
            baseMapper.updateById(attach);
        }
    }

    @Override
    public KnowledgeReparseVo reparseKnowledge(Long knowledgeId) {
        List<KnowledgeAttach> attachments = baseMapper.selectList(
            Wrappers.<KnowledgeAttach>lambdaQuery().eq(KnowledgeAttach::getKnowledgeId, knowledgeId));
        int submitted = 0;
        int skipped = 0;
        IKnowledgeAttachService proxy = SpringUtils.getBean(IKnowledgeAttachService.class);
        for (KnowledgeAttach attachment : attachments) {
            if (KnowledgeAttachStatus.PARSING.getCode().equals(attachment.getStatus())) {
                skipped++;
            } else {
                proxy.parse(attachment.getId());
                submitted++;
            }
        }
        return new KnowledgeReparseVo(submitted, skipped, attachments.size());
    }
}
