/*
 * Copyright (c) 2020-2022 MangoPlex
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package multipacks.management.legacy;

import java.io.File;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;

import multipacks.packs.legacy.Pack;
import multipacks.packs.legacy.PackIdentifier;
import multipacks.packs.legacy.PackIndex;
import multipacks.plugins.MultipacksPlugin;

public abstract class PacksRepository {
	/**
	 * Query packs inside this repository.
	 * @param query Packs query filter, pass null to get all. In some repositories, this method may
	 * returns an empty iterator when you pass null to this argument (usually happens to online
	 * repositories).
	 * @return The iterator which can be used for further processing.
	 * @see #getPack(PackIndex)
	 * @see #getPackAsync(PackIndex)
	 */
	public abstract Iterator<PackIndex> queryPacks(PackIdentifier query);

	/**
	 * Get the pack from pack index (which could have been obtained from {@link #queryPacks(PackIdentifier)}).
	 * In some repositories, this may blocks the current thread to downloads the pack and store it inside
	 * temporary directory.
	 * @param index The pack index.
	 * @return The pack. May returns null if this repository can't find your pack.
	 * @see #queryPacks(PackIdentifier)
	 * @see #getPackAsync(PackIndex)
	 */
	public abstract Pack getPack(PackIndex index);

	/**
	 * Get pack from pack index asynchronously.
	 * @param index The pack index.
	 * @see #queryPacks(PackIdentifier)
	 */
	public CompletableFuture<Pack> getPackAsync(PackIndex index) {
		return CompletableFuture.supplyAsync(() -> {
			return getPack(index);
		});
	}

	/**
	 * Parse repository string.
	 */
	public static PacksRepository parseRepository(File root, String str) {
		for (MultipacksPlugin plug : MultipacksPlugin.PLUGINS) {
			PacksRepository repo = plug.parseRepository(root, str);
			if (repo != null) return repo;
		}
		return null;
	}
}