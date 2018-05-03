package com.soywiz.korge.input.component

import com.soywiz.korge.input.*
import com.soywiz.korge.render.Texture
import com.soywiz.korge.tests.ViewsForTesting
import com.soywiz.korge.view.image
import org.junit.Test
import kotlin.test.assertEquals

class MouseComponentTest : ViewsForTesting() {
	@Test
	fun name() = syncTest {
		val log = arrayListOf<String>()
		val tex = Texture(ag.createTexture(), 16, 16)
		val image = views.image(tex)
		views.stage += image

		image.onOver { log += "onOver" }
		image.onOut { log += "onOut" }
		image.onClick { log += "onClick" }
		image.onDown { log += "onDown" }
		image.onUp { log += "onUp" }
		image.onMove { log += "onMove" }

		input.mouse.setTo(8.0, 8.0); views.update(0); this.step(0)
		assertEquals("onOver", log.joinToString(","))
		input.mouse.setTo(20.0, 20.0); views.update(0); this.step(0)
		assertEquals("onOver,onOut", log.joinToString(","))
		input.mouse.setTo(8.0, 8.0); input.mouseButtons = 1; views.update(0); this.step(0)
		assertEquals("onOver,onOut,onOver,onDown", log.joinToString(","))
		input.mouse.setTo(10.0, 10.0); views.update(0); this.step(0)
		assertEquals("onOver,onOut,onOver,onDown,onMove", log.joinToString(","))
	}
}
