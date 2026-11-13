package net.jxdg.travel.modules.product.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductExcelRow {
    @ExcelProperty("产品标题")
    private String title;
    @ExcelProperty("品类")
    private String category;
    @ExcelProperty("天数")
    private Integer days;
    @ExcelProperty("出发地")
    private String departure;
    @ExcelProperty("目的地")
    private String destination;
    @ExcelProperty("成人价")
    private BigDecimal adultPrice;
    @ExcelProperty("儿童价")
    private BigDecimal childPrice;
    @ExcelProperty("单房差")
    private BigDecimal singleRoomDiff;
    @ExcelProperty("库存")
    private Integer stock;
    @ExcelProperty("费用包含")
    private String includeText;
    @ExcelProperty("费用不含")
    private String excludeText;
}
