package net.jxdg.travel.modules.order.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class TouristRow {
    @ExcelProperty("订单号")
    private String orderNo;
    @ExcelProperty("姓名")
    private String name;
    @ExcelProperty("证件类型")
    private String idType;
    @ExcelProperty("证件号")
    private String idNoMask;
    @ExcelProperty("手机号")
    private String phoneMask;
    @ExcelProperty("成人/儿童")
    private String isAdult;
    @ExcelProperty("核销状态")
    private String verifyStatus;
}
