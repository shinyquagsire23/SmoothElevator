package org.zzl.minegaming.SmoothElevator;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.milkbowl.vault.permission.Permission;
import net.minecraft.server.v1_8_R2.EntityFallingBlock;
import net.minecraft.server.v1_8_R2.EntityTypes;
import net.minecraft.server.v1_8_R2.IBlockData;
import net.minecraft.server.v1_8_R2.WorldServer;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_8_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R2.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.bukkit.PermissionsEx;
import lib.PatPeter.SQLibrary.*;

public class Plugin extends JavaPlugin implements Listener 
{
	public static SQLite mysql;
	public static Logger log;
	public static HashMap<String, Request> requests = new HashMap<String,Request>();
	public static net.milkbowl.vault.permission.Permission vaultperms = null;
	public static Permission perms = null;
	public static Logger logger;
	
	public void onEnable()
	{
		log = getLogger();
        mysql = new SQLite(getLogger(),
                "[SmoothElevator]",
                this.getDataFolder().getAbsolutePath(),
                "sqlite");
        try {
            mysql.open();
			mysql.createTable("CREATE TABLE IF NOT EXISTS elevators (X INTEGER, Y INTEGER, Z INTEGER, World TEXT, X1 INTEGER, Y1 INTEGER, Z1 INTEGER, X2 INTEGER, Y2 INTEGER, Z2 INTEGER, X3 INTEGER, Y3 INTEGER, Z3 INTEGER, X4 INTEGER, Y4 INTEGER, Z4 INTEGER, X5 INTEGER, Y5 INTEGER, Z5 INTEGER, type INTEGER, height INTEGER, wait DOUBLE, waittop DOUBLE, speed DOUBLE )");
        } catch (Exception e) {
            log.info(e.getMessage());
        }
        registerEntityType(NewFloatingBlock.class, "FallingBlock", 21);
        
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        
        setupPermissions();
	}
	
	/**
	 * Registers custom entity class at the native minecraft entity enum
	 * 
	 * @param inClass	class of the entity
	 * @param name		minecraft entity name
	 * @param inID		minecraft entity id
	 */
	public static void registerEntityType(Class<?> inClass, String name, int inID)
	{
		try
		{
            @SuppressWarnings("rawtypes")
            Class[] args = new Class[3];
            args[0] = Class.class;
            args[1] = String.class;
            args[2] = int.class;
 
            a(inClass, name, inID);
        }
		catch (Exception e)
		{
            e.printStackTrace();
        }
	}
	
	/*
	 * Since 1.7.2 added a check in their entity registration, simply bypass it and write to the maps ourself.
	 */
	private static void a(Class paramClass, String paramString, int paramInt)
	{
		try
		{
			((Map) getPrivateStatic(EntityTypes.class, "c")).put(paramString, paramClass);
			((Map) getPrivateStatic(EntityTypes.class, "d")).put(paramClass, paramString);
			((Map) getPrivateStatic(EntityTypes.class, "e")).put(Integer.valueOf(paramInt), paramClass);
			((Map) getPrivateStatic(EntityTypes.class, "f")).put(paramClass, Integer.valueOf(paramInt));
			((Map) getPrivateStatic(EntityTypes.class, "g")).put(paramString, Integer.valueOf(paramInt));
		}
		catch (Exception exc)
		{
			// Unable to register the new class.
		}
	}
	
	private static Object getPrivateStatic(Class clazz, String f) throws Exception
	{
		Field field = clazz.getDeclaredField(f);
		field.setAccessible(true);
		return field.get(null);
	}
	
	private boolean setupVaultPermissions() 
	{
        RegisteredServiceProvider<net.milkbowl.vault.permission.Permission> rsp = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        vaultperms = rsp.getProvider();
        logger.log(Level.INFO, "Hooked into Vault Permissions!");
        return vaultperms != null;
    }
	
	private boolean setupPermissions(){
		
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        perms = rsp.getProvider();
		return perms != null;
	}
	
	public boolean hasPermission(Player p, String perm)
	{
		if(Bukkit.getServer().getPluginManager().isPluginEnabled("PermissionsEx")){
		    PermissionManager permissions = PermissionsEx.getPermissionManager();

		    // Permission check
		    if(permissions.has(p, perm)){
		    return true;
		    } else {
		    return false;
		    }
		}
		else if(Bukkit.getServer().getPluginManager().isPluginEnabled("Vault"))
		{
			try
			{
			if(perms.has(p, perm))
				return true;
			else
				return false;
			}
			catch(Exception e)
			{
				//setupVaultPermissions();
				if(perms.has(p, perm))
					return true;
				else
					return false;
			}
		}
		else 
		{
		   if(p.hasPermission(perm))
			   return true;
		   else
			   return false;
		}
	}
	
	public void onDisable()
	{
		mysql.close();
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args)
	{
		if(cmd.getName().equalsIgnoreCase("se"))
		{
			if(sender instanceof Player)
			{
				if(!hasPermission((Player)sender,"smoothelevator.admin"))
				{
					sender.sendMessage("You do not have permission to do that!");
					return true;
				}
			}
			if(args.length == 0)
			{
				//This guy needs some help!
				sender.sendMessage("Usage: /se create <type> <height>");
			}
			if(args.length > 0)
			{
				if(args[0].equalsIgnoreCase("create") && args.length > 2)
				{
					if(!(sender instanceof Player))
					{
						sender.sendMessage("Only Players can issue this command!");
						return true;
					}
					Player player = (Player) sender;
					double wait = 1;
					double waittop = 2;
					double speed = 0.3;
					if(args.length > 4)
					{
						wait = Double.parseDouble(args[3]);
						waittop = Double.parseDouble(args[4]);
					}
					if(args.length > 5)
						speed = Double.parseDouble(args[5]);
					player.sendMessage("Right click the top left elevator block...");
					requests.put(player.getName(), new Request(Integer.parseInt(args[1]),Integer.parseInt(args[2]),wait,waittop,speed));
					return true;
				}
				else if(args[0].equalsIgnoreCase("remove"))
				{
					if(!(sender instanceof Player))
					{
						sender.sendMessage("Only Players can issue this command!");
						return true;
					}
					Player player = (Player) sender;
					requests.put(player.getName(), new Request(2,-1));
					player.sendMessage("Right click one of the elevator switches to remove");
				}
				else if(args[0].equalsIgnoreCase("cancel"))
				{
					Player player = (Player) sender;
					requests.remove(player.getName());
					player.sendMessage("Action Canceled.");
				}
				else if(args[0].equalsIgnoreCase("clean"))
				{
					Player player;
					String world;
					if(sender instanceof Player)
					{
						player = (Player) sender;
						player.leaveVehicle();
						world = player.getWorld().getName();
					}
					else
					{
						world = "world";
					}
					if(args.length > 1)
						world = args[0];
					World w = Bukkit.getWorld(world);
					for(Entity e : w.getEntities())
					{
						if(e instanceof NewFloatingBlock || e instanceof EntityFallingBlock)
						{
							e.eject();
							e.remove();
							e.teleport(new Location(e.getWorld(),0,0,0));
						}
					}
				}
			}
			return true;
		}
		return false;
	}
	
	@EventHandler
	public void onInteract(PlayerInteractEvent event) throws SQLException
	{
		if(event.getAction() != Action.RIGHT_CLICK_BLOCK)
			return;
		if(requests.containsKey(event.getPlayer().getName()))
		{
			Request r = requests.get(event.getPlayer().getName());
			Player player = event.getPlayer();
			if(r.height >= 0)
			{
			if(r.stage == 0)
			{
				r.loc0 = event.getClickedBlock().getLocation();
				player.sendMessage("Elevator Corner Set!\n\nRight Click Bottom Right Corner...");
				r.stage++;
				event.setCancelled(true);
				return;
			}
			else if(r.stage == 1)
			{
				r.loc3 = event.getClickedBlock().getLocation();
				player.sendMessage("Elevator Cuboid Set!\n\nRight Click Top Right Riding Area...");
				r.stage++;
				event.setCancelled(true);
				return;
			}
			if(r.stage == 2)
			{
				r.loc4 = event.getClickedBlock().getLocation();
				player.sendMessage("Riding Corner Set!\n\nRight Click Bottom Right Corner...");
				r.stage++;
				event.setCancelled(true);
				return;
			}
			else if(r.stage == 3)
			{
				r.loc5 = event.getClickedBlock().getLocation();
				player.sendMessage("Riding Cuboid Set!\n\nRight Click Bottom Button...");
				r.stage++;
				event.setCancelled(true);
				return;
			}
			else if(r.stage == 4)
			{
				r.loc1 = event.getClickedBlock().getLocation();
				player.sendMessage("Button Location Set!\n\nRight Click Top Floor Button...");
				r.stage++;
				event.setCancelled(true);
				return;
			}
			else if(r.stage == 5)
			{
				r.loc2 = event.getClickedBlock().getLocation();
				player.sendMessage("Button Location Set!");
				Location l = r.loc0;
				ResultSet rs = mysql.query("INSERT OR REPLACE INTO elevators VALUES (" + l.getX() + ", " + l.getY() + ", " + l.getZ() + ", '" + l.getWorld().getName() + "', " + r.loc1.getX() + ", " + r.loc1.getY() + ", " + r.loc1.getZ() + ", "  + r.loc2.getX() + ", " + r.loc2.getY() + ", " + r.loc2.getZ() + ", " + r.loc3.getX() + ", " + r.loc3.getY() + ", " + r.loc3.getZ() + ", " + r.loc4.getX() + ", " + r.loc4.getY() + ", " + r.loc4.getZ() + ", " + r.loc5.getX() + ", " + r.loc5.getY() + ", " + r.loc5.getZ() + ", " + r.type + ", " + r.height + ", " + r.wait + ", " + r.waittop + ", " + r.speed + ");");
				rs.close();
				requests.remove(player.getName());
				player.sendMessage("Elevator successfully created!");
				return;
			}
			}
			else if(r.height == -1)
			{
				Location l = event.getClickedBlock().getLocation();
				ResultSet rs = mysql.query("select * from elevators where (x1 = " + l.getX() + " and y1 = " + l.getY() + " and z1 = " + l.getZ() + ") OR ( x2 = " + l.getX() + " and y2 = " + l.getY() + " and z2 = " + l.getZ() + ");");
				if(!rs.next())
				{
					event.getPlayer().sendMessage("This block isn't registered as a switch!");
					return;
				}
				rs.close();
				rs = mysql.query("delete from elevators where (x1 = " + l.getX() + " and y1 = " + l.getY() + " and z1 = " + l.getZ() + ") OR ( x2 = " + l.getX() + " and y2 = " + l.getY() + " and z2 = " + l.getZ() + ");");
				requests.remove(player.getName());
				rs.close();
				player.sendMessage("Elevator successfully removed!");
				return;
			}
		}
		else if(event.getClickedBlock().getType() != Material.AIR)
		{
			Location l = event.getClickedBlock().getLocation();
			ResultSet rs = mysql.query("select * from elevators where (x1 = " + l.getX() + " and y1 = " + l.getY() + " and z1 = " + l.getZ() + ");");
			try
			{
			if(!rs.next())
			{
				rs = mysql.query("select * from elevators where (x2 = " + l.getX() + " and y2 = " + l.getY() + " and z2 = " + l.getZ() + ");");
				if(!rs.next())
					return;
				long wait = (long)(rs.getDouble("wait") * 2);
				if(wait < 0)
					wait = 20;
				//event.getPlayer().sendMessage(rs.getDouble("speed") + "");
				getServer().getScheduler().scheduleSyncDelayedTask(this, new ElevateDelay(this, event.getPlayer(), rs, false, true), wait);
			}
			else
			{
				long wait = (long)(rs.getDouble("wait") * 2);
				if(wait < 0)
					wait = 20;
				//event.getPlayer().sendMessage(rs.getDouble("speed") + "");
				getServer().getScheduler().scheduleSyncDelayedTask(this, new ElevateDelay(this, event.getPlayer(), rs, true, true), wait);
				event.setCancelled(true);
			}
			}
			catch(Exception e)
			{
				e.printStackTrace();
				rs.close();
			}
		}
		
	}

	public NewFloatingBlock spawnFallingBlock(Location location, org.bukkit.Material material, byte data) throws IllegalArgumentException {
        Validate.notNull(location, "Location cannot be null");
        Validate.notNull(material, "Material cannot be null");
        Validate.isTrue(material.isBlock(), "Material must be a block");

        
        
        double x = location.getBlockX() + 0.5;
        double y = location.getBlockY() + 1.5;
        double z = location.getBlockZ() + 0.5;
        WorldServer world = ((CraftWorld)location.getWorld()).getHandle();
        
        IBlockData tempBlock = net.minecraft.server.v1_8_R2.Block.getByCombinedId(material.getId());
        tempBlock = tempBlock.getBlock().fromLegacyData(data);
        
        NewFloatingBlock entity = new NewFloatingBlock(world, x, y, z, tempBlock, data);
        entity.ticksLived = 1;

        world.addEntity(entity, SpawnReason.CUSTOM);
        return entity;
    }
	
	public void Elevate(Location l, float height, Player player, double speed)
	{
		//Location l = player.getLocation().getBlock().getLocation();
		Location ol = l.add(0,0,0);
		Material idd = ol.getBlock().getType();
		byte data = ol.getBlock().getData();
		ol.getBlock().setTypeId(0);
		NewFloatingBlock f = spawnFallingBlock(l, idd, data);
		if(player != null)
		{
			//f.setPassenger(player);
			//f.setPassenger(player);
			((CraftPlayer)player).getHandle().mount(f);
			//player.sendMessage("You were mounted!");
		}
		ElevatorChecker ele = new ElevatorChecker(0,f,height + 1,l,player);
		ele.speed = speed;
		int id = getServer().getScheduler().scheduleSyncRepeatingTask(this, ele, 0L,1L);
		ele.id = id;
	}
	
	public void Elevate(Location l, float height, Player[] player)
	{
		
	}
	
	public void Elevate(float height, Player player)
	{
		Elevate(player.getLocation().getBlock().getLocation(),height,player, 0.3);
	}
	
	public void Elevate(Location l, float height, double speed)
	{
		//Location l = player.getLocation().getBlock().getLocation();
		Location ol = l.add(0,0,0);
		Material idd = ol.getBlock().getType();
		byte data = ol.getBlock().getData();
		ol.getBlock().setTypeId(0);
		NewFloatingBlock f = spawnFallingBlock(l, idd, data);
		ElevatorChecker ele = new ElevatorChecker(0,f,height + 1,l);
		ele.speed = speed;
		System.out.println(f.getBlockId() + ", " + Material.LADDER.getId());
		int id = getServer().getScheduler().scheduleSyncRepeatingTask(this, ele, 0L,1L);
		ele.id = id;
	}
	
	public class ElevatorChecker implements Runnable 
	{
		public int id = 0;
		NewFloatingBlock ff;
		float fheight;
		Location fl;
		Player player;
		boolean atTop = false;
		boolean noPlayer = false;
		double speed = 0.3;
		
		public ElevatorChecker(int id, NewFloatingBlock f, float height, Location l)
		{
			this(id,f,height,l,null);
			noPlayer = true;
		}
		
		public ElevatorChecker(int id, NewFloatingBlock f, float height, Location l, Player p)
		{
			this.id = id;
			ff = f;
			fheight = height;
			fl = l;
			player = p;
			if(player == null)
				noPlayer = true;
		}
		
		public void run() 
		{
			if(fheight > 0)
				ff.setVelocity(new Vector(0,speed,0));
			else ff.setVelocity(new Vector(0,-speed,0));
			boolean ready = (ff.getLocation().getY() >= (fl.getY() + fheight - 0.2));
			if(fheight < 0)
				ready = (ff.getLocation().getY() <= (fl.getY() + fheight + 1));
			if(ready)
			{
				fl = fl.add(0, fheight - 1, 0);
				ff.teleport(fl);
				atTop = true;
				Block b = ff.getLocation().getBlock();
				b.setTypeIdAndData(ff.getBlock().getBlock().getId(ff.getBlock().getBlock()), ff.getBlockData(), false);
				//b.setTypeId(ff.getBlockId(), false);
				//b.setData(ff.getBlockData());
				ff.remove();
				if(!noPlayer)
				{
					fl.add(0.5,1.0,0.5);
					fl.setYaw(player.getLocation().getYaw());
					fl.setPitch(player.getLocation().getPitch());
					if(fheight < 0)
						fl.add(0,1.5,0);
					player.teleport(fl);
				}
				Bukkit.getScheduler().cancelTask(id);
				
			}
		}
	}
	
	public class Request
	{
		public int type;
		public int height;
		public int stage = 0;
		public double wait;
		public double waittop;
		public double speed;
		public Location loc0;
		public Location loc1;
		public Location loc2;
		public Location loc3;
		public Location loc4;
		public Location loc5;
		
		public Request(int type, int height)
		{
			this(type,height,1,2,0.3);
		}
		
		public Request(int type, int height, double wait, double waittop, double speed)
		{
			this.type = type;
			this.height = height;
			this.wait = wait;
			this.waittop = waittop;
			this.speed = speed;
			System.out.println(speed);
		}
	}
	
	class ElevateDelay implements Runnable
	{
		Player player;
		ResultSet rs;
		boolean up = false;
		Plugin p;
		boolean spring;
		
		@Override
		public void run() 
		{
			try
			{
			int height = rs.getInt("height");
			if(!up)
				height = -height;
			Location l = new Location(Bukkit.getWorld(rs.getString("World")),rs.getInt("X"),rs.getInt("Y"),rs.getInt("Z"));
			Location l2 = new Location(Bukkit.getWorld(rs.getString("World")),rs.getInt("X3"),rs.getInt("Y3"),rs.getInt("Z3"));
			Location l3 = new Location(Bukkit.getWorld(rs.getString("World")),rs.getInt("X4"),rs.getInt("Y4"),rs.getInt("Z4"));
			Location l4  = new Location(Bukkit.getWorld(rs.getString("World")),rs.getInt("X5"),rs.getInt("Y5"),rs.getInt("Z5"));
			Location distance = l.clone().subtract(l2);
			double record = 1.38;
			try
			{
				player = null;
				if(l.getBlock().getType() == Material.AIR || l.getBlock().getType() == Material.WATER || l.getBlock().getType() == Material.STATIONARY_WATER)
				{
					if(!up)
					{
						l.add(0,-height, 0);
						l2.add(0,-height, 0);
						l3.add(0,-height, 0);
						l4.add(0,-height, 0);
					}
					else
					{
						l.add(0,height, 0);
						l2.add(0,height, 0);
						l3.add(0,height, 0);
						l4.add(0,height, 0);
						height = -height;
					}
				}
				else
				{
					if(!up)
						height = -height;
				}
				
				if(distance.getX() < 0)
					distance.setX(-distance.getX());
				if(distance.getZ() < 0)
					distance.setZ(-distance.getZ());
				if(distance.getX() > 0)
					record += distance.getX() + 0.2;
				
				List<Player> playerList = new ArrayList<Player>();
				for(Entity e : l.getChunk().getEntities())
				{
					if(!(e instanceof Player))
						continue;
					
					if(e.getLocation().distance(l) < record)
					{
						if(player == null)
							player = (Player) e;
						playerList.add((Player)e);
					}
				}

				for(Entity e : l2.getChunk().getEntities())
				{
					if(!(e instanceof Player))
						continue;
					//System.out.println(e.getLocation().distance(l));
					//if(e.getLocation().distance(l) < record)
					//{
						if(player == null)
							player = (Player) e;
						playerList.add((Player)e);
						//record = e.getLocation().distance(l);
					//}
				}
				Location loc1 = l3;
                Location loc2 = l4;

                int startX = Math.min(loc1.getBlockX(), loc2.getBlockX());
                int endX = Math.max(loc1.getBlockX(), loc2.getBlockX());

                int startY = Math.min(loc1.getBlockY(), loc2.getBlockY());
                int endY = Math.max(loc1.getBlockY(), loc2.getBlockY());

                int startZ = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
                int endZ = Math.max(loc1.getBlockZ(), loc2.getBlockZ());

                Iterator<Player> i = playerList.iterator();
                Player p = null;
                try
                {
                p = i.next();
                }
                catch(Exception e)
                {
                	
                }
                List<Location> mountedLocs = new ArrayList<Location>();
                HashMap<Location, Player> playerMounts = new HashMap<Location, Player>();
                if(p != null)
                {
                	for (int x = startX; x <= endX; x++) {
                		for (int y = startY; y <= endY; y++) {
                			for (int z = startZ; z <= endZ; z++) 
                			{
                				Location loc = new Location(l.getWorld(), x,y,z);
                				try
                				{
                					System.out.println("ElevatorChecker started!attempt");
                					if(p.getLocation().getBlock().getLocation().subtract(0, 1, 0).equals(loc) && loc.getBlock().getType() != Material.AIR && loc.getBlock().getType() != Material.LADDER && loc.getBlock().getType() != Material.WOODEN_DOOR && loc.getBlock().getType() != Material.FENCE_GATE && loc.getBlock().getType() != Material.IRON_DOOR && loc.getBlock().getType() != Material.TORCH)
                					{
                						//Elevate(loc,height, p, rs.getDouble("speed"));
                						mountedLocs.add(loc);
                						//playerMounts.put(loc, p);
                						//System.out.println("ElevatorChecker started!succes");
                						Elevate(loc, height, p, rs.getDouble("speed"));
                						p = i.next();
                					}
                				}
                				catch(Exception e)
                				{
                					e.printStackTrace();
                					//break;
                				}
                			}
                		}
                	} 
                }
                
                loc1 = l;
                loc2 = l2;
                //System.out.println("ElevatorChecker startedlsl!");
                startX = Math.min(loc1.getBlockX(), loc2.getBlockX());
                endX = Math.max(loc1.getBlockX(), loc2.getBlockX());

                startY = Math.min(loc1.getBlockY(), loc2.getBlockY());
                endY = Math.max(loc1.getBlockY(), loc2.getBlockY());

                startZ = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
                endZ = Math.max(loc1.getBlockZ(), loc2.getBlockZ());
                
                for (int x = startX; x <= endX; x++) {
                    for (int y = startY; y <= endY; y++) {
                        for (int z = startZ; z <= endZ; z++) 
                        {
                        	Location loc = new Location(l.getWorld(), x,y,z);
                        	try
                        	{
                        		if(!mountedLocs.contains(loc) && loc.getBlock().getType() != Material.AIR && (loc.getBlock().getType() == Material.LADDER || loc.getBlock().getType() == Material.WOODEN_DOOR || loc.getBlock().getType() == Material.FENCE_GATE || loc.getBlock().getType() == Material.IRON_DOOR || loc.getBlock().getType() == Material.TORCH))
                        			Elevate(loc,height, rs.getDouble("speed"));
                        	}
                        	catch(Exception e)
                        	{
                        		//break;
                        	}
                        }
                    }
                }   

                for (int x = startX; x <= endX; x++) {
                    for (int y = startY; y <= endY; y++) {
                        for (int z = startZ; z <= endZ; z++) 
                        {
                        	Location loc = new Location(l.getWorld(), x,y,z);
                        	try
                        	{
                        		if(!mountedLocs.contains(loc) && loc.getBlock().getType() != Material.AIR && loc.getBlock().getType() != Material.LADDER && loc.getBlock().getType() != Material.WOODEN_DOOR && loc.getBlock().getType() != Material.FENCE_GATE && loc.getBlock().getType() != Material.IRON_DOOR && loc.getBlock().getType() != Material.TORCH)
                        			Elevate(loc,height, rs.getDouble("speed"));
                        	}
                        	catch(Exception e)
                        	{
                        		//break;
                        	}
                        }
                    }
                }   
				
                
                
				//player.sendMessage(rs.getInt("type") + "");
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			if(spring && rs.getInt("type") == 0)
			{
				double speed = rs.getDouble("speed");
				speed *= 20;
				speed *= height;
				getServer().getScheduler().scheduleSyncDelayedTask(p, new ElevateDelay(p, player, rs, !up, false), (long) ((rs.getDouble("waittop") * 20) + speed));
			}
			else
				rs.close();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		
		public ElevateDelay(Plugin pl, Player p, ResultSet r, boolean up, boolean spring)
		{
			player = p;
			rs = r;
			this.up = up;
			this.p = pl;
			this.spring = spring;
		}
	}
}
