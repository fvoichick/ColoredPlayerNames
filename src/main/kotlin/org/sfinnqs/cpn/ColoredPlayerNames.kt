/**
 * ColoredPlayerNames - A Bukkit plugin for changing name colors
 * Copyright (C) 2019 sfinnqs
 *
 * This file is part of ColoredPlayerNames.
 *
 * ColoredPlayerNames is free software; you can redistribute it and/or modify it
 * under the terms of version 3 of the GNU General Public License as published
 * by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, see <https://www.gnu.org/licenses>.
 */
package org.sfinnqs.cpn

import net.gravitydevelopment.updater.Updater
import net.gravitydevelopment.updater.Updater.UpdateType.DEFAULT
import org.bstats.bukkit.Metrics
import org.bukkit.Bukkit
import org.bukkit.command.TabExecutor
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Logger

/**
 * The ColoredPlayerNames plugin. This is the main entry point into this plugin's functionality.
 *
 * @author Finn
 */
class ColoredPlayerNames : JavaPlugin() {

    /** The configuration, reset every time the configuration is reloaded. */
    private var privateConfig: CpnConfig? = null
    /** An internally accessible configuration, drawing from [privateConfig] */
    internal val cpnConfig: CpnConfig
        get() = privateConfig ?: reload().first
    /** A data structure that keeps track of assigned colors, reset when the config is reloaded */
    private var privateColors: PlayerColors? = null
    /** An internally-accessible player colors object, drawing from [privateColors] */
    internal val playerColors: PlayerColors
        get() = privateColors ?: reload().second


    /**
     * When this plugin is enabled, the configuration is loaded and the auto-updater is run if
     * enabled. Listener events are registered, command executors are set up, and every player
     * currently online is assigned a random color. Also, bStats is enabled here.
     */
    override fun onEnable() {

        privateLogger = logger

        reload()
        if (cpnConfig.autoUpdate)
            Updater(this, ID, file, DEFAULT, true)

        server.pluginManager.registerEvents(CpnListener(this), this)

        setupCommand("changecolor", ChangeColorExecutor(this))
        setupCommand("coloredplayernames", CpnExecutor(this))

        Metrics(this)

    }

    /**
     * Uncolors all players, reloads the configuration, then re-colors all players.
     */
    @Synchronized
    fun reload(): Pair<CpnConfig, PlayerColors> {
        uncolorAll()
        saveDefaultConfig()
        reloadConfig()
        val newConfig = CpnConfig(this)
        privateConfig = newConfig
        newConfig.writeToFile()
        val scoreboardManager = server.scoreboardManager
        val newColors = if (scoreboardManager != null && newConfig.scoreboard) {
            PlayerColors(newConfig, scoreboardManager.mainScoreboard)
        } else {
            PlayerColors(newConfig)
        }
        privateColors = newColors

        for (player in server.onlinePlayers)
            if (player.hasPermission("coloredplayernames.color"))
                playerColors.changeColor(player)
            else
                playerColors[player] = null

        return newConfig to newColors
    }

    /**
     * When this plugin is disabled, all players are uncolored so that they are not on any team.
     */
    override fun onDisable() = uncolorAll()

    /**
     * Ensures that a command with the given [name] (or alias) exists, and then assigns the
     * [executor] to be both the command executor and the tab completer.
     */
    private fun setupCommand(name: String, executor: TabExecutor) {
        val command = getCommand(name)
        if (command == null) {
            logger.severe {
                "Command not found: $name"
            }
        } else {
            command.setExecutor(executor)
            command.tabCompleter = executor
        }
    }

    /**
     * Sets each player's color to null. This removes each player from their team in the scoreboard.
     */
    private fun uncolorAll() = server.onlinePlayers.forEach { privateColors?.set(it, null) }

    companion object {
        /** A private logger set in [onEnable] and used by the [logger] getter */
        private var privateLogger: Logger? = null
        /** This logger is the logger for this plugin if available, or the general Bukkit one. */
        val logger
            get() = privateLogger ?: Bukkit.getLogger()

        /** The ID of this plugin on BukkitDev, used by the updater. */
        private const val ID = 80947
    }

}
