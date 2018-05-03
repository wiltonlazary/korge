package com.soywiz.korge.sample

import com.soywiz.korge.Korge
import com.soywiz.korge.atlas.Atlas
import com.soywiz.korge.bitmapfont.BitmapFont
import com.soywiz.korge.ext.particle.ParticleEmitter
import com.soywiz.korge.ext.particle.attachParticleAndWait
import com.soywiz.korge.input.onClick
import com.soywiz.korge.input.onOut
import com.soywiz.korge.input.onOver
import com.soywiz.korge.resources.Path
import com.soywiz.korge.scene.Module
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.time.milliseconds
import com.soywiz.korge.time.seconds
import com.soywiz.korge.time.sleep
import com.soywiz.korge.time.waitFrame
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.Text
import com.soywiz.korge.view.image
import com.soywiz.korge.view.text
import com.soywiz.korim.color.Colors
import com.soywiz.korio.async.go
import com.soywiz.korio.async.sleep
import com.soywiz.korma.random.get
import java.util.*

object Sample2 : Module() {
	@JvmStatic fun main(args: Array<String>) = Korge(Sample2)

	override val mainScene: Class<out Scene> = MainScene::class.java

	class MainScene(
		@Path("font/font.fnt") val font: BitmapFont,
		@Path("spriter-sample1/demo.json") val atlas1: Atlas,
		@Path("particle/particle.pex") val emitter: ParticleEmitter
	) : Scene() {
		val random = Random()

		suspend override fun sceneInit(sceneView: Container) {
			val rect = views.solidRect(100, 100, Colors.RED)
			//val text = views.text("HELLO", color = Colors.RED, font = font)
			val text = views.text("HELLO", color = Colors.RED)
			sceneView += rect
			sceneView += text

			val particles = emitter.create(200.0, 200.0)
			sceneView += particles
			particles.speed = 2.0

			val image = views.image(atlas1.textures["arms/forearm_jump_0.png"]!!.texture).apply {
				x = 100.0
				y = 100.0
				alpha = 0.7
			}

			image.onOver {
				image.alpha = 1.0
			}

			image.onOut {
				image.alpha = 0.7
			}

			sceneView += image

			for (n in 0 until 10) {
				go {
					sceneView.sleep(random[100, 400].milliseconds)
					while (true) {
						sceneView.attachParticleAndWait(
							emitter,
							random[100.0, views.virtualWidth.toDouble()],
							random[100.0, views.virtualHeight.toDouble()],
							time = random[300, 500], speed = random[1.0, 2.0]
						)
						sceneView.sleep(random[0, 50].milliseconds)
						//println("done!")
					}
				}
			}

			go {
				while (true) {
					//println(views.nativeMouseX)
					particles.emitterPos.x = particles.parent?.localMouseX ?: 0.0
					particles.emitterPos.y = particles.parent?.localMouseY ?: 0.0
					particles.emitting = (views.input.mouseButtons == 0)
					particles.waitFrame()
					//println(":::")
				}
			}

			go {
				particles.waitComplete()
				println("No more particles!")
			}

			//particles.y = views.nativeMouseY

			go {
				class TextHolder(val text: Text) {
					var value: Int
						get() = this.text.text.toIntOrNull() ?: 0
						set(value) {
							this.text.text = "$value"
						}
				}

				val textHolder = TextHolder(text)
				text.tween(textHolder::value[1000], time = 1.seconds)
			}

			rect.onClick {
				println("click!")
			}

			go {
				//rect.tween((SolidRect::color..Colors.BLUE).color(), time = 1000)
				rect.tween(
					(rect::colorMul[Colors.BLUE]).color(),
					(rect::x[200]).delay(200.milliseconds).duration(600.milliseconds),
					rect::y[100],
					rect::width[200],
					rect::height[90],
					rect::rotationDegrees[90],
					time = 1.seconds,
					easing = Easings.EASE_IN_OUT_QUAD
				)
				//println(rect.color)
			}
		}
	}
}
