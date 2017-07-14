package com.ickshonpe.tetris.desktop

import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.ickshonpe.tetris.TetrisGame

object DesktopLauncher {
    @JvmStatic fun main(arg: Array<String>) {
        val config = LwjglApplicationConfiguration()
        config.width = 400
        config.height = 800
        LwjglApplication(TetrisGame(), config)        
    }
}
