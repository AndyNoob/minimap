package comfortable_andy.minimap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;

@SuppressWarnings("unused")
public final class MinimapMain extends JavaPlugin {

    private FileConfiguration dataFile;
    private final File dataFileFile = new File(getDataFolder(), "data.yml");

    @Override
    public void onEnable() {
        // Plugin startup logic
        Objects.requireNonNull(getCommand("minimap")).setExecutor(this);
        saveResource("data.yml", false);
        this.dataFile = YamlConfiguration.loadConfiguration(dataFileFile);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Ye can't use this, dummy!");
            return true;
        }
        final ItemStack map = new ItemStack(Material.FILLED_MAP);
        map.editMeta(MapMeta.class, meta -> {
            final int id = this.dataFile.getInt("map-id", -1);
            final MapView view = Objects.requireNonNull(
                    id == -1 ? Bukkit.createMap(player.getWorld()) : Bukkit.getMap(id)
            );
            view.getRenderers().forEach(view::removeRenderer);
            view.addRenderer(new MinimapRenderer());
            meta.setMapView(view);
            this.dataFile.set("map-id", view.getId());
            try {
                this.dataFile.save(dataFileFile);
            } catch (IOException e) {
                getLogger().log(Level.WARNING, "Couldn't save data file?", e);
            }
        });
        player.getInventory().addItem(map);
        return true;
    }
}
