package my.nvinz.core.vnizcore;

import my.nvinz.core.vnizcore.events.BlockEvents;
import my.nvinz.core.vnizcore.events.ChatEvents;
import my.nvinz.core.vnizcore.events.Commands;
import my.nvinz.core.vnizcore.events.GameEvents;
import my.nvinz.core.vnizcore.game.Items;
import my.nvinz.core.vnizcore.game.Menu;
import my.nvinz.core.vnizcore.game.Stage;
import my.nvinz.core.vnizcore.game.Variables;
import my.nvinz.core.vnizcore.resources.Resource;
import my.nvinz.core.vnizcore.resources.ResourceBuilder;
import my.nvinz.core.vnizcore.resources.ResourceSpawn;
import my.nvinz.core.vnizcore.teams.Team;
import my.nvinz.core.vnizcore.teams.TeamBuilder;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class VnizCore extends JavaPlugin {

    public Stage stage;
    public Stage.Status stageStatus;
    public Variables variables;
    public Items items;
    public ResourceSpawn resourceSpawn;

    public List<Team> teams = new ArrayList<>();
    public List<Player> players = new ArrayList<>();    // TODO Make non-full server game
    public Map<Player, Team> players_and_teams = new HashMap<>();
    public Map<Team, Material> teams_beds = new HashMap<>();
    public List<Resource> resources = new ArrayList<>();

    @Override
    public void onEnable() {
        variables =  new Variables(this);
        items = new Items(this);

        registerEvents(this);
        setCommandsExecutors(this);
        setupConfig(this);
        parseTeams(this);
        parseResources(this);
        setupStage(this);

        resourceSpawn = new ResourceSpawn(this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    /*
     *  PLUGIN SETUP
     */

    /*
     *  Register events:
     *  chatEvents
     *  blockEvents
     *  gameEvents
     */
    void registerEvents(VnizCore plugin){
        try {
             ChatEvents chatEvents = new ChatEvents(plugin);
             BlockEvents blockEvents = new BlockEvents(plugin);
             GameEvents gameEvents = new GameEvents(plugin);
             Menu menu = new Menu();
             plugin.getServer().getPluginManager().registerEvents(blockEvents, plugin);
             plugin.getServer().getPluginManager().registerEvents(chatEvents, plugin);
             plugin.getServer().getPluginManager().registerEvents(gameEvents, plugin);
             plugin.getServer().getPluginManager().registerEvents(menu, plugin);
        } catch (Exception e) {
             plugin.getServer().getConsoleSender().sendMessage("Error registering events: " + e.getMessage());
        }
    }

    /*
     *  Set commands executor
     *  plugin.yml - commands
     */
    void setCommandsExecutors(VnizCore plugin){
        try {
            plugin.getCommand("none").setExecutor(new Commands(plugin));
            plugin.getCommand("teams").setExecutor(new Commands(plugin));
            plugin.getCommand("start").setExecutor(new Commands(plugin));
            plugin.getCommand("test").setExecutor(new Commands(plugin));
            plugin.getCommand("jointeam").setExecutor(new Commands(plugin));
        } catch (Exception e) {
            plugin.getServer().getConsoleSender().sendMessage("Error setting commands executors: " + e.getMessage());
        }

    }

    /*
     *  Read config file
     *  config.yml
     *  TODO create if not exists
     */
    void setupConfig(VnizCore plugin){
        try {
            plugin.getConfig().options().copyDefaults(true);
            //plugin.saveConfig();
        } catch (Exception e) {
            plugin.getServer().getConsoleSender().sendMessage("Error setup config: " + e.getMessage());
        }
    }

    /*
     *  Parse teams from config
     *  config.yml
     *  teams.*
     */
    void parseTeams(VnizCore plugin){
        try {
            Map<String, Object> teams_cfg = plugin.getConfig().getConfigurationSection("teams").getValues(false);
            teams_cfg.forEach((team_cfg, obj) -> {

                plugin.getServer().getConsoleSender().sendMessage("Building team with parameters: ");
                World world = plugin.getServer().getWorld(plugin.getConfig().getString("arena.world"));

                TeamBuilder teamBuilder = new TeamBuilder(plugin);
                teamBuilder.setTeamColor(team_cfg)
                        .setTeamName(plugin.getConfig().getString("teams." + team_cfg + ".name"))
                        .setChatColor(team_cfg)
                        .setSpawnPoint(plugin.getConfig().getString("teams." + team_cfg + ".spawn"), world)
                        .setBedMaterial(team_cfg)
                        .setMaxPlayers(plugin.getConfig().getInt("teams." + team_cfg + ".max-players"));

                // TODO Check if bed is staying
                // TODO add yaw & pitch
                teamBuilder.buildTeam();
            });
        } catch (Exception e) {
            plugin.getServer().getConsoleSender().sendMessage("Error parsing team: " + e.getMessage());
        }
    }

    /*
     *  Parse resources from config
     *  config.yml
     *  resources.*
     */
    void parseResources(VnizCore plugin){
        try{
            Map<String, Object> resources_cfg = plugin.getConfig().getConfigurationSection("resources").getValues(false);
            resources_cfg.forEach((resource_cfg, obj) -> {
                plugin.getServer().getConsoleSender().sendMessage("Building material with parameters: ");
                World world = plugin.getServer().getWorld(plugin.getConfig().getString("arena.world"));
                List<Location> locations = new ArrayList<>();
                List<String> locations_cfg = plugin.getConfig().getStringList("resources."+resource_cfg+".spawns");
                locations_cfg.forEach(location -> {
                    locations.add(setupLocation(world, location));
                });

                ResourceBuilder resourceBuilder = new ResourceBuilder(this);
                resourceBuilder.setName(plugin.getConfig().getString("resources." + resource_cfg + ".name"))
                        .setMaterial(setupMaterial(resource_cfg.toUpperCase()))
                .setBlock(setupBlock(resource_cfg.toUpperCase()))
                .setTimer(plugin.getConfig().getInt("resources." + resource_cfg + ".timer"))
                .setLocations(locations);
                resourceBuilder.buildResource();
            });
        } catch (Exception e) {
            plugin.getServer().getConsoleSender().sendMessage("Error parsing material: " + e.getMessage());
        }
    }

    /*
     *  Setup Stage
     */
    void setupStage(VnizCore plugin){
        stage = new Stage(plugin);
        stageStatus = Stage.Status.LOBBY;
    }

    /*
     *  TOOLS
     */

    /**
     *  Tells message to everyone on server
     *  @param message {@code String}
     *  TODO not for all server
     */
    public void makeAnnouncement(String message){
        for (Player players: this.getServer().getOnlinePlayers()){
            players.sendMessage(message);
        }
    }

    /**
     *  Tells message to team players
     *  @param team {@code Team}
     *  @param message {@code String}
     */
    public void makeTeamAnnouncement(Team team, String message){
        team.players.forEach(player -> {
            player.sendMessage(message);
        });
    }

    /**
     *  Add player to team
     *  @param player {@code Player}
     *  @param team {@code Team}
     */
    public void addPlayerToTeam(Player player, Team team){
        team.addPlayer(player);
        players_and_teams.put(player, team);
        player.sendMessage(ChatColor.GRAY+"Вы присоединились к команде " +
                players_and_teams.get(player).chatColor+players_and_teams.get(player).teamName);
    }

    /**
     *  Add player from his team
     *  @param player {@code Player}
     */
    public void removePlayerFromTeam(Player player){
        try {
            players_and_teams.get(player).removePlayer(player);
            players_and_teams.remove(player, players_and_teams.get(player));
        } catch (NullPointerException e) {}
    }

    /**
     *  Build double[]
     *  @param location {@code String}
     *  @return double[]
     *  [0] X, [1] Y, [2] Z
     *  TODO add yaw & pitch
     */
    public double[] parseLocation(String location){
        double[] coords = {0.0, 0.0, 0.0};
        StringTokenizer st = new StringTokenizer(location.replace(',', ' '));
        while (st.hasMoreTokens()) {
            coords[0] = Double.parseDouble(st.nextToken());     // X
            coords[1] = Double.parseDouble(st.nextToken());     // Y
            coords[2] = Double.parseDouble(st.nextToken());     // Z
        }
        return coords;
    }

    /**
     *  Build Location
     *  Uses parseLocation
     *  @param world {@code World}
     *  @param position {@code String}
     *  @return Location
     *  TODO add yaw & pitch
     */
    public Location setupLocation(World world, String position) {
        double[] coords = parseLocation(position);
        Location location = new Location(world, coords[0], coords[1], coords[2]);
        return location;
    }

    /**
     *  Build resource material
     *  @param name {@code String}
     *  Supports [bricks, iron, gold, diamond, emerald, coal, quartz, redstone]
     *  @return Material
     *  TODO test lapis
     */
    Material setupMaterial(String name){
        List<String> variants = new ArrayList<String>(){
            {
                add("");
                add("_INGOT");
            }
        };
        for (String variant : variants){
            if (Material.getMaterial(name + variant) != null){
                return Material.getMaterial(name + variant);
            }
        }
        return null;
    }

    /**
     *  Build resource block material
     *  @param name {@code String}
     *  Supports [bricks, iron, gold, diamond, emerald, coal, quartz, redstone]
     *  @return Material
     *  TODO test lapis
     */
    Material setupBlock(String name){
        List<String> variants = new ArrayList<String>(){
            {
                add("_BLOCK");
            }
        };
        if (name.equals("BRICK")){
            return Material.TERRACOTTA;
        }
        for (String variant : variants){
            if (Material.getMaterial(name + variant) != null){
                return Material.getMaterial(name + variant);
            }
        }
        return null;
    }

    /*
     *  Checks is any team has no players
     *  If true
     *      delete it from List<Team> teams
     *      make announce
     *  TODO add sound effect
     */
    public void isTeamLost(){
        teams.forEach(team-> {
            if (team.players.isEmpty()){
               makeAnnouncement(ChatColor.GRAY + "Команда " +
                        team.chatColor + team.teamName +
                        (ChatColor.GRAY + " проиграла."));
                teams.remove(team);
                checkWinner();
            }
        });
        checkWinner();
    }

    /*
     *  Checks if List<Team> teams only one team left
     *  If true
     *      change Stage status
     *      make announce
     * TODO add sound effect
     */
    public void checkWinner(){
        if (teams.size() == 1){
            stage.inAftergame();
            makeAnnouncement(ChatColor.GREEN+"Победила команда " + teams.get(0).chatColor + teams.get(0).teamName + ChatColor.GREEN+"!");
        }
    }

    /**
     *  When player gets critical damage in Event
     *  @param player {@code Player}
     *  TODO add sound effect
     */
    public void killAndTp(Player player){
        player.playEffect(EntityEffect.HURT);
        player.setHealth(20);
        player.setFoodLevel(20);
        player.getInventory().clear();
        player.teleport(players_and_teams.get(player).spawnPoint);

        try {
            if (!players_and_teams.get(player).bedStanding) {
                player.sendMessage(ChatColor.RED + "Вы выбыли из игры.");
                makeAnnouncement(players_and_teams.get(player).chatColor + player.getName() + ChatColor.GRAY + " выбыл из игры.");
                removePlayerFromTeam(player);
                isTeamLost();
            }
        } catch (Exception e) {}
    }
}
