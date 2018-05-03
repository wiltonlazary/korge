package com.soywiz.korge.ext.particle

import com.soywiz.korag.AG
import com.soywiz.korge.render.Texture
import com.soywiz.korge.render.readTexture
import com.soywiz.korge.resources.Path
import com.soywiz.korge.resources.ResourcesRoot
import com.soywiz.korge.view.BlendMode
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.Views
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.color.RGBAf
import com.soywiz.korinject.AsyncFactory
import com.soywiz.korio.JvmField
import com.soywiz.korio.math.toRadians
import com.soywiz.korio.serialization.xml.readXml
import com.soywiz.korio.vfs.VfsFile
import com.soywiz.korma.geom.Point2d
import com.soywiz.korma.random.MtRand
import com.soywiz.korma.random.nextDouble
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

//e: java.lang.UnsupportedOperationException: Class literal annotation arguments are not yet supported: Factory
//@AsyncFactoryClass(ParticleEmitter.Factory::class)
class ParticleEmitter(val views: Views) {
	enum class Type { GRAVITY, RADIAL }

	var texture: Texture? = null
	var sourcePosition = Point2d()
	var sourcePositionVariance = Point2d()
	var speed = 0.0
	var speedVariance = 0.0
	var lifeSpan = 0.0
	var lifespanVariance = 0.0
	var angle = 0.0
	var angleVariance = 0.0
	var gravity = Point2d()
	var radialAcceleration = 0.0
	var tangentialAcceleration = 0.0
	var radialAccelVariance = 0.0
	var tangentialAccelVariance = 0.0
	var startColor = RGBAf(1f, 1f, 1f, 1f)
	var startColorVariance = RGBAf(0f, 0f, 0f, 0f)
	var endColor = RGBAf(1f, 1f, 1f, 1f)
	var endColorVariance = RGBAf(0f, 0f, 0f, 0f)
	var maxParticles = 0
	var startSize = 0.0
	var startSizeVariance = 0.0
	var endSize = 0.0
	var endSizeVariance = 0.0
	var duration = 0.0
	var emitterType = Type.GRAVITY
	var maxRadius = 0.0
	var maxRadiusVariance = 0.0
	var minRadius = 0.0
	var minRadiusVariance = 0.0
	var rotatePerSecond = 0.0
	var rotatePerSecondVariance = 0.0
	var blendFactors = BlendMode.NORMAL.factors
	var rotationStart = 0.0
	var rotationStartVariance = 0.0
	var rotationEnd = 0.0
	var rotationEndVariance = 0.0

	fun create(x: Double = 0.0, y: Double = 0.0, time: Int = Int.MAX_VALUE): ParticleEmitterView = ParticleEmitterView(this, Point2d(x, y)).apply {
		this.timeUntilStop = time
	}

	suspend fun load(file: VfsFile): ParticleEmitter = this.apply {
		val particleXml = file.readXml()

		var blendFuncSource = AG.BlendFactor.ONE
		var blendFuncDestination = AG.BlendFactor.ONE

		for (item in particleXml.allChildrenNoComments) {
			fun point() = Point2d(item.double("x"), item.double("y"))
			fun scalar() = item.double("value")
			fun blendFactor() = when (scalar().toInt()) {
				0 -> AG.BlendFactor.ZERO
				1 -> AG.BlendFactor.ONE
				0x300 -> AG.BlendFactor.SOURCE_COLOR
				0x301 -> AG.BlendFactor.ONE_MINUS_SOURCE_COLOR
				0x302 -> AG.BlendFactor.SOURCE_ALPHA
				0x303 -> AG.BlendFactor.ONE_MINUS_SOURCE_ALPHA
				0x304 -> AG.BlendFactor.DESTINATION_ALPHA
				0x305 -> AG.BlendFactor.ONE_MINUS_DESTINATION_ALPHA
				0x306 -> AG.BlendFactor.DESTINATION_COLOR
				0x307 -> AG.BlendFactor.ONE_MINUS_DESTINATION_COLOR
				else -> AG.BlendFactor.ONE
			}

			fun angle() = toRadians(item.double("value"))
			fun color(): RGBAf = RGBAf(item.double("red"), item.double("green"), item.double("blue"), item.double("alpha"))

			when (item.name.toLowerCase()) {
				"texture" -> texture = file.parent[item.str("name")].readTexture(views.ag)
				"sourceposition" -> sourcePosition = point()
				"sourcepositionvariance" -> sourcePositionVariance = point()
				"speed" -> speed = scalar()
				"speedvariance" -> speedVariance = scalar()
				"particlelifespan" -> lifeSpan = scalar()
				"particlelifespanvariance" -> lifespanVariance = scalar()
				"angle" -> angle = angle()
				"anglevariance" -> angleVariance = angle()
				"gravity" -> gravity = point()
				"radialacceleration" -> radialAcceleration = scalar()
				"tangentialacceleration" -> tangentialAcceleration = scalar()
				"radialaccelvariance" -> radialAccelVariance = scalar()
				"tangentialaccelvariance" -> tangentialAccelVariance = scalar()
				"startcolor" -> startColor = color()
				"startcolorvariance" -> startColorVariance = color()
				"finishcolor" -> endColor = color()
				"finishcolorvariance" -> endColorVariance = color()
				"maxparticles" -> maxParticles = scalar().toInt()
				"startparticlesize" -> startSize = scalar()
				"startparticlesizevariance" -> startSizeVariance = scalar()
				"finishparticlesize" -> endSize = scalar()
				"finishparticlesizevariance" -> endSizeVariance = scalar()
				"duration" -> duration = scalar()
				"emittertype" -> emitterType = when (scalar().toInt()) { 0 -> Type.GRAVITY; 1 -> Type.RADIAL; else -> Type.GRAVITY; }
				"maxradius" -> maxRadius = scalar()
				"maxradiusvariance" -> maxRadiusVariance = scalar()
				"minradius" -> minRadius = scalar()
				"minradiusvariance" -> minRadiusVariance = scalar()
				"rotatepersecond" -> rotatePerSecond = scalar()
				"rotatepersecondvariance" -> rotatePerSecondVariance = scalar()
				"blendfuncsource" -> blendFuncSource = blendFactor()
				"blendfuncdestination" -> blendFuncDestination = blendFactor()
				"rotationstart" -> rotationStart = angle()
				"rotationstartvariance" -> rotationStartVariance = angle()
				"rotationend" -> rotationEnd = angle()
				"rotationendvariance" -> rotationEndVariance = angle()
			}
		}

		blendFactors = AG.Blending(blendFuncSource, blendFuncDestination)
	}

	data class Particle(
		@JvmField var x: Double = 0.0,
		@JvmField var y: Double = 0.0,
		@JvmField var scale: Double = 1.0,
		@JvmField var rotation: Double = 0.0,
		@JvmField var currentTime: Double = 0.0,
		@JvmField var totalTime: Double = 0.0,

		//@JvmField val colorArgb: RGBAf = RGBAf(),
		//@JvmField val colorArgbDelta: RGBAf = RGBAf(),

		@JvmField var colorR: Double = 1.0,
		@JvmField var colorG: Double = 1.0,
		@JvmField var colorB: Double = 1.0,
		@JvmField var colorA: Double = 1.0,

		@JvmField var colorRdelta: Double = 0.0,
		@JvmField var colorGdelta: Double = 0.0,
		@JvmField var colorBdelta: Double = 0.0,
		@JvmField var colorAdelta: Double = 0.0,

		@JvmField var startX: Double = 0.0,
		@JvmField var startY: Double = 0.0,
		@JvmField var velocityX: Double = 0.0,
		@JvmField var velocityY: Double = 0.0,
		@JvmField var radialAcceleration: Double = 0.0,
		@JvmField var tangentialAcceleration: Double = 0.0,
		@JvmField var emitRadius: Double = 0.0,
		@JvmField var emitRadiusDelta: Double = 0.0,
		@JvmField var emitRotation: Double = 0.0,
		@JvmField var emitRotationDelta: Double = 0.0,
		@JvmField var rotationDelta: Double = 0.0,
		@JvmField var scaleDelta: Double = 0.0
	) {
		val colorInt: Int get() = RGBA.packf(colorR.toFloat(), colorG.toFloat(), colorB.toFloat(), colorA.toFloat())
		val alive: Boolean get() = this.currentTime < this.totalTime
	}

	class Simulator(private val emitter: ParticleEmitter, var emitterPos: Point2d = Point2d()) {
		val random = MtRand()
		var totalElapsedTime = 0
		var timeUntilStop = Int.MAX_VALUE
		var emitting = true
		val textureWidth = emitter.texture?.width ?: 16
		val particles = (0 until emitter.maxParticles).map { init(Particle()) }
		val aliveCount: Int get() = particles.count { it.alive }
		val anyAlive: Boolean get() = aliveCount > 0

		private fun randomVariance(base: Double, variance: Double): Double {
			return base + variance * (random.nextDouble() * 2.0 - 1.0)
		}

		fun init(particle: Particle): Particle {
			val lifespan = randomVariance(emitter.lifeSpan, emitter.lifespanVariance)

			particle.currentTime = 0.0
			particle.totalTime = max(0.0, lifespan)

			val emitterX = emitterPos.x
			val emitterY = emitterPos.y

			particle.x = randomVariance(emitterX, emitter.sourcePositionVariance.x)
			particle.y = randomVariance(emitterY, emitter.sourcePositionVariance.y)
			particle.startX = emitterX
			particle.startY = emitterY

			val angle = randomVariance(emitter.angle, emitter.angleVariance)
			val speed = randomVariance(emitter.speed, emitter.speedVariance)
			particle.velocityX = speed * cos(angle)
			particle.velocityY = speed * sin(angle)

			val startRadius = randomVariance(emitter.maxRadius, emitter.maxRadiusVariance)
			val endRadius = randomVariance(emitter.minRadius, emitter.minRadiusVariance)
			particle.emitRadius = startRadius
			particle.emitRadiusDelta = (endRadius - startRadius) / lifespan
			particle.emitRotation = randomVariance(emitter.angle, emitter.angleVariance)
			particle.emitRotationDelta = randomVariance(emitter.rotatePerSecond, emitter.rotatePerSecondVariance)
			particle.radialAcceleration = randomVariance(emitter.radialAcceleration, emitter.radialAccelVariance)
			particle.tangentialAcceleration = randomVariance(emitter.tangentialAcceleration, emitter.tangentialAccelVariance)

			val startSize = max(0.1, randomVariance(emitter.startSize, emitter.startSizeVariance))
			val endSize = max(0.1, randomVariance(emitter.endSize, emitter.endSizeVariance))
			particle.scale = startSize / textureWidth
			particle.scaleDelta = ((endSize - startSize) / lifespan) / textureWidth

			particle.colorR = randomVariance(emitter.startColor.rd, emitter.startColorVariance.rd)
			particle.colorG = randomVariance(emitter.startColor.gd, emitter.startColorVariance.gd)
			particle.colorB = randomVariance(emitter.startColor.bd, emitter.startColorVariance.bd)
			particle.colorA = randomVariance(emitter.startColor.ad, emitter.startColorVariance.ad)

			val endColorR = randomVariance(emitter.endColor.rd, emitter.endColorVariance.rd)
			val endColorG = randomVariance(emitter.endColor.gd, emitter.endColorVariance.gd)
			val endColorB = randomVariance(emitter.endColor.bd, emitter.endColorVariance.bd)
			val endColorA = randomVariance(emitter.endColor.ad, emitter.endColorVariance.ad)

			particle.colorRdelta = ((endColorR - particle.colorR) / lifespan)
			particle.colorGdelta = ((endColorG - particle.colorG) / lifespan)
			particle.colorBdelta = ((endColorB - particle.colorB) / lifespan)
			particle.colorAdelta = ((endColorA - particle.colorA) / lifespan)

			val startRotation = randomVariance(emitter.rotationStart, emitter.rotationStartVariance)
			val endRotation = randomVariance(emitter.rotationEnd, emitter.rotationEndVariance)

			particle.rotation = startRotation
			particle.rotationDelta = (endRotation - startRotation) / lifespan

			return particle
		}

		fun advance(particle: Particle, _elapsedTime: Double) {
			val restTime = particle.totalTime - particle.currentTime
			val elapsedTime = if (restTime > _elapsedTime) _elapsedTime else restTime
			particle.currentTime += elapsedTime

			when (emitter.emitterType) {
				ParticleEmitter.Type.RADIAL -> {
					particle.emitRotation += particle.emitRotationDelta * elapsedTime
					particle.emitRadius += particle.emitRadiusDelta * elapsedTime
					particle.x = emitter.sourcePosition.x - cos(particle.emitRotation) * particle.emitRadius
					particle.y = emitter.sourcePosition.y - sin(particle.emitRotation) * particle.emitRadius
				}
				ParticleEmitter.Type.GRAVITY -> {
					val distanceX = particle.x - particle.startX
					val distanceY = particle.y - particle.startY
					val distanceScalar = max(0.01, sqrt(distanceX * distanceX + distanceY * distanceY))
					var radialX = distanceX / distanceScalar
					var radialY = distanceY / distanceScalar
					var tangentialX = radialX
					var tangentialY = radialY

					radialX *= particle.radialAcceleration
					radialY *= particle.radialAcceleration

					val newY = tangentialX
					tangentialX = -tangentialY * particle.tangentialAcceleration
					tangentialY = newY * particle.tangentialAcceleration

					particle.velocityX += elapsedTime * (emitter.gravity.x + radialX + tangentialX)
					particle.velocityY += elapsedTime * (emitter.gravity.y + radialY + tangentialY)
					particle.x += particle.velocityX * elapsedTime
					particle.y += particle.velocityY * elapsedTime
				}
			}

			particle.scale += particle.scaleDelta * elapsedTime
			particle.rotation += particle.rotationDelta * elapsedTime

			particle.colorR += (particle.colorRdelta * elapsedTime).toFloat()
			particle.colorG += (particle.colorGdelta * elapsedTime).toFloat()
			particle.colorB += (particle.colorBdelta * elapsedTime).toFloat()
			particle.colorA += (particle.colorAdelta * elapsedTime).toFloat()

			if (!particle.alive && emitting) init(particle)
		}

		fun simulate(time: Double) {
			totalElapsedTime += (time * 1000.0).toInt()

			if (totalElapsedTime >= timeUntilStop) {
				emitting = false
			}

			for (p in particles) advance(p, time)
		}
	}

	class Factory(
		val path: Path,
		val views: Views,
		val resourcesRoot: ResourcesRoot
	) : AsyncFactory<ParticleEmitter> {
		suspend override fun create(): ParticleEmitter = resourcesRoot[path].readParticle(views)
	}
}

suspend fun VfsFile.readParticle(views: Views): ParticleEmitter = ParticleEmitter(views).load(this)

suspend fun Container.attachParticleAndWait(particle: ParticleEmitter, x: Double, y: Double, time: Int = 1000, speed: Double = 1.0) {
	val p = particle.create(x, y, time)
	p.speed = speed
	this += p
	p.waitComplete()
	this -= p
}
