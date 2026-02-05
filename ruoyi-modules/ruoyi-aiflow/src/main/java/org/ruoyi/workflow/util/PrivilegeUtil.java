package org.ruoyi.workflow.util;

import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import org.ruoyi.common.core.exception.base.BaseException;
import org.ruoyi.workflow.base.ThreadContext;
import org.ruoyi.workflow.enums.ErrorEnum;

import static org.ruoyi.workflow.cosntant.AdiConstant.*;

public class PrivilegeUtil {

    private PrivilegeUtil() {
    }

    public static <T> T checkAndGetByUuid(String uuid, QueryChainWrapper<T> lambdaQueryChainWrapper, ErrorEnum exceptionMessage) {
        return checkAndGet(null, uuid, lambdaQueryChainWrapper, exceptionMessage);
    }

    public static <T> T checkAndGet(Long id, String uuid, QueryChainWrapper<T> lambdaQueryChainWrapper, ErrorEnum exceptionMessage) {
        T target;
        if (Boolean.TRUE.equals(ThreadContext.getCurrentUser().getIsAdmin())) {
            target = lambdaQueryChainWrapper.eq(null != id, COLUMN_NAME_ID, id).eq(null != uuid, COLUMN_NAME_UUID, uuid).eq(COLUMN_NAME_IS_DELETE, false).oneOpt().orElse(null);
        } else {
            target = lambdaQueryChainWrapper.eq(null != id, COLUMN_NAME_ID, id).eq(null != uuid, COLUMN_NAME_UUID, uuid).eq(COLUMN_NAME_USER_ID, ThreadContext.getCurrentUserId()).eq(COLUMN_NAME_IS_DELETE, false).oneOpt().orElse(null);
        }
        if (null == target) {
            throw new BaseException(exceptionMessage.getInfo());
        }
        return target;
    }

}
