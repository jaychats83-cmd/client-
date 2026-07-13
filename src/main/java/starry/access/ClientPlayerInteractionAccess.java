package starry.access;

/** Safe runtime access to block-breaking state implemented by the interaction-manager mixin. */
public interface ClientPlayerInteractionAccess {
    float starry$getBreakingProgress();
    void starry$setBreakingProgress(float progress);
    boolean starry$isBreakingBlock();
}
