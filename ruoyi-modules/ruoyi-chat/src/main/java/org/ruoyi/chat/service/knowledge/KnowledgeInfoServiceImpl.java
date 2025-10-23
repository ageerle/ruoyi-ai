package org.ruoyi.chat.service.knowledge;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.ruoyi.chain.loader.ResourceLoader;
import org.ruoyi.chain.loader.ResourceLoaderFactory;
import org.ruoyi.chat.enums.ChatModeType;
import org.ruoyi.common.core.domain.model.LoginUser;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.domain.*;
import org.ruoyi.domain.bo.KnowledgeInfoBo;
import org.ruoyi.domain.bo.KnowledgeInfoUploadBo;
import org.ruoyi.domain.bo.StoreEmbeddingBo;
import org.ruoyi.domain.vo.ChatModelVo;
import org.ruoyi.domain.vo.KnowledgeInfoVo;
import org.ruoyi.mapper.*;
import org.ruoyi.service.IChatModelService;
import org.ruoyi.service.IKnowledgeInfoService;
import org.ruoyi.service.VectorStoreService;
import org.ruoyi.system.service.ISysOssService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识库Service业务层处理
 *
 * @author ageerle
 * @date 2025-04-08
 */
@RequiredArgsConstructor
@Service
public class KnowledgeInfoServiceImpl implements IKnowledgeInfoService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeInfoServiceImpl.class);

    private final KnowledgeInfoMapper baseMapper;

    private final VectorStoreService vectorStoreService;

    private final ResourceLoaderFactory resourceLoaderFactory;

    private final KnowledgeFragmentMapper fragmentMapper;

    private final KnowledgeAttachMapper attachMapper;

    private final IChatModelService chatModelService;

    private final ISysOssService ossService;

    private final KnowledgeRoleMapper knowledgeRoleMapper;

    private final KnowledgeRoleRelationMapper knowledgeRoleRelationMapper;

    /**
     * 查询知识库
     */
    @Override
    public KnowledgeInfoVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询知识库列表
     */
    @Override
    public TableDataInfo<KnowledgeInfoVo> queryPageList(KnowledgeInfoBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<KnowledgeInfo> lqw = buildQueryWrapper(bo);
        Page<KnowledgeInfoVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 根据知识库角色查询知识库列表
     */
    @Override
    public TableDataInfo<KnowledgeInfoVo> queryPageListByRole(KnowledgeInfoBo bo, PageQuery pageQuery) {
        // 查询用户关联角色
        LoginUser loginUser = LoginHelper.getLoginUser();
        
        // 构建查询条件
        LambdaQueryWrapper<KnowledgeInfo> lqw = buildQueryWrapper(bo);
        
        // 管理员用户直接查询所有数据
        if (Objects.equals(loginUser.getUserId(), 1L)) {
            Page<KnowledgeInfoVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
            return TableDataInfo.build(result);
        }
        
        // 检查用户是否配置了角色信息
        if (StringUtils.isNotEmpty(loginUser.getKroleGroupIds()) && StringUtils.isNotEmpty(loginUser.getKroleGroupType())) {
            // 角色/角色组id列表
            List<String> groupIdList = Arrays.stream(loginUser.getKroleGroupIds().split(","))
                    .filter(StringUtils::isNotEmpty)
                    .toList();
            
            // 查询用户关联的角色
            List<KnowledgeRole> knowledgeRoles = new ArrayList<>();
            LambdaQueryWrapper<KnowledgeRole> roleLqw = Wrappers.lambdaQuery();
            if ("role".equals(loginUser.getKroleGroupType())) {
                roleLqw.in(KnowledgeRole::getId, groupIdList);
            } else {
                roleLqw.in(KnowledgeRole::getGroupId, groupIdList);
            }
            knowledgeRoles = knowledgeRoleMapper.selectList(roleLqw);

            // 如果用户有关联角色
            if (!CollectionUtils.isEmpty(knowledgeRoles)) {
                // 查询这些角色关联的知识库
                LambdaQueryWrapper<KnowledgeRoleRelation> relationLqw = Wrappers.lambdaQuery();
                relationLqw.in(KnowledgeRoleRelation::getKnowledgeRoleId, 
                    knowledgeRoles.stream().map(KnowledgeRole::getId).filter(Objects::nonNull).collect(Collectors.toList()));
                List<KnowledgeRoleRelation> knowledgeRoleRelations = knowledgeRoleRelationMapper.selectList(relationLqw);

                // 如果角色关联了知识库
                if (!CollectionUtils.isEmpty(knowledgeRoleRelations)) {
                    // 查询用户自己的知识库和角色分配的知识库
                    lqw.and(q -> q.eq(KnowledgeInfo::getUid, loginUser.getUserId())
                            .or()
                            .in(KnowledgeInfo::getId,
                                    knowledgeRoleRelations.stream()
                                            .map(KnowledgeRoleRelation::getKnowledgeId)
                                            .filter(Objects::nonNull)
                                            .collect(Collectors.toList())
                            )
                    );
                } else {
                    // 用户没有关联任何知识库，只显示自己的
                    lqw.eq(KnowledgeInfo::getUid, loginUser.getUserId());
                }
            } else {
                // 用户没有关联角色，只显示自己的
                lqw.eq(KnowledgeInfo::getUid, loginUser.getUserId());
            }
        } else {
            // 用户没有配置角色信息，只显示自己的
            lqw.eq(KnowledgeInfo::getUid, loginUser.getUserId());
        }
        
        Page<KnowledgeInfoVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询知识库列表
     */
    @Override
    public List<KnowledgeInfoVo> queryList(KnowledgeInfoBo bo) {
        LambdaQueryWrapper<KnowledgeInfo> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<KnowledgeInfo> buildQueryWrapper(KnowledgeInfoBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<KnowledgeInfo> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getKid()), KnowledgeInfo::getKid, bo.getKid());
        lqw.eq(bo.getUid() != null, KnowledgeInfo::getUid, bo.getUid());
        lqw.like(StringUtils.isNotBlank(bo.getKname()), KnowledgeInfo::getKname, bo.getKname());
        lqw.eq(bo.getShare() != null, KnowledgeInfo::getShare, bo.getShare());
        lqw.eq(StringUtils.isNotBlank(bo.getDescription()), KnowledgeInfo::getDescription,
                bo.getDescription());
        lqw.eq(StringUtils.isNotBlank(bo.getKnowledgeSeparator()), KnowledgeInfo::getKnowledgeSeparator,
                bo.getKnowledgeSeparator());
        lqw.eq(StringUtils.isNotBlank(bo.getQuestionSeparator()), KnowledgeInfo::getQuestionSeparator,
                bo.getQuestionSeparator());
        lqw.eq(bo.getOverlapChar() != null, KnowledgeInfo::getOverlapChar, bo.getOverlapChar());
        lqw.eq(bo.getRetrieveLimit() != null, KnowledgeInfo::getRetrieveLimit, bo.getRetrieveLimit());
        lqw.eq(bo.getTextBlockSize() != null, KnowledgeInfo::getTextBlockSize, bo.getTextBlockSize());
        return lqw;
    }

    /**
     * 新增知识库
     */
    @Override
    public Boolean insertByBo(KnowledgeInfoBo bo) {
        KnowledgeInfo add = MapstructUtils.convert(bo, KnowledgeInfo.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改知识库
     */
    @Override
    public Boolean updateByBo(KnowledgeInfoBo bo) {
        KnowledgeInfo update = MapstructUtils.convert(bo, KnowledgeInfo.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(KnowledgeInfo entity) {
        // TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除知识库
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOne(KnowledgeInfoBo bo) {
        KnowledgeInfo knowledgeInfo = MapstructUtils.convert(bo, KnowledgeInfo.class);
        if (StringUtils.isBlank(bo.getKid())) {
            String kid = RandomUtil.randomString(10);
            if (knowledgeInfo != null) {
                knowledgeInfo.setKid(kid);
                knowledgeInfo.setUid(LoginHelper.getLoginUser().getUserId());
            }
            baseMapper.insert(knowledgeInfo);
            if (knowledgeInfo != null) {
                vectorStoreService.createSchema(String.valueOf(knowledgeInfo.getId()), bo.getEmbeddingModelName());
            }
        } else {
            baseMapper.updateById(knowledgeInfo);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeKnowledge(String kid) {
        Map<String, Object> map = new HashMap<>();
        KnowledgeInfo knowledgeInfo = baseMapper.selectByKid(kid);

        check(knowledgeInfo);
        map.put("kid", knowledgeInfo.getKid());
        // 删除向量数据
        vectorStoreService.removeById(String.valueOf(knowledgeInfo.getId()), knowledgeInfo.getVectorModelName());
        // 删除附件和知识片段
        fragmentMapper.deleteByMap(map);
        attachMapper.deleteByMap(map);
        // 删除知识库
        map.put("kid", knowledgeInfo.getKid());
        baseMapper.deleteByMap(map);
    }

    @Override
    public void upload(KnowledgeInfoUploadBo bo) {
        storeContent(bo.getFile(), bo.getKid());
    }

    public void storeContent(MultipartFile file, String kid) {
        String fileName = file.getOriginalFilename();
        List<String> chunkList = new ArrayList<>();
        KnowledgeAttach knowledgeAttach = new KnowledgeAttach();
        knowledgeAttach.setKid(kid);
        String docId = RandomUtil.randomString(10);
        knowledgeAttach.setDocId(docId);
        knowledgeAttach.setDocName(fileName);
        knowledgeAttach.setDocType(fileName.substring(fileName.lastIndexOf(".") + 1));
        String content = "";
        ResourceLoader resourceLoader = resourceLoaderFactory.getLoaderByFileType(knowledgeAttach.getDocType());
        // 文档分段入库
        List<String> fids = new ArrayList<>();
        try {
            content = resourceLoader.getContent(file.getInputStream());
            chunkList = resourceLoader.getChunkList(content, kid);
            List<KnowledgeFragment> knowledgeFragmentList = new ArrayList<>();
            if (CollUtil.isNotEmpty(chunkList)) {
                for (int i = 0; i < chunkList.size(); i++) {
                    // 生成知识片段ID
                    String fid = RandomUtil.randomString(10);
                    fids.add(fid);
                    KnowledgeFragment knowledgeFragment = new KnowledgeFragment();
                    knowledgeFragment.setKid(kid);
                    knowledgeFragment.setDocId(docId);
                    knowledgeFragment.setFid(fid);
                    knowledgeFragment.setIdx(i);
                    knowledgeFragment.setContent(chunkList.get(i));
                    knowledgeFragment.setCreateTime(new Date());
                    knowledgeFragmentList.add(knowledgeFragment);
                }
            }
            fragmentMapper.insertBatch(knowledgeFragmentList);
        } catch (IOException e) {
            log.error("保存知识库信息失败！{}", e.getMessage());
        }
        knowledgeAttach.setContent(content);
        knowledgeAttach.setCreateTime(new Date());
        attachMapper.insert(knowledgeAttach);

        // 通过kid查询知识库信息
        KnowledgeInfoVo knowledgeInfoVo = baseMapper.selectVoOne(Wrappers.<KnowledgeInfo>lambdaQuery()
                .eq(KnowledgeInfo::getId, kid));

        // 通过向量模型查询模型信息
        ChatModelVo chatModelVo = chatModelService.selectModelByName(knowledgeInfoVo.getEmbeddingModelName());
        // 未查到指定模型时，回退为向量分类最高优先级模型
        if (chatModelVo == null) {
            chatModelVo = chatModelService.selectModelByCategoryWithHighestPriority(ChatModeType.VECTOR.getCode());
        }
        StoreEmbeddingBo storeEmbeddingBo = new StoreEmbeddingBo();
        storeEmbeddingBo.setKid(kid);
        storeEmbeddingBo.setDocId(docId);
        storeEmbeddingBo.setFids(fids);
        storeEmbeddingBo.setChunkList(chunkList);
        storeEmbeddingBo.setVectorStoreName(knowledgeInfoVo.getVectorModelName());
        storeEmbeddingBo.setEmbeddingModelName(knowledgeInfoVo.getEmbeddingModelName());
        storeEmbeddingBo.setApiKey(chatModelVo.getApiKey());
        storeEmbeddingBo.setBaseUrl(chatModelVo.getApiHost());
        vectorStoreService.storeEmbeddings(storeEmbeddingBo);
    }

    /**
     * 检查用户是否有删除知识库权限
     *
     * @param knowledgeInfo 知识库
     */
    public void check(KnowledgeInfo knowledgeInfo) {
        LoginUser loginUser = LoginHelper.getLoginUser();
        if (!knowledgeInfo.getUid().equals(loginUser.getUserId())) {
            throw new SecurityException("权限不足");
        }
    }

}
