package net.jxdg.travel.modules.content.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import net.jxdg.travel.common.*;
import net.jxdg.travel.modules.content.entity.*;
import net.jxdg.travel.modules.content.mapper.*;
import net.jxdg.travel.modules.product.entity.BizProduct;
import net.jxdg.travel.modules.product.mapper.BizProductMapper;
import net.jxdg.travel.modules.system.entity.SysSensitiveWord;
import net.jxdg.travel.modules.system.mapper.SysSensitiveWordMapper;
import net.jxdg.travel.security.LoginUser;
import net.jxdg.travel.security.SecurityUtils;
import net.jxdg.travel.tenant.TenantContext;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequiredArgsConstructor
public class ContentController {

    private final BizUgcPostMapper postMapper;
    private final BizUgcCommentMapper commentMapper;
    private final BizUgcLikeMapper likeMapper;
    private final BizUgcReportMapper reportMapper;
    private final BizCustomRequestMapper customRequestMapper;
    private final BizCustomQuoteMapper customQuoteMapper;
    private final BizLiveRoomMapper liveRoomMapper;
    private final SysSensitiveWordMapper sensitiveWordMapper;
    private final BizProductMapper productMapper;

    // ---------------- UGC 后台 ----------------
    @GetMapping("/ugc/posts")
    public Result<PageResult<BizUgcPost>> posts(@RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "20") long size,
                                                @RequestParam(required = false) Integer status,
                                                @RequestParam(required = false) String type) {
        return Result.ok(PageResult.of(postMapper.selectPage(new Page<>(page, size),
                Wrappers.<BizUgcPost>lambdaQuery()
                        .eq(status != null, BizUgcPost::getStatus, status)
                        .eq(type != null && !type.isBlank(), BizUgcPost::getType, type)
                        .orderByDesc(BizUgcPost::getId))));
    }

    @PostMapping("/ugc/posts/{id}/audit")
    @Audit(module = "社区", action = "UGC 审核")
    public Result<Void> audit(@PathVariable Long id, @RequestParam boolean approve,
                              @RequestParam(required = false) String remark) {
        BizUgcPost p = postMapper.selectById(id);
        if (p == null) throw BizException.notFound("内容不存在");
        p.setStatus(approve ? 1 : 2);
        p.setAuditRemark(remark);
        postMapper.updateById(p);
        return Result.ok();
    }

    @DeleteMapping("/ugc/posts/{id}")
    @Audit(module = "社区", action = "删除 UGC")
    public Result<Void> deletePost(@PathVariable Long id) {
        postMapper.deleteById(id);
        return Result.ok();
    }

    @GetMapping("/ugc/reports")
    public Result<List<Map<String, Object>>> reports() {
        List<BizUgcReport> list = reportMapper.selectList(Wrappers.<BizUgcReport>lambdaQuery()
                .orderByDesc(BizUgcReport::getId));
        return Result.ok(list.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("report", r);
            BizUgcPost p = postMapper.selectById(r.getPostId());
            m.put("postTitle", p == null ? "" : p.getTitle());
            return m;
        }).toList());
    }

    @PostMapping("/ugc/reports/{id}/handle")
    @Audit(module = "社区", action = "处理举报")
    public Result<Void> handleReport(@PathVariable Long id, @RequestParam boolean takeDown) {
        BizUgcReport r = reportMapper.selectById(id);
        if (r == null) throw BizException.notFound("举报不存在");
        r.setStatus(takeDown ? 2 : 1);
        reportMapper.updateById(r);
        if (takeDown) {
            BizUgcPost p = postMapper.selectById(r.getPostId());
            if (p != null) {
                p.setStatus(2);
                postMapper.updateById(p);
            }
        }
        return Result.ok();
    }

    // ---------------- 游客社区 ----------------
    @PostMapping("/content/ugc")
    @Audit(module = "社区", action = "发布 UGC")
    public Result<BizUgcPost> publish(@RequestBody Map<String, Object> body) {
        LoginUser u = SecurityUtils.current();
        if (!"MEMBER".equals(u.getUserType())) throw BizException.forbidden("仅游客会员可发布");
        String title = String.valueOf(body.getOrDefault("title", ""));
        String content = String.valueOf(body.getOrDefault("content", ""));
        boolean sensitive = isSensitive(title + content);
        BizUgcPost p = new BizUgcPost();
        p.setTenantId(TenantContext.getTenantId());
        p.setMemberId(u.getId());
        p.setType(String.valueOf(body.getOrDefault("type", "note")));
        p.setTitle(sensitive ? "[待审核内容]" : title);
        p.setContent(sensitive ? "[内容含敏感词，已提交人工审核]" : content);
        p.setImages(body.get("images") == null ? null : String.valueOf(body.get("images")));
        p.setVideoUrl(body.get("videoUrl") == null ? null : String.valueOf(body.get("videoUrl")));
        p.setProductId(body.get("productId") == null ? null : Long.valueOf(String.valueOf(body.get("productId"))));
        p.setDestinationId(body.get("destinationId") == null ? null : Long.valueOf(String.valueOf(body.get("destinationId"))));
        p.setStatus(0);
        p.setLikes(0);
        p.setComments(0);
        p.setFavorites(0);
        p.setCreatedAt(LocalDateTime.now());
        postMapper.insert(p);
        return Result.ok(p);
    }

    @PostMapping("/content/ugc/{id}/like")
    public Result<Map<String, Object>> like(@PathVariable Long id, @RequestParam(defaultValue = "like") String type) {
        LoginUser u = SecurityUtils.current();
        BizUgcLike exist = likeMapper.selectOne(Wrappers.<BizUgcLike>lambdaQuery()
                .eq(BizUgcLike::getPostId, id).eq(BizUgcLike::getMemberId, u.getId())
                .eq(BizUgcLike::getType, type).last("limit 1"));
        BizUgcPost p = postMapper.selectById(id);
        if (p == null) throw BizException.notFound("内容不存在");
        if (exist != null) {
            likeMapper.deleteById(exist.getId());
            if ("like".equals(type)) p.setLikes(Math.max(0, p.getLikes() - 1));
            else p.setFavorites(Math.max(0, p.getFavorites() - 1));
            postMapper.updateById(p);
            return Result.ok(Map.of("active", false));
        }
        BizUgcLike like = new BizUgcLike();
        like.setTenantId(TenantContext.getTenantId());
        like.setPostId(id);
        like.setMemberId(u.getId());
        like.setType(type);
        like.setCreatedAt(LocalDateTime.now());
        likeMapper.insert(like);
        if ("like".equals(type)) p.setLikes(p.getLikes() + 1);
        else p.setFavorites(p.getFavorites() + 1);
        postMapper.updateById(p);
        return Result.ok(Map.of("active", true));
    }

    @PostMapping("/content/ugc/{id}/comment")
    public Result<BizUgcComment> comment(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        LoginUser u = SecurityUtils.current();
        String content = String.valueOf(body.getOrDefault("content", ""));
        if (isSensitive(content)) throw new BizException("评论包含敏感词，请修改后重试");
        BizUgcComment c = new BizUgcComment();
        c.setTenantId(TenantContext.getTenantId());
        c.setPostId(id);
        c.setMemberId(u.getId());
        c.setContent(content);
        c.setStatus(1);
        c.setCreatedAt(LocalDateTime.now());
        commentMapper.insert(c);
        BizUgcPost p = postMapper.selectById(id);
        if (p != null) {
            p.setComments((p.getComments() == null ? 0 : p.getComments()) + 1);
            postMapper.updateById(p);
        }
        return Result.ok(c);
    }

    @PostMapping("/content/ugc/{id}/report")
    public Result<Void> report(@PathVariable Long id, @RequestParam String reason) {
        LoginUser u = SecurityUtils.current();
        BizUgcReport r = new BizUgcReport();
        r.setTenantId(TenantContext.getTenantId());
        r.setPostId(id);
        r.setMemberId(u.getId());
        r.setReason(reason);
        r.setStatus(0);
        r.setCreatedAt(LocalDateTime.now());
        reportMapper.insert(r);
        return Result.ok();
    }

    // ---------------- 定制游 ----------------
    @GetMapping("/custom/requests")
    public Result<List<Map<String, Object>>> customRequests(@RequestParam(required = false) Integer status) {
        List<BizCustomRequest> list = customRequestMapper.selectList(Wrappers.<BizCustomRequest>lambdaQuery()
                .eq(status != null, BizCustomRequest::getStatus, status).orderByDesc(BizCustomRequest::getId));
        return Result.ok(list.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("request", r);
            m.put("quotes", customQuoteMapper.selectList(Wrappers.<BizCustomQuote>lambdaQuery()
                    .eq(BizCustomQuote::getRequestId, r.getId())));
            return m;
        }).toList());
    }

    @PostMapping("/custom/requests/{id}/quote")
    @Audit(module = "定制游", action = "定制报价")
    public Result<BizCustomQuote> quote(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        BizCustomRequest req = customRequestMapper.selectById(id);
        if (req == null) throw BizException.notFound("定制需求不存在");
        BizCustomQuote q = new BizCustomQuote();
        q.setTenantId(TenantContext.getTenantId());
        q.setRequestId(id);
        q.setAmount(new java.math.BigDecimal(String.valueOf(body.getOrDefault("amount", "0"))));
        q.setContent(String.valueOf(body.getOrDefault("content", "")));
        q.setStatus(0);
        q.setCreatedAt(LocalDateTime.now());
        customQuoteMapper.insert(q);
        req.setStatus(1);
        customRequestMapper.updateById(req);
        return Result.ok(q);
    }

    @PostMapping("/custom/quotes/{id}/accept")
    @Audit(module = "定制游", action = "接受定制报价")
    public Result<Void> acceptQuote(@PathVariable Long id) {
        BizCustomQuote q = customQuoteMapper.selectById(id);
        if (q == null) throw BizException.notFound("报价不存在");
        q.setStatus(1);
        customQuoteMapper.updateById(q);
        BizCustomRequest req = customRequestMapper.selectById(q.getRequestId());
        if (req != null) {
            req.setStatus(2);
            customRequestMapper.updateById(req);
        }
        return Result.ok();
    }

    /** 行程规划器：按目的地/天数/预算生成草案 */
    @PostMapping("/content/planner")
    public Result<Map<String, Object>> planner(@RequestBody Map<String, Object> body) {
        String destination = String.valueOf(body.getOrDefault("destination", ""));
        int days = Integer.parseInt(String.valueOf(body.getOrDefault("days", "3")));
        List<BizProduct> related = productMapper.selectList(Wrappers.<BizProduct>lambdaQuery()
                .like(BizProduct::getDestination, destination).eq(BizProduct::getStatus, 1)
                .orderByAsc(BizProduct::getAdultPrice).last("limit 3"));
        List<Map<String, Object>> draft = new ArrayList<>();
        for (int i = 1; i <= days; i++) {
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("day", i);
            d.put("title", "第 " + i + " 天 · " + (destination.isBlank() ? "自由活动" : destination));
            d.put("suggestion", related.isEmpty() ? "推荐当地特色体验"
                    : related.get((i - 1) % related.size()).getTitle());
            draft.add(d);
        }
        return Result.ok(Map.of("destination", destination, "days", days, "draft", draft,
                "reference", related.stream().map(p -> Map.of("id", p.getId(), "title", p.getTitle(),
                        "price", p.getAdultPrice())).toList()));
    }

    // ---------------- 直播/海报 ----------------
    @GetMapping("/content/live")
    public Result<List<BizLiveRoom>> liveRooms() {
        return Result.ok(liveRoomMapper.selectList(Wrappers.<BizLiveRoom>lambdaQuery().orderByDesc(BizLiveRoom::getId)));
    }

    @PostMapping("/content/live")
    @Audit(module = "直播", action = "保存直播")
    public Result<Void> saveLive(@RequestBody BizLiveRoom l) {
        if (l.getTenantId() == null) l.setTenantId(TenantContext.getTenantId());
        if (l.getId() == null) liveRoomMapper.insert(l);
        else liveRoomMapper.updateById(l);
        return Result.ok();
    }

    @PostMapping("/content/poster")
    public Result<Map<String, Object>> poster(@RequestParam Long productId) {
        BizProduct p = productMapper.selectById(productId);
        if (p == null) throw BizException.notFound("线路不存在");
        String link = "/share/product/" + productId;
        return Result.ok(Map.of("productId", productId, "title", p.getTitle(), "cover", p.getCover() == null ? "" : p.getCover(),
                "link", link, "qrContent", link, "tip", "海报模板可后台配置，小程序/App 端生成图片分享"));
    }

    private boolean isSensitive(String text) {
        if (text == null) return false;
        for (SysSensitiveWord w : sensitiveWordMapper.selectList(Wrappers.<SysSensitiveWord>lambdaQuery())) {
            if (text.contains(w.getWord())) return true;
        }
        return false;
    }
}
