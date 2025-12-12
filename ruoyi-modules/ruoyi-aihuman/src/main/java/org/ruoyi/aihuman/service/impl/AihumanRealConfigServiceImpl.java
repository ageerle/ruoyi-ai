package org.ruoyi.aihuman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinNT;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.ruoyi.aihuman.domain.AihumanRealConfig;
import org.ruoyi.aihuman.domain.bo.AihumanRealConfigBo;
import org.ruoyi.aihuman.domain.vo.AihumanRealConfigVo;
import org.ruoyi.aihuman.mapper.AihumanRealConfigMapper;
import org.ruoyi.aihuman.service.AihumanRealConfigService;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.redis.utils.RedisUtils;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 真人交互数字人配置Service业务层处理
 *
 * @author ageerle
 * @date Tue Oct 21 11:46:52 GMT+08:00 2025
 */
@RequiredArgsConstructor
@Service
public class AihumanRealConfigServiceImpl implements AihumanRealConfigService {

    private static final Logger log = LoggerFactory.getLogger(AihumanRealConfigServiceImpl.class);
    private final AihumanRealConfigMapper baseMapper;
    // 存储当前运行的进程，用于停止操作
    private volatile Process runningProcess = null;

    /**
     * 查询真人交互数字人配置
     */
    @Override
    public AihumanRealConfigVo queryById(Integer id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询真人交互数字人配置列表
     */
    @Override
    public TableDataInfo<AihumanRealConfigVo> queryPageList(AihumanRealConfigBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<AihumanRealConfig> lqw = buildQueryWrapper(bo);
        Page<AihumanRealConfigVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询真人交互数字人配置列表
     */
    @Override
    public List<AihumanRealConfigVo> queryList(AihumanRealConfigBo bo) {
        LambdaQueryWrapper<AihumanRealConfig> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<AihumanRealConfig> buildQueryWrapper(AihumanRealConfigBo bo) {
        LambdaQueryWrapper<AihumanRealConfig> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getName()), AihumanRealConfig::getName, bo.getName());
        lqw.like(StringUtils.isNotBlank(bo.getAvatars()), AihumanRealConfig::getAvatars, bo.getAvatars());
        lqw.like(StringUtils.isNotBlank(bo.getModels()), AihumanRealConfig::getModels, bo.getModels());
        lqw.eq(StringUtils.isNotBlank(bo.getAvatarsParams()), AihumanRealConfig::getAvatarsParams, bo.getAvatarsParams());
        lqw.eq(StringUtils.isNotBlank(bo.getModelsParams()), AihumanRealConfig::getModelsParams, bo.getModelsParams());
        lqw.eq(StringUtils.isNotBlank(bo.getAgentParams()), AihumanRealConfig::getAgentParams, bo.getAgentParams());
        lqw.eq(bo.getCreateTime() != null, AihumanRealConfig::getCreateTime, bo.getCreateTime());
        lqw.eq(bo.getUpdateTime() != null, AihumanRealConfig::getUpdateTime, bo.getUpdateTime());
        lqw.eq(bo.getStatus() != null, AihumanRealConfig::getStatus, bo.getStatus());
        lqw.eq(bo.getPublish() != null, AihumanRealConfig::getPublish, bo.getPublish());
        lqw.eq(StringUtils.isNotBlank(bo.getRunParams()), AihumanRealConfig::getRunParams, bo.getRunParams());
        // 添加runStatus字段的查询条件
        lqw.eq(StringUtils.isNotBlank(bo.getRunStatus()), AihumanRealConfig::getRunStatus, bo.getRunStatus());
        lqw.eq(StringUtils.isNotBlank(bo.getCreateDept()), AihumanRealConfig::getCreateDept, bo.getCreateDept());
        lqw.eq(StringUtils.isNotBlank(bo.getCreateBy()), AihumanRealConfig::getCreateBy, bo.getCreateBy());
        lqw.eq(StringUtils.isNotBlank(bo.getUpdateBy()), AihumanRealConfig::getUpdateBy, bo.getUpdateBy());
        return lqw;
    }

    /**
     * 新增真人交互数字人配置
     */
    @Override
    public Boolean insertByBo(AihumanRealConfigBo bo) {
        AihumanRealConfig add = MapstructUtils.convert(bo, AihumanRealConfig.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改真人交互数字人配置
     */
    @Override
    public Boolean updateByBo(AihumanRealConfigBo bo) {
        AihumanRealConfig update = MapstructUtils.convert(bo, AihumanRealConfig.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(AihumanRealConfig entity) {
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除真人交互数字人配置
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Integer> ids, Boolean isValid) {
        if (isValid) {
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    /**
     * 执行真人交互数字人配置
     * 通过主键获取数据库记录，然后从run_params字段读取命令并执行
     */
    @Override
    public Boolean runByBo(AihumanRealConfigBo bo) {
        try {
            // 1. 通过主键获取数据库记录
            Integer id = bo.getId();
            if (id == null) {
                log.error("执行命令失败：主键ID为空");
                throw new RuntimeException("执行命令失败：主键ID为空");
            }

            // 检查是否已经有对应的进程在运行
            String redisKey = "aihuman:process:" + id;
            String existingPid = RedisUtils.getCacheObject(redisKey);
            if (StringUtils.isNotEmpty(existingPid) && isProcessRunning(existingPid)) {
                log.warn("ID为{}的配置已有进程在运行，进程ID: {}", id, existingPid);
                // 刷新run_status状态为运行中
                AihumanRealConfig updateStatus = new AihumanRealConfig();
                updateStatus.setId(id);
                updateStatus.setRunStatus("1"); // 1表示运行中
                baseMapper.updateById(updateStatus);
                return true;
            }

            // 查询数据库记录
            AihumanRealConfig config = baseMapper.selectById(id);
            if (config == null) {
                log.error("执行命令失败：未找到ID为{}的配置记录", id);
                throw new RuntimeException("执行命令失败：未找到对应的配置记录");
            }

            // 2. 从记录中获取run_params字段
            String runParams = config.getRunParams();
            if (StringUtils.isBlank(runParams)) {
                log.error("执行命令失败：ID为{}的记录中run_params字段为空", id);
                throw new RuntimeException("执行命令失败：run_params字段为空");
            }

            // 3. 解析并执行命令
            // 将多行命令合并为一个命令字符串
            String[] commands = runParams.split("\\r?\\n");
            if (commands.length == 0) {
                log.error("执行命令失败：runParams中没有有效的命令");
                throw new RuntimeException("执行命令失败：runParams中没有有效的命令");
            }

            // 将所有命令合并到一个命令字符串中，使用&&连接，确保在同一个进程中执行
            StringBuilder mergedCmd = new StringBuilder();
            for (int i = 0; i < commands.length; i++) {
                String command = commands[i].trim();
                if (command.isEmpty()) {
                    continue;
                }

                if (mergedCmd.length() > 0) {
                    mergedCmd.append(" && ");
                }

                mergedCmd.append(command);
            }

            String cmd = "cmd.exe /c " + mergedCmd.toString();
            log.info("准备执行合并命令：{}", cmd);

            // 更新数据库中的运行状态为运行中
            AihumanRealConfig updateStatus = new AihumanRealConfig();
            updateStatus.setId(id);
            updateStatus.setRunStatus("1"); // 1表示运行中
            baseMapper.updateById(updateStatus);

            // 使用线程池执行命令并监听输出
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    Process process = Runtime.getRuntime().exec(cmd);
                    // 保存进程引用，用于后续停止操作
                    runningProcess = process;

                    // 获取进程ID并保存到Redis
                    String pid = getProcessId(process);
                    if (!"unknown".equals(pid)) {
                        RedisUtils.setCacheObject(redisKey, pid);
                        log.info("保存进程ID到Redis：key={}, pid={}", redisKey, pid);
                    }

                    // 读取标准输出
                    new Thread(() -> {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                log.info("[LiveTalking] {}", line);
                            }
                        } catch (IOException e) {
                            log.error("读取命令输出失败", e);
                        }
                    }).start();

                    // 读取debug输出
                    new Thread(() -> {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                log.debug("[LiveTalking DEBUG] {}", line);
                            }
                        } catch (IOException e) {
                            log.error("读取命令debug输出失败", e);
                        }
                    }).start();

                    // 等待进程结束
                    int exitCode = process.waitFor();
                    log.info("LiveTalking进程结束，退出码: {}", exitCode);

                    // 进程结束后更新数据库状态为已停止
                    AihumanRealConfig endStatus = new AihumanRealConfig();
                    endStatus.setId(id);
                    endStatus.setRunStatus("0"); // 0表示已停止
                    baseMapper.updateById(endStatus);

                    // 进程结束后从Redis中删除进程ID
                    RedisUtils.deleteObject(redisKey);
                    log.info("从Redis中删除进程ID：key={}", redisKey);

                    // 进程结束后清空引用
                    runningProcess = null;
                } catch (Exception e) {
                    log.error("执行命令失败", e);
                    // 发生异常时更新数据库状态为失败
                    try {
                        AihumanRealConfig errorStatus = new AihumanRealConfig();
                        errorStatus.setId(id);
                        errorStatus.setRunStatus("2"); // 2表示启动失败
                        baseMapper.updateById(errorStatus);
                    } catch (Exception ex) {
                        log.error("更新状态失败", ex);
                    }
                    // 发生异常时从Redis中删除进程ID
                    RedisUtils.deleteObject(redisKey);
                    // 发生异常时清空引用
                    runningProcess = null;
                }
            });

            executor.shutdown();
            return true;
        } catch (Exception e) {
            log.error("执行命令过程中发生异常", e);
            return false;
        }
    }

    /**
     * 检查进程是否正在运行
     *
     * @param pid 进程ID
     * @return 是否正在运行
     */
    private boolean isProcessRunning(String pid) {
        if (StringUtils.isEmpty(pid) || "unknown".equals(pid)) {
            return false;
        }

        try {
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
            ProcessBuilder processBuilder;

            if (isWindows) {
                processBuilder = new ProcessBuilder("tasklist", "/FI", "PID eq " + pid);
            } else {
                processBuilder = new ProcessBuilder("ps", "-p", pid);
            }

            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            // 在Windows上，tasklist命令如果找不到进程，退出码也是0，但输出中不会包含PID
            if (isWindows) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains(pid)) {
                            return true;
                        }
                    }
                }
                return false;
            } else {
                // 在Linux/Mac上，ps命令如果找不到进程，退出码不为0
                return exitCode == 0;
            }
        } catch (Exception e) {
            log.error("检查进程是否运行失败, pid={}", pid, e);
            return false;
        }
    }

    /**
     * 停止正在运行的真人交互数字人配置任务
     */
    @Override
    public Boolean stopByBo(AihumanRealConfigBo bo) {
        try {
            Integer id = bo.getId();
            String redisKey = "aihuman:process:" + id;

            // 首先检查Redis中是否有对应的进程ID
            String pid = RedisUtils.getCacheObject(redisKey);
            if (StringUtils.isNotEmpty(pid)) {
                // 如果Redis中有进程ID，先尝试通过进程ID停止进程
                try {
                    // 根据操作系统类型，使用不同的命令终止进程树
                    if (System.getProperty("os.name").toLowerCase().contains("win")) {
                        // Windows系统使用taskkill命令终止进程树
                        log.info("通过Redis中的PID停止进程: taskkill /F /T /PID {}", pid);
                        Process killProcess = Runtime.getRuntime().exec("taskkill /F /T /PID " + pid);
                        // 等待kill命令执行完成
                        killProcess.waitFor(5, TimeUnit.SECONDS);
                    } else {
                        // Linux/Mac系统使用pkill命令终止进程树
                        Runtime.getRuntime().exec("pkill -P " + pid);
                    }
                } catch (Exception e) {
                    log.error("通过Redis中的PID停止进程失败", e);
                }
            }

            // 然后检查本地runningProcess引用
            if (runningProcess != null && runningProcess.isAlive()) {
                log.info("正在停止LiveTalking进程...");
                // 强制销毁进程树，确保完全停止
                destroyProcessTree(runningProcess);

                // 更新数据库中的运行状态为已停止
                AihumanRealConfig updateStatus = new AihumanRealConfig();
                updateStatus.setId(id);
                updateStatus.setRunStatus("0"); // 0表示已停止
                baseMapper.updateById(updateStatus);

                runningProcess = null;
                log.info("LiveTalking进程已停止");
            } else {
                log.warn("没有正在运行的LiveTalking进程");
                // 确保数据库中的状态也是已停止
                AihumanRealConfig updateStatus = new AihumanRealConfig();
                updateStatus.setId(id);
                updateStatus.setRunStatus("0"); // 0表示已停止
                baseMapper.updateById(updateStatus);
            }

            // 无论如何都从Redis中删除进程ID
            RedisUtils.deleteObject(redisKey);
            log.info("从Redis中删除进程ID：key={}", redisKey);

            return true;
        } catch (Exception e) {
            log.error("停止进程时发生异常", e);
            // 发生异常时也尝试从Redis中删除进程ID
            try {
                RedisUtils.deleteObject("aihuman:process:" + bo.getId());
            } catch (Exception ex) {
                log.error("从Redis中删除进程ID失败", ex);
            }
            return false;
        }
    }

    /**
     * 销毁进程及其子进程（进程树）
     *
     * @param process 要销毁的进程
     */
    private void destroyProcessTree(Process process) {
        try {
            if (process.isAlive()) {
                // 获取进程ID
                String pid = getProcessId(process);
                log.info("获取到进程ID: {}", pid);

                // 根据操作系统类型，使用不同的命令终止进程树
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    // Windows系统使用taskkill命令终止进程树
                    log.info("执行taskkill命令终止进程树: taskkill /F /T /PID {}", pid);
                    Process killProcess = Runtime.getRuntime().exec("taskkill /F /T /PID " + pid);
                    // 等待kill命令执行完成
                    killProcess.waitFor(5, TimeUnit.SECONDS);
                } else {
                    // Linux/Mac系统使用pkill命令终止进程树
                    Runtime.getRuntime().exec("pkill -P " + pid);
                    process.destroy();
                }
            }
        } catch (Exception e) {
            log.error("销毁进程树时发生异常", e);
            // 如果出现异常，尝试使用普通销毁方法
            process.destroy();
            try {
                // 强制销毁
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
            } catch (Exception ex) {
                log.error("强制销毁进程失败", ex);
            }
        }
    }

    /**
     * 获取进程ID
     *
     * @param process 进程对象
     * @return 进程ID
     */
    private String getProcessId(Process process) {
        try {
            // 不同JVM实现可能有所不同，这里尝试通过反射获取
            if (process.getClass().getName().equals("java.lang.Win32Process") ||
                    process.getClass().getName().equals("java.lang.ProcessImpl")) {
                Field f = process.getClass().getDeclaredField("handle");
                f.setAccessible(true);
                long handl = f.getLong(process);
                Kernel32 kernel = Kernel32.INSTANCE;
                WinNT.HANDLE handle = new WinNT.HANDLE();
                handle.setPointer(Pointer.createConstant(handl));
                return String.valueOf(kernel.GetProcessId(handle));
            } else if (process.getClass().getName().equals("java.lang.UNIXProcess")) {
                Field f = process.getClass().getDeclaredField("pid");
                f.setAccessible(true);
                return String.valueOf(f.getInt(process));
            }
        } catch (Exception e) {
            log.error("获取进程ID失败", e);
        }

        // 如果反射获取失败，尝试通过wmic命令获取
        try {
            // 对于Windows系统，可以尝试使用wmic命令获取进程ID
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                ProcessHandle.Info info = process.toHandle().info();
                return String.valueOf(process.toHandle().pid());
            }
        } catch (Exception e) {
            log.error("通过ProcessHandle获取进程ID失败", e);
        }

        return "unknown";
    }

    @PreDestroy
    public void onDestroy() {
        if (runningProcess != null && runningProcess.isAlive()) {
            try {
                log.info("应用关闭，正在停止数字人进程");
                destroyProcessTree(runningProcess);

                // 查找所有运行状态为运行中的配置，并更新为已停止
                LambdaQueryWrapper<AihumanRealConfig> lqw = Wrappers.lambdaQuery();
                lqw.eq(AihumanRealConfig::getRunStatus, "1");
                List<AihumanRealConfig> runningConfigs = baseMapper.selectList(lqw);
                for (AihumanRealConfig config : runningConfigs) {
                    config.setRunStatus("0");
                    baseMapper.updateById(config);

                    // 从Redis中删除对应的进程ID记录
                    String redisKey = "aihuman:process:" + config.getId();
                    RedisUtils.deleteObject(redisKey);
                    log.info("应用关闭，从Redis中删除进程ID：key={}", redisKey);
                }
            } catch (Exception e) {
                log.error("停止数字人进程失败", e);
                // 即使发生异常，也尝试清理Redis中的进程ID记录
                try {
                    LambdaQueryWrapper<AihumanRealConfig> lqw = Wrappers.lambdaQuery();
                    lqw.eq(AihumanRealConfig::getRunStatus, "1");
                    List<AihumanRealConfig> runningConfigs = baseMapper.selectList(lqw);
                    for (AihumanRealConfig config : runningConfigs) {
                        RedisUtils.deleteObject("aihuman:process:" + config.getId());
                    }
                } catch (Exception ex) {
                    log.error("清理Redis中的进程ID记录失败", ex);
                }
            }
        }
    }

    // JNA接口定义，用于Windows系统获取进程ID
    interface Kernel32 extends Library {
        Kernel32 INSTANCE = (Kernel32) Native.loadLibrary("kernel32", Kernel32.class);

        int GetProcessId(WinNT.HANDLE hProcess);
    }
}