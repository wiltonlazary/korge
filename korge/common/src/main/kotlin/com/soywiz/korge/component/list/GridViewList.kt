package com.soywiz.korge.component.list

import com.soywiz.korge.view.Container
import com.soywiz.korge.view.View

class GridViewList(
	val row0: View,
	val row1: View,
	val cellSelector: (View) -> Pair<View, View>,
	val initialRows: Int,
	val initialColumns: Int,
	val container: Container = row0.parent!!
) {
	private val rowsData = arrayListOf<ViewList>()
	var columns: Int = initialColumns
		set(value) {
			field = value
			update()
		}
	var rows: Int = initialRows
		set(value) {
			field = value
			update()
		}

	private fun addItem() {
		val n = container.children.size
		val item = row0.clone()
		container += item
		item.setMatrixInterpolated(n.toDouble(), row0.localMatrix, row1.localMatrix)
		val select = cellSelector(item)
		rowsData += ViewList(select.first, select.second, columns)
	}

	private fun removeLastItem() {
		container.children.lastOrNull()?.removeFromParent()
		rowsData.removeAt(rowsData.size - 1)
	}

	fun update() {
		while (rowsData.size < rows) addItem()
		while (rowsData.size > rows) removeLastItem()
		for (rowData in rowsData) rowData.length = columns
	}

	init {
		container.removeChildren()
		update()
	}

	val length: Int get() = columns * rows

	operator fun get(row: Int): ViewList = this.rowsData[row]
	operator fun get(row: Int, column: Int): View? = this[row][column]
}
