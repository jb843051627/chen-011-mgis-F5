package com.fc.v2.use;

import com.fc.v2.common.use.UseAction;
import com.fc.v2.common.use.UseFlow;
import com.fc.v2.common.use.UseFlowException;
import com.fc.v2.common.use.UseSignRole;
import com.fc.v2.common.use.UseStation;
import com.fc.v2.model.auto.TMgisUseEntry;
import com.fc.v2.model.custom.UseAcceptRequest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 走阶引擎纯逻辑单测：受理五样、各阶应签人数、省厅双签计数、页面只显当前轮。
 */
class UseFlowTest {

    private static UseAcceptRequest req() {
        UseAcceptRequest r = new UseAcceptRequest();
        r.setApplicantUnit("甲院");
        r.setPurpose("用图");
        r.setSecretLevel("秘密");
        r.setMapYear(2025);
        r.setSheetCodes(Arrays.asList("A01", "A02"));
        return r;
    }

    private static TMgisUseEntry entry(UseStation st, UseAction act, UseSignRole role, int round) {
        TMgisUseEntry e = new TMgisUseEntry();
        e.setStation(st.name());
        e.setAction(act.name());
        e.setSignRole(role == null ? null : role.name());
        e.setRoundNo(round);
        return e;
    }

    @Test
    void 五样齐才过() {
        List<String> codes = UseFlow.validateAccept(req());
        assertEquals(Arrays.asList("A01", "A02"), codes);
    }

    @Test
    void 缺样与重号空行都顶回() {
        UseAcceptRequest r1 = req();
        r1.setApplicantUnit(" ");
        assertThrows(UseFlowException.class, () -> UseFlow.validateAccept(r1));
        UseAcceptRequest r2 = req();
        r2.setMapYear(null);
        assertThrows(UseFlowException.class, () -> UseFlow.validateAccept(r2));
        UseAcceptRequest r3 = req();
        r3.setMapYear(1800);
        assertThrows(UseFlowException.class, () -> UseFlow.validateAccept(r3));
        UseAcceptRequest r4 = req();
        r4.setSheetCodes(Arrays.asList("A01", " A01 "));
        assertThrows(UseFlowException.class, () -> UseFlow.validateAccept(r4));
        UseAcceptRequest r5 = req();
        r5.setSheetCodes(Arrays.asList("A01", " "));
        assertThrows(UseFlowException.class, () -> UseFlow.validateAccept(r5));
    }

    @Test
    void 各阶应签人数() {
        assertEquals(1, UseFlow.needCountOf(UseStation.WINDOW));
        assertEquals(1, UseFlow.needCountOf(UseStation.CITY));
        assertEquals(2, UseFlow.needCountOf(UseStation.PROVINCE));
        assertEquals(1, UseFlow.needCountOf(UseStation.ISSUE));
    }

    @Test
    void 省厅两名分工各数一枚() {
        List<TMgisUseEntry> chain = new ArrayList<TMgisUseEntry>();
        chain.add(entry(UseStation.PROVINCE, UseAction.CONFIRM, UseSignRole.SECRET, 1));
        assertEquals(1, UseFlow.tallySigned(chain, UseStation.PROVINCE, 1));
        chain.add(entry(UseStation.PROVINCE, UseAction.CONFIRM, UseSignRole.PURPOSE, 1));
        assertEquals(2, UseFlow.tallySigned(chain, UseStation.PROVINCE, 1));
        // 发回、否决不数
        chain.add(entry(UseStation.PROVINCE, UseAction.SEND_BACK, null, 1));
        assertEquals(2, UseFlow.tallySigned(chain, UseStation.PROVINCE, 1));
    }

    @Test
    void 旧轮签不计入当前轮() {
        List<TMgisUseEntry> chain = new ArrayList<TMgisUseEntry>();
        chain.add(entry(UseStation.PROVINCE, UseAction.CONFIRM, UseSignRole.SECRET, 1));
        chain.add(entry(UseStation.CITY, UseAction.VERIFY, null, 1));
        // 发回后重审：市局新一轮一枚，旧轮市局不算
        chain.add(entry(UseStation.CITY, UseAction.VERIFY, null, 2));
        assertEquals(1, UseFlow.tallySigned(chain, UseStation.CITY, 2));
        assertEquals(1, UseFlow.tallySigned(chain, UseStation.CITY, 1));
    }

    @Test
    void 另一名分工认得准() {
        List<TMgisUseEntry> chain = new ArrayList<TMgisUseEntry>();
        chain.add(entry(UseStation.PROVINCE, UseAction.CONFIRM, UseSignRole.SECRET, 1));
        assertEquals(UseSignRole.SECRET.name(),
                UseFlow.otherConfirm(chain, UseSignRole.PURPOSE, 1).getSignRole());
        assertEquals(null, UseFlow.otherConfirm(chain, UseSignRole.SECRET, 1));
    }

    @Test
    void 页面只显当前轮_窗口始终是头轮() {
        List<TMgisUseEntry> chain = new ArrayList<TMgisUseEntry>();
        chain.add(entry(UseStation.WINDOW, UseAction.ACCEPT, null, 1));
        chain.add(entry(UseStation.CITY, UseAction.VERIFY, null, 1));
        chain.add(entry(UseStation.PROVINCE, UseAction.CONFIRM, UseSignRole.SECRET, 1));
        chain.add(entry(UseStation.PROVINCE, UseAction.SEND_BACK, null, 1));
        chain.add(entry(UseStation.CITY, UseAction.VERIFY, null, 2));
        Map<String, List<TMgisUseEntry>> page = UseFlow.pageOf(chain, 2);
        // 市局只显第二轮一枚（旧票退出，不并堂）
        assertEquals(1, page.get(UseStation.CITY.name()).size());
        assertEquals(Integer.valueOf(2), page.get(UseStation.CITY.name()).get(0).getRoundNo());
        // 省厅第二轮还没签，页面上没有省厅（旧轮会签、发回都退出）
        assertTrue(!page.containsKey(UseStation.PROVINCE.name()));
        // 窗口受理始终显头一轮那一枚
        assertEquals(1, page.get(UseStation.WINDOW.name()).size());
    }

    @Test
    void 动作与分工认中文名() {
        assertEquals(UseAction.CONFIRM, UseAction.require("省厅会签"));
        assertEquals(UseAction.VETO, UseAction.require("VETO"));
        assertEquals(UseSignRole.SECRET, UseSignRole.require("密级脱密"));
        assertThrows(IllegalArgumentException.class, () -> UseAction.require("画押"));
    }
}
