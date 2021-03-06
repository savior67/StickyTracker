package com.aetheriumwars.stickytracker.tracker;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.util.EulerAngle;
import com.aetheriumwars.stickytracker.StickyTracker;

import de.slikey.effectlib.effect.LineEffect;
import de.slikey.effectlib.util.DynamicLocation;
import de.slikey.effectlib.util.ParticleEffect;

public class Tracker {
	
	private static int visibility = 10; //how far can you see the path
	private static String playerWithTrackerHead = "Mercury777";
	
	private  boolean playerHid = false; //true if the owner has the trail hidden
	private UUID ownerId;
	private UUID targetId;
	private LineEffect lineEffect;
	
	private Entity armorStand;
	//private Entity slime;
	
	public Tracker(Player owner, Player target) {
		this.ownerId = owner.getUniqueId();
		this.targetId = target.getUniqueId();
		this.show();
		addTracker(this);
	}
	
	public Tracker(UUID owner, UUID target) {
		this.ownerId = owner;
		this.targetId = target;
		this.show();
		addTracker(this);	
	}
	
	public Tracker(String ownerId, String targetId) {
		this.ownerId = UUID.fromString(ownerId);
		this.targetId = UUID.fromString(targetId);
		this.show();
		addTracker(this);	
	}
	
	
	public static void addTracker(Tracker t) {
		//a player can only own 1 tracker at a time
		if(StickyTracker.getTrackers().containsKey(t.getOwnerID()))
			removeTrackerOwnedBy((Player) t.getOwner());

		StickyTracker.getTrackers().put(t.getOwnerID(), t);
		StickyTracker.saveTrackers();
	}
	
	//removes the tracker owned by player p, or attached to player p
	public static boolean removeTrackerOwnedBy(Player p) {
		if(hasTracker(p)) {
			Tracker t = StickyTracker.getTrackers().get(p.getUniqueId());
			t.removeTrail();
			StickyTracker.getTrackers().remove(p.getUniqueId());
			//create an explosion at targets location
			Player target = (Player) t.getTarget();
			Location loc = target.getLocation();
			target.getWorld().createExplosion(loc.getX(), loc.getY()+1, loc.getZ(), 0, false, false);
			
			StickyTracker.saveTrackers();
			return true;
		}
		else {
			return false;
		}

	}
	
	//returns true if it removed the tracker attached to the player
	public static boolean removeTrackerAttachedTo(Player p) {
		Tracker t = isBeingTracked(p);
		if(t != null) {
			return removeTrackerOwnedBy((Player) t.getOwner());
		}
		else {
			return false;
		}
	}
	
	//Returns true if the player has a tracker
	public static boolean hasTracker(Player p) {
		if(StickyTracker.getTrackers().get(p.getUniqueId()) == null)
			return false;
		else
			return true;
	}
	
	//Returns the tracker if the player is being tracked, otherwise returns null
	public static Tracker isBeingTracked(Player p) {
		for(Tracker t: StickyTracker.getTrackers().values()) {
			if(t.getTargetID().equals(p.getUniqueId()))
				return t;
		}
		return null;
	}
	
	//Returns the tracker owned by the player
	public static Tracker getTracker(Player p) {
		return StickyTracker.getTrackers().get(p.getUniqueId());
	}
	
	//actually attaches the tracker to the target player
	private void spawnTracker() {
		Player p = (Player) getTarget();
		ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
		SkullMeta sm = (SkullMeta) skull.getItemMeta();
		sm.setOwner(playerWithTrackerHead);
		skull.setItemMeta(sm);
		
		World w = p.getWorld();
		armorStand = w.spawnEntity(p.getLocation(), EntityType.ARMOR_STAND);
		
		/*slime = w.spawnEntity(p.getLocation(), EntityType.SLIME);
		((Slime) slime).setInvulnerable(true);
		((Slime) slime).setAI(false);
		((Slime) slime).setSize(2);
		slime.setPassenger(armorStand);*/
		p.setPassenger(armorStand);
		((ArmorStand) armorStand).setHelmet(skull);
		((ArmorStand) armorStand).setVisible(false);
		((ArmorStand) armorStand).setSmall(true);
		((ArmorStand) armorStand).setBodyPose(new EulerAngle(0.0, 3.57, 0.0));
	}
	
	//despawns the physical tracker object attached to the player
	private void despawnTracker() {
		Player p = (Player) getTarget();
		if(armorStand != null) {
			armorStand.remove();
			armorStand = null;
		}
		
		/*if(slime != null) {
			slime.remove();
			slime = null;
		}*/
		return;
	}
	
	public UUID getOwnerID() {
		return ownerId;
	}
	//the player who placed the tracker
	public OfflinePlayer getOwner() {
		return Bukkit.getPlayer(ownerId);
	}
	
	//the player being tracked
	public OfflinePlayer getTarget() {
		return Bukkit.getPlayer(targetId);
	}
	
	public UUID getTargetID() {
		return targetId;
	}
	
	public LineEffect getEffect() {
		return lineEffect;
	}
	
	public boolean isHidden() {
		if(lineEffect == null)
			return true;
		else
			return false;
	}
	
	//returns true if the player has the trail hidden
	public boolean hiddenByPlayer() {
		return playerHid;
	}
	
	public void setHiddenByPlayer(boolean hid) {
		this.playerHid = hid;
	}
	
	//hides the tracker and the trail
	public void hide() {
		despawnTracker();
		removeTrail();
	}
	
	//shows the tracker if it is hidden
	public void show() {
		if(getOwner() != null && getTarget() != null) {
			hide(); //remove the old stuff so there's no doubling up of trails/trackers
			generateTrail();
			spawnTracker();
		}
	}
	
	private void removeTrail() {
		if(lineEffect != null) {
			lineEffect.cancel();
			this.lineEffect = null;
		}
	}
	
	//generates and updates the trail of particles leading to the player
	public void generateTrail() {
		OfflinePlayer owner = getOwner();
		OfflinePlayer target = getTarget();
		
		if(owner != null && owner.isOnline() && target != null && target.isOnline()) {
			this.lineEffect = new LineEffect(StickyTracker.getEffectManager());
			//lineEffect.particle = ParticleEffect.REDSTONE;
			lineEffect.period = 1;
			lineEffect.visibleRange = visibility;
			lineEffect.isZigZag = false;
			lineEffect.updateLocations = true;
			lineEffect.updateDirections = true;
			//loc.addOffset(getOwner().getVelocity().setY(getOwner().getLocation().getY() - getOwner().getEyeHeight()) );
			lineEffect.iterations = -1;
			lineEffect.particle = ParticleEffect.PORTAL;
			
			lineEffect.setDynamicOrigin(new DynamicLocation(new Location(((Player) owner).getWorld(), 0, 0, 0)));
			lineEffect.setDynamicTarget(new DynamicLocation(new Location(((Player) owner).getWorld(), 0, 0, 0)));
			//lineEffect.particleOffsetX = 1;
			//lineEffect.particleOffsetY = 1;
			lineEffect.start();
		}
	}

}
