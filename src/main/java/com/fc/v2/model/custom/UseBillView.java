package com.fc.v2.model.custom;

import com.fc.v2.model.auto.TMgisUseBill;
import com.fc.v2.model.auto.TMgisUseChecklist;
import com.fc.v2.model.auto.TMgisUseEntry;
import com.fc.v2.model.auto.TMgisUseSheet;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.List;

/**
 * 核准单页面：件停在哪一阶，就只开哪一阶的口子；每一阶只显当前轮落笔的话——
 * 省厅发回市局后，市局、省厅先前各笔退出页面，只显重审后的这一份，链上旧笔都还在
 * （查链走 /mgisUseBill/{id}/chain）。
 *
 * <p>签了几个人，页面摆出来的（talliedSignCount，当场自签署链重算）与单据里存下的
 * （storedSignCount，件自己记的 sign_count）是两个地方口径，两个数必须一致；
 * 差一笔就算这一阶没过（countConsistent=false）。</p>
 *
 * @author fuce
 * @date 2026-09-29
 */
@ApiModel(value = "UseBillView", description = "核准单页面")
public class UseBillView implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 核准单 */
    @ApiModelProperty(value = "核准单")
    private final TMgisUseBill bill;

    /** 眼下停在哪一阶 */
    @ApiModelProperty(value = "眼下停在哪一阶")
    private final String currentStation;

    /** 是否已经终局（已核准 / 已否决 / 已放成果） */
    @ApiModelProperty(value = "是否已终局")
    private final boolean terminal;

    /** 本阶应签人数 */
    @ApiModelProperty(value = "本阶应签人数")
    private final int needCount;

    /** 单据里存下的本阶已签人数（件自己记） */
    @ApiModelProperty(value = "单据存下的已签人数")
    private final int storedSignCount;

    /** 页面自签署链当场重算的本阶已签人数 */
    @ApiModelProperty(value = "链上重算的已签人数")
    private final int talliedSignCount;

    /** 两个口径是不是同一个数（少一个就算这一阶没过） */
    @ApiModelProperty(value = "两口径已签人数是否一致")
    private final boolean countConsistent;

    /** 逐阶页面（四阶全列，未到的阶空着） */
    @ApiModelProperty(value = "逐阶页面")
    private final List<StationPage> stations;

    /** 申请图幅行（受理即锁） */
    @ApiModelProperty(value = "申请图幅行")
    private final List<TMgisUseSheet> sheets;

    /** 领用清单行（未生成时空着） */
    @ApiModelProperty(value = "领用清单行")
    private final List<TMgisUseChecklist> checklist;

    public UseBillView(TMgisUseBill bill, String currentStation, boolean terminal,
                       int needCount, int storedSignCount, int talliedSignCount,
                       List<StationPage> stations, List<TMgisUseSheet> sheets,
                       List<TMgisUseChecklist> checklist) {
        this.bill = bill;
        this.currentStation = currentStation;
        this.terminal = terminal;
        this.needCount = needCount;
        this.storedSignCount = storedSignCount;
        this.talliedSignCount = talliedSignCount;
        this.countConsistent = storedSignCount == talliedSignCount;
        this.stations = stations;
        this.sheets = sheets;
        this.checklist = checklist;
    }

    public TMgisUseBill getBill() {
        return bill;
    }

    public String getCurrentStation() {
        return currentStation;
    }

    public boolean isTerminal() {
        return terminal;
    }

    public int getNeedCount() {
        return needCount;
    }

    public int getStoredSignCount() {
        return storedSignCount;
    }

    public int getTalliedSignCount() {
        return talliedSignCount;
    }

    public boolean isCountConsistent() {
        return countConsistent;
    }

    public List<StationPage> getStations() {
        return stations;
    }

    public List<TMgisUseSheet> getSheets() {
        return sheets;
    }

    public List<TMgisUseChecklist> getChecklist() {
        return checklist;
    }

    /**
     * 一阶的页面：开不开口子、这一阶算数的几笔（当前轮；发回前旧笔已退出页面）。
     */
    @ApiModel(value = "UseStationPage", description = "一阶的页面")
    public static class StationPage implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 阶代号 */
        @ApiModelProperty(value = "阶代号")
        private final String station;

        /** 阶名 */
        @ApiModelProperty(value = "阶名")
        private final String label;

        /** 这一阶眼下开不开口子（只开眼下停的那一阶，终局后一律不开） */
        @ApiModelProperty(value = "这一阶开不开口子")
        private final boolean open;

        /** 这一阶算数的落笔（当前轮） */
        @ApiModelProperty(value = "这一阶算数的落笔")
        private final List<TMgisUseEntry> entries;

        public StationPage(String station, String label, boolean open, List<TMgisUseEntry> entries) {
            this.station = station;
            this.label = label;
            this.open = open;
            this.entries = entries;
        }

        public String getStation() {
            return station;
        }

        public String getLabel() {
            return label;
        }

        public boolean isOpen() {
            return open;
        }

        public List<TMgisUseEntry> getEntries() {
            return entries;
        }
    }
}
