package com.dong.ddrag.assistant.service;

import com.dong.ddrag.assistant.model.vo.chat.AssistantChatStreamEvent;

/**
 * SSE 事件发射器（对应走读指南「链路 C」流式入口）。
 * Agent 推理过程中产生的事件通过它推送出去，由 Controller 侧转成 SSE 的 4 种事件
 * （start / delta 增量文本 / done 完成 / error）。把"事件源"和"SSE 传输"解耦。
 */
public interface AssistantStreamEventEmitter {

    void emit(AssistantChatStreamEvent event);
}
