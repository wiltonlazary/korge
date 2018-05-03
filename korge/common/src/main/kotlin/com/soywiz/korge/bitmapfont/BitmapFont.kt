package com.soywiz.korge.bitmapfont

import com.soywiz.kds.IntMap
import com.soywiz.korag.AG
import com.soywiz.korge.plugin.KorgePlugin
import com.soywiz.korge.render.BatchBuilder2D
import com.soywiz.korge.render.Texture
import com.soywiz.korge.render.ensurePowerOfTwo
import com.soywiz.korge.render.readTexture
import com.soywiz.korge.resources.Path
import com.soywiz.korge.resources.ResourcesRoot
import com.soywiz.korge.resources.VPath
import com.soywiz.korge.view.BlendMode
import com.soywiz.korge.view.Views
import com.soywiz.korim.color.Colors
import com.soywiz.korim.font.BitmapFontGenerator
import com.soywiz.korinject.AsyncFactory
import com.soywiz.korinject.Optional
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.serialization.xml.get
import com.soywiz.korio.serialization.xml.readXml
import com.soywiz.korio.vfs.VfsFile
import com.soywiz.korma.Matrix2d

object BitmapFontPlugin : KorgePlugin() {
	suspend override fun register(views: Views) {
		views.injector
			.mapFactory {
				BitmapFontAsyncFactory(
					getOrNull(Path::class),
					getOrNull(VPath::class),
					getOrNull(FontDescriptor::class),
					get(ResourcesRoot::class),
					get(AG::class)
				)
			}
	}
}

//e: java.lang.UnsupportedOperationException: Class literal annotation arguments are not yet supported: Factory
//@AsyncFactoryClass(BitmapFontAsyncFactory::class)
class BitmapFont(
	val ag: AG,
	val fontSize: Int,
	val glyphs: IntMap<Glyph>,
	val kernings: IntMap<Kerning>
) {
	constructor(ag: AG, fontSize: Int, glyphs: Map<Int, Glyph>, kernings: Map<Int, Kerning>) : this(
		ag, fontSize, glyphs.toIntMap(), kernings.toIntMap()
	)

	class Kerning(
		val first: Int,
		val second: Int,
		val amount: Int
	) {
		companion object {
			fun buildKey(f: Int, s: Int) = f or (s shl 16)
		}
	}

	class Glyph(
		val id: Int,
		val texture: Texture,
		val xoffset: Int,
		val yoffset: Int,
		val xadvance: Int
	)

	val dummyGlyph by lazy { Glyph(-1, Texture(ag.dummyTexture, 1, 1), 0, 0, 0) }

	operator fun get(charCode: Int): Glyph = glyphs[charCode] ?: glyphs[32] ?: dummyGlyph
	operator fun get(char: Char): Glyph = this[char.toInt()]

	fun drawText(batch: BatchBuilder2D, textSize: Double, str: String, x: Int, y: Int, m: Matrix2d = Matrix2d(), colMul: Int = Colors.WHITE, colAdd: Int = 0x7f7f7f7f, blendMode: BlendMode = BlendMode.INHERIT, filtering: Boolean = true) {
		val m2 = m.clone()
		val scale = textSize / fontSize.toDouble()
		m2.pretranslate(x.toDouble(), y.toDouble())
		m2.prescale(scale, scale)
		var dx = 0
		var dy = 0
		for (n in str.indices) {
			val c1 = str[n].toInt()
			if (c1 == '\n'.toInt()) {
				dx = 0
				dy += fontSize
				continue
			}
			val c2 = str.getOrElse(n + 1) { ' ' }.toInt()
			val glyph = this[c1]
			val tex = glyph.texture
			batch.drawQuad(tex, (dx + glyph.xoffset).toFloat(), (dy + glyph.yoffset).toFloat(), m = m2, colorMul = colMul, colorAdd = colAdd, blendFactors = blendMode.factors, filtering = filtering)
			val kerningOffset = kernings[Kerning.buildKey(c1, c2)]?.amount ?: 0
			dx += glyph.xadvance + kerningOffset
		}
	}

	companion object {
		operator fun invoke(ag: AG, fontName: String, fontSize: Int, chars: String = BitmapFontGenerator.LATIN_ALL, mipmaps: Boolean = true): BitmapFont {
			return BitmapFontGenerator.generate(fontName, fontSize, chars).convert(ag, mipmaps = mipmaps)
		}
	}
}

fun BatchBuilder2D.drawText(font: BitmapFont, textSize: Double, str: String, x: Int, y: Int, m: Matrix2d = Matrix2d(), colMul: Int = Colors.WHITE, colAdd: Int = 0x7f7f7f7f, blendMode: BlendMode = BlendMode.INHERIT) {
	font.drawText(this, textSize, str, x, y, m, colMul, colAdd, blendMode)
}

suspend fun VfsFile.readBitmapFont(ag: AG): BitmapFont {
	val fntFile = this
	val xml = fntFile.readXml()
	val textures = hashMapOf<Int, Texture>()

	val fontSize = xml["info"].firstOrNull()?.int("size", 16) ?: 16

	for (page in xml["pages"]["page"]) {
		val id = page.int("id")
		val file = page.str("file")
		val texFile = fntFile.parent[file]
		val tex = texFile.readTexture(ag)
		textures[id] = tex
	}

	val texture = textures.values.first()

	val glyphs = xml["chars"]["char"].map {
		BitmapFont.Glyph(
			id = it.int("id"),
			texture = texture.slice(it.int("x"), it.int("y"), it.int("width"), it.int("height")),
			xoffset = it.int("xoffset"),
			yoffset = it.int("yoffset"),
			xadvance = it.int("xadvance")
		)
	}

	val kernings = xml["kernings"]["kerning"].map {
		BitmapFont.Kerning(
			first = it.int("first"),
			second = it.int("second"),
			amount = it.int("amount")
		)
	}

	return BitmapFont(
		ag = ag,
		fontSize = fontSize,
		glyphs = glyphs.map { it.id to it }.toMap().toIntMap(),
		kernings = kernings.map { BitmapFont.Kerning.buildKey(it.first, it.second) to it }.toMap().toIntMap()
	)
}

// @TODO: Move to kds
fun <T> Map<Int, T>.toIntMap(): IntMap<T> {
	val out = IntMap<T>()
	for ((k, v) in this) out.set(k, v)
	return out
}

annotation class FontDescriptor(val face: String, val size: Int, val chars: String = "0123456789")

class BitmapFontAsyncFactory(
	@Optional private val path: Path?,
	@Optional private val vpath: VPath?,
	@Optional private val descriptor: FontDescriptor?,
	private val resourcesRoot: ResourcesRoot,
	private val ag: AG
) : AsyncFactory<BitmapFont> {
	override suspend fun create() = if (path != null) {
		resourcesRoot[path].readBitmapFont(ag)
	} else if (vpath != null) {
		resourcesRoot[vpath.path].readBitmapFont(ag)
	} else if (descriptor != null) {
		com.soywiz.korim.font.BitmapFontGenerator.generate(descriptor.face, descriptor.size, descriptor.chars).convert(ag)
	} else {
		invalidOp("BitmapFont injection requires @Path or @FontDescriptor annotations")
	}
}

fun com.soywiz.korim.font.BitmapFont.toKorge(views: Views, mipmaps: Boolean = true): BitmapFont = convert(views.ag, mipmaps)

fun com.soywiz.korim.font.BitmapFont.convert(ag: AG, mipmaps: Boolean = true): BitmapFont {
	val font = this

	val atlasBitmap = if (mipmaps) font.atlas.ensurePowerOfTwo() else font.atlas

	val tex = Texture(ag.createTexture().upload(atlasBitmap, mipmaps), atlasBitmap.width, atlasBitmap.height)
	val glyphs = arrayListOf<BitmapFont.Glyph>()
	for (info in font.glyphInfos) {
		val bounds = info.bounds
		val texSlice = tex.slice(bounds.x, bounds.y, bounds.width, bounds.height)
		glyphs += BitmapFont.Glyph(info.id, texSlice, 0, 0, info.advance)
	}
	return BitmapFont(ag, font.size, glyphs.map { it.id to it }.toMap().toIntMap(), IntMap())
}
