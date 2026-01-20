package sdfrpe.github.io.ptc.Listeners.Game;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import sdfrpe.github.io.ptc.PTC;
import sdfrpe.github.io.ptc.Events.Player.ItemInteractEvent;
import sdfrpe.github.io.ptc.Player.GamePlayer;
import sdfrpe.github.io.ptc.Utils.Enums.Action;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public class InGameInteracts implements Listener {
    private final Map<UUID, Integer> tntCountMap = Maps.newHashMap();
    private final Map<UUID, BukkitTask> rechargeTaskMap = Maps.newHashMap();
    private final Map<UUID, Integer> savedExpMap = Maps.newHashMap();
    private final Map<UUID, Long> lastTntThrow = Maps.newHashMap();
    private static final long TNT_COOLDOWN_MS = 300;

    @EventHandler
    public void openShop(PlayerInteractEvent e) {
        if (e.getClickedBlock() != null) {
            if (e.getClickedBlock().getType().equals(Material.ENDER_PORTAL_FRAME)) {
                if (PTC.getInstance().getGameManager().getGlobalSettings().isModeCW()) {
                    e.getPlayer().sendMessage(org.bukkit.ChatColor.RED + "La tienda está deshabilitada en modo Clan War.");
                    return;
                }
                GamePlayer gamePlayer = PTC.getInstance().getGameManager().getPlayerManager().getPlayer(e.getPlayer().getUniqueId());
                PTC.getInstance().getMenuManager().openInventory("tienda.yml", gamePlayer);
            }
        }
    }

    @EventHandler
    public void InventoryInteract(InventoryClickEvent e) {
        ItemStack clicked = e.getCurrentItem();
        if (e.getCurrentItem() != null && clicked.getType() != null) {
            if (e.getCurrentItem().getType().name().contains("LEATHER")) {
                e.setCurrentItem(new ItemStack(Material.AIR, 1, (short) 0));
            }

            if (e.getCurrentItem().getType().name().contains("WOOD_")) {
                e.setCurrentItem(new ItemStack(Material.AIR, 1, (short) 0));
            }
        }
    }

    private boolean hasInventoryOpen(Player player) {
        if (player.getOpenInventory() == null) {
            return false;
        }
        InventoryType type = player.getOpenInventory().getTopInventory().getType();
        return type != InventoryType.CRAFTING;
    }

    @EventHandler
    public void playerInteractAtItem(ItemInteractEvent e) {
        if (PTC.getInstance().getGameManager().getGlobalSettings().isModeCW()) {
            return;
        }

        if (this.allowedInteracts().contains(e.getAction())) {
            Player player = e.getPlayer();

            if (hasInventoryOpen(player)) {
                return;
            }

            ItemStack itemStack = e.getItemStack();

            if (itemStack.getType() == Material.DIAMOND_SWORD) {
                UUID playerId = player.getUniqueId();
                Long lastThrow = lastTntThrow.get(playerId);

                if (lastThrow != null && (System.currentTimeMillis() - lastThrow) < TNT_COOLDOWN_MS) {
                    return;
                }

                this.handleDiamondSword(player, itemStack);
            }
        }
    }

    private void handleDiamondSword(Player player, ItemStack itemStack) {
        short durability = itemStack.getDurability();
        short maxDurability = itemStack.getType().getMaxDurability();
        short difDurability = (short) (maxDurability - durability);
        int takesMultiplier;
        if (itemStack.containsEnchantment(Enchantment.DURABILITY)) {
            takesMultiplier = itemStack.getEnchantmentLevel(Enchantment.DURABILITY);
        } else {
            takesMultiplier = 1;
        }

        int multiplier;
        int takes;
        short takeIt;

        if (itemStack.containsEnchantment(Enchantment.DAMAGE_ALL)) {
            multiplier = itemStack.getEnchantmentLevel(Enchantment.DAMAGE_ALL);

            if (multiplier < 4) {
                this.handleLowSharpnessTNT(player, itemStack, multiplier, takesMultiplier, durability, maxDurability, difDurability);
                return;
            }

            takes = multiplier * takesMultiplier;
            takeIt = (short) (maxDurability / takes);

            if (difDurability < takeIt) {
                return;
            }

            itemStack.setDurability((short) (durability + takeIt));
            lastTntThrow.put(player.getUniqueId(), System.currentTimeMillis());
            this.throwTnt(player, 1.0F);


        } else if (itemStack.containsEnchantment(Enchantment.KNOCKBACK)) {
            int knockbackLevel = itemStack.getEnchantmentLevel(Enchantment.KNOCKBACK);

            if (knockbackLevel >= 2) {
                multiplier = knockbackLevel * 4;
                takes = multiplier * takesMultiplier;
                takeIt = (short) (maxDurability / takes);

                if (difDurability < takeIt) {
                    return;
                }

                itemStack.setDurability((short) (durability + takeIt));
                player.launchProjectile(Snowball.class);
            }

        } else if (itemStack.containsEnchantment(Enchantment.FIRE_ASPECT)) {
            multiplier = itemStack.getEnchantmentLevel(Enchantment.FIRE_ASPECT) * 4;
            takes = multiplier * takesMultiplier;
            takeIt = (short) (maxDurability / takes);

            if (difDurability < takeIt) {
                return;
            }

            itemStack.setDurability((short) (durability + takeIt));
            player.launchProjectile(Fireball.class);
        }
    }

    private void handleLowSharpnessTNT(Player player, ItemStack sword, int sharpnessLevel, int takesMultiplier, short durability, short maxDurability, short difDurability) {
        UUID playerId = player.getUniqueId();

        if (this.rechargeTaskMap.containsKey(playerId)) {
            return;
        }

        int tntCount = this.tntCountMap.getOrDefault(playerId, 0);

        if (tntCount >= 2) {
            this.startRecharge(player);
            return;
        }

        int takes = sharpnessLevel * takesMultiplier;
        short takeIt = (short) (maxDurability / takes);

        if (difDurability < takeIt) {
            return;
        }

        sword.setDurability((short) (durability + takeIt));
        lastTntThrow.put(playerId, System.currentTimeMillis());
        this.throwTnt(player, 1.0F);

        tntCount++;
        this.tntCountMap.put(playerId, tntCount);

        if (tntCount >= 2) {
            this.startRecharge(player);
        }
    }

    private void startRecharge(Player player) {
        UUID playerId = player.getUniqueId();

        if (this.rechargeTaskMap.containsKey(playerId)) {
            this.rechargeTaskMap.get(playerId).cancel();
        }

        savedExpMap.put(playerId, player.getTotalExperience());
        final int[] secondsLeft = {15};

        BukkitTask rechargeTask = Bukkit.getScheduler().runTaskTimer(PTC.getInstance(), () -> {
            if (!player.isOnline()) {
                this.cancelRecharge(playerId);
                return;
            }

            ItemStack itemInHand = player.getItemInHand();
            if (itemInHand == null || itemInHand.getType() != Material.DIAMOND_SWORD) {
                return;
            }

            float progress = (15 - secondsLeft[0]) / 15.0f;
            player.setLevel(secondsLeft[0]);
            player.setExp(progress);

            secondsLeft[0]--;

            if (secondsLeft[0] < 0) {
                this.completeRecharge(player);
            }
        }, 0L, 20L);

        this.rechargeTaskMap.put(playerId, rechargeTask);
    }

    private void completeRecharge(Player player) {
        UUID playerId = player.getUniqueId();

        if (this.rechargeTaskMap.containsKey(playerId)) {
            this.rechargeTaskMap.get(playerId).cancel();
            this.rechargeTaskMap.remove(playerId);
        }

        this.tntCountMap.put(playerId, 0);

        Integer savedExp = savedExpMap.remove(playerId);
        if (savedExp != null) {
            player.setTotalExperience(0);
            player.setLevel(0);
            player.setExp(0);
            player.giveExp(savedExp);
        } else {
            player.setLevel(0);
            player.setExp(0);
        }
    }

    private void cancelRecharge(UUID playerId) {
        if (this.rechargeTaskMap.containsKey(playerId)) {
            this.rechargeTaskMap.get(playerId).cancel();
            this.rechargeTaskMap.remove(playerId);
        }

        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            Integer savedExp = savedExpMap.remove(playerId);
            if (savedExp != null) {
                player.setTotalExperience(0);
                player.setLevel(0);
                player.setExp(0);
                player.giveExp(savedExp);
            }
        } else {
            savedExpMap.remove(playerId);
        }
    }

    @EventHandler
    public void playerBowEvent(EntityShootBowEvent e) {
        if (PTC.getInstance().getGameManager().getGlobalSettings().isModeCW()) {
            return;
        }

        if (e.getEntity() instanceof Player) {
            if (e.getBow() != null) {
                Player player = (Player) e.getEntity();
                ItemStack bow = e.getBow();
                if (bow.containsEnchantment(Enchantment.ARROW_DAMAGE) && this.handleBowShot(bow)) {
                    e.getProjectile().setMetadata("streak", new FixedMetadataValue(PTC.getInstance(), bow.getEnchantmentLevel(Enchantment.ARROW_DAMAGE)));
                }
            }
        }
    }

    @EventHandler
    public void ProjectileHit(ProjectileHitEvent e) {
        if (PTC.getInstance().getGameManager().getGlobalSettings().isModeCW()) {
            return;
        }

        if (e.getEntity() != null) {
            if (e.getEntity().hasMetadata("streak")) {
                Location location = e.getEntity().getLocation();
                FixedMetadataValue meta = (FixedMetadataValue) e.getEntity().getMetadata("streak").get(0);
                if (meta != null) {
                    int level = meta.asInt();
                    (new Thread(() -> {
                        for (int i = 0; i < level; ++i) {
                            location.getWorld().strikeLightning(location);
                            if (level != 1) {
                                try {
                                    Thread.sleep(500L);
                                } catch (InterruptedException var4) {
                                    var4.printStackTrace();
                                }
                            }
                        }
                    })).start();
                }
            }
        }
    }

    private boolean handleBowShot(ItemStack bow) {
        short durability = bow.getDurability();
        short maxDurability = bow.getType().getMaxDurability();
        short difDurability = (short) (maxDurability - durability);
        short takeIt = (short) (maxDurability / 3);
        if (difDurability < takeIt) {
            return false;
        } else {
            bow.setDurability((short) (durability + takeIt));
            return true;
        }
    }

    @EventHandler
    public void onTntHitBlock(org.bukkit.event.entity.EntityChangeBlockEvent e) {
        if (!(e.getEntity() instanceof TNTPrimed)) return;

        TNTPrimed tnt = (TNTPrimed) e.getEntity();

        if (!tnt.hasMetadata("sharpnessTNT")) return;

        e.setCancelled(true);

        tnt.setFuseTicks(0);
    }

    @EventHandler
    public void onTntExplode(org.bukkit.event.entity.EntityExplodeEvent e) {
        if (!(e.getEntity() instanceof TNTPrimed)) return;

        TNTPrimed tnt = (TNTPrimed) e.getEntity();
        if (!tnt.hasMetadata("sharpnessTNT")) return;

        List<org.bukkit.block.Block> toRemove = Lists.newArrayList();

        for (org.bukkit.block.Block block : e.blockList()) {
            Material type = block.getType();

            if (type == Material.STONE
                    || type == Material.COBBLESTONE
                    || type == Material.MOSSY_COBBLESTONE
                    || type == Material.SMOOTH_BRICK
                    || type == Material.OBSIDIAN
                    || type == Material.BEDROCK
                    || type == Material.IRON_ORE
                    || type == Material.GOLD_ORE
                    || type == Material.DIAMOND_ORE
                    || type == Material.EMERALD_ORE
                    || type == Material.REDSTONE_ORE
                    || type == Material.GLOWING_REDSTONE_ORE
                    || type == Material.LAPIS_ORE
                    || type == Material.COAL_ORE
                    || type == Material.QUARTZ_ORE) {
                toRemove.add(block);
            }
        }

        e.blockList().removeAll(toRemove);
    }

    private void throwTnt(Player player, float multiplier) {
        Location spawnLoc = player.getEyeLocation().clone();
        Vector direction = player.getLocation().getDirection().normalize();
        spawnLoc.add(direction.clone().multiply(0.5));

        final TNTPrimed tnt = player.getWorld().spawn(spawnLoc, TNTPrimed.class);

        tnt.setYield(4.2F);
        tnt.setIsIncendiary(false);
        tnt.setFuseTicks(80);

        tnt.setMetadata("sharpnessTNT", new FixedMetadataValue(PTC.getInstance(), player.getUniqueId().toString()));

        Vector velocity = direction.multiply(0.4F + (multiplier * 0.5F)).add(new Vector(0.0D, 0.3D, 0.0D));

        tnt.setVelocity(velocity);

        new org.bukkit.scheduler.BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (tnt.isDead() || !tnt.isValid()) {
                    cancel();
                    return;
                }

                ticks++;

                if (ticks <= 10) {
                    return;
                }

                if (tnt.getVelocity().length() < 0.08D) {
                    tnt.setFuseTicks(0);
                    cancel();
                }
            }
        }.runTaskTimer(PTC.getInstance(), 0L, 1L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onExplosionKnockback(org.bukkit.event.entity.EntityDamageByEntityEvent e) {
        if (e.isCancelled()) return;
        if (!(e.getDamager() instanceof TNTPrimed)) return;
        if (!(e.getEntity() instanceof Player)) return;

        TNTPrimed tnt = (TNTPrimed) e.getDamager();

        if (!tnt.hasMetadata("sharpnessTNT")) return;

        Player player = (Player) e.getEntity();

        Bukkit.getScheduler().runTaskLater(PTC.getInstance(), () -> {
            if (player.isOnline() && player.isValid()) {
                Vector currentVelocity = player.getVelocity();

                Vector reducedVelocity = currentVelocity.multiply(0.18);

                if (reducedVelocity.getY() > 0.22) {
                    reducedVelocity.setY(0.22);
                }

                double horizontalSpeed = Math.sqrt(
                        reducedVelocity.getX() * reducedVelocity.getX() +
                                reducedVelocity.getZ() * reducedVelocity.getZ()
                );

                if (horizontalSpeed > 0.35) {
                    double scale = 0.35 / horizontalSpeed;
                    reducedVelocity.setX(reducedVelocity.getX() * scale);
                    reducedVelocity.setZ(reducedVelocity.getZ() * scale);
                }

                player.setVelocity(reducedVelocity);
            }
        }, 1L);
    }

    private List<Action> allowedInteracts() {
        return Lists.newArrayList(new Action[]{Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK});
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent e) {
        UUID playerId = e.getPlayer().getUniqueId();

        this.cancelRecharge(playerId);
        this.tntCountMap.remove(playerId);
        this.lastTntThrow.remove(playerId);

        e.setQuitMessage(null);
    }
}