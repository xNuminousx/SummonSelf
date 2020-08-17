package me.numin.summonself;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.ability.ComboAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.util.Collision;
import com.projectkorra.projectkorra.ability.util.ComboManager;
import com.projectkorra.projectkorra.airbending.AirShield;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.firebending.FireShield;
import com.projectkorra.projectkorra.util.ClickType;
import com.projectkorra.projectkorra.util.DamageHandler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;

public class SummonSelf extends AirAbility implements AddonAbility, ComboAbility {

    private ArmorStand armorStand;
    private ArrayList<Integer> armorStands = new ArrayList<Integer>();
    private Location location;
    private Location origin;
    private Vector direction;

    private boolean standIsCreated;
    private boolean pushUser;

    private double damage;
    private double radius;
    private double range;
    private double speed;
    private double targetLift;
    private double targetPush;
    private double userLift;
    private double userPush;

    private long cooldown;
    private long time;

    public SummonSelf(Player player) {
        super(player);

        if (!bPlayer.canBendIgnoreBinds(this))
            return;

        origin = player.getLocation().add(0, 1, 0);
        location = origin.clone();
        direction = location.getDirection().clone().normalize();
        pushUser = true;

        cooldown = ConfigManager.getConfig().getLong("ExtraAbilities.SummonSelf.Cooldown");
        radius = ConfigManager.getConfig().getDouble("ExtraAbilities.SummonSelf.Radius");
        range = ConfigManager.getConfig().getDouble("ExtraAbilities.SummonSelf.Range");
        speed = ConfigManager.getConfig().getDouble("ExtraAbilities.SummonSelf.Speed");
        damage = ConfigManager.getConfig().getDouble("ExtraAbilities.SummonSelf.Damage");
        targetLift = ConfigManager.getConfig().getDouble("ExtraAbilities.SummonSelf.Velocities.Target.Lift");
        targetPush = ConfigManager.getConfig().getDouble("ExtraAbilities.SummonSelf.Velocities.Target.Push");
        userLift = ConfigManager.getConfig().getDouble("ExtraAbilities.SummonSelf.Velocities.User.Lift");
        userPush = ConfigManager.getConfig().getDouble("ExtraAbilities.SummonSelf.Velocities.User.Push");

        if (player.isSprinting()) {
            start();
            time = System.currentTimeMillis();
        }
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline() || GeneralMethods.isRegionProtectedFromBuild(this, getLocation())) {
            remove();
            return;
        }

        if (origin.distance(getLocation()) > range || System.currentTimeMillis() > time + 2000) {
            remove();
            return;
        }

        if (!standIsCreated)
            armorStand = getArmorStand();

        for (Block block : GeneralMethods.getBlocksAroundPoint(armorStand.getLocation(), radius - 1)) {
            if (block.isLiquid() || GeneralMethods.isSolid(block)) {
                armorStand.getWorld().spawnParticle(Particle.CLOUD, armorStand.getLocation().add(0, 1, 0), 20, 0.1, 0.1, 0.1, 0.2);
                remove();
                return;
            }
        }

        for (Entity entity : GeneralMethods.getEntitiesAroundPoint(armorStand.getLocation(), radius)) {
            if (entity instanceof LivingEntity && entity.getUniqueId() != player.getUniqueId() && entity.getUniqueId() != armorStand.getUniqueId()) {
                Vector vector = armorStand.getLocation().getDirection().normalize();
                if (targetLift != 0) vector.setY(targetLift);

                entity.getWorld().spawnParticle(Particle.CLOUD, entity.getLocation().add(0, 1, 0), 20, 0.1, 0.1, 0.1, 0.2);
                entity.setVelocity(vector.multiply(targetPush));
                DamageHandler.damageEntity(entity, damage, this);

                destroyArmorStands();
                remove();
                return;
            }
        }
        checkCollisions();
        moveArmorStand();
    }

    private void checkCollisions() {
        CoreAbility airShield = CoreAbility.getAbility(AirShield.class);
        CoreAbility fireShield = CoreAbility.getAbility(FireShield.class);
        CoreAbility summonSelf = CoreAbility.getAbility(SummonSelf.class);
        CoreAbility[] abilities = {airShield, fireShield};
        for (CoreAbility ability : abilities)
            ProjectKorra.getCollisionManager().addCollision(new Collision(ability, summonSelf, false, true));
    }

    private void destroyArmorStands() {
        for (World world : Bukkit.getServer().getWorlds())
            for (Entity entity : world.getEntities())
                if (armorStands.contains(entity.getEntityId()))
                    entity.remove();
    }

    private ArmorStand getArmorStand() {
        ArmorStand stand = player.getWorld().spawn(origin, ArmorStand.class);
        stand.setCanPickupItems(false);
        stand.setGravity(true);
        stand.setVisible(false);
        stand.setMarker(false);
        stand.setCollidable(true);

        armorStands.add(stand.getEntityId());
        standIsCreated = true;
        return stand;
    }

    private void moveArmorStand() {
        if (pushUser) {
            Vector vector = player.getLocation().getDirection().normalize();
            if (userLift != 0) vector.setY(userLift);
            player.setVelocity(vector.multiply(-userPush));
            pushUser = false;
        }

        Location newStandLoc = location.add(direction.multiply(speed));
        armorStand.teleport(newStandLoc);

        player.getWorld().spawnParticle(Particle.SPELL, armorStand.getLocation().add(0, 0.4, 0), 10, 0.07, 0.9, 0.07, 0);
        player.getWorld().spawnParticle(Particle.SPELL, armorStand.getLocation().add(0, 0.5, 0), 10, 0.7, 0.1, 0.7, 0);
        player.getWorld().spawnParticle(Particle.SPELL, armorStand.getLocation().add(0, 2, 0), 10, 0.3, 0.3, 0.3, 0);
        player.getWorld().spawnParticle(Particle.CLOUD, armorStand.getLocation().add(0, 1, 0), 1, 1, 1, 1, 0);
    }

    @Override
    public void remove() {
        destroyArmorStands();
        bPlayer.addCooldown(this);
        super.remove();
    }

    @Override
    public boolean isSneakAbility() {
        return true;
    }

    @Override
    public boolean isHarmlessAbility() {
        return false;
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public double getCollisionRadius() {
        return radius;
    }

    @Override
    public String getName() {
        return "SummonSelf";
    }

    @Override
    public Location getLocation() {
        return armorStands.isEmpty() ? player.getLocation() : armorStand.getLocation();
    }

    @Override
    public void load() {
        ConfigManager.getConfig().addDefault("ExtraAbilities.SummonSelf.Cooldown", 10000);
        ConfigManager.getConfig().addDefault("ExtraAbilities.SummonSelf.Range", 15);
        ConfigManager.getConfig().addDefault("ExtraAbilities.SummonSelf.Radius", 2);
        ConfigManager.getConfig().addDefault("ExtraAbilities.SummonSelf.Speed", 1);
        ConfigManager.getConfig().addDefault("ExtraAbilities.SummonSelf.Damage", 3);
        ConfigManager.getConfig().addDefault("ExtraAbilities.SummonSelf.Velocities.Target.Lift", 0.5);
        ConfigManager.getConfig().addDefault("ExtraAbilities.SummonSelf.Velocities.Target.Push", 1.5);
        ConfigManager.getConfig().addDefault("ExtraAbilities.SummonSelf.Velocities.User.Lift", 0);
        ConfigManager.getConfig().addDefault("ExtraAbilities.SummonSelf.Velocities.User.Push", 2);
        ConfigManager.defaultConfig.save();

        ProjectKorra.getCollisionInitializer().addComboAbility(this);
        ProjectKorra.log.info("Enabled " + getName() + " by " + getAuthor());
    }

    @Override
    public void stop() {
        ProjectKorra.log.info("Disabled " + getName() + " by " + getAuthor());
    }

    @Override
    public String getAuthor() {
        return "Numin";
    }

    @Override
    public String getVersion() {
        return "2.0";
    }

    @Override
    public Object createNewComboInstance(Player player) {
        return new SummonSelf(player);
    }

    @Override
    public ArrayList<ComboManager.AbilityInformation> getCombination() {
        ArrayList<ComboManager.AbilityInformation> combo = new ArrayList<ComboManager.AbilityInformation>();
        combo.add(new ComboManager.AbilityInformation("AirBlast", ClickType.SHIFT_DOWN));
        combo.add(new ComboManager.AbilityInformation("AirBlast", ClickType.SHIFT_UP));
        combo.add(new ComboManager.AbilityInformation("AirBlast", ClickType.SHIFT_DOWN));
        combo.add(new ComboManager.AbilityInformation("AirBlast", ClickType.SHIFT_UP));
        combo.add(new ComboManager.AbilityInformation("AirBurst", ClickType.LEFT_CLICK));
        return combo;
    }

    @Override
    public String getDescription() {
        return "While in a powerful rush, you're able to take your momentum and force it outward in your direction! Launch a clone-like current of yourself towards opponents to deal knockback and damage.";
    }

    @Override
    public String getInstructions() {
        return "AirBlast (Tap shift 2x) > AirBurst (Sprint + Left-click)";
    }
}
