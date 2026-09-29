# 用密成果提供使用逐级核准规矩 → 系统口径对照

政务服务和监管处把用密成果的提供使用整个搬进系统的规矩，在系统里只有一处出处：
`com.fc.v2.common.use.UseFlow`（走阶推演）、`UseChainJudge`（拉链）。窗口受理门、
签署门、领用清单、核准单页面、链校验全从这两处过——两处口径合不上便是事故，
所以别处不许另写一套。

一份用密成果申请，全程只凭系统里这一张核准单，一单对一家单位一册图，单上分四步：
窗口受理、市局核用途、省厅两人会签、窗口凭单放成果。

| 处里的说法 | 系统口径 | 落点 |
| --- | --- | --- |
| 进门一张申请单填清楚，缺一项受理不下去 | 五样：申请单位、用途、图幅代号（一幅一行、册内不重号）、密级、出图年份；缺样、空行、重号、年份不在常理内，整单顶回一笔不落 | `UseFlow.validateAccept`、`TMgisUseBillServiceImpl.accept` |
| 核准单号系统给，请求说了不算 | `YM`+雪花号由系统划号段；受理人自登录态取 | `TMgisUseBillServiceImpl.accept` |
| 第二步市局核用途真不真、与作业范围搭不搭 | 市局一名签 `VERIFY`，意见必填；核过往省厅，核不过写明 `VETO` 不予核准 | `UseFlow.packSignPayload`、`advance` |
| 第三步省厅两人各签各的 | 密级脱密 `CONFIRM/SECRET`、用途核准 `CONFIRM/PURPOSE` 两枚；同一分钟并发落签也是两笔、不并成一条；唯一键 `(bill_id,station,action,sign_role,round_no)` 兜底并发 | `UseSignRole`、`advance`、`uk_use_entry_point` |
| 只签一枚，件走不到第四步 | 省厅已签人数未到 2，件停在 PROVINCE；条件更新当场重算两枚，第二枚才核准给号、回窗口 | `UseFlow.tallySigned`、`advance` |
| 替签不作数 | 另一枚已是同一签署人，第二枚当场顶回，一件不写 | `TMgisUseBillServiceImpl.sign` |
| 次序不可颠倒，省厅两枚没齐窗口点不出放成果 | 件停哪阶只开哪阶口子，隔阶落签顶回；放成果另须件已核准、清单已生成；链校验在进 ISSUE 前再数一遍两枚不同分工 | `UseFlow.checkStation`、`UseChainJudge.verifyForward` |
| 签了几个人由件自己记，谁也不能手填 | 件上 `sign_count` 与页面自签署链当场重算的数同出 `tallySigned` 一处；页面把两数并列，不一致 `countConsistent=false`，这一阶没过 | `UseFlow.tallySigned`、`UseBillView` |
| 省厅嫌脱密说明单薄发回市局 | `SEND_BACK` 缘由必填，只许省厅发回市局；轮次加一、件回市局、已签人数清零 | `advance`、`UseFlow.legalTransition` |
| 前头各处落过的签一笔勾销，从市局重新攒，废票与新票不许同堂并摆 | 签署链只增不改，旧轮各笔原样留链；页面只显当前轮（窗口受理始终是头轮），废票退出页面不与新票并排；查链仍见旧笔 | `UseFlow.pageOf`、`t_mgis_use_entry` |
| 哪一处写明不予核准，这份件定死，事后加不进一枚 | 市局、省厅两阶可 `VETO`，理由必填；`status=2` 后任何签署、清单都顶回 | `UseFlow.checkWritable` |
| 核准之后各栏锁住，要动另起新件 | 省厅两枚齐即 `status=1`、给批准文号 `PZ…`、回窗口；此后除生成清单、放成果外一律顶回；无 add/edit/remove 端点 | `UseFlow.checkWritable`、`advance` |
| 核准件一键生成领用清单 | 核准后 `POST /{id}/checklist` 一次生成；图幅代号、密级、出图年份、批准文号从核准那一份带过，窗口不誊第二遍；一份件只一条清单 `QY…`，第二回原样回不重出 | `TMgisUseBillServiceImpl.generateChecklist` |
| 一家单位一册图对一条清单 | 清单行照申请册内行次带出，逐行带密级、出图年份、批准文号 | `t_mgis_use_checklist` |
| 四步各落过谁的签能从末阶一级级捋回去，与当日屏上同模样、同次序 | 每笔记前一笔哈希（头一笔认 64 个 0），哈希押进单、序号、轮次、阶、动作、分工、话、签署人、时辰；顺拉自受理念到放成果逐笔重算，倒拉沿前哈希念回受理；少笔、错序、时辰倒挂、内容被改即不合格。页面表式就用现成那张申请单（逐阶 StationPage 复用申请单口径），不另起炉灶 | `UseChainJudge.verifyForward`/`verifyBackward`、`UseFlow.pageOf` |
| 此路只经「签署」一道门 | 画押（核用途/会签/放成果）、驳回（发回市局）、否决（不予核准）统打 `POST /mgisUseBill/sign` 进，动作由请求指认 | `MgisUseBillController.sign` |
| 签署登出什么结果页面不预置，成不成凭回传 | 签署人自登录态取、时辰服务层写死，请求带了不算；回话只回落了的那一笔与件眼下局面，页面按回话刷新；同点第二回 `duplicated=true` 回头一笔 | `MgisUseBillController`、`UseSignResult` |
| 绕开门面直接立单改单的路堵严 | 控制器无 add/edit/remove；立单只经受理门，改动只经签署门；早年 `approve/reject/rollback` 三签名保留但已封死、不放行 | `MgisUseBillController`、`TMgisUseBillServiceImpl` |
| 传下签名分毫莫动 | 签署人、意见、缘由服务层只读不改写；链笔一经落库无 update/delete 入口 | `t_mgis_use_entry`、`sign` |
| 再扩本事添个重载 | 接口早年四个方法签名一仍其旧；受理、会签、清单、看页、验链全走新加方法 | `ITMgisUseBillService` |

## 四阶（`UseStation`）与动作（`UseAction`）

| 阶 | 应签 | 这一阶收的动作 |
| --- | --- | --- |
| WINDOW 窗口受理(0) | 1（受理） | ACCEPT（走受理门，签署门不收） |
| CITY 市局核用途(1) | 1 | VERIFY 核用途；VETO 不予核准 |
| PROVINCE 省厅会签(2) | 2（SECRET 密级脱密 + PURPOSE 用途核准） | CONFIRM（带 signRole，两名各一枚）；SEND_BACK 发回市局；VETO 不予核准 |
| ISSUE 窗口放成果(3) | 1 | ISSUE 凭单放成果（须已核准 + 清单已生成） |

隔阶落签当场顶回；并发撞同一签，条件更新（`WHERE station/round_no/status 现值`）
兜底，后到者重读重算，两枚都不丢、不并成一条。

## 端点

| 端点 | 用途 |
| --- | --- |
| `POST /mgisUseBill/accept` | 窗口受理（立单唯一门；权限 `mgisUseBill:accept`） |
| `POST /mgisUseBill/sign` | 签署（唯一动作门；VERIFY/CONFIRM/SEND_BACK/VETO/ISSUE；权限 `mgisUseBill:sign`） |
| `POST /mgisUseBill/{id}/checklist` | 核准件一键生成领用清单（只生成一次；权限 `mgisUseBill:sign`） |
| `GET  /mgisUseBill/list` | 核准单台账（按单号、单位、阶、情形查） |
| `GET  /mgisUseBill/{id}` | 核准单页面：只开眼下那一阶的口子、每阶只显当前轮、已签人数两口径对账 |
| `GET  /mgisUseBill/{id}/chain` | 签署链校验：顺拉倒拉，两程皆通才算合格 |

无 add/edit/remove 端点。

建表脚本：`sql/mgis_use.sql`（核准单、签署链、申请图幅行、领用清单行四表；
含早期 `t_mgis_use_bill` 占位骨架的老库补列语句）。新库按 `doc/schema/mgis.sql`
全量建表已带全四表。
