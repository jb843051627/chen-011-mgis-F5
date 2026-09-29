package com.fc.v2.model.custom;

import com.fc.v2.model.auto.TMgisUseBill;
import com.fc.v2.model.auto.TMgisUseEntry;
import com.fc.v2.model.auto.TMgisUseSheet;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.List;

/**
 * 窗口受理回话：立起的核准单、受理那一笔、挂齐的图幅行。
 * 生效与否只认接口回话——核准单号、件停在哪阶都由系统给，请求里说了不算。
 *
 * @author fuce
 * @date 2026-09-29
 */
@ApiModel(value = "UseAcceptResult", description = "窗口受理回话")
public class UseAcceptResult implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 立起的核准单 */
    @ApiModelProperty(value = "核准单")
    private final TMgisUseBill bill;

    /** 受理那一笔（链上头一笔，认 64 个 0 的前哈希） */
    @ApiModelProperty(value = "受理那一笔")
    private final TMgisUseEntry entry;

    /** 挂齐并锁定的图幅行 */
    @ApiModelProperty(value = "申请图幅行")
    private final List<TMgisUseSheet> sheets;

    public UseAcceptResult(TMgisUseBill bill, TMgisUseEntry entry, List<TMgisUseSheet> sheets) {
        this.bill = bill;
        this.entry = entry;
        this.sheets = sheets;
    }

    public TMgisUseBill getBill() {
        return bill;
    }

    public TMgisUseEntry getEntry() {
        return entry;
    }

    public List<TMgisUseSheet> getSheets() {
        return sheets;
    }
}
