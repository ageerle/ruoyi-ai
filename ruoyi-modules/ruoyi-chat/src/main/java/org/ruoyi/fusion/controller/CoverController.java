package org.ruoyi.chat.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.system.domain.vo.cover.CoverParamVo;
import org.ruoyi.system.domain.vo.cover.CoverVo;
import org.ruoyi.system.domain.vo.cover.CoverCallbackVo;
import org.ruoyi.system.domain.vo.cover.MusicVo;
import org.ruoyi.system.service.ICoverService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 绘声美音-翻唱
 *
 * @author NSL
 * @since  2024-12-25
 */
@Api(tags = "歌曲翻唱")
@RequiredArgsConstructor
@RestController
@RequestMapping("/cover")
public class CoverController extends BaseController {

    private final ICoverService coverService;

    @ApiOperation(value = "查找歌曲")
    @GetMapping("/searchMusic")
    public R<List<MusicVo>> searchMusic(String musicName) {
        return R.ok(coverService.searchMusic(musicName));
    }

    @ApiOperation(value = "翻唱歌曲")
    @PostMapping("/saveCoverTask")
    public R<Void> saveCoverTask(@RequestBody CoverParamVo coverParamVo) {
        coverService.saveCoverTask(coverParamVo);
        return R.ok("翻唱歌曲处理中请等待10分钟-30分钟，翻唱结果请到翻唱记录中查询！");
    }

    @ApiOperation(value = "查询翻唱记录")
    @PostMapping("/searchCoverRecord")
    public R<TableDataInfo<CoverVo>> searchCoverRecord(@RequestBody PageQuery pageQuery) {
        return R.ok(coverService.searchCoverRecord(pageQuery));
    }

    @ApiOperation(value = "翻唱回调接口")
    @PostMapping("/callback")
    public R<Void> callback(@RequestBody CoverCallbackVo coverCallbackVo) {
        coverService.callback(coverCallbackVo);
        return R.ok();
    }
}
