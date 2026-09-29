package com.fc.v2.model.custom;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.List;

/**
 * 窗口受理请求。进门那一张申请单填清楚：申请单位、用途、要哪几幅图的代号、
 * 密级、出图年份，五样缺一项受理不下去。请求只带填报原值，核准单号由系统给，
 * 请求里说了不算。
 *
 * @author fuce
 * @date 2026-09-29
 */
@ApiModel(value = "UseAcceptRequest", description = "窗口受理请求")
public class UseAcceptRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 申请单位 */
    @ApiModelProperty(value = "申请单位", required = true)
    private String applicantUnit;

    /** 用途 */
    @ApiModelProperty(value = "用途", required = true)
    private String purpose;

    /** 密级 */
    @ApiModelProperty(value = "密级", required = true)
    private String secretLevel;

    /** 出图年份 */
    @ApiModelProperty(value = "出图年份", required = true)
    private Integer mapYear;

    /** 要哪几幅图的代号（一册图，册内不重号） */
    @ApiModelProperty(value = "图幅代号清单", required = true)
    private List<String> sheetCodes;

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

    public List<String> getSheetCodes() {
        return sheetCodes;
    }

    public void setSheetCodes(List<String> sheetCodes) {
        this.sheetCodes = sheetCodes;
    }
}
