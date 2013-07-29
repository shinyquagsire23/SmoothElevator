package org.zzl.minegaming.SmoothElevator;

import java.lang.reflect.Field;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_6_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_6_R1.entity.*;

import net.minecraft.server.v1_6_R1.*;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

public class FloatingBlock extends EntityFallingBlock
{
	boolean ignoreGravity = true;

	private float brightness = 0.0F;

	public Material getMaterial()
	{
		return Material.getMaterial(this.id);
	}

	public int getBlockId() {
		return this.id;
	}

	public byte getBlockData()
	{
		return (byte)this.data;
	}

	public boolean getDropItem()
	{
		return false;
	}

	public void setPassenger(Entity e)
	{
		this.passenger = e;
	}

	public void setPassenger(Player p)
	{
		this.passenger = ((CraftPlayer)p).getHandle();
	}

	public FloatingBlock(World paramWorld)
	{
		super(paramWorld);
		this.dropItem = false;
	}

	public FloatingBlock(World paramWorld, double paramDouble1, double paramDouble2, double paramDouble3, int paramInt)
	{
		super(paramWorld, paramDouble1, paramDouble2, paramDouble3, paramInt);
		setBrightness(Block.lightEmission[this.id]);
		try
		{
			setFinalStatic(getClass().getField("boundingBox"), AxisAlignedBB.a(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	static void setFinalStatic(Field field, Object newValue) throws Exception
	{
		field.setAccessible(true);

		Field modifiersField = Field.class.getDeclaredField("modifiers");
		modifiersField.setAccessible(true);
		modifiersField.set(field, Integer.valueOf(field.getModifiers() & 0xFFFFFFEF));

		field.set(null, newValue);
	}

	public FloatingBlock(World paramWorld, double paramDouble1, double paramDouble2, double paramDouble3, int paramInt, int paramInt2)
	{
		super(paramWorld, paramDouble1, paramDouble2, paramDouble3, paramInt, paramInt2);
		setBrightness(Block.lightEmission[this.id]);
	}

	@Override
	public void l_()
	{
		this.lastX = this.locX;
		this.lastY = this.locY;
		this.lastZ = this.locZ;
		this.c += 1;

		if (!this.ignoreGravity)
		{
			this.motY -= 0.03999999910593033D;
		}
		move(this.motX, this.motY, this.motZ);
		this.motX *= 0.9800000190734863D;
		if (!this.ignoreGravity)
		{
			this.motY *= 0.9800000190734863D;
		}
		this.motZ *= 0.9800000190734863D;

		if (!this.world.isStatic) {
			int i = MathHelper.floor(this.locX);
			int j = MathHelper.floor(this.locY);
			int k = MathHelper.floor(this.locZ);

			if (this.c == 1) {
				if ((this.c == 1) && (this.world.getTypeId(i, j, k) == this.id)) {
					this.world.setTypeIdUpdate(i, j, k, 0);
				}
				else if (!this.ignoreGravity) {
					die();
				}
			}

			if (this.onGround)
			{
				this.motX *= 0.699999988079071D;
				this.motZ *= 0.699999988079071D;
				if (!this.ignoreGravity)
				{
					this.motY *= -0.5D;
				}

				if ((this.world.getTypeId(i, j, k) != Block.PISTON_MOVING.id) && (!this.ignoreGravity))
				{
					die();
				}
				else if (((this.c > 100) && (!this.world.isStatic) && ((j < 1) || (j > 256))) || ((this.c > 600) && 
						(!this.ignoreGravity)))
				{
					if (this.dropItem) b(this.id, 1);
					die();
				}
			}
		}
	}

	public Vector getVelocity() {
		return new Vector(this.motX, this.motY, this.motZ);
	}

	public void setVelocity(Vector vel) {
		this.motX = vel.getX();
		this.motY = vel.getY();
		this.motZ = vel.getZ();
		this.velocityChanged = true;
	}

	public CraftWorld getWorld() {
		return ((WorldServer)this.world).getWorld();
	}

	public boolean teleport(Location location) {
		return teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN);
	}

	public boolean teleport(Location location, PlayerTeleportEvent.TeleportCause cause) {
		this.world = ((CraftWorld)location.getWorld()).getHandle();
		setLocation(location.getX(), location.getY(), location.getZ(), 
				location.getYaw(), location.getPitch());

		return true;
	}

	public void setYaw(float yaw) {
		this.yaw = yaw;
	}

	public float getYaw() {
		return this.yaw;
	}

	public void setPitch(float pitch) {
		this.pitch = pitch;
	}

	public float getPitch() {
		return this.pitch;
	}

	public Location getLocation()
	{
		return new Location(getWorld(), this.locX, this.locY, this.locZ);
	}

	public void setLocation(Location l)
	{
		this.locX = l.getX();
		this.locY = l.getY();
		this.locZ = l.getZ();
	}

	public void remove()
	{
		die();
	}

	public float c(float f)
	{
		return this.brightness;
	}

	public void setBrightness(float brightness) {
		this.brightness = brightness;
	}

	public float getBrightness() {
		return this.brightness;
	}
}