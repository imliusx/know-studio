package know.studio.arag.conversation.domain;

import know.studio.arag.conversation.api.ConversationMessage;

import java.util.List;

public interface ConversationSummaryPort {

    String summarize(String previousSummary, List<ConversationMessage> messages);
}
