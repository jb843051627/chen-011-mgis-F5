package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Date;

/**
 * 用密核准申请图幅行对象 t_mgis_use_sheet
 *
 * <p>受理那一刻把申请的图幅代号一次挂齐，受理即锁：核准之后各栏锁住，
 * 图幅行也不动；要改只能另起新件。密级、出图年份是受理五样里的单件口径，
 * 存在核准单上；一行一幅，册内行次自 1 连排，册内代号不重号。</p>
 *
 * @author fuce
 * @date 2026-09-29
 */
@TableName("t_mgis_use_sheet")
@ApiModel(value = "TMgisUseSheet", description = "用密核准申请图幅行(受理即锁)")
public class TMgisUseSheet implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 核准单主键 */
    @TableField("bill_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "核准单主键")
    private Long billId;

    /** 册内行次（自 1 连排） */
    @TableField("line_no")
    @ApiModelProperty(value = "册内行次")
    private Integer lineNo;

    /** 图幅代号 */
    @TableField("sheet_code")
    @ApiModelProperty(value = "图幅代号")
    private String sheetCode;

    /** 创建者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public Integer getLineNo() {
        return lineNo;
    }

    public void setLineNo(Integer lineNo) {
        this.lineNo = lineNo;
    }

    public String getSheetCode() {
        return sheetCode;
    }

    public void setSheetCode(String sheetCode) {
        this.sheetCode = sheetCode;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
