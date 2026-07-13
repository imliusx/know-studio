package know.studio.agent.domain;

import know.studio.agent.api.IntentResult;

public interface IntentClassifier {

    IntentResult classify(String message, boolean toolMode);
}
