package mariculture.platform;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import mariculture.domain.fish.FishText;

/** Turns domain text lines into chat components. */
public final class Texts {
    private Texts() {}

    public static Component line(FishText.Line line) {
        Object[] args = new Object[line.args().length];
        for (int i = 0; i < args.length; i++) {
            Object a = line.args()[i];
            args[i] = a instanceof String s && s.startsWith("lang:") ? Component.translatable(s.substring(5)) : a;
        }
        return Component.translatable(line.key(), args).withStyle(ChatFormatting.GRAY);
    }
}
