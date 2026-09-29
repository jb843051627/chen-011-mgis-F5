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
 * 用密核准签署链对象 t_mgis_use_entry
 *
 * <p>只增不改：签署人、时辰在签署那一下由系统写死，替签不作数，事后由不得人
 * 回头补录，也不许挪时辰。故本表不设 update 列，服务层不提供改、删入口。
 * 省厅发回市局时前各阶签不是删笔——旧轮各笔原样留在链上，页面只显新一轮，
 * 废票与新票不并堂；顺拉倒拉一笔都不能少。</p>
 *
 * @author fuce
 * @date 2026-09-29
 */
@TableName("t_mgis_use_entry")
@ApiModel(value = "TMgisUseEntry", description = "用密核准签署链(只增不改)")
public class TMgisUseEntry implements Serializable {
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

    /** 链上序号（自 1 连排） */
    @TableField("seq")
    @ApiModelProperty(value = "链上序号(自1连排)")
    private Integer seq;

    /** 第几轮（发回重审一轮加一） */
    @TableField("round_no")
    @ApiModelProperty(value = "第几轮")
    private Integer roundNo;

    /** 签署阶 WINDOW窗口受理 CITY市局核用途 PROVINCE省厅会签 ISSUE窗口放成果 */
    @TableField("station")
    @ApiModelProperty(value = "签署阶")
    private String station;

    /** 签署动作 ACCEPT受理 VERIFY核用途 CONFIRM会签 SEND_BACK发回 VETO不予核准 ISSUE放成果 */
    @TableField("action")
    @ApiModelProperty(value = "签署动作")
    private String action;

    /** 省厅会签分工 SECRET密级脱密 PURPOSE用途（其余阶为空） */
    @TableField("sign_role")
    @ApiModelProperty(value = "省厅会签分工")
    private String signRole;

    /** 这一笔写的话（JSON，签署即定） */
    @TableField("payload")
    @ApiModelProperty(value = "这一笔写的话(JSON)")
    private String payload;

    /** 签署人（签署那一下系统写死，替签不作数） */
    @TableField("penned_by")
    @ApiModelProperty(value = "签署人")
    private String pennedBy;

    /** 签署时辰（系统写死，以秒计） */
    @TableField("penned_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "签署时辰")
    private Date pennedAt;

    /** 前一笔哈希（头一笔为 64 个 0） */
    @TableField("prev_hash")
    @ApiModelProperty(value = "前一笔哈希")
    private String prevHash;

    /** 本笔哈希 */
    @TableField("hash")
    @ApiModelProperty(value = "本笔哈希")
    private String hash;

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

    public Integer getSeq() {
        return seq;
    }

    public void setSeq(Integer seq) {
        this.seq = seq;
    }

    public Integer getRoundNo() {
        return roundNo;
    }

    public void setRoundNo(Integer roundNo) {
        this.roundNo = roundNo;
    }

    public String getStation() {
        return station;
    }

    public void setStation(String station) {
        this.station = station;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getSignRole() {
        return signRole;
    }

    public void setSignRole(String signRole) {
        this.signRole = signRole;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getPennedBy() {
        return pennedBy;
    }

    public void setPennedBy(String pennedBy) {
        this.pennedBy = pennedBy;
    }

    public Date getPennedAt() {
        return pennedAt;
    }

    public void setPennedAt(Date pennedAt) {
        this.pennedAt = pennedAt;
    }

    public String getPrevHash() {
        return prevHash;
    }

    public void setPrevHash(String prevHash) {
        this.prevHash = prevHash;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
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
