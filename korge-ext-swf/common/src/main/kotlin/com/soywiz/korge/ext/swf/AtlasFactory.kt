package com.soywiz.korge.ext.swf

import com.soywiz.korge.render.TextureWithBitmapSlice
import com.soywiz.korge.view.Views
import com.soywiz.korge.view.texture
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.slice
import com.soywiz.kds.Extra
import com.soywiz.korio.util.nextAlignedTo
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.Size
import com.soywiz.korma.geom.binpack.BinPacker
import com.soywiz.korma.numeric.nextPowerOfTwo

data class BitmapWithScale(val bitmap: Bitmap, val scale: Double, val bounds: Rectangle) : Extra by Extra.Mixin() {
	val width: Int = bitmap.width
	val height: Int = bitmap.height
}

/*
suspend fun List<BitmapWithScale>.toAtlas(views: Views, mipmaps: Boolean): List<TextureWithBitmapSlice> {
	return this.map {
		TextureWithBitmapSlice(views.texture(it.bitmap), it.bitmap.slice(RectangleInt(0, 0, it.bitmap.width, it.bitmap.height)), it.scale)
	}
}
*/


suspend fun <T> Map<T, BitmapWithScale>.toAtlas(views: Views, maxTextureSide:Int, mipmaps: Boolean): Map<T, TextureWithBitmapSlice> {
	//val packs = BinPacker.packSeveral(2048.0, 2048.0, this) { Size(it.width + 4, it.height + 4) }
	val values = this.values.toList()
	val packs = BinPacker.packSeveral(maxTextureSide.toDouble(), maxTextureSide.toDouble(), values) { Size((it.width + 4).nextAlignedTo(4), (it.height + 4).nextAlignedTo(4)) }
	val bitmapsToTextures = hashMapOf<BitmapWithScale, TextureWithBitmapSlice>()
	val premult = this.values.firstOrNull()?.bitmap?.premult ?: true
	for (pack in packs) {
		val width = pack.width.toInt().nextPowerOfTwo
		val height = pack.height.toInt().nextPowerOfTwo
		val bmp = Bitmap32(width, height, premult = premult)
		for ((ibmp, rect) in pack.items) {
			val dx = rect.x.toInt() + 2
			val dy = rect.y.toInt() + 2

			bmp.put(ibmp.bitmap.toBMP32(), dx, dy)

			//val dwidth = rect.width.toInt()
			//val dheight = rect.height.toInt()
			//val dxr = dx + width - 1
			//Bitmap32.copyRect(bmp, dx, dy, bmp, dx - 1, dy, 1, dheight)
			//Bitmap32.copyRect(bmp, dx, dy, bmp, dx - 2, dy, 1, dheight)
			//Bitmap32.copyRect(bmp, dxr, dy, bmp, dxr + 1, dy, 1, dheight)
			//Bitmap32.copyRect(bmp, dxr, dy, bmp, dxr + 2, dy, 1, dheight)
		}

		val texture = views.texture(bmp, mipmaps = mipmaps)

		for ((ibmp, rect) in pack.items) {
			val rect2 = Rectangle(rect.x + 2, rect.y + 2, rect.width - 4, rect.height - 4)
			bitmapsToTextures[ibmp] = TextureWithBitmapSlice(
				texture = texture.slice(rect2),
				bitmapSlice = bmp.slice(rect2.toInt()),
				scale = ibmp.scale,
				bounds = ibmp.bounds
			)
		}
	}
	return this.mapValues { bitmapsToTextures[it.value]!! }
}

