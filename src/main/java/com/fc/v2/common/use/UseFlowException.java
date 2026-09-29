package com.fc.v2.common.use;

/**
 * 用密核准走阶违章：五样缺项、隔阶落签、替签、省厅只签一枚、发回后还往旧票上挂、
 * 否决/核准后再落签、清单未生成就放成果等，一律当场顶回，一笔不生效。
 *
 * @author fuce
 * @date 2026-09-29
 */
public class UseFlowException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UseFlowException(String message) {
        super(message);
    }
}
