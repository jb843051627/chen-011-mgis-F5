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
 * 用密成果领用清单行对象 t_mgis_use_checklist
 *
 * <p>核准件一键生成：图幅代号、密级、出图年份、批准文号统统从核准那一份带过，
 * 窗口不再誊第二遍；一家单位一册图对一条清单。本行只在生成那一刻由系统写入，
 * 之后不改不删——手抄错一个字，两头账又是一年糊涂官司。</p>
 *
 * @author fuce
 * @date 2026-09-29
 */
@TableName("t_mgis_use_checklist")
@ApiModel(value = "TMgisUseChecklist", description = "用密成果领用清单行(核准件一键带出)")
public class TMgisUseChecklist implements Serializable {
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

    /** 领用清单号（同单一个号） */
    @TableField("checklist_no")
    @ApiModelProperty(value = "领用清单号")
    private String checklistNo;

    /** 清单行次（照申请册内行次） */
    @TableField("line_no")
    @ApiModelProperty(value = "清单行次")
    private Integer lineNo;

    /** 图幅代号（核准件带过） */
    @TableField("sheet_code")
    @ApiModelProperty(value = "图幅代号")
    private String sheetCode;

    /** 密级（核准单带过） */
    @TableField("secret_level")
    @ApiModelProperty(value = "密级")
    private String secretLevel;

    /** 出图年份（核准单带过） */
    @TableField("map_year")
    @ApiModelProperty(value = "出图年份")
    private Integer mapYear;

    /** 批准文号（核准件带过） */
    @TableField("approved_doc_no")
    @ApiModelProperty(value = "批准文号")
    private String approvedDocNo;

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

    public String getChecklistNo() {
        return checklistNo;
    }

    public void setChecklistNo(String checklistNo) {
        this.checklistNo = checklistNo;
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

    public String getSecretLevel() {
        return secretLevel;
    }

    public void setSecretLevel(String secretLevel) {
        this.secretLevel = secretLevel;
    }

    public Integer getMapYear() {
        return mapYear;
    }

    public void setMapYear(Integer mapYear) {
        this.mapYear = mapYear;
    }

    public String getApprovedDocNo() {
        return approvedDocNo;
    }

    public void setApprovedDocNo(String approvedDocNo) {
        this.approvedDocNo = approvedDocNo;
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
