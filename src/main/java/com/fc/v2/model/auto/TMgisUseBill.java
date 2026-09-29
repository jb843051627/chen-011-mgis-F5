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
 * 用密成果提供使用逐级核准单对象 t_mgis_use_bill
 *
 * <p>一份申请全程只凭这一张单，四步走：窗口受理、市局核用途、省厅两人会签、
 * 窗口凭单放成果。进门五样（申请单位、用途、图幅代号、密级、出图年份）自受理
 * 落笔起快照进单、往后锁定；省厅两枚齐即核准给号，要动只能另起新件。
 * 本阶已签人数 sign_count 由件自己记（与签署链同一处算），谁也不能往页面上手填。</p>
 *
 * @author fuce
 * @date 2026-09-29
 */
@TableName("t_mgis_use_bill")
@ApiModel(value = "TMgisUseBill", description = "用密成果提供使用逐级核准单")
public class TMgisUseBill implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 核准单号（系统给号，YM 打头） */
    @TableField("bill_no")
    @ApiModelProperty(value = "核准单号")
    private String billNo;

    /** 申请单位（受理五样之一） */
    @TableField("applicant_unit")
    @ApiModelProperty(value = "申请单位")
    private String applicantUnit;

    /** 用途（受理五样之一；市局核真假与作业范围搭不搭） */
    @TableField("purpose")
    @ApiModelProperty(value = "用途")
    private String purpose;

    /** 密级（受理五样之一；省厅一名盯密级与脱密） */
    @TableField("secret_level")
    @ApiModelProperty(value = "密级")
    private String secretLevel;

    /** 出图年份（受理五样之一） */
    @TableField("map_year")
    @ApiModelProperty(value = "出图年份")
    private Integer mapYear;

    /** 眼下停在哪一阶 WINDOW窗口受理 CITY市局核用途 PROVINCE省厅会签 ISSUE窗口放成果 */
    @TableField("station")
    @ApiModelProperty(value = "眼下停在哪一阶")
    private String station;

    /** 第几轮（省厅发回市局一轮加一） */
    @TableField("round_no")
    @ApiModelProperty(value = "第几轮")
    private Integer roundNo;

    /** 本阶应签人数（窗口、市局 1；省厅 2；放成果 1） */
    @TableField("need_count")
    @ApiModelProperty(value = "本阶应签人数")
    private Integer needCount;

    /** 本阶已签人数（件自己记，不许手填；与链上同口径） */
    @TableField("sign_count")
    @ApiModelProperty(value = "本阶已签人数")
    private Integer signCount;

    /** 情形 0在核 1已核准 2已否决（否决即定死） */
    @TableField("status")
    @ApiModelProperty(value = "情形 0在核 1已核准 2已否决")
    private Integer status;

    /** 批准文号（省厅两签齐时系统给号，清单照带） */
    @TableField("approved_doc_no")
    @ApiModelProperty(value = "批准文号")
    private String approvedDocNo;

    /** 核准定稿经办（末笔签署人） */
    @TableField("approved_by")
    @ApiModelProperty(value = "核准定稿经办")
    private String approvedBy;

    /** 核准时辰（系统写死，以秒计） */
    @TableField("approved_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "核准时辰")
    private Date approvedAt;

    /** 领用清单号（一键生成时给号；一家单位一册图对一条清单） */
    @TableField("checklist_no")
    @ApiModelProperty(value = "领用清单号")
    private String checklistNo;

    /** 清单已生成 0否 1是（核准后只生成一次） */
    @TableField("checklist_gen")
    @ApiModelProperty(value = "清单已生成 0否 1是")
    private Integer checklistGen;

    /** 放成果经办（窗口凭单发放） */
    @TableField("issued_by")
    @ApiModelProperty(value = "放成果经办")
    private String issuedBy;

    /** 放成果时辰（系统写死，以秒计） */
    @TableField("issued_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "放成果时辰")
    private Date issuedAt;

    /** 创建者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /** 更新者 */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    /** 备注 */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

    /** 删除标记 0正常 1删除（服务层不辟删门） */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
    private Integer delFlag;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public String getApplicantUnit() {
        return applicantUnit;
    }

    public void setApplicantUnit(String applicantUnit) {
        this.applicantUnit = applicantUnit;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
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

    public String getStation() {
        return station;
    }

    public void setStation(String station) {
        this.station = station;
    }

    public Integer getRoundNo() {
        return roundNo;
    }

    public void setRoundNo(Integer roundNo) {
        this.roundNo = roundNo;
    }

    public Integer getNeedCount() {
        return needCount;
    }

    public void setNeedCount(Integer needCount) {
        this.needCount = needCount;
    }

    public Integer getSignCount() {
        return signCount;
    }

    public void setSignCount(Integer signCount) {
        this.signCount = signCount;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getApprovedDocNo() {
        return approvedDocNo;
    }

    public void setApprovedDocNo(String approvedDocNo) {
        this.approvedDocNo = approvedDocNo;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public Date getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Date approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getChecklistNo() {
        return checklistNo;
    }

    public void setChecklistNo(String checklistNo) {
        this.checklistNo = checklistNo;
    }

    public Integer getChecklistGen() {
        return checklistGen;
    }

    public void setChecklistGen(Integer checklistGen) {
        this.checklistGen = checklistGen;
    }

    public String getIssuedBy() {
        return issuedBy;
    }

    public void setIssuedBy(String issuedBy) {
        this.issuedBy = issuedBy;
    }

    public Date getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Date issuedAt) {
        this.issuedAt = issuedAt;
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

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }
}
