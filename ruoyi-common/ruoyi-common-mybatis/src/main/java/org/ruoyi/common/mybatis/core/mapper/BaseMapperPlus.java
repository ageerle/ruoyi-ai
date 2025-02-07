package org.ruoyi.common.mybatis.core.mapper;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.ReflectionKit;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.ruoyi.common.core.utils.MapstructUtils;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 自定义 Mapper 接口, 实现 自定义扩展
 *
 * @param <T> table 泛型
 * @param <V> vo 泛型
 * @author Lion Li
 * @since 2021-05-13
 */
@SuppressWarnings("unchecked")
public interface BaseMapperPlus<T, V> extends BaseMapper<T> {

    Log log = LogFactory.getLog(BaseMapperPlus.class);

    default Class<V> currentVoClass() {
        return (Class<V>) ReflectionKit.getSuperClassGenericType(this.getClass(), BaseMapperPlus.class, 1);
    }

    default Class<T> currentModelClass() {
        return (Class<T>) ReflectionKit.getSuperClassGenericType(this.getClass(), BaseMapperPlus.class, 0);
    }

    default List<T> selectList() {
        return this.selectList(new QueryWrapper<>());
    }

    /**
     * 批量插入
     */
    default boolean insertBatch(Collection<T> entityList) {
        return Db.saveBatch(entityList);
    }

    /**
     * 批量更新
     */
    default boolean updateBatchById(Collection<T> entityList) {
        return Db.updateBatchById(entityList);
    }

    /**
     * 批量插入或更新
     */
    default boolean insertOrUpdateBatch(Collection<T> entityList) {
        return Db.saveOrUpdateBatch(entityList);
    }

    /**
     * 批量插入(包含限制条数)
     */
    default boolean insertBatch(Collection<T> entityList, int batchSize) {
        return Db.saveBatch(entityList, batchSize);
    }

    /**
     * 批量更新(包含限制条数)
     */
    default boolean updateBatchById(Collection<T> entityList, int batchSize) {
        return Db.updateBatchById(entityList, batchSize);
    }

    /**
     * 批量插入或更新(包含限制条数)
     */
    default boolean insertOrUpdateBatch(Collection<T> entityList, int batchSize) {
        return Db.saveOrUpdateBatch(entityList, batchSize);
    }

    /**
     * 插入或更新(包含限制条数)
     */
    default boolean insertOrUpdate(T entity) {
        return Db.saveOrUpdate(entity);
    }

    default V selectVoById(Serializable id) {
        return selectVoById(id, this.currentVoClass());
    }

    /**
     * 根据 ID 查询
     */
    default <C> C selectVoById(Serializable id, Class<C> voClass) {
        T obj = this.selectById(id);
        if (ObjectUtil.isNull(obj)) {
            return null;
        }
        return MapstructUtils.convert(obj, voClass);
    }

    default List<V> selectVoBatchIds(Collection<? extends Serializable> idList) {
        return selectVoBatchIds(idList, this.currentVoClass());
    }

    /**
     * 查询（根据ID 批量查询）
     */
    default <C> List<C> selectVoBatchIds(Collection<? extends Serializable> idList, Class<C> voClass) {
        List<T> list = this.selectBatchIds(idList);
        if (CollUtil.isEmpty(list)) {
            return CollUtil.newArrayList();
        }
        return MapstructUtils.convert(list, voClass);
    }

    default List<V> selectVoByMap(Map<String, Object> map) {
        return selectVoByMap(map, this.currentVoClass());
    }

    /**
     * 查询（根据 columnMap 条件）
     */
    default <C> List<C> selectVoByMap(Map<String, Object> map, Class<C> voClass) {
        List<T> list = this.selectByMap(map);
        if (CollUtil.isEmpty(list)) {
            return CollUtil.newArrayList();
        }
        return MapstructUtils.convert(list, voClass);
    }

    default V selectVoOne(Wrapper<T> wrapper) {
        return selectVoOne(wrapper, this.currentVoClass());
    }

    /**
     * 根据 entity 条件，查询一条记录
     */
    default <C> C selectVoOne(Wrapper<T> wrapper, Class<C> voClass) {
        T obj = this.selectOne(wrapper);
        if (ObjectUtil.isNull(obj)) {
            return null;
        }
        return MapstructUtils.convert(obj, voClass);
    }

    default List<V> selectVoList() {
        return selectVoList(new QueryWrapper<>(), this.currentVoClass());
    }

    default List<V> selectVoList(Wrapper<T> wrapper) {
        return selectVoList(wrapper, this.currentVoClass());
    }

    /**
     * 根据 entity 条件，查询全部记录
     */
    default <C> List<C> selectVoList(Wrapper<T> wrapper, Class<C> voClass) {
        List<T> list = this.selectList(wrapper);
        if (CollUtil.isEmpty(list)) {
            return CollUtil.newArrayList();
        }
        return MapstructUtils.convert(list, voClass);
    }

    default <P extends IPage<V>> P selectVoPage(IPage<T> page, Wrapper<T> wrapper) {
        return selectVoPage(page, wrapper, this.currentVoClass());
    }

    /**
     * 分页查询VO
     */
    default <C, P extends IPage<C>> P selectVoPage(IPage<T> page, Wrapper<T> wrapper, Class<C> voClass) {
        IPage<T> pageData = this.selectPage(page, wrapper);
        IPage<C> voPage = new Page<>(pageData.getCurrent(), pageData.getSize(), pageData.getTotal());
        if (CollUtil.isEmpty(pageData.getRecords())) {
            return (P) voPage;
        }
        voPage.setRecords(MapstructUtils.convert(pageData.getRecords(), voClass));
        return (P) voPage;
    }

    default <C> List<C> selectObjs(Wrapper<T> wrapper, Function<? super Object, C> mapper) {
        return this.selectObjs(wrapper).stream().filter(Objects::nonNull).map(mapper).collect(Collectors.toList());
    }

}
