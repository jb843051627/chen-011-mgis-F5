package com.fc.v2.common.use;

/**
 * 用密核准四阶：窗口受理、市局核用途、省厅会签、窗口放成果。
 * 次序写死在这里，只许一阶接一阶，别处不许另排。
 *
 * @author fuce
 * @date 2026-09-29
 */
public enum UseStation {

    /** 窗口受理：进门一张申请单，五样缺一受理不下去；受理即落第一笔 */
    WINDOW("窗口受理", 0),
    /** 市局成果管理科核用途：用途真不真、与允许的作业范围搭不搭，一名签 */
    CITY("市局核用途", 1),
    /** 省厅成果管理处会签：密级脱密一名、用途核准一名，两名各签各的、缺一不可 */
    PROVINCE("省厅会签", 2),
    /** 窗口凭单放成果：省厅两枚齐、领用清单已生成，才点得出发放 */
    ISSUE("窗口放成果", 3);

    private final String label;
    private final int order;

    UseStation(String label, int order) {
        this.label = label;
        this.order = order;
    }

    public String getLabel() {
        return label;
    }

    /** 阶次（窗口受理为 0，依次加一） */
    public int getOrder() {
        return order;
    }

    /**
     * 按代号或中文阶名认领；认不得即驳回。
     */
    public static UseStation require(String code) {
        if (code != null) {
            String s = code.trim();
            for (UseStation st : values()) {
                if (st.name().equalsIgnoreCase(s) || st.label.equals(s)) {
                    return st;
                }
            }
        }
        throw new IllegalArgumentException("环节不识：" + code);
    }
}
