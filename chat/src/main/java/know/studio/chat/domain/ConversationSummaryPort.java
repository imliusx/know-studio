package know.studio.chat.domain;

import know.studio.chat.api.ConversationMessage;

import java.util.List;

public interface ConversationSummaryPort {

    String summarize(String previousSummary, List<ConversationMessage> messages);
}
