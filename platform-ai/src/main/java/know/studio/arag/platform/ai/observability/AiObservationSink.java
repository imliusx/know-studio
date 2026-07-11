package know.studio.arag.platform.ai.observability;

@FunctionalInterface
public interface AiObservationSink {

    AiObservationSink NOOP = observation -> { };

    void record(AiObservation observation);
}
