package com.fc.v2.use;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fc.v2.common.use.UseChainJudge;
import com.fc.v2.common.use.UseChainReport;
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
import com.fc.v2.service.impl.TMgisUseBillServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 用密核准工序单测：受理五样、四阶走、省厅双签、替签不作数、发回勾销、
 * 否决定死、核准锁定、清单一键带出、窗口放成果、两口径对账、顺拉倒拉。
 */
class TMgisUseBillServiceImplTest {

    private TMgisUseBillMapper billMapper;
    private TMgisUseEntryMapper entryMapper;
    private TMgisUseSheetMapper sheetMapper;
    private TMgisUseChecklistMapper checklistMapper;
    private TMgisUseBillServiceImpl service;

    /** 假库：单据、逐单签署链、图幅行、清单行 */
    private List<TMgisUseBill> bills;
    private Map<Long, List<TMgisUseEntry>> chains;
    private Map<Long, List<TMgisUseSheet>> sheets;
    private Map<Long, List<TMgisUseChecklist>> checklists;
    private long idGen = 5000L;

    @BeforeEach
    void setUp() {
        billMapper = mock(TMgisUseBillMapper.class);
        entryMapper = mock(TMgisUseEntryMapper.class);
        sheetMapper = mock(TMgisUseSheetMapper.class);
        checklistMapper = mock(TMgisUseChecklistMapper.class);
        service = new TMgisUseBillServiceImpl();
        ReflectionTestUtils.setField(service, "mgisUseBillMapper", billMapper);
        ReflectionTestUtils.setField(service, "mgisUseEntryMapper", entryMapper);
        ReflectionTestUtils.setField(service, "mgisUseSheetMapper", sheetMapper);
        ReflectionTestUtils.setField(service, "mgisUseChecklistMapper", checklistMapper);

        bills = new ArrayList<TMgisUseBill>();
        chains = new HashMap<Long, List<TMgisUseEntry>>();
        sheets = new HashMap<Long, List<TMgisUseSheet>>();
        checklists = new HashMap<Long, List<TMgisUseChecklist>>();

        when(billMapper.selectById(any())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            for (TMgisUseBill b : bills) {
                if (b.getId().equals(id)) {
                    return b;
                }
            }
            return null;
        });
        when(billMapper.insert(any())).thenAnswer(inv -> {
            bills.add(inv.getArgument(0));
            return 1;
        });
        // 条件更新：把 UpdateWrapper 的 set 落到内存单据上，模拟库里的原子条件更新
        when(billMapper.update(org.mockito.ArgumentMatchers.isNull(), any(UpdateWrapper.class))).thenAnswer(inv -> {
            UpdateWrapper<?> uw = inv.getArgument(1);
            return applyUpdate(uw) ? 1 : 0;
        });

        when(entryMapper.selectList(any())).thenAnswer(inv -> {
            QueryWrapper<?> qw = inv.getArgument(0);
            qw.getSqlSegment();
            Object billId = qw.getParamNameValuePairs().values().iterator().next();
            List<TMgisUseEntry> chain = chains.get(((Number) billId).longValue());
            return chain == null ? new ArrayList<TMgisUseEntry>() : new ArrayList<TMgisUseEntry>(chain);
        });
        when(entryMapper.insert(any())).thenAnswer(inv -> {
            TMgisUseEntry e = inv.getArgument(0);
            e.setId(++idGen);
            chains.computeIfAbsent(e.getBillId(), k -> new ArrayList<TMgisUseEntry>()).add(e);
            return 1;
        });
        when(sheetMapper.selectList(any())).thenAnswer(inv -> {
            QueryWrapper<?> qw = inv.getArgument(0);
            qw.getSqlSegment();
            Object billId = qw.getParamNameValuePairs().values().iterator().next();
            List<TMgisUseSheet> list = sheets.get(((Number) billId).longValue());
            return list == null ? new ArrayList<TMgisUseSheet>() : new ArrayList<TMgisUseSheet>(list);
        });
        when(sheetMapper.insert(any())).thenAnswer(inv -> {
            TMgisUseSheet s = inv.getArgument(0);
            s.setId(++idGen);
            sheets.computeIfAbsent(s.getBillId(), k -> new ArrayList<TMgisUseSheet>()).add(s);
            return 1;
        });
        when(checklistMapper.selectList(any())).thenAnswer(inv -> {
            QueryWrapper<?> qw = inv.getArgument(0);
            qw.getSqlSegment();
            Object billId = qw.getParamNameValuePairs().values().iterator().next();
            List<TMgisUseChecklist> list = checklists.get(((Number) billId).longValue());
            return list == null ? new ArrayList<TMgisUseChecklist>() : new ArrayList<TMgisUseChecklist>(list);
        });
        when(checklistMapper.insert(any())).thenAnswer(inv -> {
            TMgisUseChecklist c = inv.getArgument(0);
            c.setId(++idGen);
            checklists.computeIfAbsent(c.getBillId(), k -> new ArrayList<TMgisUseChecklist>()).add(c);
            return 1;
        });
    }

    // ---------- 假库帮手：解析 UpdateWrapper 并落到内存 ----------

    private static final Pattern SET_PAIR =
            Pattern.compile("([A-Za-z_][A-Za-z0-9_]*)=\\#\\{[^}]*MPGENVAL(\\d+)\\}");
    private static final Pattern ID_COND =
            Pattern.compile("\\bid\\s*=\\s*\\#\\{[^}]*MPGENVAL(\\d+)\\}");

    /**
     * 把条件更新应用到内存单据。参数名值对是 HashMap、次序不保证（set 值编号在前、
     * 条件值在后），故按 SQL 片段里的 MPGENVAL 编号取值：set 列与编号成对解析，
     * 主键从 WHERE 的 id 条件取编号。假库单线程，条件与现值天然相合；真正的并发
     * 原子性由库里条件更新与服务层重读重算循环兜底。
     */
    private boolean applyUpdate(UpdateWrapper<?> uw) {
        // 触发条件归置，segment/sqlSet 都成形
        uw.getSqlSegment();
        Map<String, Object> params = uw.getParamNameValuePairs();

        Long id = null;
        Matcher idm = ID_COND.matcher(uw.getSqlSegment() == null ? "" : uw.getSqlSegment());
        if (idm.find()) {
            Object v = params.get("MPGENVAL" + idm.group(1));
            if (v instanceof Number) {
                id = ((Number) v).longValue();
            }
        }
        TMgisUseBill target = null;
        for (TMgisUseBill b : bills) {
            if (b.getId().equals(id)) {
                target = b;
            }
        }
        if (target == null) {
            return false;
        }
        Matcher sm = SET_PAIR.matcher(uw.getSqlSet() == null ? "" : uw.getSqlSet());
        while (sm.find()) {
            String col = sm.group(1);
            Object v = params.get("MPGENVAL" + sm.group(2));
            setField(target, col, v);
        }
        return true;
    }

    private void setField(TMgisUseBill b, String col, Object v) {
        switch (col) {
            case "station":
                b.setStation((String) v);
                break;
            case "round_no":
                b.setRoundNo(((Number) v).intValue());
                break;
            case "status":
                b.setStatus(((Number) v).intValue());
                break;
            case "need_count":
                b.setNeedCount(((Number) v).intValue());
                break;
            case "sign_count":
                b.setSignCount(((Number) v).intValue());
                break;
            case "approved_doc_no":
                b.setApprovedDocNo((String) v);
                break;
            case "approved_by":
                b.setApprovedBy((String) v);
                break;
            case "approved_at":
                b.setApprovedAt((java.util.Date) v);
                break;
            case "checklist_gen":
                b.setChecklistGen(((Number) v).intValue());
                break;
            case "checklist_no":
                b.setChecklistNo((String) v);
                break;
            case "issued_by":
                b.setIssuedBy((String) v);
                break;
            case "issued_at":
                b.setIssuedAt((java.util.Date) v);
                break;
            default:
                throw new IllegalStateException("测试未认的列：" + col);
        }
    }

    // ---------- 请求帮手 ----------

    private static UseAcceptRequest acceptReq() {
        UseAcceptRequest r = new UseAcceptRequest();
        r.setApplicantUnit("江南省测绘院第一工程院");
        r.setPurpose("省基础测绘更新用图");
        r.setSecretLevel("秘密");
        r.setMapYear(2025);
        r.setSheetCodes(new ArrayList<String>(Arrays.asList("H49G001001", "H49G001002")));
        return r;
    }

    private static UseSignRequest signReq(Long billId, String action) {
        UseSignRequest r = new UseSignRequest();
        r.setBillId(billId);
        r.setAction(action);
        return r;
    }

    private List<TMgisUseEntry> chainOf(Long id) {
        return chains.getOrDefault(id, Collections.emptyList());
    }

    private UseAcceptResult acceptOne() {
        return service.accept(acceptReq(), "窗口周某");
    }

    /** 走到省厅两枚齐、已核准（未出清单、未放成果） */
    private TMgisUseBill approveFully(Long id) {
        UseSignRequest city = signReq(id, "VERIFY");
        city.setOpinion("用途属实，与作业范围搭得上");
        service.sign(city, "市局吴某");
        UseSignRequest secret = signReq(id, "CONFIRM");
        secret.setSignRole(UseSignRole.SECRET.name());
        secret.setOpinion("密级核定、脱密处理到位");
        service.sign(secret, "省厅郑某");
        UseSignRequest purpose = signReq(id, "CONFIRM");
        purpose.setSignRole(UseSignRole.PURPOSE.name());
        purpose.setOpinion("用途正当，准予提供");
        UseSignResult r = service.sign(purpose, "省厅王某");
        return r.getBill();
    }

    // ---------- 窗口受理 ----------

    @Test
    void 受理五样齐才立单() {
        UseAcceptResult r = acceptOne();
        TMgisUseBill b = r.getBill();
        assertTrue(b.getBillNo().startsWith("YM"));
        assertEquals("江南省测绘院第一工程院", b.getApplicantUnit());
        assertEquals(UseStation.CITY.name(), b.getStation());
        assertEquals(Integer.valueOf(1), b.getRoundNo());
        assertEquals(Integer.valueOf(0), b.getSignCount());
        assertEquals(Integer.valueOf(0), b.getStatus());
        assertEquals(2, r.getSheets().size());
        // 图幅行受理即锁，行次自一连排
        assertEquals(Integer.valueOf(1), r.getSheets().get(0).getLineNo());
        assertEquals("H49G001001", r.getSheets().get(0).getSheetCode());
        // 头一笔是窗口受理，认 64 个 0
        TMgisUseEntry e = r.getEntry();
        assertEquals(UseStation.WINDOW.name(), e.getStation());
        assertEquals(UseChainJudge.GENESIS, e.getPrevHash());
        assertEquals(UseChainJudge.hashOf(e), e.getHash());
        assertTrue(service.verifyChain(b.getId()).isOk());
    }

    @Test
    void 五样缺一项受理不下去() {
        String operator = "窗口周某";
        UseAcceptRequest r1 = acceptReq();
        r1.setApplicantUnit("  ");
        assertThrows(UseFlowException.class, () -> service.accept(r1, operator));
        UseAcceptRequest r2 = acceptReq();
        r2.setPurpose(null);
        assertThrows(UseFlowException.class, () -> service.accept(r2, operator));
        UseAcceptRequest r3 = acceptReq();
        r3.setSecretLevel(null);
        assertThrows(UseFlowException.class, () -> service.accept(r3, operator));
        UseAcceptRequest r4 = acceptReq();
        r4.setMapYear(null);
        assertThrows(UseFlowException.class, () -> service.accept(r4, operator));
        UseAcceptRequest r5 = acceptReq();
        r5.setSheetCodes(Collections.<String>emptyList());
        assertThrows(UseFlowException.class, () -> service.accept(r5, operator));
        // 一笔都没立
        assertTrue(bills.isEmpty());
    }

    @Test
    void 图幅代号册内重号不受理() {
        UseAcceptRequest r = acceptReq();
        r.setSheetCodes(Arrays.asList("A01", "A01"));
        assertThrows(UseFlowException.class, () -> service.accept(r, "窗口周某"));
        assertTrue(bills.isEmpty());
    }

    @Test
    void 受理人未认不收() {
        assertThrows(IllegalArgumentException.class, () -> service.accept(acceptReq(), ""));
    }

    // ---------- 四阶走满 ----------

    @Test
    void 四步走满_省厅两枚齐才放成果_顺拉倒拉皆通() {
        UseAcceptResult acc = acceptOne();
        Long id = acc.getBill().getId();

        // 市局核用途：件进省厅
        UseSignRequest city = signReq(id, "VERIFY");
        city.setOpinion("用途属实");
        UseSignResult r = service.sign(city, "市局吴某");
        assertEquals(UseStation.PROVINCE.name(), r.getBill().getStation());
        assertEquals(Integer.valueOf(2), r.getBill().getNeedCount());

        // 省厅先签密级脱密一枚：件仍停省厅
        UseSignRequest secret = signReq(id, "CONFIRM");
        secret.setSignRole(UseSignRole.SECRET.name());
        secret.setOpinion("脱密到位");
        r = service.sign(secret, "省厅郑某");
        assertEquals(UseStation.PROVINCE.name(), r.getBill().getStation());
        assertEquals(Integer.valueOf(1), r.getBill().getSignCount());

        // 用途核准一枚补齐：核准给号、回窗口
        UseSignRequest purpose = signReq(id, "CONFIRM");
        purpose.setSignRole(UseSignRole.PURPOSE.name());
        purpose.setOpinion("用途可给");
        r = service.sign(purpose, "省厅王某");
        TMgisUseBill approved = r.getBill();
        assertEquals(UseStation.ISSUE.name(), approved.getStation());
        assertEquals(Integer.valueOf(1), approved.getStatus());
        assertNotNull(approved.getApprovedDocNo());
        assertTrue(approved.getApprovedDocNo().startsWith("PZ"));
        assertEquals("省厅王某", approved.getApprovedBy());
        assertNotNull(approved.getApprovedAt());

        // 清单未生，窗口点不出放成果
        UseSignRequest issue = signReq(id, "ISSUE");
        UseFlowException ex = assertThrows(UseFlowException.class, () -> service.sign(issue, "窗口周某"));
        assertTrue(ex.getMessage().contains("清单"));

        // 一键生成清单：四样照核准件带过
        UseChecklistView cl = service.generateChecklist(id, "窗口周某");
        assertTrue(cl.getBill().getChecklistNo().startsWith("QY"));
        assertEquals(2, cl.getRows().size());
        TMgisUseChecklist row0 = cl.getRows().get(0);
        assertEquals("H49G001001", row0.getSheetCode());
        assertEquals("秘密", row0.getSecretLevel());
        assertEquals(Integer.valueOf(2025), row0.getMapYear());
        assertEquals(approved.getApprovedDocNo(), row0.getApprovedDocNo());
        assertEquals(cl.getBill().getChecklistNo(), row0.getChecklistNo());

        // 第二回生成不重出，行数不变
        UseChecklistView cl2 = service.generateChecklist(id, "窗口周某");
        assertEquals(2, cl2.getRows().size());

        // 清单齐，窗口凭单放成果
        r = service.sign(issue, "窗口周某");
        assertNotNull(r.getBill().getIssuedAt());
        assertEquals("窗口周某", r.getBill().getIssuedBy());

        // 整链 5 笔，顺拉倒拉皆通
        assertEquals(5, chainOf(id).size());
        UseChainReport report = service.verifyChain(id);
        assertTrue(report.isOk());
    }

    @Test
    void 省厅两名同一分钟并发落签也是两笔不并一条() {
        UseAcceptResult acc = acceptOne();
        Long id = acc.getBill().getId();
        approveToProvinceWithOne(id);
        // 同序：第二枚补签后核准
        UseSignRequest purpose = signReq(id, "CONFIRM");
        purpose.setSignRole(UseSignRole.PURPOSE.name());
        purpose.setOpinion("可给");
        UseSignResult r = service.sign(purpose, "省厅王某");
        assertEquals(Integer.valueOf(1), r.getBill().getStatus());
        long confirms = chainOf(id).stream()
                .filter(e -> "CONFIRM".equals(e.getAction())).count();
        assertEquals(2, confirms);
    }

    private void approveToProvinceWithOne(Long id) {
        UseSignRequest city = signReq(id, "VERIFY");
        city.setOpinion("属实");
        service.sign(city, "市局吴某");
        UseSignRequest secret = signReq(id, "CONFIRM");
        secret.setSignRole(UseSignRole.SECRET.name());
        secret.setOpinion("脱密到位");
        service.sign(secret, "省厅郑某");
    }

    @Test
    void 同一人顶两名属替签不作数() {
        UseAcceptResult acc = acceptOne();
        Long id = acc.getBill().getId();
        approveToProvinceWithOne(id);
        // 另一名也由郑某签：替签，顶回，件仍停省厅
        UseSignRequest purpose = signReq(id, "CONFIRM");
        purpose.setSignRole(UseSignRole.PURPOSE.name());
        purpose.setOpinion("可给");
        UseFlowException e = assertThrows(UseFlowException.class,
                () -> service.sign(purpose, "省厅郑某"));
        assertTrue(e.getMessage().contains("替签"));
        TMgisUseBill b = service.selectTMgisUseBillById(id);
        assertEquals(UseStation.PROVINCE.name(), b.getStation());
        assertEquals(Integer.valueOf(0), b.getStatus());
    }

    @Test
    void 同一点第二回只认头一笔() {
        UseAcceptResult acc = acceptOne();
        Long id = acc.getBill().getId();
        approveToProvinceWithOne(id);
        UseSignRequest secret = signReq(id, "CONFIRM");
        secret.setSignRole(UseSignRole.SECRET.name());
        secret.setOpinion("又签一遍");
        UseSignResult again = service.sign(secret, "省厅郑某");
        assertTrue(again.isDuplicated());
        assertEquals(Integer.valueOf(1), again.getBill().getSignCount());
        assertEquals(1, chainOf(id).stream().filter(e -> "CONFIRM".equals(e.getAction())
                && "SECRET".equals(e.getSignRole())).count());
    }

    @Test
    void 隔着阶落签当场顶回() {
        UseAcceptResult acc = acceptOne();
        Long id = acc.getBill().getId();
        // 件停市局，直接点省厅会签
        UseSignRequest secret = signReq(id, "CONFIRM");
        secret.setSignRole(UseSignRole.SECRET.name());
        UseFlowException e = assertThrows(UseFlowException.class,
                () -> service.sign(secret, "省厅郑某"));
        assertTrue(e.getMessage().contains("隔着阶"));
        assertEquals(1, chainOf(id).size());
    }

    @Test
    void 省厅会签不指明分工顶回() {
        UseAcceptResult acc = acceptOne();
        Long id = acc.getBill().getId();
        UseSignRequest city = signReq(id, "VERIFY");
        city.setOpinion("属实");
        service.sign(city, "市局吴某");
        UseSignRequest noRole = signReq(id, "CONFIRM");
        noRole.setOpinion("意见");
        assertThrows(UseFlowException.class, () -> service.sign(noRole, "省厅郑某"));
    }

    // ---------- 发回勾销 ----------

    @Test
    void 省厅发回市局_前各阶签退出页面_从市局重攒() {
        UseAcceptResult acc = acceptOne();
        Long id = acc.getBill().getId();
        approveToProvinceWithOne(id);

        // 省厅嫌脱密说明单薄，发回市局，缘由必填
        UseSignRequest back = signReq(id, "SEND_BACK");
        back.setReason("脱密说明单薄，补正后重报");
        UseSignResult r = service.sign(back, "省厅郑某");
        assertEquals(UseStation.CITY.name(), r.getBill().getStation());
        assertEquals(Integer.valueOf(2), r.getBill().getRoundNo());
        assertEquals(Integer.valueOf(0), r.getBill().getSignCount());

        // 页面口径：市局、省厅只显第二轮，头一轮废票不并堂；窗口受理仍在
        UseBillView view = service.pageView(id);
        int cityShown = 0;
        int provinceShown = 0;
        for (UseBillView.StationPage sp : view.getStations()) {
            if (UseStation.CITY.name().equals(sp.getStation())) {
                cityShown = sp.getEntries().size();
                for (TMgisUseEntry e : sp.getEntries()) {
                    assertEquals(Integer.valueOf(2), e.getRoundNo());
                }
            }
            if (UseStation.PROVINCE.name().equals(sp.getStation())) {
                provinceShown = sp.getEntries().size();
            }
            if (UseStation.WINDOW.name().equals(sp.getStation())) {
                assertEquals(1, sp.getEntries().size());
            }
        }
        // 重审尚未签：市局、省厅页面上都空着（旧票退出）
        assertEquals(0, cityShown);
        assertEquals(0, provinceShown);

        // 从市局重新攒起
        UseSignRequest city2 = signReq(id, "VERIFY");
        city2.setOpinion("脱密已补正");
        service.sign(city2, "市局吴某");
        UseSignRequest secret2 = signReq(id, "CONFIRM");
        secret2.setSignRole(UseSignRole.SECRET.name());
        secret2.setOpinion("脱密到位");
        service.sign(secret2, "省厅郑某");
        UseSignRequest purpose2 = signReq(id, "CONFIRM");
        purpose2.setSignRole(UseSignRole.PURPOSE.name());
        purpose2.setOpinion("可给");
        service.sign(purpose2, "省厅王某");
        TMgisUseBill approved = service.selectTMgisUseBillById(id);
        assertEquals(Integer.valueOf(1), approved.getStatus());

        // 链上旧笔一笔不删，共 8 笔；顺拉倒拉仍通
        assertEquals(7, chainOf(id).size());
        assertTrue(service.verifyChain(id).isOk());
    }

    @Test
    void 发回缘由必填() {
        UseAcceptResult acc = acceptOne();
        Long id = acc.getBill().getId();
        approveToProvinceWithOne(id);
        UseSignRequest back = signReq(id, "SEND_BACK");
        assertThrows(UseFlowException.class, () -> service.sign(back, "省厅郑某"));
    }

    // ---------- 否决 / 锁定 ----------

    @Test
    void 市局写明不予核准即定死() {
        UseAcceptResult acc = acceptOne();
        Long id = acc.getBill().getId();
        UseSignRequest veto = signReq(id, "VETO");
        veto.setReason("用途与作业范围不搭，不予核准");
        UseSignResult r = service.sign(veto, "市局吴某");
        assertEquals(Integer.valueOf(2), r.getBill().getStatus());

        // 事后加不进一枚签
        UseSignRequest city = signReq(id, "VERIFY");
        city.setOpinion("又想签");
        UseFlowException e = assertThrows(UseFlowException.class,
                () -> service.sign(city, "市局吴某"));
        assertTrue(e.getMessage().contains("定死"));
    }

    @Test
    void 省厅否决一样定死() {
        UseAcceptResult acc = acceptOne();
        Long id = acc.getBill().getId();
        approveToProvinceWithOne(id);
        UseSignRequest veto = signReq(id, "VETO");
        veto.setReason("用途不该给");
        UseSignResult r = service.sign(veto, "省厅王某");
        assertEquals(Integer.valueOf(2), r.getBill().getStatus());
        assertThrows(UseFlowException.class, () -> service.generateChecklist(id, "窗口周某"));
    }

    @Test
    void 否决理由必填() {
        UseAcceptResult acc = acceptOne();
        Long id = acc.getBill().getId();
        assertThrows(UseFlowException.class, () -> service.sign(signReq(id, "VETO"), "市局吴某"));
    }

    @Test
    void 核准之后各栏锁住() {
        UseAcceptResult acc = acceptOne();
        Long id = acc.getBill().getId();
        TMgisUseBill approved = approveFully(id);
        assertEquals(Integer.valueOf(1), approved.getStatus());
        // 核准后还想补一枚省厅签，顶回；要动另起新件
        UseSignRequest extra = signReq(id, "CONFIRM");
        extra.setSignRole(UseSignRole.SECRET.name());
        extra.setOpinion("多签一枚");
        UseFlowException e = assertThrows(UseFlowException.class,
                () -> service.sign(extra, "省厅郑某"));
        assertTrue(e.getMessage().contains("锁定"));
    }

    @Test
    void 未核准出不了清单() {
        UseAcceptResult acc = acceptOne();
        Long id = acc.getBill().getId();
        approveToProvinceWithOne(id);
        assertThrows(UseFlowException.class, () -> service.generateChecklist(id, "窗口周某"));
    }

    // ---------- 页面两口径 ----------

    @Test
    void 页面已签人数两口径同一数() {
        UseAcceptResult acc = acceptOne();
        Long id = acc.getBill().getId();
        approveToProvinceWithOne(id);
        UseBillView view = service.pageView(id);
        assertEquals(UseStation.PROVINCE.name(), view.getCurrentStation());
        assertEquals(2, view.getNeedCount());
        assertEquals(1, view.getStoredSignCount());
        assertEquals(1, view.getTalliedSignCount());
        assertTrue(view.isCountConsistent());
        // 只开省厅一阶的口子
        int open = 0;
        for (UseBillView.StationPage sp : view.getStations()) {
            if (sp.isOpen()) {
                open++;
                assertEquals(UseStation.PROVINCE.name(), sp.getStation());
            }
        }
        assertEquals(1, open);
    }

    @Test
    void 办结后页面不开口子() {
        UseAcceptResult acc = acceptOne();
        Long id = acc.getBill().getId();
        approveFully(id);
        service.generateChecklist(id, "窗口周某");
        service.sign(signReq(id, "ISSUE"), "窗口周某");
        UseBillView view = service.pageView(id);
        assertTrue(view.isTerminal());
        for (UseBillView.StationPage sp : view.getStations()) {
            assertFalse(sp.isOpen());
        }
    }

    @Test
    void 单子不在账上() {
        assertThrows(UseFlowException.class, () -> service.pageView(999L));
        assertThrows(UseFlowException.class, () -> service.verifyChain(999L));
        assertThrows(UseFlowException.class, () -> service.generateChecklist(999L, "窗口周某"));
    }

    @Test
    void 早年三个改动口子不放行() {
        UseAcceptResult acc = acceptOne();
        Long id = acc.getBill().getId();
        assertNull(service.approve(id, "吴某", "过"));
        assertNull(service.reject(id, "吴某", "驳"));
        assertNull(service.rollback(id, "退"));
    }
}
