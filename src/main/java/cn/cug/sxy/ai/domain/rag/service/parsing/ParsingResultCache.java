package cn.cug.sxy.ai.domain.rag.service.parsing;

import cn.cug.sxy.ai.domain.rag.model.parsing.StructuredDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 解析结果缓存服务。
 * <p>
 * 核心功能：
 * 1. 缓存每页的解析结果（按文档hash存储）
 * 2. 记录失败页面，支持重试
 * 3. 合并成功页面和重试页面的结果
 * <p>
 * 避免重复解析已成功的页面，节省 token 和时间。
 *
 * @author jerryhotton
 */
@Slf4j
@Component
public class ParsingResultCache {

    // 缓存：文档hash -> 解析状态
    private final ConcurrentHashMap<String, DocumentParsingState> cache = new ConcurrentHashMap<>();

    // 缓存过期时间（毫秒），默认24小时
    private static final long CACHE_EXPIRE_MS = 24 * 60 * 60 * 1000L;

    /**
     * 生成文档的唯一标识（基于文件路径和修改时间）。
     */
    public String generateDocumentKey(String filePath, long fileSize, long lastModified) {
        try {
            String input = filePath + "|" + fileSize + "|" + lastModified;
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            // 降级：使用简单拼接
            return filePath + "_" + fileSize + "_" + lastModified;
        }
    }

    /**
     * 创建或获取文档解析状态。
     */
    public DocumentParsingState getOrCreateState(String documentKey, int totalPages) {
        return cache.compute(documentKey, (key, existingState) -> {
            if (existingState != null && !existingState.isExpired()) {
                // 检查总页数是否一致
                if (existingState.getTotalPages() == totalPages) {
                    log.debug("找到有效的解析缓存: key={}, 成功页数={}, 失败页数={}",
                            key, existingState.getSuccessfulPages().size(),
                            existingState.getFailedPages().size());
                    return existingState;
                } else {
                    log.info("文档页数变化，清除旧缓存: key={}, 旧页数={}, 新页数={}",
                            key, existingState.getTotalPages(), totalPages);
                }
            }
            // 创建新状态
            return DocumentParsingState.builder()
                    .documentKey(key)
                    .totalPages(totalPages)
                    .successfulPages(new ConcurrentHashMap<>())
                    .failedPages(new ConcurrentHashMap<>())
                    .createdAt(LocalDateTime.now())
                    .lastUpdatedAt(LocalDateTime.now())
                    .build();
        });
    }

    /**
     * 获取现有的解析状态（不创建新的）。
     */
    public Optional<DocumentParsingState> getState(String documentKey) {
        DocumentParsingState state = cache.get(documentKey);
        if (state != null && !state.isExpired()) {
            return Optional.of(state);
        }
        return Optional.empty();
    }

    /**
     * 记录页面解析成功。
     */
    public void recordPageSuccess(String documentKey, int pageNo, StructuredDocument.Page page) {
        DocumentParsingState state = cache.get(documentKey);
        if (state != null) {
            state.getSuccessfulPages().put(pageNo, page);
            state.getFailedPages().remove(pageNo); // 移除失败记录（如果有）
            state.setLastUpdatedAt(LocalDateTime.now());
            log.debug("记录页面解析成功: doc={}, page={}", documentKey.substring(0, 8), pageNo);
        }
    }

    /**
     * 记录页面解析失败。
     */
    public void recordPageFailure(String documentKey, int pageNo, String errorMessage) {
        DocumentParsingState state = cache.get(documentKey);
        if (state != null) {
            state.getFailedPages().put(pageNo, PageFailureInfo.builder()
                    .pageNo(pageNo)
                    .errorMessage(errorMessage)
                    .failedAt(LocalDateTime.now())
                    .retryCount(state.getFailedPages().containsKey(pageNo) ?
                            state.getFailedPages().get(pageNo).getRetryCount() + 1 : 0)
                    .build());
            state.setLastUpdatedAt(LocalDateTime.now());
            log.debug("记录页面解析失败: doc={}, page={}, error={}",
                    documentKey.substring(0, 8), pageNo, errorMessage);
        }
    }

    /**
     * 获取需要重试的页面列表。
     */
    public List<Integer> getFailedPageNumbers(String documentKey) {
        DocumentParsingState state = cache.get(documentKey);
        if (state != null) {
            return new ArrayList<>(state.getFailedPages().keySet());
        }
        return Collections.emptyList();
    }

    /**
     * 获取需要处理的页面列表（排除已成功的）。
     */
    public List<Integer> getPendingPageNumbers(String documentKey, int totalPages) {
        DocumentParsingState state = cache.get(documentKey);
        if (state == null) {
            // 没有缓存，所有页面都需要处理
            List<Integer> allPages = new ArrayList<>();
            for (int i = 1; i <= totalPages; i++) {
                allPages.add(i);
            }
            return allPages;
        }

        // 返回未成功的页面
        List<Integer> pendingPages = new ArrayList<>();
        for (int i = 1; i <= totalPages; i++) {
            if (!state.getSuccessfulPages().containsKey(i)) {
                pendingPages.add(i);
            }
        }
        return pendingPages;
    }

    /**
     * 检查是否有缓存的成功结果。
     */
    public boolean hasSuccessfulPages(String documentKey) {
        DocumentParsingState state = cache.get(documentKey);
        return state != null && !state.getSuccessfulPages().isEmpty();
    }

    /**
     * 获取所有成功的页面（按页码排序）。
     */
    public List<StructuredDocument.Page> getSuccessfulPages(String documentKey) {
        DocumentParsingState state = cache.get(documentKey);
        if (state == null) {
            return Collections.emptyList();
        }

        return state.getSuccessfulPages().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .toList();
    }

    /**
     * 合并缓存的成功页面和新解析的页面。
     */
    public List<StructuredDocument.Page> mergePages(String documentKey,
                                                     Map<Integer, StructuredDocument.Page> newPages) {
        DocumentParsingState state = cache.get(documentKey);
        if (state == null) {
            return new ArrayList<>(newPages.values());
        }

        // 合并：新解析的页面优先
        Map<Integer, StructuredDocument.Page> merged = new TreeMap<>();
        merged.putAll(state.getSuccessfulPages());
        merged.putAll(newPages);

        return new ArrayList<>(merged.values());
    }

    /**
     * 检查解析是否完成（所有页面都成功）。
     */
    public boolean isParsingComplete(String documentKey) {
        DocumentParsingState state = cache.get(documentKey);
        if (state == null) {
            return false;
        }
        return state.getSuccessfulPages().size() == state.getTotalPages();
    }

    /**
     * 获取解析进度摘要。
     */
    public String getProgressSummary(String documentKey) {
        DocumentParsingState state = cache.get(documentKey);
        if (state == null) {
            return "无缓存";
        }
        return String.format("成功: %d/%d 页, 失败: %d 页",
                state.getSuccessfulPages().size(),
                state.getTotalPages(),
                state.getFailedPages().size());
    }

    /**
     * 清除指定文档的缓存。
     */
    public void clearCache(String documentKey) {
        cache.remove(documentKey);
        log.debug("清除文档缓存: key={}", documentKey);
    }

    /**
     * 清除所有过期缓存。
     */
    public int cleanupExpiredCache() {
        int removed = 0;
        Iterator<Map.Entry<String, DocumentParsingState>> iterator = cache.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, DocumentParsingState> entry = iterator.next();
            if (entry.getValue().isExpired()) {
                iterator.remove();
                removed++;
            }
        }
        if (removed > 0) {
            log.info("清理过期缓存: {} 个", removed);
        }
        return removed;
    }

    /**
     * 获取缓存统计信息。
     */
    public CacheStats getStats() {
        int totalDocs = cache.size();
        int completeDocs = 0;
        int partialDocs = 0;
        int totalSuccessPages = 0;
        int totalFailedPages = 0;

        for (DocumentParsingState state : cache.values()) {
            if (state.isExpired()) continue;

            if (state.getSuccessfulPages().size() == state.getTotalPages()) {
                completeDocs++;
            } else if (!state.getSuccessfulPages().isEmpty()) {
                partialDocs++;
            }
            totalSuccessPages += state.getSuccessfulPages().size();
            totalFailedPages += state.getFailedPages().size();
        }

        return CacheStats.builder()
                .totalDocuments(totalDocs)
                .completeDocuments(completeDocs)
                .partialDocuments(partialDocs)
                .totalSuccessPages(totalSuccessPages)
                .totalFailedPages(totalFailedPages)
                .build();
    }

    // ==================== 数据类 ====================

    /**
     * 文档解析状态
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentParsingState {
        private String documentKey;
        private int totalPages;
        private ConcurrentHashMap<Integer, StructuredDocument.Page> successfulPages;
        private ConcurrentHashMap<Integer, PageFailureInfo> failedPages;
        private LocalDateTime createdAt;
        private LocalDateTime lastUpdatedAt;

        public boolean isExpired() {
            if (lastUpdatedAt == null) return true;
            long ageMs = java.time.Duration.between(lastUpdatedAt, LocalDateTime.now()).toMillis();
            return ageMs > CACHE_EXPIRE_MS;
        }

        public boolean hasFailedPages() {
            return failedPages != null && !failedPages.isEmpty();
        }

        public int getSuccessCount() {
            return successfulPages != null ? successfulPages.size() : 0;
        }

        public int getFailedCount() {
            return failedPages != null ? failedPages.size() : 0;
        }
    }

    /**
     * 页面失败信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageFailureInfo {
        private int pageNo;
        private String errorMessage;
        private LocalDateTime failedAt;
        private int retryCount;
    }

    /**
     * 缓存统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CacheStats {
        private int totalDocuments;
        private int completeDocuments;
        private int partialDocuments;
        private int totalSuccessPages;
        private int totalFailedPages;
    }
}

