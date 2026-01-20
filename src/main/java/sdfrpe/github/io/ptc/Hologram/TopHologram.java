package sdfrpe.github.io.ptc.Hologram;

import org.holoeasy.HoloEasy;
import org.holoeasy.hologram.Hologram;
import org.holoeasy.line.TextLine;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;

import java.util.ArrayList;
import java.util.List;

public class TopHologram extends Hologram {
    private final String hologramId;
    private final TopType type;
    private final TopDesignConfig designConfig;
    private final List<TextLine> textLines;

    public TopHologram(@NotNull HoloEasy lib, @NotNull String id, @NotNull TopType type, @NotNull Location location, @NotNull TopDesignConfig designConfig, List<TopEntry> entries) {
        super(lib, location);
        this.hologramId = id;
        this.type = type;
        this.designConfig = designConfig;
        this.textLines = new ArrayList<>();

        initializeLines(entries);
    }

    private void initializeLines(List<TopEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            TextLine emptyLine = textLine(player -> ChatColor.GRAY + "Sin datos disponibles");
            textLines.add(emptyLine);
            LogSystem.debug(LogCategory.CORE, "No entries to display for:", hologramId);
            return;
        }

        TopDesignConfig.TopDesign design = designConfig.getDesign(type);
        double yOffset = 0.0;

        TextLine titleLine = textLine(player ->
                ChatColor.translateAlternateColorCodes('&', design.getTitle())
        ).yOffset(yOffset);
        textLines.add(titleLine);
        yOffset -= design.getSpacing();

        for (TopEntry entry : entries) {
            final String lineText = formatEntry(entry, design);
            TextLine entryLine = textLine(player -> lineText).yOffset(yOffset);
            textLines.add(entryLine);
            yOffset -= design.getSpacing();
        }

        LogSystem.debug(LogCategory.CORE, "Hologram initialized:", hologramId, "-", textLines.size() + " lines");
    }

    private String formatEntry(TopEntry entry, TopDesignConfig.TopDesign design) {
        String format;

        if (type == TopType.CLAN_LEVEL && entry.hasClan()) {
            format = design.getEntryFormat();
        } else if (type == TopType.CLAN_LEVEL && !entry.hasClan()) {
            format = design.getEntryFormatNoClan();
        } else {
            format = design.getEntryFormat();
        }

        String valueDisplay;
        if (type.isPlaytime() && entry.hasFormattedValue()) {
            valueDisplay = entry.getFormattedValue();
        } else {
            valueDisplay = formatValue(entry.getValue());
        }

        String formatted = format
                .replace("{position}", String.valueOf(entry.getPosition()))
                .replace("{player}", entry.getPlayerName())
                .replace("{value}", valueDisplay)
                .replace("{clan}", entry.getClanName() != null ? entry.getClanName() : "");

        return ChatColor.translateAlternateColorCodes('&', formatted);
    }

    private String formatValue(long value) {
        if (value >= 1000000) {
            return String.format("%.1fM", value / 1000000.0);
        } else if (value >= 1000) {
            return String.format("%.1fK", value / 1000.0);
        }
        return String.valueOf(value);
    }

    public String getHologramId() {
        return hologramId;
    }

    public TopType getType() {
        return type;
    }
}