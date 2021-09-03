package me.arasple.mc.trchat.internal.service

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import me.arasple.mc.trchat.TrChat
import me.arasple.mc.trchat.api.TrChatFiles
import org.bukkit.event.player.PlayerJoinEvent
import taboolib.common.LifeCycle
import taboolib.common.env.DependencyDownloader.readFully
import taboolib.common.platform.*
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.*
import taboolib.module.lang.sendLang
import taboolib.platform.util.sendLang
import java.io.BufferedInputStream
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * @author Arasple
 * @date 2019/11/29 21:04
 */
@PlatformSide([Platform.BUKKIT])
@SkipTo(LifeCycle.INIT)
object Updater {

    private var api_url = "https://api.github.com/repos/FlickerProjects/$pluginId/releases/latest"
    private val notified = mutableListOf<UUID>()
    private var notify = false
    private var current_version = pluginVersion.split("-")[0].toDoubleOrNull() ?: -1.0
    private var latest_Version = -1.0

    @Awake(LifeCycle.INIT)
    fun init() {
        if (current_version < 0) {
            console().sendLang("Error-Version")
            disablePlugin()
            return
        }

        if (TrChatFiles.settings.getBoolean("GENERAL.CHECK-UPDATE", true)) {
            submit(delay = 20, period = 10 * 60 * 20, async = true) {
                grabInfo()
            }
        }
    }

    private fun notifyVersion(information: String) {
        if (latest_Version > current_version) {
            console().sendLang("Plugin-Updater-Header", current_version, latest_Version)
            console().sendMessage(information)
            console().sendLang("Plugin-Updater-Footer")
            if (latest_Version - current_version >= 0.2) {
                console().sendLang("Plugin-Updater-Too-Old")
            }
        } else {
            if (!notify) {
                notify = true
                if (current_version > latest_Version) {
                    console().sendLang("Plugin-Updater-Dev")
                } else {
                    console().sendLang("Plugin-Updater-Latest")
                }
            }
        }
    }

    private fun grabInfo() {
        if (latest_Version > 0) {
            return
        }
        kotlin.runCatching {
            URL(api_url).openStream().use { inputStream ->
                BufferedInputStream(inputStream).use { bufferedInputStream ->
                    val read = readFully(bufferedInputStream, StandardCharsets.UTF_8)
                    val json = JsonParser().parse(read) as JsonObject
                    val latestVersion = json["tag_name"].asDouble
                    latest_Version = latestVersion
                    notifyVersion(json["body"].asString)
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onJoin(e: PlayerJoinEvent) {
        val player = e.player
        if (player.hasPermission("trmenu.admin") && latest_Version > current_version && !notified.contains(player.uniqueId)) {
            player.sendLang("Plugin-Updater-Old", latest_Version)
            notified.add(player.uniqueId)
        }
    }
}