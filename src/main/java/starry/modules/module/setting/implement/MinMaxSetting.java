package starry.modules.module.setting.implement;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import starry.modules.module.setting.Setting;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

@Getter
@Setter
@Accessors(chain = true)
public class MinMaxSetting extends Setting {
    private float minValue, maxValue, min, max;
    private boolean integer;

    public MinMaxSetting(String name, String description) {
        super(name, description);
    }

    public MinMaxSetting range(float min, float max) {
        this.min = min;
        this.max = max;
        return this;
    }

    public MinMaxSetting range(int min, int max) {
        this.min = min;
        this.max = max;
        this.integer = true;
        return this;
    }

    public MinMaxSetting defaultValue(float minValue, float maxValue) {
        this.minValue = minValue;
        this.maxValue = maxValue;
        return this;
    }

    public MinMaxSetting defaultValue(int minValue, int maxValue) {
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.integer = true;
        return this;
    }

    public int getIntMin() {
        return (int) minValue;
    }

    public int getIntMax() {
        return (int) maxValue;
    }

    public float getRandomValue() {
        if (integer) {
            int min = getIntMin();
            int max = getIntMax();
            return min + ThreadLocalRandom.current().nextInt(Math.max(1, max - min + 1));
        }
        float min = minValue;
        float max = maxValue;
        return min + ThreadLocalRandom.current().nextFloat() * (max - min);
    }

    public int getRandomValueInt() {
        return (int) getRandomValue();
    }

    public MinMaxSetting visible(Supplier<Boolean> visible) {
        setVisible(visible);
        return this;
    }
}
