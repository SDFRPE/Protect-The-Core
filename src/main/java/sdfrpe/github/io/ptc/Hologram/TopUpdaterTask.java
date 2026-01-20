package sdfrpe.github.io.ptc.Hologram;

import org.bukkit.scheduler.BukkitRunnable;
import sdfrpe.github.io.ptc.Utils.LogSystem;
import sdfrpe.github.io.ptc.Utils.LogSystem.LogCategory;

public class TopUpdaterTask extends BukkitRunnable {
    private final HologramManager hologramManager;
    private final TopDataFetcher dataFetcher;

    public TopUpdaterTask(HologramManager hologramManager, TopDataFetcher dataFetcher) {
        this.hologramManager = hologramManager;
        this.dataFetcher = dataFetcher;
    }

    @Override
    public void run() {
        try {
            LogSystem.debug(LogCategory.CORE, "Actualizando hologramas de TOPs...");
            hologramManager.updateAllHolograms();
            LogSystem.debug(LogCategory.CORE, "Hologramas actualizados correctamente");
        } catch (Exception e) {
            LogSystem.error(LogCategory.CORE, "Error actualizando hologramas:", e.getMessage());
        }
    }
}