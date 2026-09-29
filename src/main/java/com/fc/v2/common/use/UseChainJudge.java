package com.fc.v2.common.use;

import com.fc.v2.model.auto.TMgisUseEntry;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 签署链校验：核准放成果之后，整张单顺着签署先后拉一遍、倒着也拉一遍。
 *
 * <p>每一笔记「签署人、时辰、写的话、前一笔哈希」，签署那一下由系统写死，
 * 事后由不得人回头补录，也不许挪时辰。顺拉从窗口受理念到放成果，逐笔重算哈希、
 * 逐笔对前一笔的接扣，须与当日逐阶页面上留下的内容一字不差；倒拉从末阶沿前一笔
 * 哈希念回受理，一笔都不能少。少一笔、次序错一处，或哪一笔系事后人手补进去的
 * （哈希对不上、时辰倒挂），这份核准即不合格。发回勾销不删笔：旧轮各笔原样在链上，
 * 跨轮只许「省厅发回 → 市局重审」，废票与新票在链上分得清、在页面上不并堂。</p>
 *
 * @author fuce
 * @date 2026-09-29
 */
public final class UseChainJudge {

    private UseChainJudge() {
    }

    /** 头一笔的前哈希：64 个 0，窗口受理那一笔认它 */
    public static final String GENESIS =
            "0000000000000000000000000000000000000000000000000000000000000000";

    /**
     * 本笔哈希：把这一笔的单、序号、轮次、阶、动作、分工、写的话、签署人、
     * 签署时辰、前一笔哈希一并押进 SHA-256。动其中任何一处，哈希即对不上。
     */
    public static String hashOf(TMgisUseEntry e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.getBillId()).append('\n')
                .append(e.getSeq()).append('\n')
                .append(e.getRoundNo()).append('\n')
                .append(e.getStation()).append('\n')
                .append(e.getAction()).append('\n')
                .append(e.getSignRole() == null ? "" : e.getSignRole()).append('\n')
                .append(e.getPayload() == null ? "" : e.getPayload()).append('\n')
                .append(e.getPennedBy() == null ? "" : e.getPennedBy()).append('\n')
                .append(e.getPennedAt() == null ? "" : penText(e)).append('\n')
                .append(e.getPrevHash());
        return sha256Hex(sb.toString());
    }

    /**
     * 顺拉：从窗口受理念到放成果（或不予核准）。
     * 序号自 1 连排、头一笔是窗口受理且认 GENESIS、每笔接得住前一笔哈希、
     * 内容重算一字不差、时辰只增不减、走阶次序合法——哪一处对不上即不合格。
     */
    public static Leg verifyForward(List<TMgisUseEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return Leg.fail(null, "链上一笔都没有，窗口受理那一笔不见了");
        }
        TMgisUseEntry prev = null;
        int expectSeq = 1;
        for (TMgisUseEntry cur : entries) {
            Long at = cur.getSeq() == null ? null : cur.getSeq().longValue();
            if (cur.getSeq() == null || cur.getSeq() != expectSeq) {
                return Leg.fail(at, "第 " + expectSeq + " 笔序号断档或错位，链上少笔或次序错乱");
            }
            if (expectSeq == 1) {
                if (!UseAction.ACCEPT.name().equals(cur.getAction())) {
                    return Leg.fail(at, "头一笔不是窗口受理，来路不正");
                }
                if (!UseStation.WINDOW.name().equals(cur.getStation())) {
                    return Leg.fail(at, "头一笔不落窗口受理，来路不正");
                }
                if (!GENESIS.equals(cur.getPrevHash())) {
                    return Leg.fail(at, "受理那一笔的前哈希不是 64 个 0，来路不正");
                }
            } else {
                if (!prev.getHash().equals(cur.getPrevHash())) {
                    return Leg.fail(at, "第 " + expectSeq + " 笔接不住前一笔，链在这里断开");
                }
                if (prev.getPennedAt() != null && cur.getPennedAt() != null
                        && cur.getPennedAt().before(prev.getPennedAt())) {
                    return Leg.fail(at, "第 " + expectSeq + " 笔时辰早于前一笔，疑似事后挪时辰补录");
                }
                if (!UseFlow.legalTransition(prev, cur)) {
                    return Leg.fail(at, "第 " + expectSeq + " 笔走阶次序错乱："
                            + UseFlow.labelOf(prev.getStation()) + " → "
                            + UseFlow.labelOf(cur.getStation()) + " 不合章程");
                }
                // 省厅两枚没齐，窗口点不出放成果：挪到窗口放成果前，须数得到
                // 密级脱密、用途核准两名各签一枚（同一分工两枚不算两名）
                if (UseAction.ISSUE.name().equals(cur.getAction())
                        && UseStation.PROVINCE.name().equals(prev.getStation())
                        && provinceConfirms(entries, cur) < UseSignRole.PROVINCE_NEED) {
                    return Leg.fail(at, "省厅只签了一枚，件走不到窗口放成果——两枚点齐才放行");
                }
            }
            if (cur.getHash() == null || !cur.getHash().equals(hashOf(cur))) {
                return Leg.fail(at, "第 " + expectSeq + " 笔内容被改动（哈希对不上），疑似事后人手补录");
            }
            prev = cur;
            expectSeq++;
        }
        return Leg.ok();
    }

    /**
     * 倒拉：从末阶沿前一笔哈希念回窗口受理。
     * 每步的前哈希都须在链上找得到且只找得到一笔，念到头须是受理，
     * 步数须与链上总笔数相合——少一笔即不合格。
     */
    public static Leg verifyBackward(List<TMgisUseEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return Leg.fail(null, "链上一笔都没有，无从倒拉");
        }
        Map<String, TMgisUseEntry> byHash = new HashMap<String, TMgisUseEntry>();
        for (TMgisUseEntry e : entries) {
            if (e.getHash() != null && byHash.put(e.getHash(), e) != null) {
                return Leg.fail(e.getSeq() == null ? null : e.getSeq().longValue(),
                        "两笔同一个哈希，链上有鬼");
            }
        }
        TMgisUseEntry cur = entries.get(entries.size() - 1);
        int steps = 1;
        while (!GENESIS.equals(cur.getPrevHash())) {
            TMgisUseEntry prev = byHash.get(cur.getPrevHash());
            if (prev == null) {
                return Leg.fail(cur.getSeq() == null ? null : cur.getSeq().longValue(),
                        "倒拉断在第 " + cur.getSeq() + " 笔：前一笔找不到，链上少笔");
            }
            cur = prev;
            steps++;
        }
        if (!UseAction.ACCEPT.name().equals(cur.getAction())) {
            return Leg.fail(cur.getSeq() == null ? null : cur.getSeq().longValue(),
                    "倒拉念到头不是窗口受理，来路不正");
        }
        if (steps != entries.size()) {
            return Leg.fail(null, "倒拉只念回 " + steps + " 笔，链上共 " + entries.size()
                    + " 笔，有笔不在链上（疑似事后塞入）");
        }
        return Leg.ok();
    }

    /** 签署时辰一律精确到秒（库面 DATETIME 以秒计，签署与校验同一口径） */
    static String penText(TMgisUseEntry e) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(e.getPennedAt());
    }

    /**
     * 截至放成果那一笔（含其前各笔），省厅这一轮凑齐了几枚不同分工的会签。
     * 密级脱密、用途核准各一枚才算两枚；同一分工落两枚只算一枚。
     */
    private static int provinceConfirms(List<TMgisUseEntry> entries, TMgisUseEntry issueEntry) {
        java.util.Set<String> roles = new java.util.HashSet<String>();
        int round = issueEntry.getRoundNo() == null ? 1 : issueEntry.getRoundNo();
        for (TMgisUseEntry e : entries) {
            if (e == issueEntry) {
                break;
            }
            if (e.getRoundNo() != null && e.getRoundNo() == round
                    && UseStation.PROVINCE.name().equals(e.getStation())
                    && UseAction.CONFIRM.name().equals(e.getAction())
                    && e.getSignRole() != null) {
                roles.add(e.getSignRole());
            }
        }
        return roles.size();
    }

    private static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不在台上", e);
        }
    }

    /**
     * 一程校验的结论：顺拉一程、倒拉一程，各自回话。
     */
    public static final class Leg {
        private final boolean ok;
        private final Long breakSeq;
        private final String message;

        private Leg(boolean ok, Long breakSeq, String message) {
            this.ok = ok;
            this.breakSeq = breakSeq;
            this.message = message;
        }

        public static Leg ok() {
            return new Leg(true, null, "通");
        }

        public static Leg fail(Long breakSeq, String message) {
            return new Leg(false, breakSeq, message);
        }

        public boolean isOk() {
            return ok;
        }

        /** 断在第几笔（链上序号），不断为 null */
        public Long getBreakSeq() {
            return breakSeq;
        }

        public String getMessage() {
            return message;
        }
    }
}
