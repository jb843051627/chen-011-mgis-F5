package com.fc.v2.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.use.UseChainReport;
import com.fc.v2.model.auto.TMgisUseBill;
import com.fc.v2.model.custom.UseAcceptRequest;
import com.fc.v2.model.custom.UseAcceptResult;
import com.fc.v2.model.custom.UseBillView;
import com.fc.v2.model.custom.UseChecklistView;
import com.fc.v2.model.custom.UseSignRequest;
import com.fc.v2.model.custom.UseSignResult;

import java.util.List;

/**
 * 用密成果提供使用逐级核准单 Service接口。
 *
 * <p>门面只留「受理」「签署」两扇写入门：受理立单只经窗口受理门，画押/驳回/否决
 * 统经签署门由服务层执掌；绕开门面直接立单、改单的路不辟。以下早年四个方法签名
 * 一仍其旧（门脸已不再调用），新本事（受理、会签、清单、看页、验链）一律添新方法，
 * 不动旧签名。</p>
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITMgisUseBillService {

    /** 按主键回查单据 */
    TMgisUseBill selectTMgisUseBillById(Long id);

    /** 签批一票：返回更新后的单据；被拒返回 null */
    TMgisUseBill approve(Long id, String approver, String comment);

    /** 否决：返回更新后的单据；被拒返回 null */
    TMgisUseBill reject(Long id, String approver, String comment);

    /** 退回上一环节：返回更新后的单据；被拒返回 null */
    TMgisUseBill rollback(Long id, String comment);

    // ---- 以下为新本事：旧签名分毫不动，新意思添新方法（重载门） ----

    /** 窗口受理：五样缺一受理不下去；受理立单、图幅挂齐、落头一笔，件进市局 */
    UseAcceptResult accept(UseAcceptRequest req, String operator);

    /** 签署（唯一动作门）：市局核用途、省厅两人会签、发回市局、不予核准、窗口放成果 */
    UseSignResult sign(UseSignRequest req, String operator);

    /** 核准件一键生成领用清单：图幅代号、密级、出图年份、批准文号照核准件带过；只生成一次 */
    UseChecklistView generateChecklist(Long billId, String operator);

    /** 核准单页面：逐阶当前轮落笔、申请图幅、领用清单、已签人数两口径对账 */
    UseBillView pageView(Long billId);

    /** 签署链校验：顺拉倒拉，两程皆通这份核准才算合格 */
    UseChainReport verifyChain(Long billId);

    /** 台账查询 */
    List<TMgisUseBill> selectTMgisUseBillList(QueryWrapper<TMgisUseBill> queryWrapper);
}
