package know.studio.arag.agent.domain;

import know.studio.arag.agent.api.IntentResult;

public interface IntentClassifier {

    IntentResult classify(String message, boolean toolMode);
}
