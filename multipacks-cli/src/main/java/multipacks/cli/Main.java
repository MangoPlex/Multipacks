package multipacks.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import multipacks.bundling.BundleIgnore;
import multipacks.management.PacksRepository;
import multipacks.packs.PackIdentifier;
import multipacks.versioning.Version;

public class Main {
	public static void main(String[] args) {
		Platform currentPlatform = Platform.getPlatform();

		if (currentPlatform == Platform.UNKNOWN) {
			System.err.println("Unsupported platform: " + System.getProperty("os.name"));
			System.err.println("If you think this platform should be supported, please open new issue in our GitHub repository.");
			System.exit(1);
			return;
		}

		if (args.length == 0) {
			System.out.println("Multipacks CLI");
			System.out.println("Usage: java multipacks.cli.Main <subcommand...> [options...]");
			System.out.println("Subcommands:");
			System.out.println("  list            List repositories or packs");
			System.out.println("  pack            Pack command");
			System.out.println();
			System.out.println("Options:");
			System.out.println("  -F  --filter <ID> [>=, >, <=, <]<Version>");
			System.out.println("        Set filter for querying packs");
			System.out.println("  -R  --repo <'#' + Index | 'file:/path/'>");
			System.out.println("        Select repository (see index with 'list repo')");
			System.out.println("  -O  --output </path/to/output>");
			System.out.println("        Set output path");
			System.out.println("      --skip");
			System.out.println("        Skip 'pack init' prompts");
			System.out.println("      --ignore-errors");
			System.out.println("        Ignore errors as much as possible");
			System.out.println("  -I  --ignore <Pack feature>");
			System.out.println("        Ignore pack features (use -I multiple times to ignores more)");
			System.out.println("        Available features to ignore: " + String.join(", ", Stream.of(BundleIgnore.values()).map(v -> v.toString().toLowerCase()).toArray(String[]::new)));
			return;
		}

		MultipacksCLI cli = new MultipacksCLI(currentPlatform);
		List<String> regularArguments = new ArrayList<>();

		for (int i = 0; i < args.length; i++) {
			String s = args[i];

			if (!s.startsWith("-")) {
				regularArguments.add(s);
				continue;
			}

			// TODO: Use switch-case?

			if (s.equals("-F") || s.equals("--filter")) {
				String id = args[++i];
				String versionStr = args[++i];
				cli.filter = new PackIdentifier(id, new Version(versionStr));
			} else if (s.equals("-R") || s.equals("--repo")) {
				String repoStr = args[++i];

				if (repoStr.startsWith("#")) {
					int repoIdx = Integer.parseInt(repoStr.substring(1));

					if (repoIdx >= cli.repositories.size()) {
						System.err.println("Repository #" + repoIdx + " doesn't exists (Out of bound)");
						System.err.println("Tip: Use 'list repo' to view all repositories and its index");
						System.exit(1);
						return;
					}

					cli.selectedRepository = cli.repositories.get(repoIdx);
				} else {
					cli.selectedRepository = PacksRepository.parseRepository(null, repoStr);

					if (cli.selectedRepository == null) {
						System.err.println("Unknown repository string: " + repoStr);
						System.err.println("Valid string formats:");
						System.err.println(" - '#' + Index");
						System.err.println(" - 'file:/path/to/repository'");
						System.exit(1);
						return;
					}
				}
			} else if (s.equals("--skip")) {
				cli.skipPrompts = true;
			} else if (s.equals("--ignore-errors")) {
				cli.ignoreErrors = true;
			} else if (s.equals("--ignore")) {
				String featureString = args[++i];

				try {
					BundleIgnore ignore = BundleIgnore.valueOf(featureString.toUpperCase());
					cli.bundleIgnoreFeatures.add(ignore);
				} catch (IllegalArgumentException e) {
					System.err.println("Unknown feature: " + featureString);
					System.exit(1);
					return;
				}
			} else {
				System.err.println("Unknown option: " + s);
				System.exit(1);
				return;
			}
		}

		cli.exec(regularArguments.toArray(String[]::new));
	}
}
