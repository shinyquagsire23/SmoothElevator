package org.zzl.minegaming.SmoothElevator;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.util.Vector;

import net.minecraft.server.AxisAlignedBB;
import net.minecraft.server.Block;
import net.minecraft.server.BlockSand;
import net.minecraft.server.EntityFallingBlock;
import net.minecraft.server.ItemStack;
import net.minecraft.server.MathHelper;
import net.minecraft.server.World;
import net.minecraft.server.Entity;
import net.minecraft.server.WorldServer;

public class FloatingBlock extends EntityFallingBlock
{
	boolean ignoreGravity = true;
	public Material getMaterial()
	{
		return Material.getMaterial(id);
	}
	public int getBlockId()
	{
		return id;
	}

	public byte getBlockData()
	{
		return (byte) data;
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
		dropItem = false;
	}

	public FloatingBlock(World paramWorld, double paramDouble1, double paramDouble2, double paramDouble3, int paramInt) 
	{
		super(paramWorld, paramDouble1, paramDouble2, paramDouble3, paramInt);
		this.setBrightness(Block.lightEmission[id]);
		try
		{
			setFinalStatic(this.getClass().getField("boundingBox"), AxisAlignedBB.a(0,0,0,1,1,1));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		//this.boundingBox = ;
	}
	
	static void setFinalStatic(Field field, Object newValue) throws Exception {
	      field.setAccessible(true);

	      Field modifiersField = Field.class.getDeclaredField("modifiers");
	      modifiersField.setAccessible(true);
	      modifiersField.set(field, field.getModifiers() & ~Modifier.FINAL);

	      field.set(null, newValue);
	   }

	public FloatingBlock(World paramWorld, double paramDouble1, double paramDouble2, double paramDouble3, int paramInt, int paramInt2) 
	{
		super(paramWorld, paramDouble1, paramDouble2, paramDouble3, paramInt, paramInt2);
		this.setBrightness(Block.lightEmission[id]);
	}

	@Override
	public void j_()
	{

		this.lastX = this.locX;
		this.lastY = this.locY;
		this.lastZ = this.locZ;
		this.c += 1;
		

				if(!ignoreGravity)
				{
					this.motY -= 0.03999999910593033D;
				}
				move(this.motX, this.motY, this.motZ);
				this.motX *= 0.9800000190734863D;
				if(!ignoreGravity)
				{
					this.motY *= 0.9800000190734863D;
				}
				this.motZ *= 0.9800000190734863D;


	     if (!this.world.isStatic) {
	       int i = MathHelper.floor(this.locX);
	       int j = MathHelper.floor(this.locY);
	       int k = MathHelper.floor(this.locZ);

	       if (this.c == 1) {
	         if ((this.c == 1) && (this.world.getTypeId(i, j, k) == this.id))
	           this.world.setTypeId(i, j, k, 0);
	         else 
					  {
						if(!ignoreGravity)
	          	die();
	         }
	       }

	       if (this.onGround) 
 		   {
	        this.motX *= 0.699999988079071D;
	        this.motZ *= 0.699999988079071D;
					  if(!ignoreGravity)
					  {
	         	this.motY *= -0.5D;
					  }

	         if (this.world.getTypeId(i, j, k) != Block.PISTON_MOVING.id && !ignoreGravity) 
					  {
	           die();
	           /*if (((!this.world.mayPlace(this.id, i, j, k, true, 1, null)) || (BlockSand.canFall(this.world, i, j - 1, k)) || (!this.world.setTypeIdAndData(i, j, k, this.id, this.data))) && 
	             (!this.world.isStatic) && 
	             (this.dropItem)) a(new ItemStack(this.id, 1, this.data), 0.0F);
	         }*/
	       }
	       else if (((this.c > 100) && (!this.world.isStatic) && ((j < 1) || (j > 256))) || (this.c > 600)) {
	         if(!ignoreGravity)
						{
							if (this.dropItem) b(this.id, 1);
	         		die();
						}
	     }}}
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
		return ((WorldServer) this.world).getWorld();
	}

	public boolean teleport(Location location) {
		return teleport(location, TeleportCause.PLUGIN);
	}

	public boolean teleport(Location location, TeleportCause cause) {
		this.world = ((CraftWorld) location.getWorld()).getHandle();
		this.setLocation(location.getX(), location.getY(), location.getZ(),
				location.getYaw(), location.getPitch());
		// entity.setLocation() throws no event, and so cannot be cancelled
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
		return new Location(this.getWorld(), locX,locY,locZ);
	}

	public void setLocation(Location l)
	{
		locX = l.getX();
		locY = l.getY();
		locZ = l.getZ();
	}

	public void remove()
	{
		die();
	}

	private float brightness = 0F;

	@Override
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
