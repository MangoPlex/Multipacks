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
package multipacks.spigot.serving;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

import org.bukkit.entity.Player;

import multipacks.utils.PlatformAPI;

/**
 * It's not the kind of HTTP or TCP server, but instead an interface for servers to serves the pack to
 * players. This server is responsible for taking the pack and force player to install it. 
 * @author nahkd
 *
 */
@PlatformAPI
public interface PackServer {
	/**
	 * Serve the bundled pack to player.
	 * @param player Player that will be receiving the pack.
	 * @param prompt Message that will be dusplayed on player's pack load request screen, or null to use
	 * default prompt.
	 * @param stream Bundled pack but as {@link InputStream}.
	 * @return Async object: true if player accepted the pack and it is installed, false if they rejected
	 * request.
	 */
	CompletableFuture<Boolean> serve(Player player, String prompt, InputStream stream) throws IOException;

	/**
	 * Serve the artifact as bundled pack to player. This method by default does not perform any caching.
	 * @param player Player that will be receiving the pack.
	 * @param prompt Message that will be displayed on player's pack load request screen, or null to use
	 * default prompt.
	 * @param artifact Built artifact location.
	 * @return Async object: true if player accepted the pack and it is installed, false if they rejected
	 * request.
	 */
	default CompletableFuture<Boolean> serve(Player player, String prompt, File artifact) throws IOException {
		try (FileInputStream in = new FileInputStream(artifact)) {
			return serve(player, prompt, in);
		}
	}
}
