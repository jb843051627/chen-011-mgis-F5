package com.fc.v2.common.use;

/**
 * 签署动作。每个动作归哪一阶写死在这里：
 * 受理在窗口、核用途在市局、会签在省厅（两名分工会签）、发回只许省厅发回市局、
 * 否决在市局或省厅、放成果在窗口。
 *
 * <p>门面只留「签署」一道门：画押（受理/核用途/会签/放成果）、驳回（发回市局）、
 * 否决（不予核准）三事统归服务层执掌，动作由请求指认。</p>
 *
 * @author fuce
 * @date 2026-09-29
 */
public enum UseAction {

    /** 窗口受理：五样缺一受理不下去；受理即落第一笔、件进市局 */
    ACCEPT("窗口受理", UseStation.WINDOW),
    /** 市局核用途：用途真不真、与作业范围搭不搭，一名签过件进省厅 */
    VERIFY("市局核用途", UseStation.CITY),
    /** 省厅会签：密级脱密、用途核准两名各签各的（分工见 {@link UseSignRole}） */
    CONFIRM("省厅会签", UseStation.PROVINCE),
    /** 驳回·发回市局：只许省厅嫌脱密说明单薄时发回，前各阶签一笔勾销，从市局重攒 */
    SEND_BACK("发回市局", UseStation.PROVINCE),
    /** 否决·不予核准：市局或省厅写明即定死（不钉死阶，落在哪阶由当前阶裁决） */
    VETO("不予核准", null),
    /** 窗口凭单放成果：省厅两枚齐、领用清单已生成才点得出 */
    ISSUE("窗口放成果", UseStation.ISSUE);

    private final String label;
    private final UseStation station;

    UseAction(String label, UseStation station) {
        this.label = label;
        this.station = station;
    }

    public String getLabel() {
        return label;
    }

    /**
     * 这一笔常落在哪一阶；{@link #VETO} 可落市局或省厅，返回 null，
     * 具体落在哪阶以件当前停的阶为准。
     */
    public UseStation getStation() {
        return station;
    }

    /** 否决可落在哪几阶：市局、省厅 */
    public boolean vetoAllowedAt(UseStation st) {
        return st == UseStation.CITY || st == UseStation.PROVINCE;
    }

    /**
     * 按代号或中文名认领动作；认不得即驳回。
     */
    public static UseAction require(String code) {
        if (code != null) {
            String s = code.trim();
            for (UseAction a : values()) {
                if (a.name().equalsIgnoreCase(s) || a.label.equals(s)) {
                    return a;
                }
            }
        }
        throw new IllegalArgumentException("签署动作不识：" + code);
    }
}
