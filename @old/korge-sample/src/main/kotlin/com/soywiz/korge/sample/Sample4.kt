package com.soywiz.korge.sample

import com.soywiz.korge.Korge
import com.soywiz.korge.animate.AnLibrary
import com.soywiz.korge.input.onClick
import com.soywiz.korge.resources.Path
import com.soywiz.korge.scene.Module
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.get
import com.soywiz.korge.view.setText
import com.soywiz.korim.color.RGBA
import com.soywiz.korma.geom.SizeInt

object Sample4 : Module() {
	@JvmStatic fun main(args: Array<String>) = Korge(this@Sample4, debug = true)

	override val size: SizeInt = SizeInt(560, 380)
	override val mainScene: Class<out Scene> = MainScene::class.java

	class MainScene(
		@Path("texts.swf") val lib: AnLibrary
	) : Scene() {
		suspend override fun sceneInit(sceneView: Container) {
			sceneView += lib.createMainTimeLine()
			sceneView.onClick {
				sceneView["helloWorld"].setText("COOL WORLD!")
			}
		}
	}
}
