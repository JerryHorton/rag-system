package cn.cug.sxy.ai.domain.rag.service.parsing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 动态超时时间计算器。
 * <p>
 * 核心功能：
 * 1. 实时统计每页的解析时间
 * 2. 使用滑动窗口计算平均耗时
 * 3. 根据网络状况动态调整超时时间
 * <p>
 * 算法：超时时间 = 平均耗时 × 倍数因子 + 安全余量
 *
 * @author jerryhotton
 */
@Slf4j
@Component
public class DynamicTimeoutCalculator {

    // 滑动窗口大小（保留最近N次记录）
    private static final int WINDOW_SIZE = 20;

    // 默认超时时间（毫秒）
    private static final long DEFAULT_TIMEOUT_MS = 60_000L;

    // 最小超时时间（毫秒）
    private static final long MIN_TIMEOUT_MS = 15_000L;

    // 最大超时时间（毫秒）
    private static final long MAX_TIMEOUT_MS = 300_000L;

    // 超时倍数因子（平均耗时的多少倍）
    private static final double TIMEOUT_MULTIPLIER = 2.5;

    // 安全余量（毫秒）
    private static final long SAFETY_MARGIN_MS = 5_000L;

    // 滑动窗口：存储最近的页面处理耗时
    private final ConcurrentLinkedDeque<Long> recentDurations = new ConcurrentLinkedDeque<>();

    // 统计数据
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong totalDuration = new AtomicLong(0);
    private final AtomicLong minDuration = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxDuration = new AtomicLong(0);

    /**
     * 记录一次页面处理耗时。
     *
     * @param durationMs 处理耗时（毫秒）
     */
    public void recordDuration(long durationMs) {
        if (durationMs <= 0) return;

        // 添加到滑动窗口
        recentDurations.addLast(durationMs);

        // 保持窗口大小
        while (recentDurations.size() > WINDOW_SIZE) {
            recentDurations.pollFirst();
        }

        // 更新统计
        totalRequests.incrementAndGet();
        totalDuration.addAndGet(durationMs);
        minDuration.updateAndGet(current -> Math.min(current, durationMs));
        maxDuration.updateAndGet(current -> Math.max(current, durationMs));

        log.trace("记录页面耗时: {}ms, 当前窗口大小: {}", durationMs, recentDurations.size());
    }

    /**
     * 计算当前推荐的超时时间。
     *
     * @return 推荐的超时时间（毫秒）
     */
    public long calculateTimeout() {
        if (recentDurations.isEmpty()) {
            log.debug("无历史数据，使用默认超时: {}ms", DEFAULT_TIMEOUT_MS);
            return DEFAULT_TIMEOUT_MS;
        }

        // 计算平均值和标准差
        double[] stats = calculateStats();
        double average = stats[0];
        double stdDev = stats[1];

        // 超时时间 = 平均值 × 倍数 + 2倍标准差 + 安全余量
        // 这样可以覆盖大部分正常情况，同时适应网络波动
        long calculatedTimeout = (long) (average * TIMEOUT_MULTIPLIER + 2 * stdDev + SAFETY_MARGIN_MS);

        // 限制在合理范围内
        long finalTimeout = Math.max(MIN_TIMEOUT_MS, Math.min(MAX_TIMEOUT_MS, calculatedTimeout));

        log.debug("动态超时计算: 平均={}ms, 标准差={}ms, 计算值={}ms, 最终={}ms",
                (long) average, (long) stdDev, calculatedTimeout, finalTimeout);

        return finalTimeout;
    }

    /**
     * 计算当前推荐的超时时间（秒）。
     */
    public int calculateTimeoutSeconds() {
        return (int) Math.ceil(calculateTimeout() / 1000.0);
    }

    /**
     * 获取滑动窗口的平均耗时（毫秒）。
     */
    public long getAverageDuration() {
        if (recentDurations.isEmpty()) {
            return DEFAULT_TIMEOUT_MS;
        }
        return (long) calculateStats()[0];
    }

    /**
     * 获取滑动窗口的最大耗时（毫秒）。
     */
    public long getWindowMaxDuration() {
        if (recentDurations.isEmpty()) {
            return DEFAULT_TIMEOUT_MS;
        }
        return recentDurations.stream().mapToLong(Long::longValue).max().orElse(DEFAULT_TIMEOUT_MS);
    }

    /**
     * 判断当前是否处于"慢速"状态（可能网络拥堵）。
     */
    public boolean isSlowMode() {
        if (recentDurations.size() < 3) {
            return false;
        }
        // 如果最近平均耗时超过默认值的1.5倍，认为是慢速模式
        return getAverageDuration() > DEFAULT_TIMEOUT_MS * 1.5;
    }

    /**
     * 获取网络状况描述。
     */
    public String getNetworkStatusDescription() {
        if (recentDurations.isEmpty()) {
            return "未知（无历史数据）";
        }

        long avg = getAverageDuration();
        if (avg < 10_000) {
            return "极佳（<10s）";
        } else if (avg < 30_000) {
            return "良好（10-30s）";
        } else if (avg < 60_000) {
            return "一般（30-60s）";
        } else if (avg < 120_000) {
            return "较慢（60-120s）";
        } else {
            return "缓慢（>120s）";
        }
    }

    /**
     * 获取统计摘要。
     */
    public String getStatsSummary() {
        if (totalRequests.get() == 0) {
            return "无数据";
        }

        long avgTotal = totalDuration.get() / totalRequests.get();
        double[] windowStats = calculateStats();

        return String.format(
                "总请求: %d, 总平均: %dms, 窗口平均: %dms, 窗口标准差: %dms, " +
                        "历史最小: %dms, 历史最大: %dms, 当前推荐超时: %dms, 网络状况: %s",
                totalRequests.get(),
                avgTotal,
                (long) windowStats[0],
                (long) windowStats[1],
                minDuration.get() == Long.MAX_VALUE ? 0 : minDuration.get(),
                maxDuration.get(),
                calculateTimeout(),
                getNetworkStatusDescription()
        );
    }

    /**
     * 重置统计数据（用于新文档开始解析时）。
     */
    public void resetWindowStats() {
        recentDurations.clear();
        log.debug("滑动窗口已重置");
    }

    /**
     * 完全重置所有统计数据。
     */
    public void resetAllStats() {
        recentDurations.clear();
        totalRequests.set(0);
        totalDuration.set(0);
        minDuration.set(Long.MAX_VALUE);
        maxDuration.set(0);
        log.info("所有统计数据已重置");
    }

    /**
     * 计算统计数据（平均值和标准差）。
     */
    private double[] calculateStats() {
        if (recentDurations.isEmpty()) {
            return new double[]{DEFAULT_TIMEOUT_MS, 0};
        }

        // 转为数组便于计算
        long[] durations = recentDurations.stream().mapToLong(Long::longValue).toArray();
        int n = durations.length;

        // 计算平均值
        double sum = 0;
        for (long d : durations) {
            sum += d;
        }
        double average = sum / n;

        // 计算标准差
        double varianceSum = 0;
        for (long d : durations) {
            varianceSum += Math.pow(d - average, 2);
        }
        double stdDev = Math.sqrt(varianceSum / n);

        return new double[]{average, stdDev};
    }

    /**
     * 根据页面数量估算总处理时间。
     *
     * @param pageCount   页面数量
     * @param parallelism 并行度
     * @return 估算的总时间（毫秒）
     */
    public long estimateTotalTime(int pageCount, int parallelism) {
        long avgPerPage = getAverageDuration();
        int batches = (int) Math.ceil((double) pageCount / parallelism);
        long estimated = avgPerPage * batches;

        // 加上一些额外时间用于合并和后处理
        return estimated + 10_000L;
    }

    /**
     * 获取建议的并行度（基于当前网络状况）。
     */
    public int getSuggestedParallelism(int defaultParallelism) {
        if (recentDurations.isEmpty()) {
            return defaultParallelism;
        }

        // 如果网络慢，降低并行度避免超时
        if (isSlowMode()) {
            return Math.max(1, defaultParallelism / 2);
        }

        // 如果网络快，可以适当提高并行度
        long avg = getAverageDuration();
        if (avg < 15_000) {
            return Math.min(defaultParallelism * 2, 8);
        }

        return defaultParallelism;
    }
}

