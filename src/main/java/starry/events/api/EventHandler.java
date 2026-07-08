package starry.events.api;

import starry.events.api.types.Priority;

import java.lang.annotation.*;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventHandler {
    byte value() default Priority.MEDIUM;
}