package com.fc.v2.model.custom;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

/**
 * 签署请求。门面只留「签署」一道门：画押（市局核用途、省厅会签、窗口放成果）、
 * 驳回（省厅发回市局）、否决（不予核准）三事统归服务层执掌，动作由 action 指认。
 * 签署人与签署时辰不收请求的话——系统在签署那一下自己写死，替签不作数，
 * 事后也由不得人补录、挪时辰。
 *
 * @author fuce
 * @date 2026-09-29
 */
@ApiModel(value = "UseSignRequest", description = "签署请求")
public class UseSignRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 核准单主键 */
    @ApiModelProperty(value = "核准单主键", required = true)
    private Long billId;

    /** 签署动作 VERIFY市局核用途 CONFIRM省厅会签 SEND_BACK发回市局 VETO不予核准 ISSUE窗口放成果（中文名亦可） */
    @ApiModelProperty(value = "签署动作", required = true)
    private String action;

    /** 省厅会签分工 SECRET密级脱密 PURPOSE用途核准（仅省厅会签必填，两名各签各的） */
    @ApiModelProperty(value = "省厅会签分工")
    private String signRole;

    /** 核用途 / 省厅会签意见 */
    @ApiModelProperty(value = "意见")
    private String opinion;

    /** 发回缘由（发回市局必填）/ 不予核准理由（必填，写明即定死） */
    @ApiModelProperty(value = "发回缘由/不予核准理由")
    private String reason;

    /** 窗口放成果附言（可空） */
    @ApiModelProperty(value = "放成果附言")
    private String note;

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
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

    public String getOpinion() {
        return opinion;
    }

    public void setOpinion(String opinion) {
        this.opinion = opinion;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
