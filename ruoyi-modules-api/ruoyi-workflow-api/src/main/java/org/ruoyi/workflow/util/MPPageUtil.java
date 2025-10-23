package org.ruoyi.workflow.util;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

@Slf4j
public class MPPageUtil {

    private MPPageUtil() {
    }

    public static <T, U> Page<U> convertToPage(Page<T> source, Class<U> targetRecordClass) {
        return MPPageUtil.convertToPage(source, new Page<>(), targetRecordClass, null);
    }

    public static <T, U> Page<U> convertToPage(Page<T> source, Page<U> target, Class<U> targetRecordClass) {
        return MPPageUtil.convertToPage(source, target, targetRecordClass, null);
    }

    public static <T, U> Page<U> convertToPage(Page<T> source, Page<U> target, Class<U> targetRecordClass, BiFunction<T, U, U> biFunction) {
        BeanUtils.copyProperties(source, target);
        List<U> records = new ArrayList<>();
        target.setRecords(records);
        try {
            for (T t : source.getRecords()) {
                U u = targetRecordClass.getDeclaredConstructor().newInstance();
                BeanUtils.copyProperties(t, u);
                if (null != biFunction) {
                    biFunction.apply(t, u);
                }
                records.add(u);
            }
        } catch (NoSuchMethodException e1) {
            log.error("convertTo error1", e1);
        } catch (Exception e2) {
            log.error("convertTo error2", e2);
        }

        return target;
    }

    public static <T, U> List<U> convertToList(List<T> source, Class<U> targetRecordClass) {
        return convertToList(source, targetRecordClass, null);
    }

    public static <T, U> List<U> convertToList(List<T> source, Class<U> targetRecordClass, BiFunction<T, U, U> biFunction) {
        if (CollectionUtils.isEmpty(source)) {
            return Collections.emptyList();
        }
        List<U> result = new ArrayList<>();
        for (T t : source) {
            try {
                U u = targetRecordClass.getDeclaredConstructor().newInstance();
                BeanUtils.copyProperties(t, u);
                if (null != biFunction) {
                    biFunction.apply(t, u);
                }
                result.add(u);
            } catch (NoSuchMethodException e1) {
                log.error("convertTo error1", e1);
            } catch (Exception e2) {
                log.error("convertTo error2", e2);
            }
        }
        return result;
    }

    public static <T, U> U convertTo(T source, Class<U> targetClass) {
        try {
            U target = targetClass.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, target);
            return target;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
