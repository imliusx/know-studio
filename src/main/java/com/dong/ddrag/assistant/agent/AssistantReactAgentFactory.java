package com.dong.ddrag.assistant.agent;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.dong.ddrag.assistant.memory.AssistantShortTermMemoryHook;
import com.dong.ddrag.assistant.model.enums.AssistantToolMode;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * ReactAgent 构造工厂（对应走读指南「链路 C」）——把模型、记忆 Hook、工具组装成一个可推理的 Agent。
 *
 * <p>关键装配点：
 * <ul>
 *   <li>挂 {@link AssistantShortTermMemoryHook}：每次调模型前重组上下文（短期记忆）</li>
 *   <li>{@code recursionLimit=10}：控制模型"思考→调工具→再思考"的最大循环轮数，防死循环烧钱</li>
 *   <li>按 toolMode 决定是否注册 {@link AssistantKnowledgeBaseTool}，并把 groupId/resultHolder 塞进 ToolContext
 *       ——工具从 context 取 groupId 而非 prompt，防越权（详见该工具类注释）</li>
 * </ul>
 */
@Component
public class AssistantReactAgentFactory {

    // KB_SEARCH 至少需要经历“模型决定调工具 -> 工具执行 -> 模型基于工具结果生成最终回答”，
    // 递归上限过小会导致图在最终回答前被截断，返回空 AssistantMessage。
    private static final int AGENT_RECURSION_LIMIT = 10;

    private final ChatModel chatModel;
    private final AssistantShortTermMemoryHook assistantShortTermMemoryHook;
    private final AssistantKnowledgeBaseTool assistantKnowledgeBaseTool;

    public AssistantReactAgentFactory(
            ChatModel chatModel,
            AssistantShortTermMemoryHook assistantShortTermMemoryHook,
            AssistantKnowledgeBaseTool assistantKnowledgeBaseTool
    ) {
        this.chatModel = chatModel;
        this.assistantShortTermMemoryHook = assistantShortTermMemoryHook;
        this.assistantKnowledgeBaseTool = assistantKnowledgeBaseTool;
    }

    public ReactAgent createAgent(
            String instruction,
            AssistantToolMode toolMode,
            Long groupId,
            AssistantKnowledgeBaseToolResultHolder resultHolder
    ) {
        // 当前“仅对话模式”仍复用 ReactAgent 运行时：
        // system prompt 与短期上下文保留，长期记忆工具已整体移除。
        com.alibaba.cloud.ai.graph.agent.Builder builder = ReactAgent.builder()
                .name("assistant_chat_agent")
                .model(chatModel)
                .instruction(instruction)
                .hooks(assistantShortTermMemoryHook)
                .compileConfig(CompileConfig.builder()
                        .recursionLimit(AGENT_RECURSION_LIMIT)
                        .build())
                // MemorySaver 只负责图运行态的 checkpoint，不是正式业务记忆的事实源。
                .saver(new MemorySaver());
        if (toolMode == AssistantToolMode.KB_SEARCH) {
            builder.methodTools(assistantKnowledgeBaseTool)
                    .toolContext(Map.of(
                            AssistantKnowledgeBaseTool.GROUP_ID_CONTEXT_KEY, groupId,
                            AssistantKnowledgeBaseTool.RESULT_HOLDER_CONTEXT_KEY, resultHolder
                    ));
        }
        return builder.build();
    }
}
