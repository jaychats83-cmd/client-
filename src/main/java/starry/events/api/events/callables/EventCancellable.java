package starry.events.api.events.callables;

import lombok.Setter;
import starry.events.api.events.Cancellable;
import starry.events.api.events.Event;

public abstract class EventCancellable implements Event, Cancellable {

    @Setter
    private boolean cancelled;

    protected EventCancellable() {
    }


    @Override
    public boolean isCancelled() {
        return cancelled;
    }


    @Override
    public void cancel() {
        cancelled = true;
    }
}