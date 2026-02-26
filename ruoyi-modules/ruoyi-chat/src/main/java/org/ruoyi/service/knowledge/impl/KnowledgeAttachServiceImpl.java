package org.ruoyi.service.knowledge.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.core.domain.dto.OssDTO;
import org.ruoyi.common.core.service.OssService;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.domain.bo.knowledge.KnowledgeAttachBo;
import org.ruoyi.domain.bo.knowledge.KnowledgeInfoUploadBo;
import org.ruoyi.domain.bo.vector.StoreEmbeddingBo;
import org.ruoyi.domain.entity.knowledge.KnowledgeAttach;
import org.ruoyi.domain.entity.knowledge.KnowledgeFragment;
import org.ruoyi.domain.vo.knowledge.KnowledgeAttachVo;
import org.ruoyi.domain.vo.knowledge.KnowledgeInfoVo;
import org.ruoyi.factory.ResourceLoaderFactory;
import org.ruoyi.mapper.knowledge.KnowledgeAttachMapper;
import org.ruoyi.mapper.knowledge.KnowledgeFragmentMapper;
import org.ruoyi.service.knowledge.IKnowledgeAttachService;
import org.ruoyi.service.knowledge.IKnowledgeInfoService;
import org.ruoyi.service.knowledge.ResourceLoader;
import org.ruoyi.service.vector.VectorStoreService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

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
    /**
     * 查询知识库附件
     *
     * @param id 主键
     * @return 知识库附件
     */
    @Override
    public KnowledgeAttachVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 分页查询知识库附件列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 知识库附件分页列表
     */
    @Override
    public TableDataInfo<KnowledgeAttachVo> queryPageList(KnowledgeAttachBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<KnowledgeAttach> lqw = buildQueryWrapper(bo);
        Page<KnowledgeAttachVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询符合条件的知识库附件列表
     *
     * @param bo 查询条件
     * @return 知识库附件列表
     */
    @Override
    public List<KnowledgeAttachVo> queryList(KnowledgeAttachBo bo) {
        LambdaQueryWrapper<KnowledgeAttach> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<KnowledgeAttach> buildQueryWrapper(KnowledgeAttachBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<KnowledgeAttach> lqw = Wrappers.lambdaQuery();
        lqw.orderByAsc(KnowledgeAttach::getId);
        lqw.eq(bo.getKnowledgeId() != null, KnowledgeAttach::getKnowledgeId, bo.getKnowledgeId());
        lqw.like(StringUtils.isNotBlank(bo.getName()), KnowledgeAttach::getName, bo.getName());
        lqw.eq(StringUtils.isNotBlank(bo.getType()), KnowledgeAttach::getType, bo.getType());
        lqw.eq(bo.getOssId() != null, KnowledgeAttach::getOssId, bo.getOssId());
        return lqw;
    }

    /**
     * 新增知识库附件
     *
     * @param bo 知识库附件
     * @return 是否新增成功
     */
    @Override
    public Boolean insertByBo(KnowledgeAttachBo bo) {
        KnowledgeAttach add = MapstructUtils.convert(bo, KnowledgeAttach.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改知识库附件
     *
     * @param bo 知识库附件
     * @return 是否修改成功
     */
    @Override
    public Boolean updateByBo(KnowledgeAttachBo bo) {
        KnowledgeAttach update = MapstructUtils.convert(bo, KnowledgeAttach.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(KnowledgeAttach entity){
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 校验并批量删除知识库附件信息
     *
     * @param ids     待删除的主键集合
     * @param isValid 是否进行有效性校验
     * @return 是否删除成功
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if(isValid){
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    @Override
    public void upload(KnowledgeInfoUploadBo bo) {
        MultipartFile file = bo.getFile();
        // 保存文件信息
        OssDTO ossDTO = ossService.uploadFile(file);
        Long knowledgeId = bo.getKnowledgeId();
        List<String> chunkList = new ArrayList<>();
        KnowledgeAttach knowledgeAttach = new KnowledgeAttach();
        knowledgeAttach.setKnowledgeId(bo.getKnowledgeId());
        String docId = RandomUtil.randomString(10);
        knowledgeAttach.setOssId(ossDTO.getOssId());
        knowledgeAttach.setDocId(docId);
        knowledgeAttach.setName(ossDTO.getOriginalName());
        knowledgeAttach.setType(ossDTO.getFileSuffix());
        String content = "";
        ResourceLoader resourceLoader = resourceLoaderFactory.getLoaderByFileType(knowledgeAttach.getType());
        // 文档分段入库
        List<String> fids = new ArrayList<>();
        try {
            content = resourceLoader.getContent(file.getInputStream());
            chunkList = resourceLoader.getChunkList(content, String.valueOf(knowledgeId));
            List<KnowledgeFragment> knowledgeFragmentList = new ArrayList<>();
            if (CollUtil.isNotEmpty(chunkList)) {
                for (int i = 0; i < chunkList.size(); i++) {
                    // 生成知识片段ID
                    String fid = RandomUtil.randomString(10);
                    fids.add(fid);
                    KnowledgeFragment knowledgeFragment = new KnowledgeFragment();
                    knowledgeFragment.setDocId(docId);
                    knowledgeFragment.setIdx(i);
                    knowledgeFragment.setContent(chunkList.get(i));
                    knowledgeFragment.setCreateTime(new Date());
                    knowledgeFragmentList.add(knowledgeFragment);
                }
            }
            knowledgeFragmentMapper.insertBatch(knowledgeFragmentList);
        } catch (IOException e) {
            log.error("保存知识库信息失败！{}", e.getMessage());
        }
        baseMapper.insert(knowledgeAttach);

        // 查询知识库信息
        KnowledgeInfoVo knowledgeInfoVo = knowledgeInfoService.queryById(knowledgeId);

        // 查询向量模信息
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
        vectorStoreService.storeEmbeddings(storeEmbeddingBo);
    }

}
