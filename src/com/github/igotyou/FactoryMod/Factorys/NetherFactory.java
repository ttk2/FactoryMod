package com.github.igotyou.FactoryMod.Factorys;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.github.igotyou.FactoryMod.FactoryModPlugin;
import com.github.igotyou.FactoryMod.properties.NetherFactoryProperties;
import com.github.igotyou.FactoryMod.utility.InteractionResponse;
import com.github.igotyou.FactoryMod.utility.InteractionResponse.InteractionResult;
import com.github.igotyou.FactoryMod.utility.ItemList;
import com.github.igotyou.FactoryMod.utility.NamedItemStack;

import java.util.ArrayList;
import java.util.List;

public class NetherFactory extends BaseFactory
{

	private NetherFactoryProperties netherFactoryProperties;//the properties of the production factory
	private Location netherDestination;
	private NetherOperationMode mode;
	public NetherOperationMode getMode() {
		return mode;
	}
	
	/**
	 * Constructor called when creating portal
	 */
	public NetherFactory (Location factoryLocation, Location factoryInventoryLocation, Location factoryPowerSource, Location netherDestination, 
			NetherFactoryProperties netherFactoryProperties)
	{
		super(factoryLocation, factoryInventoryLocation, factoryPowerSource, FactoryType.NETHER_FACTORY, "Nether factory");
		this.netherDestination = netherDestination;
		this.netherFactoryProperties = netherFactoryProperties;
		this.mode = NetherOperationMode.REPAIR;
	}

	/**
	 * Constructor
	 */
	public NetherFactory (Location factoryLocation, Location factoryInventoryLocation, Location factoryPowerSource, Location netherDestination,
			boolean active, double currentMaintenance,
			long timeDisrepair, NetherOperationMode mode, NetherFactoryProperties netherFactoryProperties)
	{
		super(factoryLocation, factoryInventoryLocation, factoryPowerSource, FactoryType.NETHER_FACTORY, active, "Nether factory", 0 , 0, currentMaintenance, timeDisrepair);
		this.netherFactoryProperties = netherFactoryProperties;
		this.netherDestination = netherDestination;
		this.mode = mode;
	}
		
	@Override
	public boolean isRepairing() {
		return mode == NetherOperationMode.REPAIR;
	}
	
	
	/**
	 * Returns either a success or error message.
	 * Called by the blockListener when a player left clicks the center block, with the InteractionMaterial
	 */
	@Override
	public void update()
	{
		if(!isFuelAvailable())
		{
			togglePower();
		}
	}
	
	@Override
	public List<InteractionResponse> getCentralBlockResponse(Player player)
	{
		Location playerLocation = player.getLocation();
		List<InteractionResponse> responses=new ArrayList<InteractionResponse>();
		//Is the factory off
		if (!active)
		{
			if (playerLocation.getWorld().getName().equalsIgnoreCase(FactoryModPlugin.WORLD_NAME))
			{
				//is the recipe is initiated
				if (mode == null) {
					mode = NetherOperationMode.REPAIR;
				} else {		
					mode = mode.getNext();
				}
				
				responses.add(new InteractionResponse(InteractionResult.SUCCESS, "-----------------------------------------------------"));
				responses.add(new InteractionResponse(InteractionResult.SUCCESS, "Switched mode to: " + mode.getDescription()+"."));
				responses.add(new InteractionResponse(InteractionResult.SUCCESS, "Next mode is: "+mode.getNext().getDescription()+"."));
			}
			else if (playerLocation.getWorld().getName().equalsIgnoreCase(FactoryModPlugin.NETHER_NAME))
			{
				responses.add(new InteractionResponse(InteractionResult.SUCCESS, "The " + netherFactoryProperties.getName() + " is turned off."));
			}
		}
		else
		{
			if (isFuelAvailable())
			{
				if ((playerLocation.getBlockX() == factoryLocation.getBlockX() || playerLocation.getBlockX() == netherDestination.getBlockX())
						&& ((playerLocation.getBlockY()-1)== factoryLocation.getBlockY() || (playerLocation.getBlockY()-1)== netherDestination.getBlockY())
						&& (playerLocation.getBlockZ() == factoryLocation.getBlockZ() || playerLocation.getBlockZ() == netherDestination.getBlockZ()))
				{
					responses.add(new InteractionResponse(InteractionResult.SUCCESS, "Commencing teleportation..."));
					if (playerLocation.getWorld().getName().equalsIgnoreCase(FactoryModPlugin.WORLD_NAME))
					{
						Location destination = new Location(netherDestination.getWorld(), netherDestination.getX(), netherDestination.getY(), netherDestination.getZ(), playerLocation.getYaw(), playerLocation.getPitch());
						destination.add(0.5, 1.5, 0.5);
						player.teleport(destination);
						getFuel().removeFrom(getPowerSourceInventory());
					}
					else if (playerLocation.getWorld().getName().equalsIgnoreCase(FactoryModPlugin.NETHER_NAME))
					{
						Location destination = new Location(factoryLocation.getWorld(), factoryLocation.getX(), factoryLocation.getY(), factoryLocation.getZ(), playerLocation.getYaw(), playerLocation.getPitch());
						destination.add(0.5, 1.5, 0.5);
						player.teleport(destination);
						getFuel().removeFrom(getPowerSourceInventory());
					}
				}
				else
				{
					responses.add(new InteractionResponse(InteractionResult.FAILURE, "You must stand on the center block!"));
				}
			}
			else
			{
				responses.add(new InteractionResponse(InteractionResult.FAILURE, "Factory is missing fuel! ("+getFuel().getMultiple(1).toString()+")"));
			}
		}
		return responses;
	}
	
	@Override
	public ItemList<NamedItemStack> getFuel() {
		return netherFactoryProperties.getFuel();
	}
	
	/**
	 * Returns the factory's properties
	 */
	public NetherFactoryProperties getProperties()
	{
		return netherFactoryProperties;
	}
	
	@Override
	public List<InteractionResponse> getChestResponse()
	{
		List<InteractionResponse> responses=new ArrayList<InteractionResponse>();
		String status=active ? "On" : "Off";
		//Name: Status with XX% health.
		int maxRepair = netherFactoryProperties.getRepair();
		boolean maintenanceActive = maxRepair!=0;
		int health =(!maintenanceActive) ? 100 : (int) Math.round(100*(1-currentRepair/(maxRepair)));
		responses.add(new InteractionResponse(InteractionResult.SUCCESS, netherFactoryProperties.getName()+": "+status+" with "+String.valueOf(health)+"% health."));
		//Current mode: mode description
		responses.add(new InteractionResponse(InteractionResult.SUCCESS, "Current mode: " + mode.getDescription()));
		//Nether factory links to X: Y: Z:
		responses.add(new InteractionResponse(InteractionResult.SUCCESS, "Nether Factory links to X:" + netherDestination.getBlockX() + " Y:" + netherDestination.getBlockY() + " Z:" + netherDestination.getBlockZ()));
		//[Will repair XX% of the factory]
		if(!getRepairs().isEmpty()&&maintenanceActive)
		{
			int amountAvailable=getRepairs().amountAvailable(getInventory());
			int amountRepaired=amountAvailable>currentRepair ? (int) Math.ceil(currentRepair) : amountAvailable;
			int percentRepaired=(int) (( (double) amountRepaired)/netherFactoryProperties.getRepair()*100);
			responses.add(new InteractionResponse(InteractionResult.SUCCESS,"Will repair "+String.valueOf(percentRepaired)+"% of the factory with "+getRepairs().getMultiple(amountRepaired).toString()+"."));
		}
		return responses;
	}
	
	protected void recipeFinished() {
	}

	@Override
	public ItemList<NamedItemStack> getInputs() {
		if(mode == NetherOperationMode.REPAIR)
		{
			return netherFactoryProperties.getRepairMaterials();
		}
		else
		{
			return new ItemList<NamedItemStack>();
		}
	}

	@Override
	public ItemList<NamedItemStack> getOutputs() {
		return new ItemList<NamedItemStack>();
	}

	@Override
	public ItemList<NamedItemStack> getRepairs() {
		ItemList<NamedItemStack> repairMaterials = new ItemList<NamedItemStack>();
		switch(mode) {
		case REPAIR:
			repairMaterials.addAll(netherFactoryProperties.getRepairMaterials());
			break;
		default:
			break;
		}
		return repairMaterials;
	}

	@Override
	public double getEnergyTime() {
		return netherFactoryProperties.getEnergyTime();
	}

	@Override
	public double getProductionTime() {
		switch(mode) {
		case REPAIR:
			return netherFactoryProperties.getRepairTime();
		default:
			return 1;
		}
	}

	@Override
	public int getMaxRepair() {
		return netherFactoryProperties.getRepair();
	}
	
	public Location getNetherDestinationLocation()
	{
		return netherDestination;
	}
	
	@Override
	public boolean isWhole()
	{
	//Check if power source exists
	if(factoryPowerSourceLocation.getBlock().getType().getId()== 61 || factoryPowerSourceLocation.getBlock().getType().getId()== 62)
	{
		//Check inventory location
		if(factoryInventoryLocation.getBlock().getType().getId()== 54) 	
		{
			//Check Interaction block location
			if(factoryLocation.getBlock().getType().getId()==FactoryModPlugin.NETHER_FACTORY_CENTRAL_BLOCK_MATERIAL.getId())
			{
				return true;
			}
		}
	}
	return false;
	}
	
	public enum NetherOperationMode {
		REPAIR(0, "Repair"),
		TELEPORT(1, "Teleport");
		
		private static final int MAX_ID = 2;
		private int id;
		private String description;

		private NetherOperationMode(int id, String description) {
			this.id = id;
			this.description = description;
		}
		
		public String getDescription() {
			return description;
		}

		public static NetherOperationMode byId(int id) {
			for (NetherOperationMode mode : NetherOperationMode.values()) {
				if (mode.getId() == id)
					return mode;
			}
			return null;
		}
		
		public int getId() {
			return id;
		}

		public NetherOperationMode getNext() {
			int nextId = (getId() + 1) % MAX_ID;
			return NetherOperationMode.byId(nextId);
		}
	}
}
