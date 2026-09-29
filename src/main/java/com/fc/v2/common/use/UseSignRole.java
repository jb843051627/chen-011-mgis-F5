package com.fc.v2.common.use;

/**
 * 省厅会签的两人分工。两名各签各的，凑在同一分钟里并发落签也是两笔、
 * 不并成一条；只签一名，件仍停在省厅，走不到放成果那一阶；替签不作数。
 *
 * @author fuce
 * @date 2026-09-29
 */
public enum UseSignRole {

    /** 盯密级与脱密处理到不到位的一名 */
    SECRET("密级脱密"),
    /** 看这个用途该不该给的一名 */
    PURPOSE("用途核准");

    private final String label;

    UseSignRole(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** 省厅会签应到人数：两名 */
    public static final int PROVINCE_NEED = 2;

    /**
     * 按代号或中文名认领分工；认不得即驳回。
     */
    public static UseSignRole require(String code) {
        if (code != null) {
            String s = code.trim();
            for (UseSignRole r : values()) {
                if (r.name().equalsIgnoreCase(s) || r.label.equals(s)) {
                    return r;
                }
            }
        }
        throw new IllegalArgumentException("省厅会签分工不识：" + code);
    }
}
