package com.fc.v2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.common.use.UseFlowException;
import com.fc.v2.model.auto.TMgisUseBill;
import com.fc.v2.model.custom.UseAcceptRequest;
import com.fc.v2.model.custom.UseSignRequest;
import com.fc.v2.service.ITMgisUseBillService;
import com.fc.v2.shiro.util.ShiroUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 用密成果提供使用逐级核准单 Controller。
 *
 * <p>四步走：窗口受理、市局核用途、省厅两人会签、窗口凭单放成果。写入门只留两扇：
 * 进门 {@code POST /mgisUseBill/accept} 立单，{@code POST /mgisUseBill/sign} 统管
 * 画押（核用途/会签/放成果）、驳回（省厅发回市局）、否决（不予核准）三事——动作由
 * 请求指认，章程全在服务层。绕开门面直接立单、改单、删单的路不辟：本控制器没有
 * add/edit/remove 端点。签署人自登录态取，不许请求代报；签署时辰由服务层写死；
 * 传下的签署内容分毫不动，只按回话刷新。</p>
 *
 * @author fuce
 * @date 2026-09-29
 */
@Api(value = "用密成果提供使用逐级核准单")
@Controller
@RequestMapping("/mgisUseBill")
public class MgisUseBillController extends BaseController {

    @Autowired
    private ITMgisUseBillService mgisUseBillService;

    @Log(title = "用密核准窗口受理", action = "accept")
    @ApiOperation(value = "窗口受理", notes = "进门一张申请单：申请单位、用途、图幅代号、密级、出图年份五样"
            + "缺一项受理不下去；系统给核准单号，受理即送市局。受理立单只此一门，无 add/edit/remove")
    @PostMapping("/accept")
    @RequiresPermissions("mgisUseBill:accept")
    @ResponseBody
    public AjaxResult accept(UseAcceptRequest req) {
        try {
            // 受理人自登录态取，核准单号系统给——请求里说了不算
            return retobject(200, mgisUseBillService.accept(req, ShiroUtils.getLoginName()));
        } catch (UseFlowException | IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }

    @Log(title = "用密核准签署", action = "sign")
    @ApiOperation(value = "签署", notes = "唯一动作门：VERIFY市局核用途 / CONFIRM省厅会签(密级脱密、用途核准"
            + "两名各签各的) / SEND_BACK省厅发回市局 / VETO不予核准 / ISSUE窗口放成果。"
            + "省厅两枚没齐、清单未生，窗口点不出放成果；隔阶落签、替签、否决/核准后再签一律顶回")
    @PostMapping("/sign")
    @RequiresPermissions("mgisUseBill:sign")
    @ResponseBody
    public AjaxResult sign(UseSignRequest req) {
        try {
            // 签署人自登录态取（替签不作数），签署时辰由服务层写死，传下内容分毫不动
            return retobject(200, mgisUseBillService.sign(req, ShiroUtils.getLoginName()));
        } catch (UseFlowException | IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }

    @Log(title = "用密领用清单生成", action = "checklist")
    @ApiOperation(value = "领用清单一键生成", notes = "省厅两枚齐、已核准后一键生成；图幅代号、密级、出图年份、"
            + "批准文号照核准那一份带过，窗口不再誊第二遍；一家单位一册图对一条清单，只生成一次")
    @PostMapping("/{id}/checklist")
    @RequiresPermissions("mgisUseBill:sign")
    @ResponseBody
    public AjaxResult checklist(@PathVariable("id") Long id) {
        try {
            return retobject(200, mgisUseBillService.generateChecklist(id, ShiroUtils.getLoginName()));
        } catch (UseFlowException | IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }

    @Log(title = "用密核准台账", action = "list")
    @ApiOperation(value = "核准单台账", notes = "按阶、情形、申请单位查")
    @GetMapping("/list")
    @RequiresPermissions("mgisUseBill:list")
    @ResponseBody
    public ResultTable list(TMgisUseBill record) {
        QueryWrapper<TMgisUseBill> queryWrapper = new QueryWrapper<TMgisUseBill>();
        if (record != null) {
            if (record.getBillNo() != null && !record.getBillNo().trim().isEmpty()) {
                queryWrapper.eq("bill_no", record.getBillNo().trim());
            }
            if (record.getApplicantUnit() != null && !record.getApplicantUnit().trim().isEmpty()) {
                queryWrapper.like("applicant_unit", record.getApplicantUnit().trim());
            }
            if (record.getStation() != null && !record.getStation().trim().isEmpty()) {
                queryWrapper.eq("station", record.getStation().trim());
            }
            if (record.getStatus() != null) {
                queryWrapper.eq("status", record.getStatus());
            }
        }
        queryWrapper.orderByDesc("id");
        startPage();
        com.github.pagehelper.PageInfo<TMgisUseBill> page =
                new com.github.pagehelper.PageInfo<TMgisUseBill>(
                        mgisUseBillService.selectTMgisUseBillList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    @ApiOperation(value = "核准单页面", notes = "件停在哪一阶只开哪一阶的口子；每阶只显当前轮落笔；"
            + "已签人数页面口径与单据口径当场对账")
    @GetMapping("/{id}")
    @RequiresPermissions("mgisUseBill:list")
    @ResponseBody
    public AjaxResult detail(@PathVariable("id") Long id) {
        try {
            return retobject(200, mgisUseBillService.pageView(id));
        } catch (UseFlowException | IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }

    @ApiOperation(value = "签署链校验", notes = "顺拉一遍、倒拉一遍：少一笔、次序错一处、事后补录，"
            + "这份核准即不合格；发回旧笔原样在链，页面只显新一轮")
    @GetMapping("/{id}/chain")
    @RequiresPermissions("mgisUseBill:list")
    @ResponseBody
    public AjaxResult chain(@PathVariable("id") Long id) {
        try {
            return retobject(200, mgisUseBillService.verifyChain(id));
        } catch (UseFlowException | IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }
}
