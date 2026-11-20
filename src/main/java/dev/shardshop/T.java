package dev.shardshop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.List;

/** Text helper: supports &-colors and &#RRGGBB hex, italics off. */
final class T {
    private static final LegacyComponentSerializer S =
            LegacyComponentSerializer.builder().character('&').hexColors().build();

    static final String BLUE = "&#00a2ff";

    private T() {}

    static Component c(String s) {
        return S.deserialize(s).decoration(TextDecoration.ITALIC, false);
    }

    static List<Component> lore(String... lines) {
        List<Component> out = new ArrayList<>();
        for (String l : lines) out.add(c(l));
        return out;
    }
}
