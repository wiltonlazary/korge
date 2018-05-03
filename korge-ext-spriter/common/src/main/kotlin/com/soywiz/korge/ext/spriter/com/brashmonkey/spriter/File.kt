package com.soywiz.korge.ext.spriter.com.brashmonkey.spriter

/**
 * Represents a file in a Spriter SCML file.
 * A file has an [.id], a [.name].
 * A [.size] and a [.pivot] point, i.e. origin of an image do not have to be set since a file can be a sound file.
 * @author Trixt0r
 */
class File(val id: Int, val name: String, val size: Dimension?, val pivot: Point?) {
	companion object {
		val DUMMY = File(-1, "", Dimension(0f, 0f), Point(0f, 0f))
	}

	/**
	 * Returns whether this file is a sprite, i.e. an image which is going to be animated, or not.
	 * @return whether this file is a sprite or not.
	 */
	val isSprite: Boolean get() = pivot != null && size != null

	override fun toString(): String = "${this::class}|[id: $id, name: $name, size: $size, pivot: $pivot"

}
