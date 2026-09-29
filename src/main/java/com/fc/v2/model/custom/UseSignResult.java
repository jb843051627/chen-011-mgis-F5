package com.fc.v2.model.custom;

import com.fc.v2.model.auto.TMgisUseBill;
import com.fc.v2.model.auto.TMgisUseEntry;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

/**
 * 签署回话。生效与否只认接口回话：回话里是落了的那一笔与件眼下停的阶；
 * 同一点（同阶同动作同分工同轮）第二回来的，duplicated 为真，回的是头一笔，
 * 不再落第二笔。
 *
 * @author fuce
 * @date 2026-09-29
 */
@ApiModel(value = "UseSignResult", description = "签署回话")
public class UseSignResult implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 落了的那一笔（同点重来时回的是头一笔） */
    @ApiModelProperty(value = "落了的那一笔")
    private final TMgisUseEntry entry;

    /** 件眼下的阶、轮次、已签人数、情形 */
    @ApiModelProperty(value = "件眼下状态")
    private final TMgisUseBill bill;

    /** 是否同一点第二回（只认头一笔，未再落签） */
    @ApiModelProperty(value = "是否同一点第二回")
    private final boolean duplicated;

    public UseSignResult(TMgisUseEntry entry, TMgisUseBill bill, boolean duplicated) {
        this.entry = entry;
        this.bill = bill;
        this.duplicated = duplicated;
    }

    public TMgisUseEntry getEntry() {
        return entry;
    }

    public TMgisUseBill getBill() {
        return bill;
    }

    public boolean isDuplicated() {
        return duplicated;
    }
}
