package com.fc.v2.common.use;

/**
 * 整链校验回话：顺拉一程、倒拉一程，两程都通这份核准才算合格。
 *
 * @author fuce
 * @date 2026-09-29
 */
public class UseChainReport {

    /** 链上总笔数 */
    private final int entryCount;

    /** 顺拉通不通（从窗口受理念到末阶） */
    private final boolean forwardOk;

    /** 倒拉通不通（从末阶念回窗口受理） */
    private final boolean backwardOk;

    /** 断在第几笔，不断为 null */
    private final Long breakSeq;

    /** 回话 */
    private final String message;

    public UseChainReport(int entryCount, UseChainJudge.Leg forward, UseChainJudge.Leg backward) {
        this.entryCount = entryCount;
        this.forwardOk = forward.isOk();
        this.backwardOk = backward.isOk();
        this.breakSeq = forward.getBreakSeq() != null ? forward.getBreakSeq() : backward.getBreakSeq();
        if (this.forwardOk && this.backwardOk) {
            this.message = "链完整：顺拉倒拉皆通，共 " + entryCount + " 笔，这份核准合格";
        } else {
            this.message = "顺拉：" + forward.getMessage() + "；倒拉：" + backward.getMessage()
                    + "——这份核准不合格";
        }
    }

    public int getEntryCount() {
        return entryCount;
    }

    public boolean isForwardOk() {
        return forwardOk;
    }

    public boolean isBackwardOk() {
        return backwardOk;
    }

    /** 两程都通才算合格 */
    public boolean isOk() {
        return forwardOk && backwardOk;
    }

    public Long getBreakSeq() {
        return breakSeq;
    }

    public String getMessage() {
        return message;
    }
}
