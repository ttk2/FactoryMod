package com.github.igotyou.FactoryMod.managers;

import static com.untamedears.citadel.Utility.isReinforced;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;

import com.github.igotyou.FactoryMod.FactoryModPlugin;
import com.github.igotyou.FactoryMod.Factorys.NetherFactory;
import com.github.igotyou.FactoryMod.Factorys.NetherFactory.NetherOperationMode;
import com.github.igotyou.FactoryMod.interfaces.Factory;
import com.github.igotyou.FactoryMod.interfaces.Manager;
import com.github.igotyou.FactoryMod.properties.NetherFactoryProperties;
import com.github.igotyou.FactoryMod.utility.InteractionResponse;
import com.github.igotyou.FactoryMod.utility.InteractionResponse.InteractionResult;
import com.github.igotyou.FactoryMod.utility.ItemList;
import com.github.igotyou.FactoryMod.utility.NamedItemStack;

import java.util.Iterator;

//original file:
/**
* Manager.java
* Purpose: Interface for Manager objects for basic manager functionality
*
* @author MrTwiggy
* @version 0.1 1/08/13
*/
//edited version:
/**
* Manager.java	 
* Purpose: Interface for Manager objects for basic manager functionality
* @author igotyou
*
*/

public class NetherFactoryManager implements Manager
{
	private FactoryModPlugin plugin;
	private List<NetherFactory> netherFactorys;
	private long repairTime;
	
	public NetherFactoryManager(FactoryModPlugin plugin)
	{
		this.plugin = plugin;
		netherFactorys = new ArrayList<NetherFactory>();
		//Set maintenance clock to 0
		updateFactorys();
	}
	
	public void save(File file) throws IOException 
	{
		//Takes difference between last repair update and current one and scales repair accordingly
		updateRepair(System.currentTimeMillis()-repairTime);
		repairTime=System.currentTimeMillis();
		FileOutputStream fileOutputStream = new FileOutputStream(file);
		ObjectOutputStream oos = new ObjectOutputStream(fileOutputStream);
		int version = 1;
		oos.writeInt(version);
		oos.writeInt(netherFactorys.size());
		for (NetherFactory factory : netherFactorys)
		{
			Location centerlocation = factory.getCenterLocation();
			Location inventoryLocation = factory.getInventoryLocation();
			Location powerLocation = factory.getPowerSourceLocation();
			Location netherLocation = factory.getNetherDestinationLocation();
			
			oos.writeUTF(centerlocation.getWorld().getName());
			
			oos.writeInt(centerlocation.getBlockX());
			oos.writeInt(centerlocation.getBlockY());
			oos.writeInt(centerlocation.getBlockZ());

			oos.writeInt(inventoryLocation.getBlockX());
			oos.writeInt(inventoryLocation.getBlockY());
			oos.writeInt(inventoryLocation.getBlockZ());

			oos.writeInt(powerLocation.getBlockX());
			oos.writeInt(powerLocation.getBlockY());
			oos.writeInt(powerLocation.getBlockZ());
			
			oos.writeUTF(netherLocation.getWorld().getName());
			oos.writeInt(netherLocation.getBlockX());
			oos.writeInt(netherLocation.getBlockY());
			oos.writeInt(netherLocation.getBlockZ());
			
			oos.writeBoolean(factory.getActive());
			oos.writeInt(factory.getMode().getId());
			oos.writeDouble(factory.getCurrentRepair());
			oos.writeLong(factory.getTimeDisrepair());

		}
		oos.flush();
		fileOutputStream.close();
	}

	public void load(File file) throws IOException 
	{
		try {
			repairTime=System.currentTimeMillis();
			FileInputStream fileInputStream = new FileInputStream(file);
			ObjectInputStream ois = new ObjectInputStream(fileInputStream);
			int version = ois.readInt();
			assert(version == 1);
			int count = ois.readInt();
			int i = 0;
			for (i = 0; i < count; i++)
			{
				String worldName = ois.readUTF();
				World world = plugin.getServer().getWorld(worldName);

				Location centerLocation = new Location(world, ois.readInt(), ois.readInt(), ois.readInt());
				Location inventoryLocation = new Location(world, ois.readInt(), ois.readInt(), ois.readInt());
				Location powerLocation = new Location(world, ois.readInt(), ois.readInt(), ois.readInt());
				
				String worldName2 = ois.readUTF();
				World world2 = plugin.getServer().getWorld(worldName2);
				
				Location netherLocation = new Location(world2, ois.readInt(), ois.readInt(), ois.readInt());
				
				boolean active = ois.readBoolean();
				NetherOperationMode mode = NetherFactory.NetherOperationMode.byId(ois.readInt());
				double currentRepair = ois.readDouble();
				long timeDisrepair  = ois.readLong();
				
				NetherFactory factory = new NetherFactory(centerLocation, inventoryLocation, powerLocation, netherLocation,
						active, currentRepair, timeDisrepair,
						mode,
						plugin.getNetherFactoryProperties());
				addFactory(factory);
			}
			fileInputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void updateFactorys() 
	{
		plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable()
		{
			@Override
			public void run()
			{
				for (NetherFactory factory: netherFactorys)
				{
					factory.update();
				}
			}
		}, 0L, FactoryModPlugin.PRODUCER_UPDATE_CYCLE);
	}

	public InteractionResponse createFactory(Location factoryLocation, Location inventoryLocation, Location powerSourceLocation) 
	{
		NetherFactoryProperties netherFactoryProperties = plugin.getNetherFactoryProperties();
		if (factoryLocation.getWorld().getName().equalsIgnoreCase(FactoryModPlugin.WORLD_NAME))
		{
			if (factoryLocation.getBlock().getType().equals(FactoryModPlugin.NETHER_FACTORY_CENTRAL_BLOCK_MATERIAL))
			{
				if (!factoryExistsAt(factoryLocation))
				{
					Block inventoryBlock = inventoryLocation.getBlock();
					Chest chest = (Chest) inventoryBlock.getState();
					Inventory chestInventory = chest.getInventory();
					ItemList<NamedItemStack> constructionMaterials = netherFactoryProperties.getConstructionMaterials();
					boolean hasMaterials = constructionMaterials.allIn(chestInventory);
					if (hasMaterials)
					{
						int nether_scale = FactoryModPlugin.NETHER_SCALE;
						boolean locationOk = false;
						int scanX = Math.round(factoryLocation.getBlockX()/nether_scale);
						int scanY = factoryLocation.getBlockY();
						int scanZ = Math.round(factoryLocation.getBlockZ()/nether_scale);
						Location netherLocation = new Location(Bukkit.getWorld(FactoryModPlugin.NETHER_NAME), scanX,scanY,scanZ);
						Location netherLocation1 = netherLocation.clone();
						netherLocation1.add(0, 1, 0);
						Location netherLocation2 = netherLocation.clone();
						netherLocation2.add(0, 2, 0);
						if (FactoryModPlugin.CITADEL_ENABLED)
						{
							while (!locationOk)
							{
								
								while(!locationOk)
								{
									while(!locationOk && scanY <250)
									{
										netherLocation = new Location(Bukkit.getWorld(FactoryModPlugin.NETHER_NAME), scanX,scanY,scanZ);
										netherLocation1 = netherLocation.clone();
										netherLocation1.add(0, 1, 0);
										netherLocation2 = netherLocation.clone();
										netherLocation2.add(0, 2, 0);
										if(!isReinforced(netherLocation) && !isReinforced(netherLocation1) && !isReinforced(netherLocation2))
										{
											locationOk = true;
											
										}
										scanY++;
									}
									scanZ++;
								}
								scanX++;
							}
						}
						if (!factoryExistsAt(netherLocation))
						{
							FactoryModPlugin.sendConsoleMessage(netherLocation.toString());
							FactoryModPlugin.sendConsoleMessage(FactoryModPlugin.NETHER_FACTORY_CENTRAL_BLOCK_MATERIAL.toString());
							netherLocation.getBlock().setType(FactoryModPlugin.NETHER_FACTORY_CENTRAL_BLOCK_MATERIAL);
							netherLocation.getBlock().getState().update(true);
							netherLocation1.getBlock().setType(Material.AIR);
							netherLocation1.getBlock().getState().update(true);
							netherLocation2.getBlock().setType(Material.AIR);
							netherLocation2.getBlock().getState().update(true);
							FactoryModPlugin.sendConsoleMessage(netherLocation.getBlock().getType().toString());
							if(netherLocation.getBlock().getType() != (FactoryModPlugin.NETHER_FACTORY_CENTRAL_BLOCK_MATERIAL))
							{
								return new InteractionResponse(InteractionResult.FAILURE, "For some reason the nether side obsidian block did not generate...blame bukkit");
							}
							NetherFactory netherFactory = new NetherFactory(factoryLocation, inventoryLocation, powerSourceLocation, netherLocation, plugin.getNetherFactoryProperties());
							if (netherFactoryProperties.getConstructionMaterials().removeFrom(netherFactory.getInventory()))
							{
								addFactory(netherFactory);
								return new InteractionResponse(InteractionResult.SUCCESS, "Successfully created " + netherFactoryProperties.getName());
							}
						}
						else
						{
							return new InteractionResponse(InteractionResult.FAILURE, "There is a other " + netherFactoryProperties.getName() + " too close.");
						}
					}
					return new InteractionResponse(InteractionResult.FAILURE, "Not enough materials in chest!");
				}
				return new InteractionResponse(InteractionResult.FAILURE, "There is already a " + netherFactoryProperties.getName() + " there!");
			}
			else
			{
				return new InteractionResponse(InteractionResult.FAILURE, "Wrong center block!");
			}
		}
		else
		{
			return new InteractionResponse(InteractionResult.FAILURE, netherFactoryProperties.getName() + "'s can only be built in the overworld");
		}
	}

	public InteractionResponse addFactory(Factory factory) 
	{
		NetherFactory netherFactory = (NetherFactory) factory;
		if (netherFactory.getCenterLocation().getBlock().getType().equals(FactoryModPlugin.NETHER_FACTORY_CENTRAL_BLOCK_MATERIAL) && (!factoryExistsAt(netherFactory.getCenterLocation()))
				|| !factoryExistsAt(netherFactory.getInventoryLocation()) || !factoryExistsAt(netherFactory.getPowerSourceLocation()))
		{
			netherFactorys.add(netherFactory);
			return new InteractionResponse(InteractionResult.SUCCESS, "");
		}
		else
		{
			return new InteractionResponse(InteractionResult.FAILURE, "");
		}
	}

	public NetherFactory getFactory(Location factoryLocation) 
	{
		for (NetherFactory factory : netherFactorys)
		{
			if (factory.getCenterLocation().equals(factoryLocation) || factory.getInventoryLocation().equals(factoryLocation)
					|| factory.getPowerSourceLocation().equals(factoryLocation) || factory.getNetherDestinationLocation().equals(factoryLocation))
				return factory;
		}
		return null;
	}
	
	public boolean factoryExistsAt(Location factoryLocation) 
	{
		boolean returnValue = false;
		if (getFactory(factoryLocation) != null)
		{
			returnValue = true;
		}
		return returnValue;
	}
	
	public boolean factoryWholeAt(Location factoryLocation) 
	{
		boolean returnValue = false;
		if (getFactory(factoryLocation) != null)
		{
			returnValue = getFactory(factoryLocation).isWhole();
		}
		return returnValue;
	}

	public void removeFactory(Factory factory) 
	{
		netherFactorys.remove((NetherFactory)factory);
	}
	
	public void updateRepair(long time)
	{
		for (NetherFactory factory: netherFactorys)
		{
			factory.updateRepair(time/((double)FactoryModPlugin.REPAIR_PERIOD));
		}
		long currentTime=System.currentTimeMillis();
		Iterator<NetherFactory> itr=netherFactorys.iterator();
		while(itr.hasNext())
		{
			NetherFactory factory=itr.next();
			if(currentTime>(factory.getTimeDisrepair()+FactoryModPlugin.DISREPAIR_PERIOD))
			{
				itr.remove();
			}
		}
	}
	
	public String getSavesFileName() 
	{
		return FactoryModPlugin.NETHER_FACTORY_SAVE_FILE;
	}
	
	public ItemList<NamedItemStack> getScaledMaterials(Location location)
	{
		NetherFactoryProperties properties = plugin.getNetherFactoryProperties();
		ItemList<NamedItemStack> input = properties.getConstructionMaterials();
		int scalingMode = properties.getScalingMode();
		if (scalingMode != 0)
		{
			for (NetherFactory factory : netherFactorys)
			{
				Location factoryLoc = factory.getCenterLocation();
				double distance = location.distance(factoryLoc);
				if (distance <= properties.getScalingRadius())
				{
					switch(scalingMode)
					{
					case 1:
						Math.round(distance/properties.getScalingFactor());
					case 2:
						
					default:
					}
				}
			}
		}
		return input;
	}

}
