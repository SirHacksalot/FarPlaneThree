package net.daporkchop.fp2.mc.util;

import lombok.NonNull;
import net.daporkchop.fp2.core.util.I18n;
import net.minecraft.locale.Language;

import java.util.Locale;

public class I18n1_21 implements I18n {
    @Override
    public boolean hasKey(@NonNull String key) {
        return Language.getInstance().has(key);
    }

    @Override
    public String format(@NonNull String key) {
        return Language.getInstance().getOrDefault(key);
    }

    @Override
    public String format(@NonNull String key, @NonNull Object... args) {
        return String.format(Language.getInstance().getOrDefault(key), args);
    }

    @Override
    public Locale javaLocale() {
        return Locale.ROOT;
    }
}
