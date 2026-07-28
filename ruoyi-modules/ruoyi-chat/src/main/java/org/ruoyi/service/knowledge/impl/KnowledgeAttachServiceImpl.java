package org.ruoyi.service.knowledge.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.oss.domain.vo.SysOssUploadVo;
import org.ruoyi.common.oss.domain.vo.UploadVo;
import org.ruoyi.common.oss.enums.UploadModeType;
import org.ruoyi.common.oss.factory.UploadServiceFactory;
import org.ruoyi.common.oss.service.IUploadService;
import org.ruoyi.enums.KnowledgeAttachStatus;
import org.ruoyi.common.core.domain.dto.OssDTO;
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
import org.ruoyi.factory.ResourceLoaderFactory;
import org.ruoyi.factory.VectorStoreStrategyFactory;
import org.ruoyi.mapper.knowledge.KnowledgeAttachMapper;
import org.ruoyi.mapper.knowledge.KnowledgeFragmentMapper;
import org.ruoyi.service.knowledge.IKnowledgeAttachService;
import org.ruoyi.service.knowledge.IKnowledgeInfoService;
import org.ruoyi.service.knowledge.ResourceLoader;
import org.ruoyi.service.vector.VectorStoreService;
import org.ruoyi.system.service.ISysConfigService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
    private final VectorStoreStrategyFactory vectorStoreStrategyFactory;
    private final OssService ossService;
    private final UploadServiceFactory uploadServiceFactory;
    private final ISysConfigService sysConfigService;

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

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        for (Long id : ids) {
            KnowledgeAttach attach = baseMapper.selectById(id);
            if (attach != null) {
                // 根据向量ID获取向量实体
                Long knowledgeId = attach.getKnowledgeId();
                KnowledgeInfoVo knowledgeInfoVo = knowledgeInfoService.queryById(knowledgeId);
                if (null == knowledgeInfoVo){
                    throw new ServiceException("知识库不存在");
                }
                // 获取向量存储策略
                VectorStoreService vectorStoreService = vectorStoreStrategyFactory.getStrategy(knowledgeInfoVo.getVectorModel());
                vectorStoreService.removeByDocId(attach.getDocId(), String.valueOf(attach.getKnowledgeId()));
                knowledgeFragmentMapper.delete(Wrappers.<KnowledgeFragment>lambdaQuery()
                    .eq(KnowledgeFragment::getDocId, attach.getDocId()));
                log.info("删除附件: id={}, name={}", attach.getId(), attach.getName());
            }
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    @Override
    public void upload(KnowledgeInfoUploadBo bo) {
        MultipartFile file = bo.getFile();
        // 上传文件
        String actualCode = StringUtils.defaultIfEmpty(initUploadMode(), UploadModeType.DEFAULT.getCode());
        IUploadService uploadService = uploadServiceFactory.getOriginalService(actualCode);
        MultipartFile[] files = {file};
        UploadVo uploadVo = uploadService.upload(files);
        List<SysOssUploadVo> uploadVos = uploadVo.getUploadVos();
        if (CollectionUtils.isEmpty(uploadVos)){
            throw new ServiceException("上传文件信息异常");
        }
        SysOssUploadVo ossUploadVo = uploadVos.getFirst();

        // 根据向量ID获取向量实体
        Long knowledgeId = bo.getKnowledgeId();
        KnowledgeInfoVo knowledgeInfoVo = knowledgeInfoService.queryById(knowledgeId);
        if (null == knowledgeInfoVo){
            throw new ServiceException("知识库不存在");
        }
        // 获取向量存储策略
        VectorStoreService vectorStoreService = vectorStoreStrategyFactory.getStrategy(knowledgeInfoVo.getVectorModel());
        // 同一知识库下同名文件，先删旧的（附件 + 切片 + 向量）
        String fileName = file.getOriginalFilename();
        List<KnowledgeAttach> exists = baseMapper.selectList(
            Wrappers.<KnowledgeAttach>lambdaQuery()
                .eq(KnowledgeAttach::getKnowledgeId, knowledgeId)
                .eq(KnowledgeAttach::getName, fileName));
        for (KnowledgeAttach old : exists) {
            vectorStoreService.removeByDocId(old.getDocId(), String.valueOf(old.getKnowledgeId()));
            knowledgeFragmentMapper.delete(Wrappers.<KnowledgeFragment>lambdaQuery()
                .eq(KnowledgeFragment::getDocId, old.getDocId()));
            baseMapper.deleteById(old.getId());
            log.info("删除旧附件: id={}, name={}", old.getId(), fileName);
        }

        KnowledgeAttach knowledgeAttach = new KnowledgeAttach();
        knowledgeAttach.setKnowledgeId(knowledgeId);
        knowledgeAttach.setOssId(Long.valueOf(ossUploadVo.getOssId()));
        knowledgeAttach.setDocId(RandomUtil.randomString(10));
        knowledgeAttach.setName(fileName);
        knowledgeAttach.setType(ossUploadVo.getFileType());
        knowledgeAttach.setStatus(KnowledgeAttachStatus.WAITING.getCode()); // 待解析

        baseMapper.insert(knowledgeAttach);

        if (Boolean.TRUE.equals(knowledgeInfoVo.getAutoParse())) {
            // 通过 SpringUtils 获取代理对象，确保 @Async 生效
            SpringUtils.getBean(IKnowledgeAttachService.class).parse(knowledgeAttach.getId());
        }
    }

    @Async("knowledgeParseExecutor")
    @Override
    public void parse(Long id) {
        KnowledgeAttach attach = baseMapper.selectById(id);
        if (attach == null || (!KnowledgeAttachStatus.WAITING.getCode().equals(attach.getStatus()) && !KnowledgeAttachStatus.FAILED.getCode().equals(attach.getStatus()))) {
            return;
        }

        try {
            attach.setStatus(KnowledgeAttachStatus.PARSING.getCode()); // 解析中
            baseMapper.updateById(attach);

            log.info("开始解析知识库文档... id: {}, docId: {}", id, attach.getDocId());

            Long knowledgeId = attach.getKnowledgeId();
            String docId = attach.getDocId();

            // 获取文件信息并下载
            List<OssDTO> ossDTOs = ossService.selectByIds(String.valueOf(attach.getOssId()));
            if (ossDTOs == null || ossDTOs.isEmpty()) {
                throw new RuntimeException("未找到对应的 OSS 文件信息");
            }
            OssDTO ossDTO = ossDTOs.get(0);
            String content;
            String urlStr = ossDTO.getUrl();
            ResourceLoader resourceLoader = resourceLoaderFactory.getLoaderByFileType(attach.getType());
            try (InputStream inputStream = urlStr.startsWith("http://") || urlStr.startsWith("https://") ?
                new URL(urlStr).openStream() : new FileInputStream(urlStr)) {
                // 3. 统一读取内容
                content = resourceLoader.getContent(inputStream);
            }
            List<String> chunkList = resourceLoader.getChunkList(content, String.valueOf(knowledgeId));

            List<String> fids = new ArrayList<>();
            List<KnowledgeFragment> knowledgeFragmentList = new ArrayList<>();
            if (CollUtil.isNotEmpty(chunkList)) {
                for (int i = 0; i < chunkList.size(); i++) {
                    String fid = RandomUtil.randomString(10);
                    fids.add(fid);
                    KnowledgeFragment knowledgeFragment = new KnowledgeFragment();
                    knowledgeFragment.setKnowledgeId(knowledgeId);
                    knowledgeFragment.setDocId(docId);
                    knowledgeFragment.setIdx(i);
                    knowledgeFragment.setContent(chunkList.get(i));
                    knowledgeFragment.setCreateTime(new Date());
                    knowledgeFragmentList.add(knowledgeFragment);
                }
                knowledgeFragmentMapper.delete(Wrappers.<KnowledgeFragment>lambdaQuery().eq(KnowledgeFragment::getDocId, docId));
                knowledgeFragmentMapper.insertBatch(knowledgeFragmentList);
                log.info("文档切片并入库完成，共计 {} 个片段。id: {}", chunkList.size(), id);
            }

            KnowledgeInfoVo knowledgeInfoVo = knowledgeInfoService.queryById(knowledgeId);
            // 获取向量存储策略
            VectorStoreService vectorStoreService = vectorStoreStrategyFactory.getStrategy(knowledgeInfoVo.getVectorModel());
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

    /**
     * 上传模式
     */
    private String initUploadMode(){
        return sysConfigService.selectConfigByKey("upload.mode");
    }
}
