package org.ruoyi.workflow.service;

import org.ruoyi.common.core.domain.dto.UserDTO;

import java.util.List;

/**
 * 流程设计器-获取办理人
 *
 * @author AprilWind
 */
public interface IFlwTaskAssigneeService {

    /**
     * 批量解析多个存储标识符（storageIds），按类型分类并合并查询用户列表
     * 输入格式支持多个以逗号分隔的标识（如 "user:123,role:456,789"）
     * 会自动去重返回结果，非法格式的标识将被忽略
     *
     * @param storageIds 多个存储标识符字符串（逗号分隔）
     * @return 合并后的用户列表，去重后返回，非法格式的标识将被跳过
     */
    List<UserDTO> fetchUsersByStorageIds(String storageIds);

}
