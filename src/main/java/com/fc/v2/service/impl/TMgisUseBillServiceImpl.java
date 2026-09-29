package com.fc.v2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fc.v2.common.use.UseAction;
import com.fc.v2.common.use.UseChainJudge;
import com.fc.v2.common.use.UseChainReport;
import com.fc.v2.common.use.UseFlow;
import com.fc.v2.common.use.UseFlowException;
import com.fc.v2.common.use.UseSignRole;
import com.fc.v2.common.use.UseStation;
import com.fc.v2.mapper.auto.TMgisUseBillMapper;
import com.fc.v2.mapper.auto.TMgisUseChecklistMapper;
import com.fc.v2.mapper.auto.TMgisUseEntryMapper;
import com.fc.v2.mapper.auto.TMgisUseSheetMapper;
import com.fc.v2.model.auto.TMgisUseBill;
import com.fc.v2.model.auto.TMgisUseChecklist;
import com.fc.v2.model.auto.TMgisUseEntry;
import com.fc.v2.model.auto.TMgisUseSheet;
import com.fc.v2.model.custom.UseAcceptRequest;
import com.fc.v2.model.custom.UseAcceptResult;
import com.fc.v2.model.custom.UseBillView;
import com.fc.v2.model.custom.UseChecklistView;
import com.fc.v2.model.custom.UseSignRequest;
import com.fc.v2.model.custom.UseSignResult;
import com.fc.v2.service.ITMgisUseBillService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 用密成果提供使用逐级核准单 Service业务层处理。
 *
 * <p>四步走、双签、发回勾销、核准锁定、清单带出的章程都在这一层（口径唯一出处在
 * {@link UseFlow}）；签署人、时辰在签署那一下由系统写死，替签不作数，事后由不得人
 * 回头补录，也不许挪时辰。签署链 {@code t_mgis_use_entry} 只增不改：本层只有 insert，
 * 没有 update/delete——发回勾销不是删笔，旧轮各笔原样留链，页面只显新一轮。</p>
 *
 * @author fuce
 * @date 2026-09-29
 */
@Service
public class TMgisUseBillServiceImpl implements ITMgisUseBillService {

    /** 并发双签撞在一起时，条件更新撞手后重读重算的至多回数 */
    private static final int ADVANCE_MAX_TRY = 20;

    @javax.annotation.Resource
    private TMgisUseBillMapper mgisUseBillMapper;

    @javax.annotation.Resource
    private TMgisUseEntryMapper mgisUseEntryMapper;

    @javax.annotation.Resource
    private TMgisUseSheetMapper mgisUseSheetMapper;

    @javax.annotation.Resource
    private TMgisUseChecklistMapper mgisUseChecklistMapper;

    // ============================ 早年四个签名（分毫不动，门脸已不调用） ============================
    // 新流程立单走 accept，画押/驳回/否决走 sign；这三个改动口子封死，不另写一套。

    @Override
    public TMgisUseBill selectTMgisUseBillById(Long id) {
        return this.mgisUseBillMapper.selectById(id);
    }

    @Override
    @Deprecated
    public TMgisUseBill approve(Long id, String approver, String comment) {
        // 旧占位门脸：新流程一律走 sign(CONFIRM/VERIFY/ISSUE)，此口不放行
        return null;
    }

    @Override
    @Deprecated
    public TMgisUseBill reject(Long id, String approver, String comment) {
        // 旧占位门脸：不予核准走 sign(VETO)，此口不放行
        return null;
    }

    @Override
    @Deprecated
    public TMgisUseBill rollback(Long id, String comment) {
        // 旧占位门脸：省厅发回市局走 sign(SEND_BACK)，此口不放行
        return null;
    }

    // ============================ 新本事 ============================

    /**
     * 窗口受理。五样缺一受理不下去；系统给核准单号，五样快照进单、图幅一次挂齐并锁，
     * 落链上头一笔（窗口受理，认 64 个 0），件即送市局核用途。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UseAcceptResult accept(UseAcceptRequest req, String operator) {
        if (operator == null || operator.trim().isEmpty()) {
            throw new IllegalArgumentException("受理人未认");
        }
        // 五样进门校验：缺一项受理不下去；图幅去空白、册内不许重号
        List<String> codes = UseFlow.validateAccept(req);

        long id = IdWorker.getId();
        TMgisUseBill bill = new TMgisUseBill();
        bill.setId(id);
        bill.setBillNo("YM" + id);
        bill.setApplicantUnit(req.getApplicantUnit().trim());
        bill.setPurpose(req.getPurpose().trim());
        bill.setSecretLevel(req.getSecretLevel().trim());
        bill.setMapYear(req.getMapYear());
        // 受理落笔即送市局：窗口受理那一笔在链上，件眼下停在市局
        bill.setStation(UseStation.CITY.name());
        bill.setRoundNo(1);
        bill.setNeedCount(UseFlow.needCountOf(UseStation.CITY));
        bill.setSignCount(0);
        bill.setStatus(UseFlow.STATUS_RUNNING);
        bill.setChecklistGen(0);
        bill.setDelFlag(0);
        try {
            this.mgisUseBillMapper.insert(bill);
        } catch (DuplicateKeyException e) {
            throw new UseFlowException("核准单号撞号，请重新受理");
        }

        List<TMgisUseSheet> sheets = new ArrayList<TMgisUseSheet>();
        for (int i = 0; i < codes.size(); i++) {
            TMgisUseSheet sheet = new TMgisUseSheet();
            sheet.setId(IdWorker.getId());
            sheet.setBillId(id);
            sheet.setLineNo(i + 1);
            sheet.setSheetCode(codes.get(i));
            this.mgisUseSheetMapper.insert(sheet);
            sheets.add(sheet);
        }

        TMgisUseEntry entry = new TMgisUseEntry();
        entry.setBillId(id);
        entry.setSeq(1);
        entry.setRoundNo(1);
        entry.setStation(UseStation.WINDOW.name());
        entry.setAction(UseAction.ACCEPT.name());
        entry.setPayload(UseFlow.packAcceptPayload(req, codes));
        entry.setPennedBy(operator.trim());
        entry.setPennedAt(nowSecond());
        entry.setPrevHash(UseChainJudge.GENESIS);
        entry.setHash(UseChainJudge.hashOf(entry));
        try {
            this.mgisUseEntryMapper.insert(entry);
        } catch (DuplicateKeyException e) {
            throw new UseFlowException("受理笔已落过，请刷新页面");
        }
        return new UseAcceptResult(bill, entry, sheets);
    }

    /**
     * 签署（唯一动作门）。市局核用途、省厅两人会签（密级脱密、用途核准各签各的）、
     * 省厅发回市局、不予核准、窗口放成果，全打这一个门进；签署人、时辰系统写死。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UseSignResult sign(UseSignRequest req, String operator) {
        if (req == null) {
            throw new IllegalArgumentException("签署请求为空");
        }
        if (req.getBillId() == null) {
            throw new IllegalArgumentException("核准单号未指");
        }
        if (operator == null || operator.trim().isEmpty()) {
            throw new IllegalArgumentException("签署人未认");
        }
        TMgisUseBill bill = requireBill(req.getBillId());
        UseAction action = UseAction.require(req.getAction());
        if (action == UseAction.ACCEPT) {
            throw new UseFlowException("窗口受理走受理门，签署门不收受理这一笔");
        }
        // 否决定死、核准锁定、办结，先顶回
        UseFlow.checkWritable(bill, action);

        UseSignRole role = null;
        if (req.getSignRole() != null && !req.getSignRole().trim().isEmpty()) {
            role = UseSignRole.require(req.getSignRole());
        }
        // 隔阶落签、省厅会签不指分工、两枚未齐就放成果、清单未生就放成果，在这里顶回
        UseFlow.checkStation(bill, action, role);

        UseStation current = UseStation.require(bill.getStation());
        // 不予核准那一笔记在件当前停的阶（市局或省厅），其余按动作归阶
        UseStation penStation = action == UseAction.VETO ? current : action.getStation();
        int round = bill.getRoundNo() == null ? 1 : bill.getRoundNo();

        List<TMgisUseEntry> chain = listChain(bill.getId());
        // 同一点第二回来的只认头一笔：同阶同动作同分工同轮，哪怕件已挪阶也不再落签
        TMgisUseEntry dup = findAt(chain, penStation, action, role, round);
        if (dup != null) {
            return new UseSignResult(dup, bill, true);
        }
        if (action == UseAction.CONFIRM) {
            // 替签不作数：另一名已是同一人签的，顶回
            TMgisUseEntry other = UseFlow.otherConfirm(chain, role, round);
            if (other != null && operator.trim().equals(other.getPennedBy())) {
                throw new UseFlowException("省厅两名须各签各的，同一人顶两名属替签，不作数");
            }
        }

        TMgisUseEntry entry = new TMgisUseEntry();
        entry.setBillId(bill.getId());
        entry.setSeq(chain.size() + 1);
        entry.setRoundNo(round);
        entry.setStation(penStation.name());
        entry.setAction(action.name());
        entry.setSignRole(role == null ? null : role.name());
        entry.setPayload(UseFlow.packSignPayload(action, req));
        entry.setPennedBy(operator.trim());
        entry.setPennedAt(nowSecond());
        entry.setPrevHash(chain.isEmpty() ? UseChainJudge.GENESIS : chain.get(chain.size() - 1).getHash());
        entry.setHash(UseChainJudge.hashOf(entry));
        try {
            this.mgisUseEntryMapper.insert(entry);
        } catch (DuplicateKeyException e) {
            // 并发撞同一点：唯一键兜底，认头一笔
            throw new UseFlowException("这一阶这一笔已有人落过，只认头一笔；请刷新页面再签");
        }

        advance(bill, action, role, operator);
        TMgisUseBill fresh = requireBill(bill.getId());
        return new UseSignResult(entry, fresh, false);
    }

    /**
     * 核准件一键生成领用清单。只许在省厅两枚齐、已核准之后生成，且一份件只生成一次；
     * 图幅代号、密级、出图年份、批准文号照核准那一份带过，窗口不再誊第二遍。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UseChecklistView generateChecklist(Long billId, String operator) {
        if (operator == null || operator.trim().isEmpty()) {
            throw new IllegalArgumentException("经办人未认");
        }
        TMgisUseBill bill = requireBill(billId);
        if (bill.getStatus() == null || bill.getStatus() != UseFlow.STATUS_APPROVED) {
            throw new UseFlowException("省厅两枚未齐、件未核准，出不了领用清单");
        }
        List<TMgisUseChecklist> rows = listChecklist(billId);
        if (bill.getChecklistGen() != null && bill.getChecklistGen() == 1) {
            // 一份核准件只对一条清单，第二回不重出，头一条原样回
            return new UseChecklistView(bill, rows);
        }
        List<TMgisUseSheet> sheets = listSheets(billId);
        if (sheets.isEmpty()) {
            throw new UseFlowException("核准件上一册图幅行都没有，清单无从带出");
        }
        String checklistNo = "QY" + bill.getId();
        for (TMgisUseSheet sheet : sheets) {
            TMgisUseChecklist row = new TMgisUseChecklist();
            row.setId(IdWorker.getId());
            row.setBillId(bill.getId());
            row.setChecklistNo(checklistNo);
            row.setLineNo(sheet.getLineNo());
            row.setSheetCode(sheet.getSheetCode());
            row.setSecretLevel(bill.getSecretLevel());
            row.setMapYear(bill.getMapYear());
            row.setApprovedDocNo(bill.getApprovedDocNo());
            this.mgisUseChecklistMapper.insert(row);
            rows.add(row);
        }
        UpdateWrapper<TMgisUseBill> uw = new UpdateWrapper<TMgisUseBill>();
        uw.eq("id", bill.getId())
                .eq("checklist_gen", 0)
                .set("checklist_gen", 1)
                .set("checklist_no", checklistNo);
        if (this.mgisUseBillMapper.update(null, uw) != 1) {
            throw new UseFlowException("清单已被先生成，只认头一条；请刷新页面");
        }
        bill.setChecklistGen(1);
        bill.setChecklistNo(checklistNo);
        return new UseChecklistView(bill, rows);
    }

    @Override
    public UseBillView pageView(Long billId) {
        TMgisUseBill bill = requireBill(billId);
        List<TMgisUseEntry> chain = listChain(billId);
        int round = bill.getRoundNo() == null ? 1 : bill.getRoundNo();
        java.util.Map<String, List<TMgisUseEntry>> page = UseFlow.pageOf(chain, round);

        UseStation current = UseStation.require(bill.getStation());
        boolean vetoed = bill.getStatus() != null && bill.getStatus() == UseFlow.STATUS_VETOED;
        boolean issued = bill.getIssuedAt() != null;
        boolean terminal = vetoed || issued;
        List<UseBillView.StationPage> stations = new ArrayList<UseBillView.StationPage>();
        for (UseStation st : UseStation.values()) {
            // 件停在哪一阶就只开哪一阶的口子；否决、放成果后一律不开
            boolean open = !terminal && st == current;
            List<TMgisUseEntry> entries = page.get(st.name());
            stations.add(new UseBillView.StationPage(st.name(), st.getLabel(), open,
                    entries == null ? Collections.<TMgisUseEntry>emptyList() : entries));
        }
        int stored = bill.getSignCount() == null ? 0 : bill.getSignCount();
        // 已核准待放成果时，页面口径数到窗口放成果阶（0/1）；在核时数当前阶
        int tallied = UseFlow.tallySigned(chain, current, round);
        return new UseBillView(bill, current.name(), terminal,
                bill.getNeedCount() == null ? 0 : bill.getNeedCount(), stored, tallied,
                stations, listSheets(billId), listChecklist(billId));
    }

    @Override
    public UseChainReport verifyChain(Long billId) {
        TMgisUseBill bill = requireBill(billId);
        List<TMgisUseEntry> chain = listChain(bill.getId());
        UseChainJudge.Leg forward = UseChainJudge.verifyForward(chain);
        UseChainJudge.Leg backward = UseChainJudge.verifyBackward(chain);
        return new UseChainReport(chain.size(), forward, backward);
    }

    @Override
    public List<TMgisUseBill> selectTMgisUseBillList(QueryWrapper<TMgisUseBill> queryWrapper) {
        return this.mgisUseBillMapper.selectList(queryWrapper);
    }

    // ============================ 阶内工序 ============================

    /**
     * 落笔之后挪件：市局签过往省厅；省厅数两枚，两枚齐才核准给号、回窗口；
     * 发回则轮次加一、回市局，旧各阶签退出页面（链上不删）；否决定死；放成果办结。
     * 条件更新带旧阶旧轮旧情形作比，并发双签撞手时重读重算，两枚都不丢、不并成一条。
     */
    private void advance(TMgisUseBill bill, UseAction action, UseSignRole role, String operator) {
        for (int attempt = 0; attempt < ADVANCE_MAX_TRY; attempt++) {
            TMgisUseBill cur = this.mgisUseBillMapper.selectById(bill.getId());
            List<TMgisUseEntry> chain = listChain(bill.getId());
            UseStation curStation = UseStation.require(cur.getStation());
            int curRound = cur.getRoundNo() == null ? 1 : cur.getRoundNo();
            int curStatus = cur.getStatus() == null ? UseFlow.STATUS_RUNNING : cur.getStatus();

            UpdateWrapper<TMgisUseBill> uw = new UpdateWrapper<TMgisUseBill>();
            uw.eq("id", cur.getId())
                    .eq("station", curStation.name())
                    .eq("round_no", curRound)
                    .eq("status", curStatus);

            switch (action) {
                case VERIFY: {
                    uw.set("station", UseStation.PROVINCE.name())
                            .set("need_count", UseFlow.needCountOf(UseStation.PROVINCE))
                            .set("sign_count", UseFlow.tallySigned(chain, UseStation.PROVINCE, curRound));
                    break;
                }
                case CONFIRM: {
                    int signed = UseFlow.tallySigned(chain, UseStation.PROVINCE, curRound);
                    if (signed >= UseFlow.needCountOf(UseStation.PROVINCE)) {
                        // 两枚齐：核准、给批准文号、回窗口待放成果
                        String docNo = "PZ" + cur.getId();
                        uw.set("station", UseStation.ISSUE.name())
                                .set("status", UseFlow.STATUS_APPROVED)
                                .set("approved_doc_no", docNo)
                                .set("approved_by", operator.trim())
                                .set("approved_at", nowSecond())
                                .set("need_count", UseFlow.needCountOf(UseStation.ISSUE))
                                .set("sign_count", UseFlow.tallySigned(chain, UseStation.ISSUE, curRound));
                    } else {
                        // 只回来一枚，件仍停在省厅
                        uw.set("sign_count", signed);
                    }
                    break;
                }
                case SEND_BACK: {
                    int newRound = curRound + 1;
                    uw.set("station", UseStation.CITY.name())
                            .set("round_no", newRound)
                            .set("need_count", UseFlow.needCountOf(UseStation.CITY))
                            .set("sign_count", 0);
                    break;
                }
                case VETO: {
                    uw.set("status", UseFlow.STATUS_VETOED);
                    break;
                }
                case ISSUE: {
                    uw.set("sign_count", UseFlow.tallySigned(chain, UseStation.ISSUE, curRound))
                            .set("issued_by", operator.trim())
                            .set("issued_at", nowSecond());
                    break;
                }
                default:
                    throw new UseFlowException("签署门不收这一笔：" + action);
            }
            int rows = this.mgisUseBillMapper.update(null, uw);
            if (rows == 1) {
                return;
            }
            // 件被并发的另一签挪动过：重读重算（省厅两枚并发落签，后一枚在此并上）
            sleepBrief();
        }
        throw new UseFlowException("件正被并发签署，请刷新后重来");
    }

    /** 同一点（同阶同动作同分工同轮）的那一笔，没有为 null */
    private TMgisUseEntry findAt(List<TMgisUseEntry> chain, UseStation station, UseAction action,
                                 UseSignRole role, int roundNo) {
        for (TMgisUseEntry e : chain) {
            if (e.getRoundNo() != null && e.getRoundNo() == roundNo
                    && station.name().equals(e.getStation())
                    && action.name().equals(e.getAction())) {
                String er = e.getSignRole();
                if (role == null) {
                    if (er == null) {
                        return e;
                    }
                } else if (role.name().equals(er)) {
                    return e;
                }
            }
        }
        return null;
    }

    private TMgisUseBill requireBill(Long billId) {
        if (billId == null) {
            throw new IllegalArgumentException("核准单号未指");
        }
        TMgisUseBill bill = this.mgisUseBillMapper.selectById(billId);
        if (bill == null) {
            throw new UseFlowException("核准单不在账上：" + billId);
        }
        return bill;
    }

    /** 全链按签署先后取出（顺拉倒拉都以此为本） */
    private List<TMgisUseEntry> listChain(Long billId) {
        return this.mgisUseEntryMapper.selectList(new QueryWrapper<TMgisUseEntry>()
                .eq("bill_id", billId).orderByAsc("seq"));
    }

    private List<TMgisUseSheet> listSheets(Long billId) {
        return this.mgisUseSheetMapper.selectList(new QueryWrapper<TMgisUseSheet>()
                .eq("bill_id", billId).orderByAsc("line_no"));
    }

    private List<TMgisUseChecklist> listChecklist(Long billId) {
        return this.mgisUseChecklistMapper.selectList(new QueryWrapper<TMgisUseChecklist>()
                .eq("bill_id", billId).orderByAsc("line_no"));
    }

    private static void sleepBrief() {
        try {
            Thread.sleep(20L);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    /** 签署时辰以秒计：库面 DATETIME 精确到秒，签署与校验同一口径 */
    private static Date nowSecond() {
        return new Date(System.currentTimeMillis() / 1000L * 1000L);
    }
}
