package org.ruoyi.demo.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.idev.excel.write.metadata.WriteSheet;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.constant.SystemConstants;
import org.ruoyi.common.core.utils.StreamUtils;
import org.ruoyi.common.core.utils.file.FileUtils;
import org.ruoyi.common.excel.core.DropDownOptions;
import org.ruoyi.common.excel.utils.ExcelUtil;
import org.ruoyi.common.excel.utils.ExcelWriterWrapper;
import org.ruoyi.demo.domain.vo.ExportDemoVo;
import org.ruoyi.demo.service.IExportExcelService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 导出下拉框Excel示例
 *
 * @author Emil.Zhang
 */
@Service
@RequiredArgsConstructor
public class ExportExcelServiceImpl implements IExportExcelService {

    @Override
    public void exportWithOptions(HttpServletResponse response) {
        // 创建表格数据，业务中一般通过数据库查询
        List<ExportDemoVo> excelDataList = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            // 模拟数据库中的一条数据
            ExportDemoVo everyRowData = new ExportDemoVo();
            everyRowData.setNickName("用户-" + i);
            everyRowData.setUserStatus(SystemConstants.NORMAL);
            everyRowData.setGender("1");
            everyRowData.setPhoneNumber(String.format("175%08d", i));
            everyRowData.setEmail(String.format("175%08d", i) + "@163.com");
            everyRowData.setProvinceId(i);
            everyRowData.setCityId(i);
            everyRowData.setAreaId(i);
            excelDataList.add(everyRowData);
        }

        // 通过@ExcelIgnoreUnannotated配合@ExcelProperty合理显示需要的列
        // 并通过@DropDown注解指定下拉值，或者通过创建ExcelOptions来指定下拉框
        // 使用ExcelOptions时建议指定列index，防止出现下拉列解析不对齐

        // 首先从数据库中查询下拉框内的可选项
        // 这里模拟查询结果
        List<DemoCityData> provinceList = getProvinceList(),
            cityList = getCityList(provinceList),
            areaList = getAreaList(cityList);
        int provinceIndex = 5, cityIndex = 6, areaIndex = 7;

        DropDownOptions provinceToCity = DropDownOptions.buildLinkedOptions(
            provinceList,
            provinceIndex,
            cityList,
            cityIndex,
            DemoCityData::getId,
            DemoCityData::getPid,
            everyOptions -> DropDownOptions.createOptionValue(
                everyOptions.getName(),
                everyOptions.getId()
            )
        );

        DropDownOptions cityToArea = DropDownOptions.buildLinkedOptions(
            cityList,
            cityIndex,
            areaList,
            areaIndex,
            DemoCityData::getId,
            DemoCityData::getPid,
            everyOptions -> DropDownOptions.createOptionValue(
                everyOptions.getName(),
                everyOptions.getId()
            )
        );

        // 把所有的下拉框存储
        List<DropDownOptions> options = new ArrayList<>();
        options.add(provinceToCity);
        options.add(cityToArea);

        // 到此为止所有的下拉框可选项已全部配置完毕

        // 接下来需要将Excel中的展示数据转换为对应的下拉选
        List<ExportDemoVo> outList = StreamUtils.toList(excelDataList, everyRowData -> {
            // 只需要处理没有使用@ExcelDictFormat注解的下拉框
            // 一般来说，可以直接在数据库查询即查询出省市县信息，这里通过模拟操作赋值
            everyRowData.setProvince(buildOptions(provinceList, everyRowData.getProvinceId()));
            everyRowData.setCity(buildOptions(cityList, everyRowData.getCityId()));
            everyRowData.setArea(buildOptions(areaList, everyRowData.getAreaId()));
            return everyRowData;
        });

        ExcelUtil.exportExcel(outList, "下拉框示例", ExportDemoVo.class, response, options);
    }

    private String buildOptions(List<DemoCityData> cityDataList, Integer id) {
        Map<Integer, List<DemoCityData>> groupByIdMap =
            cityDataList.stream().collect(Collectors.groupingBy(DemoCityData::getId));
        if (groupByIdMap.containsKey(id)) {
            DemoCityData demoCityData = groupByIdMap.get(id).get(0);
            return DropDownOptions.createOptionValue(demoCityData.getName(), demoCityData.getId());
        } else {
            return StrUtil.EMPTY;
        }
    }

    /**
     * 模拟查询数据库操作
     *
     * @return /
     */
    private List<DemoCityData> getProvinceList() {
        List<DemoCityData> provinceList = new ArrayList<>();

        // 实际业务中一般采用数据库读取的形式，这里直接拼接创建
        provinceList.add(new DemoCityData(0, null, "P100000"));
        provinceList.add(new DemoCityData(1, null, "P200000"));
        provinceList.add(new DemoCityData(2, null, "P300000"));

        return provinceList;
    }

    /**
     * 模拟查找数据库操作，需要连带查询出省的数据
     *
     * @param provinceList 模拟的父省数据
     * @return /
     */
    private List<DemoCityData> getCityList(List<DemoCityData> provinceList) {
        List<DemoCityData> cityList = new ArrayList<>();

        // 实际业务中一般采用数据库读取的形式，这里直接拼接创建
        cityList.add(new DemoCityData(0, 0, "C110000"));
        cityList.add(new DemoCityData(1, 0, "C120000"));
        cityList.add(new DemoCityData(2, 1, "C210000"));
        cityList.add(new DemoCityData(3, 1, "C220000"));
        cityList.add(new DemoCityData(4, 1, "C230000"));

        selectParentData(provinceList, cityList);

        return cityList;
    }

    /**
     * 模拟查找数据库操作，需要连带查询出市的数据
     *
     * @param cityList 模拟的父市数据
     * @return /
     */
    private List<DemoCityData> getAreaList(List<DemoCityData> cityList) {
        List<DemoCityData> areaList = new ArrayList<>();

        int minCount = 500;
        int maxCount = 10000;

        // 实际业务中一般采用数据库读取的形式，这里直接拼接创建
        for (int i = 0; i < RandomUtil.randomInt(minCount, maxCount); i++) {
            areaList.add(new DemoCityData(areaList.size(), 0, String.format("A11%04d", i)));
        }

        for (int i = 0; i < RandomUtil.randomInt(minCount, maxCount); i++) {
            areaList.add(new DemoCityData(areaList.size(), 1, String.format("A12%04d", i)));
        }

        for (int i = 0; i < RandomUtil.randomInt(minCount, maxCount); i++) {
            areaList.add(new DemoCityData(areaList.size(), 2, String.format("A21%04d", i)));
        }

        for (int i = 0; i < RandomUtil.randomInt(minCount, maxCount); i++) {
            areaList.add(new DemoCityData(areaList.size(), 3, String.format("A22%04d", i)));
        }

        for (int i = 0; i < RandomUtil.randomInt(minCount, maxCount); i++) {
            areaList.add(new DemoCityData(areaList.size(), 4, String.format("A23%04d", i)));
        }

        selectParentData(cityList, areaList);

        return areaList;
    }

    /**
     * 模拟数据库的查询父数据操作
     *
     * @param parentList /
     * @param sonList    /
     */
    private void selectParentData(List<DemoCityData> parentList, List<DemoCityData> sonList) {
        Map<Integer, List<DemoCityData>> parentGroupByIdMap =
            parentList.stream().collect(Collectors.groupingBy(DemoCityData::getId));

        sonList.forEach(everySon -> {
            if (parentGroupByIdMap.containsKey(everySon.getPid())) {
                everySon.setPData(parentGroupByIdMap.get(everySon.getPid()).get(0));
            }
        });
    }

    /**
     * 模拟的数据库省市县
     */
    @Data
    private static class DemoCityData {
        /**
         * 数据库id字段
         */
        private Integer id;
        /**
         * 数据库pid字段
         */
        private Integer pid;
        /**
         * 数据库name字段
         */
        private String name;
        /**
         * MyBatisPlus连带查询父数据
         */
        private DemoCityData pData;

        public DemoCityData(Integer id, Integer pid, String name) {
            this.id = id;
            this.pid = pid;
            this.name = name;
        }
    }


    @Override
    public void customExport(HttpServletResponse response) throws IOException {
        String filename = ExcelUtil.encodingFilename("自定义导出");
        FileUtils.setAttachmentResponseHeader(response, filename);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8");

        ExcelUtil.exportExcel(ExportDemoVo.class, response.getOutputStream(), wrapper -> {
            // 创建表格数据，业务中一般通过数据库查询
            List<ExportDemoVo> excelDataList = new ArrayList<>();
            for (int i = 0; i < 30; i++) {
                // 模拟数据库中的一条数据
                ExportDemoVo everyRowData = new ExportDemoVo();
                everyRowData.setNickName("用户-" + i);
                everyRowData.setUserStatus(SystemConstants.NORMAL);
                everyRowData.setGender("1");
                everyRowData.setPhoneNumber(String.format("175%08d", i));
                everyRowData.setEmail(String.format("175%08d", i) + "@163.com");
                everyRowData.setProvinceId(i);
                everyRowData.setCityId(i);
                everyRowData.setAreaId(i);
                excelDataList.add(everyRowData);
            }

            // 创建表格
            WriteSheet sheet = ExcelWriterWrapper.sheetBuilder("自定义导出demo")
                // 合并单元格
                // .registerWriteHandler(new CellMergeStrategy(excelDataList, true))
                .build();


            wrapper.write(excelDataList, sheet);

            List<ExportDemoVo> excelDataList2 = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                int index = 1000 + i;
                // 模拟数据库中的一条数据
                ExportDemoVo everyRowData = new ExportDemoVo();
                everyRowData.setNickName("用户-" + index);
                everyRowData.setUserStatus(SystemConstants.NORMAL);
                everyRowData.setGender("1");
                everyRowData.setPhoneNumber(String.format("175%08d", index));
                everyRowData.setEmail(String.format("175%08d", index) + "@163.com");
                everyRowData.setProvinceId(index);
                everyRowData.setCityId(index);
                everyRowData.setAreaId(index);
                excelDataList2.add(everyRowData);
            }

            wrapper.write(excelDataList2, sheet);

            // 或者在同一个excel中创建多个表格
            // WriteSheet sheet2 = ExcelWriterWrapper.sheetBuilder("自定义导出demo2").build();
            // wrapper.write(excelDataList2, sheet2);
        });
    }
}
