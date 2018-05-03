package com.soywiz.korge.component.list

import com.soywiz.korge.view.Container
import com.soywiz.korge.view.View
import com.soywiz.korio.async.Signal

class ViewList(
	val view0: View,
	val view1: View,
	val initialCount: Int,
	val container: Container = view0.parent!!
) {
	data class ChangeEvent(val view: View, val index: Int)

	val onRemovedView = Signal<ChangeEvent>()
	val onAddedView = Signal<ChangeEvent>()
	val children get() = container.children

	init {
		container.removeChildren()
		for (n in 0 until initialCount) addItem()
	}

	private fun addItem() {
		val n = container.children.size
		val item = view0.clone()
		container += item
		item.setMatrixInterpolated(n.toDouble(), view0.localMatrix, view1.localMatrix)
		onAddedView(ChangeEvent(item, n))
	}

	private fun removeLastItem() {
		val lastIndex = container.children.size - 1
		val item = children[lastIndex]
		item.removeFromParent()
		onRemovedView(ChangeEvent(item, lastIndex))
	}

	var length: Int
		get() = container.children.size
		set(value) {
			while (value > length) addItem()
			while (value < length) removeLastItem()
		}

	operator fun get(index: Int) = container.children.getOrNull(index)
}
