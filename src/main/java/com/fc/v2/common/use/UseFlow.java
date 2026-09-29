package com.fc.v2.common.use;

import com.alibaba.fastjson.JSONObject;
import com.fc.v2.model.auto.TMgisUseBill;
import com.fc.v2.model.auto.TMgisUseEntry;
import com.fc.v2.model.custom.UseAcceptRequest;
import com.fc.v2.model.custom.UseSignRequest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用密核准走阶引擎：用密成果提供使用核准规矩在系统里的唯一出处。
 *
 * <p>一份申请全程只凭一张核准单，四步走：窗口受理、市局核用途、省厅两人会签、
 * 窗口凭单放成果。走阶只许一阶接一阶，省厅那两枚没齐，窗口点不出放成果；
 * 章程全收在这一个类里，服务层、控制器都从这里过，别处不许另写一套。</p>
 *
 * <ul>
 *   <li><b>进门五样缺一不受理</b>：申请单位、用途、图幅代号、密级、出图年份，
 *       缺一项受理不下去；受理即落第一笔，五样快照进单，往后锁定。</li>
 *   <li><b>省厅认两个人</b>：密级脱密一名、用途核准一名，两名各签各的；
 *       同一分钟里并发落签也是两笔、不并成一条；同一人顶两名属替签，不作数。</li>
 *   <li><b>发回勾销</b>：省厅嫌脱密说明单薄发回市局，前各阶签退出页面（链上原样留），
 *       从市局重新攒起，废票与新票不并堂。</li>
 *   <li><b>否决定死、核准锁定</b>：任一处写明不予核准即定死；两枚齐即核准给号、
 *       各栏锁住，要动只能另起新件。</li>
 * </ul>
 *
 * @author fuce
 * @date 2026-09-29
 */
public final class UseFlow {

    private UseFlow() {
    }

    /** 在核 */
    public static final int STATUS_RUNNING = 0;
    /** 已核准（省厅两枚齐即核准，件回窗口待放成果） */
    public static final int STATUS_APPROVED = 1;
    /** 已否决（不予核准，定死） */
    public static final int STATUS_VETOED = 2;

    // ---- 落笔内容键 ----
    public static final String KEY_APPLICANT_UNIT = "applicantUnit";
    public static final String KEY_PURPOSE = "purpose";
    public static final String KEY_SECRET_LEVEL = "secretLevel";
    public static final String KEY_MAP_YEAR = "mapYear";
    public static final String KEY_SHEET_CODES = "sheetCodes";
    public static final String KEY_OPINION = "opinion";
    public static final String KEY_REASON = "reason";
    public static final String KEY_NOTE = "note";
    public static final String KEY_DOC_NO = "approvedDocNo";

    /** 各阶应签人数：窗口受理、市局核用途、窗口放成果各一名；省厅两名点齐 */
    public static int needCountOf(UseStation station) {
        return station == UseStation.PROVINCE ? UseSignRole.PROVINCE_NEED : 1;
    }

    /**
     * 签署前核件还收不收笔：否决定死、核准锁定（省厅两签齐后只走清单与发放）、
     * 放成果办结，三种局面什么签署都不收。
     */
    public static void checkWritable(TMgisUseBill bill, UseAction action) {
        if (bill == null) {
            throw new UseFlowException("核准单不在账上");
        }
        if (action == null) {
            throw new IllegalArgumentException("签署动作未指");
        }
        if (bill.getStatus() != null && bill.getStatus() == STATUS_VETOED) {
            throw new UseFlowException("这份件已写明不予核准，就此定死，事后加不进去一枚签；要办请另起新件");
        }
        if (bill.getIssuedAt() != null) {
            throw new UseFlowException("这份件已凭单放成果、办结，不再收签署");
        }
        if (bill.getStatus() != null && bill.getStatus() == STATUS_APPROVED && action != UseAction.ISSUE) {
            // 核准之后各栏锁住：省厅两签齐后只许生成清单、窗口放成果
            throw new UseFlowException("省厅两枚已齐、这份件已核准锁定，各栏不许再动；要改请另起新件");
        }
    }

    /**
     * 核这一笔够不够得着当前阶：件停在哪一阶就只开哪一阶的口子，隔阶落签不生效。
     * 不予核准可落市局、省厅两阶；放成果只在窗口（且须件已核准、清单已生成）。
     */
    public static void checkStation(TMgisUseBill bill, UseAction action, UseSignRole role) {
        UseStation current = UseStation.require(bill.getStation());
        if (action == UseAction.VETO) {
            if (!action.vetoAllowedAt(current)) {
                throw new UseFlowException("不予核准只许在「市局核用途」「省厅会签」两阶写明，"
                        + "件眼下停在「" + current.getLabel() + "」，这一笔不生效");
            }
            return;
        }
        if (action.getStation() != current) {
            throw new UseFlowException("件眼下停在「" + current.getLabel() + "」这一阶，只开这一阶的口子；「"
                    + action.getLabel() + "」这一笔隔着阶，不生效");
        }
        if (action == UseAction.CONFIRM) {
            if (role == null) {
                throw new UseFlowException("省厅会签须指明分工：密级脱密一名、用途核准一名，各签各的");
            }
        } else if (role != null) {
            throw new UseFlowException("只有省厅会签分密级脱密、用途两名，其余阶不带分工");
        }
        if (action == UseAction.ISSUE
                && (bill.getStatus() == null || bill.getStatus() != STATUS_APPROVED)) {
            throw new UseFlowException("省厅那两枚没齐，窗口这边点不出放成果");
        }
        if (action == UseAction.ISSUE
                && (bill.getChecklistGen() == null || bill.getChecklistGen() != 1)) {
            throw new UseFlowException("领用清单尚未一键生成，须先凭核准件出清单，再凭单放成果");
        }
    }

    /**
     * 受理五样 + 图幅清单的进门校验：申请单位、用途、图幅代号、密级、出图年份，
     * 缺一项受理不下去；图幅代号去空白、册内不许重号。
     *
     * @return 归置后的图幅代号（按原次序，去空白）
     */
    public static List<String> validateAccept(UseAcceptRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("受理请求为空");
        }
        requireText(req.getApplicantUnit(), "申请单位未填，缺一项受理不下去");
        requireText(req.getPurpose(), "用途未填，缺一项受理不下去");
        requireText(req.getSecretLevel(), "密级未填，缺一项受理不下去");
        if (req.getMapYear() == null) {
            throw new UseFlowException("出图年份未填，缺一项受理不下去");
        }
        if (req.getMapYear() < 1900 || req.getMapYear() > 2100) {
            throw new UseFlowException("出图年份不在常理内（1900..2100）：" + req.getMapYear());
        }
        List<String> raw = req.getSheetCodes();
        if (raw == null || raw.isEmpty()) {
            throw new UseFlowException("要哪几幅图的代号未填，缺一项受理不下去");
        }
        List<String> codes = new ArrayList<String>();
        for (String c : raw) {
            if (!hasText(c)) {
                throw new UseFlowException("图幅代号里有空行，图幅代号须幅幅填齐");
            }
            String t = c.trim();
            if (codes.contains(t)) {
                throw new UseFlowException("图幅代号册内重号：" + t + "，一册图里同一幅只许列一行");
            }
            codes.add(t);
        }
        return codes;
    }

    /** 受理那一笔写的话：五样快照，落笔即定，往后各阶只读取不重填 */
    public static String packAcceptPayload(UseAcceptRequest req, List<String> codes) {
        JSONObject payload = new JSONObject(true);
        payload.put(KEY_APPLICANT_UNIT, req.getApplicantUnit().trim());
        payload.put(KEY_PURPOSE, req.getPurpose().trim());
        payload.put(KEY_SECRET_LEVEL, req.getSecretLevel().trim());
        payload.put(KEY_MAP_YEAR, req.getMapYear());
        payload.put(KEY_SHEET_CODES, codes);
        return payload.toJSONString();
    }

    /**
     * 各签署动作该带什么话，定死在这里：
     * 核用途、省厅两签都要落意见；发回缘由、不予核准理由必填；放成果附言可空。
     */
    public static String packSignPayload(UseAction action, UseSignRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("签署请求为空");
        }
        JSONObject payload = new JSONObject(true);
        switch (action) {
            case VERIFY:
                payload.put(KEY_OPINION, requireText(req.getOpinion(), "市局核用途意见未填：用途真不真、与作业范围搭不搭要写明"));
                break;
            case CONFIRM:
                payload.put(KEY_OPINION, requireText(req.getOpinion(), "省厅会签意见未填"));
                break;
            case SEND_BACK:
                payload.put(KEY_REASON, requireText(req.getReason(), "发回缘由未填：脱密说明单薄在哪要说清"));
                break;
            case VETO:
                payload.put(KEY_REASON, requireText(req.getReason(), "不予核准须写明理由，写明即定死"));
                break;
            case ISSUE:
                if (hasText(req.getNote())) {
                    payload.put(KEY_NOTE, req.getNote().trim());
                }
                break;
            default:
                throw new UseFlowException("受理由窗口受理门落笔，签署门不收这一笔");
        }
        return payload.toJSONString();
    }

    /**
     * 本阶已签人数：只数当前轮、当前阶上算数的签署（省厅数两名分工，发回、否决不数）。
     * 页面摆出来的与单据里存下的 sign_count 都从这一处算，两个口径必须同一个数。
     */
    public static int tallySigned(List<TMgisUseEntry> chain, UseStation station, int roundNo) {
        int n = 0;
        for (TMgisUseEntry e : chain) {
            if (e.getRoundNo() != null && e.getRoundNo() == roundNo
                    && station.name().equals(e.getStation())
                    && UseAction.CONFIRM.name().equals(e.getAction())
                    && station == UseStation.PROVINCE) {
                n++;
            } else if (e.getRoundNo() != null && e.getRoundNo() == roundNo
                    && station.name().equals(e.getStation())
                    && (UseAction.ACCEPT.name().equals(e.getAction())
                            || UseAction.VERIFY.name().equals(e.getAction())
                            || UseAction.ISSUE.name().equals(e.getAction()))) {
                n++;
            }
        }
        return n;
    }

    /** 省厅这一名的另一名签了没有（两名单独认，不可同一人顶替） */
    public static TMgisUseEntry otherConfirm(List<TMgisUseEntry> chain, UseSignRole role, int roundNo) {
        UseSignRole other = role == UseSignRole.SECRET ? UseSignRole.PURPOSE : UseSignRole.SECRET;
        for (TMgisUseEntry e : chain) {
            if (e.getRoundNo() != null && e.getRoundNo() == roundNo
                    && UseStation.PROVINCE.name().equals(e.getStation())
                    && UseAction.CONFIRM.name().equals(e.getAction())
                    && other.name().equals(e.getSignRole())) {
                return e;
            }
        }
        return null;
    }

    /**
     * 页面口径：每一阶只显当前轮落笔的话。省厅发回市局后，市局、省厅先前各笔退出页面，
     * 只显重审后的这一份——废票与新票不并堂；链上旧笔原样都在（查链走 chain 端点）。
     *
     * @return 阶代号 → 该阶算数的落笔（按落笔先后排）
     */
    public static Map<String, List<TMgisUseEntry>> pageOf(List<TMgisUseEntry> entries, int currentRound) {
        Map<String, List<TMgisUseEntry>> page = new LinkedHashMap<String, List<TMgisUseEntry>>();
        for (UseStation st : UseStation.values()) {
            List<TMgisUseEntry> shown = new ArrayList<TMgisUseEntry>();
            if (entries != null) {
                for (TMgisUseEntry e : entries) {
                    // 窗口受理只在头一轮有，重审不回窗口；其余阶只显当前轮
                    int er = e.getRoundNo() == null ? 1 : e.getRoundNo();
                    int showRound = st == UseStation.WINDOW ? 1 : currentRound;
                    if (st.name().equals(e.getStation()) && er == showRound) {
                        shown.add(e);
                    }
                }
            }
            if (!shown.isEmpty()) {
                page.put(st.name(), shown);
            }
        }
        return page;
    }

    /**
     * 链上前后两笔的走阶次序合不合法：
     * 同轮只许往下一阶挪，或省厅阶上两名先后落签 / 落签后发回 / 落签后否决；
     * 跨轮只许「省厅发回 → 市局重审」这一种。
     */
    public static boolean legalTransition(TMgisUseEntry prev, TMgisUseEntry next) {
        if (prev == null || next == null) {
            return false;
        }
        UseAction pa = UseAction.require(prev.getAction());
        UseAction na = UseAction.require(next.getAction());
        UseStation ps = UseStation.require(prev.getStation());
        UseStation ns = UseStation.require(next.getStation());
        int dr = (next.getRoundNo() == null ? 1 : next.getRoundNo())
                - (prev.getRoundNo() == null ? 1 : prev.getRoundNo());
        if (dr == 1) {
            // 跨轮只许：省厅发回市局 → 市局重审
            return pa == UseAction.SEND_BACK && ps == UseStation.PROVINCE
                    && na == UseAction.VERIFY && ns == UseStation.CITY;
        }
        if (dr != 0) {
            return false;
        }
        if (ps == ns) {
            // 同阶同轮
            if (ps == UseStation.PROVINCE) {
                if (pa == UseAction.CONFIRM && na == UseAction.CONFIRM) {
                    // 两名各签各的：分工不同（先后不论，并发落签也算两笔）
                    return prev.getSignRole() != null
                            && !prev.getSignRole().equals(next.getSignRole());
                }
                // 头一名签过之后，省厅仍可发回或不予核准
                return pa == UseAction.CONFIRM
                        && (na == UseAction.SEND_BACK || na == UseAction.VETO);
            }
            return false;
        }
        if (ns.getOrder() != ps.getOrder() + 1) {
            return false;
        }
        // 往下一阶挪
        if (ps == UseStation.WINDOW) {
            // 市局核用途：核过往省厅送，核不过写明不予核准，同是这一阶的出口
            return pa == UseAction.ACCEPT && (na == UseAction.VERIFY || na == UseAction.VETO);
        }
        if (ps == UseStation.CITY) {
            // 省厅：头一笔可会签、可嫌脱密单批发回、可径直不予核准
            return pa == UseAction.VERIFY
                    && (na == UseAction.CONFIRM || na == UseAction.SEND_BACK || na == UseAction.VETO);
        }
        if (ps == UseStation.PROVINCE) {
            // 省厅两枚齐（末笔是会签）才到窗口放成果
            return pa == UseAction.CONFIRM && na == UseAction.ISSUE;
        }
        return false;
    }

    /** 阶代号译阶名，译不得照原样回 */
    public static String labelOf(String stationName) {
        try {
            return UseStation.require(stationName).getLabel();
        } catch (IllegalArgumentException e) {
            return String.valueOf(stationName);
        }
    }

    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static String requireText(String s, String message) {
        if (!hasText(s)) {
            throw new UseFlowException(message);
        }
        return s.trim();
    }
}
