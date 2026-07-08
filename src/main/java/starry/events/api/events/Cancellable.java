package starry.events.api.events;

public interface Cancellable {

    boolean isCancelled();

    void cancel();

}