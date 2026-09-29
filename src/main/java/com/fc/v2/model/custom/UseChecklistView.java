package com.fc.v2.model.custom;

import com.fc.v2.model.auto.TMgisUseBill;
import com.fc.v2.model.auto.TMgisUseChecklist;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.List;

/**
 * 领用清单回话：核准件一键生成，一家单位一册图对一条清单。
 * 抬头（申请单位、密级、出图年份、批准文号）与每行图幅代号统统从核准那一份带过，
 * 窗口不再誊第二遍。
 *
 * @author fuce
 * @date 2026-09-29
 */
@ApiModel(value = "UseChecklistView", description = "领用清单")
public class UseChecklistView implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 核准单抬头（单号、申请单位、密级、出图年份、批准文号、清单号） */
    @ApiModelProperty(value = "核准单抬头")
    private final TMgisUseBill bill;

    /** 清单行（图幅代号、密级、出图年份、批准文号照核准件带过） */
    @ApiModelProperty(value = "清单行")
    private final List<TMgisUseChecklist> rows;

    public UseChecklistView(TMgisUseBill bill, List<TMgisUseChecklist> rows) {
        this.bill = bill;
        this.rows = rows;
    }

    public TMgisUseBill getBill() {
        return bill;
    }

    public List<TMgisUseChecklist> getRows() {
        return rows;
    }
}
