package org.ruoyi.service.impl;

import cn.hutool.core.util.ObjectUtil;
import java.util.stream.Collectors;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.core.page.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.ruoyi.domain.KnowledgeAttachPic;
import org.ruoyi.domain.vo.KnowledgeAttachVo;
import org.ruoyi.mapper.KnowledgeAttachPicMapper;
import org.ruoyi.mapper.KnowledgeFragmentMapper;
import org.ruoyi.system.service.ISysOssService;
import org.springframework.stereotype.Service;
import org.ruoyi.domain.bo.KnowledgeAttachBo;

import org.ruoyi.domain.KnowledgeAttach;
import org.ruoyi.mapper.KnowledgeAttachMapper;
import org.ruoyi.service.IKnowledgeAttachService;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * 知识库附件Service业务层处理
 *
 * @author ageerle
 * @date 2025-04-08
 */
@RequiredArgsConstructor
@Service
public class KnowledgeAttachServiceImpl implements IKnowledgeAttachService {

  private final KnowledgeAttachMapper baseMapper;
  private final KnowledgeFragmentMapper fragmentMapper;

  private final ISysOssService ossService;

  private final KnowledgeAttachPicMapper picMapper;

  /**
   * 查询知识库附件
   */
  @Override
  public KnowledgeAttachVo queryById(Long id) {
    return baseMapper.selectVoById(id);
  }

  /**
   * 查询知识库附件列表
   */
  @Override
  public TableDataInfo<KnowledgeAttachVo> queryPageList(KnowledgeAttachBo bo, PageQuery pageQuery) {
    LambdaQueryWrapper<KnowledgeAttach> lqw = buildQueryWrapper(bo);
    Page<KnowledgeAttachVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
    return TableDataInfo.build(result);
  }

  /**
   * 查询知识库附件列表
   */
  @Override
  public List<KnowledgeAttachVo> queryList(KnowledgeAttachBo bo) {
    LambdaQueryWrapper<KnowledgeAttach> lqw = buildQueryWrapper(bo);
    return baseMapper.selectVoList(lqw);
  }

  private LambdaQueryWrapper<KnowledgeAttach> buildQueryWrapper(KnowledgeAttachBo bo) {
    Map<String, Object> params = bo.getParams();
    LambdaQueryWrapper<KnowledgeAttach> lqw = Wrappers.lambdaQuery();
    lqw.eq(StringUtils.isNotBlank(bo.getKid()), KnowledgeAttach::getKid, bo.getKid());
    lqw.eq(StringUtils.isNotBlank(bo.getDocId()), KnowledgeAttach::getDocId, bo.getDocId());
    lqw.like(StringUtils.isNotBlank(bo.getDocName()), KnowledgeAttach::getDocName, bo.getDocName());
    lqw.eq(StringUtils.isNotBlank(bo.getDocType()), KnowledgeAttach::getDocType, bo.getDocType());
    lqw.eq(StringUtils.isNotBlank(bo.getContent()), KnowledgeAttach::getContent, bo.getContent());
    return lqw;
  }

  /**
   * 新增知识库附件
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
  private void validEntityBeforeSave(KnowledgeAttach entity) {
    //TODO 做一些数据校验,如唯一约束
  }

  /**
   * 批量删除知识库附件
   */
  @Override
  public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
    if (isValid) {
      //TODO 做一些业务上的校验,判断是否需要校验
    }
    return baseMapper.deleteBatchIds(ids) > 0;
  }

  @Override
  public void removeKnowledgeAttach(String docId) {
    Map<String, Object> map = new HashMap<>();
    map.put("doc_id", docId);
    List<KnowledgeAttachVo> knowledgeAttachVos = baseMapper.selectVoByMap(map);
    if (ObjectUtil.isNotEmpty(knowledgeAttachVos)) {
      Collection<Long> ossIds = knowledgeAttachVos.stream()
          .map(KnowledgeAttachVo::getOssId)
          .collect(Collectors.toList());
      //删除oss
      ossService.deleteWithValidByIds(ossIds, false);
      //删除图片oss
      List<KnowledgeAttachPic> knowledgeAttachPics = picMapper.selectList(
          new LambdaQueryWrapper<KnowledgeAttachPic>()
              .in(KnowledgeAttachPic::getKid,
                  knowledgeAttachVos.stream().map(KnowledgeAttachVo::getKid)
                      .collect(Collectors.toList()))
              .in(KnowledgeAttachPic::getAid,
                  knowledgeAttachVos.stream().map(KnowledgeAttachVo::getId)
                      .collect(Collectors.toList()))
          );
      if (ObjectUtil.isNotEmpty(knowledgeAttachPics)) {
        Collection<Long> tossIds = knowledgeAttachPics.stream()
            .map(KnowledgeAttachPic::getOssId)
            .collect(Collectors.toList());
        ossService.deleteWithValidByIds(tossIds, false);
        List<Long> collect = knowledgeAttachPics.stream().map(KnowledgeAttachPic::getId)
            .collect(Collectors.toList());
        picMapper.deleteByIds(collect);
      }
    }
    baseMapper.deleteByMap(map);
    fragmentMapper.deleteByMap(map);
  }

  @Override
  public String translationByFile(MultipartFile file, String targetLanguage) {
        /*String fileName = file.getOriginalFilename();
        String docType = fileName.substring(fileName.lastIndexOf(".")+1);
        String content = "";
        ResourceLoader resourceLoader = resourceLoaderFactory.getLoaderByFileType(docType);
        try {
            content = resourceLoader.getContent(file.getInputStream());
        } catch (IOException e) {
            throw new BaseException("该文件类型暂不支持！");
        }
        // 翻译模型固定为gpt-4o-mini
        String model = "gpt-4o-mini";
        ChatMessageBo chatMessageBo = new ChatMessageBo();
        chatMessageBo.setUserId(getUserId());
        chatMessageBo.setModelName(model);
        chatMessageBo.setContent(content);
        chatMessageBo.setDeductCost(0.01);
        chatMessageBo.setTotalTokens(0);
        OpenAiStreamClient openAiStreamClient = chatConfig.getOpenAiStreamClient();
        List<Message> messageList = new ArrayList<>();
        Message sysMessage = Message.builder().role(Message.Role.SYSTEM).content("你是一位精通各国语言的翻译大师\n" +
            "\n" +
            "请将用户输入词语翻译成{" + targetLanguage + "}\n" +
            "\n" +
            "==示例输出==\n" +
            "**原文** : <这里显示要翻译的原文信息>\n" +
            "**翻译** : <这里显示翻译之后的结果>\n" +
            "**总结** : <这里是对关键信息一个总结>\n" +
            "**提取的关键信息** : <这里返回关键信息>\n" +
            "==示例结束==\n" +
            "\n" +
            "注意：请严格按示例进行输出，返回markdown格式").build();
        messageList.add(sysMessage);
        Message message = Message.builder().role(Message.Role.USER).content(content).build();
        messageList.add(message);
        ChatCompletionResponse chatCompletionResponse = null;
        try {
            ChatCompletion chatCompletion = ChatCompletion
                .builder()
                .messages(messageList)
                .model(model)
                .stream(false)
                .build();
            chatCompletionResponse = openAiStreamClient.chatCompletion(chatCompletion);
        }catch (Exception e) {
            throw new BaseException("调用大模型失败，请检查密钥是否正确！");
        }
        return chatCompletionResponse.getChoices().get(0).getMessage().getContent().toString();*/
    return "接口开发中!";
  }
}
