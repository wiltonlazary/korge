package com.soywiz.korge.service.storage

import com.soywiz.korio.serialization.Mapper
import com.soywiz.korio.serialization.json.Json
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

class StorageItem<T : Any>(val storage: IStorage, val clazz: KClass<T>, val key: String, val gen: () -> T) {
	var value: T
		set(value) = run { storage[key] = Json.encodeUntyped(Mapper.toUntyped(clazz, value)) }
		get () {
			if (key !in storage) storage[key] = Json.encodeUntyped(Mapper.toUntyped(clazz, gen()))
			return Json.decodeToType(storage[key], clazz)
		}

	fun remove() = storage.remove(key)

	inline operator fun getValue(thisRef: Any, property: KProperty<*>): T = value
	inline operator fun setValue(thisRef: Any, property: KProperty<*>, value: T): Unit = run { this.value = value }
}

inline fun <reified T : Any> IStorage.item(key: String, noinline gen: () -> T) = StorageItem(this, T::class, key, gen)
