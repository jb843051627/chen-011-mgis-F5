package com.fc.v2.use;

import com.fc.v2.common.use.UseAction;
import com.fc.v2.common.use.UseChainJudge;
import com.fc.v2.common.use.UseSignRole;
import com.fc.v2.common.use.UseStation;
import com.fc.v2.model.auto.TMgisUseEntry;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 签署链单测：顺拉倒拉，少一笔、错一处、替签、事后补录都算不合格。
 */
class UseChainJudgeTest {

    private static final long BILL_ID = 9L;

    private static Date at(String s) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(s);
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /** 往链尾落一笔（哈希按章程押好） */
    private static TMgisUseEntry pen(List<TMgisUseEntry> chain, UseStation station, UseAction action,
                                    UseSignRole role, int round, String payload, String by, String at) {
        TMgisUseEntry e = new TMgisUseEntry();
        e.setBillId(BILL_ID);
        e.setSeq(chain.size() + 1);
        e.setRoundNo(round);
        e.setStation(station.name());
        e.setAction(action.name());
        e.setSignRole(role == null ? null : role.name());
        e.setPayload(payload);
        e.setPennedBy(by);
        e.setPennedAt(at(at));
        e.setPrevHash(chain.isEmpty() ? UseChainJudge.GENESIS
                : chain.get(chain.size() - 1).getHash());
        e.setHash(UseChainJudge.hashOf(e));
        chain.add(e);
        return e;
    }

    /** 一张走满四阶、省厅两人会签的单子 */
    private static List<TMgisUseEntry> fullChain() {
        List<TMgisUseEntry> chain = new ArrayList<TMgisUseEntry>();
        pen(chain, UseStation.WINDOW, UseAction.ACCEPT, null, 1, "{\"applicantUnit\":\"甲院\"}", "窗口周某", "2026-09-20 09:00:00");
        pen(chain, UseStation.CITY, UseAction.VERIFY, null, 1, "{\"opinion\":\"用途属实\"}", "市局吴某", "2026-09-20 10:00:00");
        pen(chain, UseStation.PROVINCE, UseAction.CONFIRM, UseSignRole.SECRET, 1, "{\"opinion\":\"脱密到位\"}", "省厅郑某", "2026-09-20 11:00:00");
        pen(chain, UseStation.PROVINCE, UseAction.CONFIRM, UseSignRole.PURPOSE, 1, "{\"opinion\":\"用途可给\"}", "省厅王某", "2026-09-20 11:00:30");
        pen(chain, UseStation.ISSUE, UseAction.ISSUE, null, 1, "{}", "窗口周某", "2026-09-20 15:00:00");
        return chain;
    }

    /** 一张省厅发回市局、重审后走满的单子 */
    private static List<TMgisUseEntry> sendBackChain() {
        List<TMgisUseEntry> chain = new ArrayList<TMgisUseEntry>();
        pen(chain, UseStation.WINDOW, UseAction.ACCEPT, null, 1, "{}", "窗口周某", "2026-09-20 09:00:00");
        pen(chain, UseStation.CITY, UseAction.VERIFY, null, 1, "{}", "市局吴某", "2026-09-20 10:00:00");
        pen(chain, UseStation.PROVINCE, UseAction.CONFIRM, UseSignRole.SECRET, 1, "{}", "省厅郑某", "2026-09-20 11:00:00");
        pen(chain, UseStation.PROVINCE, UseAction.SEND_BACK, null, 1, "{\"reason\":\"脱密说明单薄\"}", "省厅郑某", "2026-09-20 11:30:00");
        pen(chain, UseStation.CITY, UseAction.VERIFY, null, 2, "{}", "市局吴某", "2026-09-21 09:00:00");
        pen(chain, UseStation.PROVINCE, UseAction.CONFIRM, UseSignRole.SECRET, 2, "{}", "省厅郑某", "2026-09-21 10:00:00");
        pen(chain, UseStation.PROVINCE, UseAction.CONFIRM, UseSignRole.PURPOSE, 2, "{}", "省厅王某", "2026-09-21 10:00:20");
        pen(chain, UseStation.ISSUE, UseAction.ISSUE, null, 2, "{}", "窗口周某", "2026-09-21 14:00:00");
        return chain;
    }

    @Test
    void 顺拉倒拉皆通才算合格_双签() {
        List<TMgisUseEntry> chain = fullChain();
        assertTrue(UseChainJudge.verifyForward(chain).isOk());
        assertTrue(UseChainJudge.verifyBackward(chain).isOk());
    }

    @Test
    void 发回重审走满亦合格() {
        List<TMgisUseEntry> chain = sendBackChain();
        assertTrue(UseChainJudge.verifyForward(chain).isOk());
        assertTrue(UseChainJudge.verifyBackward(chain).isOk());
    }

    @Test
    void 省厅只签一枚放不出成果() {
        List<TMgisUseEntry> chain = new ArrayList<TMgisUseEntry>();
        pen(chain, UseStation.WINDOW, UseAction.ACCEPT, null, 1, "{}", "窗口周某", "2026-09-20 09:00:00");
        pen(chain, UseStation.CITY, UseAction.VERIFY, null, 1, "{}", "市局吴某", "2026-09-20 10:00:00");
        pen(chain, UseStation.PROVINCE, UseAction.CONFIRM, UseSignRole.SECRET, 1, "{}", "省厅郑某", "2026-09-20 11:00:00");
        // 只有密级脱密一枚，直接窗口放成果：省厅→窗口次序错乱
        pen(chain, UseStation.ISSUE, UseAction.ISSUE, null, 1, "{}", "窗口周某", "2026-09-20 15:00:00");
        assertFalse(UseChainJudge.verifyForward(chain).isOk());
    }

    @Test
    void 省厅两名同一分工即次序错乱() {
        List<TMgisUseEntry> chain = new ArrayList<TMgisUseEntry>();
        pen(chain, UseStation.WINDOW, UseAction.ACCEPT, null, 1, "{}", "窗口", "2026-09-20 09:00:00");
        pen(chain, UseStation.CITY, UseAction.VERIFY, null, 1, "{}", "市局", "2026-09-20 10:00:00");
        pen(chain, UseStation.PROVINCE, UseAction.CONFIRM, UseSignRole.SECRET, 1, "{}", "郑某", "2026-09-20 11:00:00");
        pen(chain, UseStation.PROVINCE, UseAction.CONFIRM, UseSignRole.SECRET, 1, "{}", "王某", "2026-09-20 11:01:00");
        // 同一分工两枚，不许并作两名
        assertFalse(UseChainJudge.verifyForward(chain).isOk());
    }

    @Test
    void 受理后直接放成果即跳阶() {
        List<TMgisUseEntry> chain = new ArrayList<TMgisUseEntry>();
        pen(chain, UseStation.WINDOW, UseAction.ACCEPT, null, 1, "{}", "窗口", "2026-09-20 09:00:00");
        pen(chain, UseStation.ISSUE, UseAction.ISSUE, null, 1, "{}", "窗口", "2026-09-20 10:00:00");
        assertFalse(UseChainJudge.verifyForward(chain).isOk());
    }

    @Test
    void 发回只许省厅发回市局() {
        List<TMgisUseEntry> chain = new ArrayList<TMgisUseEntry>();
        pen(chain, UseStation.WINDOW, UseAction.ACCEPT, null, 1, "{}", "窗口", "2026-09-20 09:00:00");
        pen(chain, UseStation.CITY, UseAction.VERIFY, null, 1, "{}", "市局", "2026-09-20 10:00:00");
        // 市局阶上凭空发回（无省厅会签前置、且落点不对）
        pen(chain, UseStation.CITY, UseAction.SEND_BACK, null, 2, "{}", "市局", "2026-09-20 11:00:00");
        assertFalse(UseChainJudge.verifyForward(chain).isOk());
    }

    @Test
    void 少一笔即不合格() {
        List<TMgisUseEntry> chain = fullChain();
        chain.remove(2); // 抽走省厅头一枚
        assertFalse(UseChainJudge.verifyForward(chain).isOk());
        assertFalse(UseChainJudge.verifyBackward(chain).isOk());
    }

    @Test
    void 事后改话即不合格() {
        List<TMgisUseEntry> chain = fullChain();
        chain.get(2).setPayload("{\"opinion\":\"改过的话\"}");
        assertFalse(UseChainJudge.verifyForward(chain).isOk());
        assertTrue(UseChainJudge.verifyForward(chain).getMessage().contains("哈希"));
    }

    @Test
    void 挪时辰即不合格() {
        List<TMgisUseEntry> chain = fullChain();
        TMgisUseEntry e = chain.get(3);
        e.setPennedAt(at("2026-09-19 08:00:00"));
        e.setHash(UseChainJudge.hashOf(e));
        assertFalse(UseChainJudge.verifyForward(chain).isOk());
        assertTrue(UseChainJudge.verifyForward(chain).getMessage().contains("时辰"));
    }

    @Test
    void 事后塞笔倒拉念不回来() {
        List<TMgisUseEntry> chain = fullChain();
        TMgisUseEntry ghost = new TMgisUseEntry();
        ghost.setBillId(BILL_ID);
        ghost.setSeq(chain.size() + 1);
        ghost.setRoundNo(1);
        ghost.setStation(UseStation.ISSUE.name());
        ghost.setAction(UseAction.ISSUE.name());
        ghost.setPayload("{}");
        ghost.setPennedBy("旁人");
        ghost.setPennedAt(at("2026-09-21 01:00:00"));
        ghost.setPrevHash(UseChainJudge.GENESIS);
        ghost.setHash(UseChainJudge.hashOf(ghost));
        chain.add(ghost);
        assertFalse(UseChainJudge.verifyForward(chain).isOk());
        assertFalse(UseChainJudge.verifyBackward(chain).isOk());
    }

    @Test
    void 头一笔不是受理即来路不正() {
        List<TMgisUseEntry> chain = new ArrayList<TMgisUseEntry>();
        pen(chain, UseStation.CITY, UseAction.VERIFY, null, 1, "{}", "市局", "2026-09-20 09:00:00");
        assertFalse(UseChainJudge.verifyForward(chain).isOk());
    }

    @Test
    void 空链不合格() {
        assertFalse(UseChainJudge.verifyForward(new ArrayList<TMgisUseEntry>()).isOk());
        assertFalse(UseChainJudge.verifyBackward(new ArrayList<TMgisUseEntry>()).isOk());
    }
}
