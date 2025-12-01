package cn.cug.sxy.ai.domain.rag.service.orchestration;

import cn.cug.sxy.ai.domain.rag.model.entity.Query;
import cn.cug.sxy.ai.domain.rag.model.entity.Response;
import cn.cug.sxy.ai.domain.rag.model.plan.TaskExecutionContext;
import cn.cug.sxy.ai.domain.rag.model.plan.TaskNode;
import cn.cug.sxy.ai.domain.rag.model.plan.TaskPlan;
import cn.cug.sxy.ai.types.exception.TaskAbortException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * 负责执行 LLM 规划出的 TaskPlan（DAG），实现"图的遍历"。
 * 支持并行执行无依赖的任务，提升性能。
 */
@Slf4j
@Service
public class TaskOrchestrator {

    private final ToolRegistry toolRegistry;
    private final FallbackManager fallbackManager;
    private final TaskExecutionService taskExecutionService;
    private final Executor taskExecutor;

    @Value("${rag.orchestration.enable-parallel:true}")
    private boolean enableParallel;

    public TaskOrchestrator(ToolRegistry toolRegistry,
                           FallbackManager fallbackManager,
                           TaskExecutionService taskExecutionService,
                           @Qualifier("taskExecutionExecutor") Executor taskExecutor) {
        this.toolRegistry = toolRegistry;
        this.fallbackManager = fallbackManager;
        this.taskExecutionService = taskExecutionService;
        this.taskExecutor = taskExecutor;
    }

    /**
     * 执行任务计划。
     * 支持并行执行无依赖的任务，提升性能。
     *
     * @return 最终的 Response（如有），否则可能是综合后的字符串等。
     */
    public Object executePlan(TaskPlan plan, Query query, cn.cug.sxy.ai.domain.rag.model.strategy.QueryStrategy strategy) {
        if (plan == null || !plan.hasTasks()) {
            throw new IllegalArgumentException("TaskPlan 为空，无法执行");
        }
        TaskExecutionContext context = new TaskExecutionContext(query);
        context.put("strategy", strategy);
        
        // 构建依赖图
        Map<Integer, TaskNode> nodeMap = new HashMap<>();
        Map<Integer, Integer> indegree = new HashMap<>();
        Map<Integer, List<Integer>> graph = new HashMap<>();
        Map<Integer, Object> stepResults = new ConcurrentHashMap<>();
        
        for (TaskNode node : plan.getTasks()) {
            int step = node.getStep();
            nodeMap.put(step, node);
            indegree.putIfAbsent(step, 0);
        }
        for (TaskNode node : plan.getTasks()) {
            int step = node.getStep();
            for (Integer dep : node.safeDependencies()) {
                if (!nodeMap.containsKey(dep)) {
                    continue;
                }
                graph.computeIfAbsent(dep, k -> new ArrayList<>()).add(step);
                indegree.put(step, indegree.getOrDefault(step, 0) + 1);
            }
        }
        
        Queue<Integer> readyQueue = new ArrayDeque<>();
        indegree.forEach((k, v) -> {
            if (v == 0) {
                readyQueue.add(k);
            }
        });
        
        Object lastResult = null;
        
        // 并行执行循环
        while (!readyQueue.isEmpty() || !stepResults.isEmpty()) {
            // 收集所有可执行的任务（入度为0）
            List<Integer> readySteps = new ArrayList<>();
            while (!readyQueue.isEmpty()) {
                readySteps.add(readyQueue.poll());
            }
            
            if (readySteps.isEmpty()) {
                break;
            }
            
            if (enableParallel && readySteps.size() > 1) {
                // 并行执行所有可执行任务
                log.info("并行执行 {} 个任务: {}", readySteps.size(), readySteps);
                List<CompletableFuture<StepResult>> futures = readySteps.stream()
                        .map(step -> CompletableFuture.supplyAsync(() -> {
                            TaskNode node = nodeMap.get(step);
                            if (node == null) {
                                return new StepResult(step, null, null);
                            }
                            try {
                                RagTool tool = toolRegistry.getTool(node.getToolName());
                                Object result = executeNode(tool, node, context);
                                return new StepResult(step, result, null);
                            } catch (TaskAbortException tae) {
                                log.warn("任务在节点 {} 中止: {}", step, tae.getMessage());
                                return new StepResult(step, null, tae);
                            } catch (Exception ex) {
                                log.error("任务节点 {} 执行异常", step, ex);
                                return new StepResult(step, null, ex);
                            }
                        }, taskExecutor))
                        .collect(Collectors.toList());
                
                // 等待所有任务完成
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                
                // 处理完成的任务
                for (CompletableFuture<StepResult> future : futures) {
                    try {
                        StepResult stepResult = future.get();
                        int step = stepResult.step;
                        Object result = stepResult.result;
                        Exception error = stepResult.error;
                        
                        if (error instanceof TaskAbortException) {
                            log.warn("任务计划在节点 {} 中止", step);
                            break;
                        }
                        
                        if (error != null) {
                            log.error("任务节点 {} 执行失败，继续执行其他节点", step);
                            continue;
                        }
                        
                        stepResults.put(step, result);
                        lastResult = result;
                        
                        // 更新下游节点的入度
                        for (Integer next : graph.getOrDefault(step, Collections.emptyList())) {
                            int d = indegree.get(next) - 1;
                            indegree.put(next, d);
                            if (d == 0) {
                                readyQueue.add(next);
                            }
                        }
                    } catch (Exception e) {
                        log.error("获取任务结果失败", e);
                    }
                }
            } else {
                // 串行执行（单个任务或禁用并行）
                for (Integer step : readySteps) {
                    TaskNode node = nodeMap.get(step);
                    if (node == null) {
                        continue;
                    }
                    try {
                        RagTool tool = toolRegistry.getTool(node.getToolName());
                        lastResult = executeNode(tool, node, context);
                        stepResults.put(step, lastResult);
                        
                        // 更新下游节点的入度
                        for (Integer next : graph.getOrDefault(step, Collections.emptyList())) {
                            int d = indegree.get(next) - 1;
                            indegree.put(next, d);
                            if (d == 0) {
                                readyQueue.add(next);
                            }
                        }
                    } catch (TaskAbortException tae) {
                        log.warn("任务在节点 {} 中止: {}", step, tae.getMessage());
                        break;
                    }
                }
            }
        }
        
        // 如果最后的结果本身就是 Response，直接返回
        if (lastResult instanceof Response response) {
            return response;
        }

        // 如果最后的结果是字符串，包装成一个 Response 对象返回
        if (lastResult instanceof String finalAnswer) {
            Response response = new Response();
            response.setQueryId(query.getId());
            response.setSessionId(query.getSessionId());
            response.setStatus("COMPLETED");
            response.setAnswerText(finalAnswer);
            response.setCreateTime(java.time.LocalDateTime.now());
            response.setCompleteTime(java.time.LocalDateTime.now());
            return response;
        }

        // 兜底：返回最后一个结果的字符串表示形式
        return String.valueOf(lastResult);
    }
    
    /**
     * 任务执行结果。
     */
    private record StepResult(int step, Object result, Exception error) {}

    private Object executeNode(RagTool tool, TaskNode node, TaskExecutionContext context) {
        // 如果工具名称为 "AUTO" 或找不到对应的 RagTool，使用 ChatClient 自动执行
        if ("AUTO".equals(node.getToolName()) || tool == null) {
            log.info("使用 ChatClient 自动执行任务节点: step={}, description={}", 
                    node.getStep(), node.getDescription());
            context.incrementAttempt("AUTO");
            try {
                return taskExecutionService.executeTaskNode(node, context);
            } catch (Exception ex) {
                log.error("ChatClient 自动执行失败: step={}, error={}", node.getStep(), ex.getMessage(), ex);
                // 对于 AUTO 执行失败，尝试使用回退策略
                if (tool != null) {
                    // 如果有备用的 RagTool，可以尝试使用
                    log.info("尝试使用备用工具: {}", tool.getName());
                    return executeWithTool(tool, node, context);
                }
                FallbackDecision decision = fallbackManager.handleToolFailure(
                        node.getToolName(), ex, context);
                return handleDecision(decision, node, context);
            }
        }
        
        // 使用传统的 RagTool 执行
        return executeWithTool(tool, node, context);
    }

    private Object executeWithTool(RagTool tool, TaskNode node, TaskExecutionContext context) {
        context.incrementAttempt(tool.getName());
        try {
            log.info("执行 TaskNode step={}, tool={}", node.getStep(), tool.getName());
            Object result = tool.execute(context, node);
            if (result == null) {
                FallbackDecision decision = fallbackManager.handleEmptyResult(tool.getName(), context);
                return handleDecision(decision, node, context);
            }
            return result;
        } catch (Exception ex) {
            FallbackDecision decision = fallbackManager.handleToolFailure(tool.getName(), ex, context);
            return handleDecision(decision, node, context);
        }
    }

    private Object handleDecision(FallbackDecision decision, TaskNode current, TaskExecutionContext context) {
        if (decision == null) {
            throw new TaskAbortException("无可用回退策略");
        }
        switch (decision.getAction()) {
            case RETRY_SAME_TOOL -> {
                return executeNode(toolRegistry.getTool(current.getToolName()), current, context);
            }
            case SWITCH_TOOL -> {
                String nextTool = decision.getNextToolName();
                if (nextTool == null) {
                    throw new TaskAbortException("回退策略未指定工具");
                }
                TaskNode fallbackNode = TaskNode.builder()
                        .step(current.getStep())
                        .toolName(nextTool)
                        .description("fallback-from-" + current.getToolName())
                        .dependencies(current.safeDependencies())
                        .build();
                return executeNode(toolRegistry.getTool(nextTool), fallbackNode, context);
            }
            case CLARIFY -> {
                // 第二级：任务级澄清，返回澄清消息
                Response response = new Response();
                response.setQueryId(context.getQuery().getId());
                response.setSessionId(context.getQuery().getSessionId());
                response.setStatus("CLARIFY");
                response.setAnswerText(decision.getMessage());
                response.setCreateTime(java.time.LocalDateTime.now());
                response.setCompleteTime(java.time.LocalDateTime.now());
                return response;
            }
            case ABORT -> throw new TaskAbortException(decision.getMessage());
            default -> throw new TaskAbortException("未知回退动作");
        }
    }
}


