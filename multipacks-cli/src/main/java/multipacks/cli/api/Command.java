package multipacks.cli.api;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import multipacks.cli.api.annotations.Argument;
import multipacks.cli.api.annotations.Option;
import multipacks.cli.api.annotations.Subcommand;
import multipacks.cli.api.internal.ArgumentInfo;
import multipacks.cli.api.internal.OptionInfo;

/**
 * Abstract class for all commands and subcommands.
 * @author nahkd
 * @see Argument
 * @see Option
 * @see Subcommand
 *
 */
public abstract class Command {
	/**
	 * Called when this command is executed. This method will be called after all arguments and options are
	 * populated with user's data.
	 * @throws CommandException
	 */
	protected abstract void onExecute() throws CommandException;

	/**
	 * Called after subcommand is executed, or after {@link #onExecute()} if there's no subcommands.
	 * @throws CommandException
	 */
	protected void postExecute() throws CommandException {
		// NO-OP
	}

	private boolean isBuilt = false;
	private ArgumentInfo[] arguments;
	private Map<String, OptionInfo> options;
	private Map<String, Command> subcommands;

	private void build() {
		if (isBuilt) return;

		boolean hasOptionalArgs = false;

		this.options = new HashMap<>();
		this.subcommands = new HashMap<>();
		List<ArgumentInfo> arguments = new ArrayList<>();

		for (Field f : getClass().getDeclaredFields()) {
			Argument argAnnotation = f.getDeclaredAnnotation(Argument.class);
			if (argAnnotation != null) {
				arguments.add(new ArgumentInfo(this, argAnnotation, f));
				if (argAnnotation.optional()) hasOptionalArgs = true;
			}

			Option optAnnotation = f.getDeclaredAnnotation(Option.class);
			if (optAnnotation != null) {
				OptionInfo optInfo = new OptionInfo(this, optAnnotation, f);
				for (String opt : optAnnotation.value()) options.put(opt, optInfo);
			}

			Subcommand cmdAnnotation = f.getDeclaredAnnotation(Subcommand.class);
			if (cmdAnnotation != null) {
				try {
					subcommands.put(cmdAnnotation.value(), (Command) f.get(this));
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}
			}
		}

		for (Method m : getClass().getDeclaredMethods()) {
			Argument argAnnotation = m.getDeclaredAnnotation(Argument.class);
			if (argAnnotation != null) arguments.add(new ArgumentInfo(this, argAnnotation, m));

			Option optAnnotation = m.getDeclaredAnnotation(Option.class);
			if (optAnnotation != null) {
				OptionInfo optInfo = new OptionInfo(this, optAnnotation, m);
				for (String opt : optAnnotation.value()) options.put(opt, optInfo);
			}
		}

		Collections.sort(arguments);
		this.arguments = arguments.toArray(ArgumentInfo[]::new);

		// Validate rules:
		// If there's no subcommands, optional arguments must be placed after required arguments.
		// If there's at least 1 subcommand, no optional arguments are allowed.
		if (hasOptionalArgs) {
			if (subcommands.size() > 0) throw new RuntimeException("No optional arguments are allowed when there's at least 1 subcommand.");
			boolean currentOptionalState = false;

			for (ArgumentInfo info : arguments) {
				if (currentOptionalState && !info.declared.optional()) throw new RuntimeException("Argument #" + info.declared.value() + " is required, but there's at least 1 optional argument before it.");
				if (info.declared.optional()) currentOptionalState = true;
			}
		}

		isBuilt = true;
	}

	public void execute(Parameters params) throws CommandException {
		build();

		checkOptions(params);

		for (ArgumentInfo arg : arguments) {
			checkOptions(params);

			if (params.endOfParams()) {
				if (arg.declared.optional()) break;
				throw new CommandException("Argument #" + arg.declared.value() + " is required");
			}

			arg.set(params.getThenAdvance());
		}

		checkOptions(params);
		onExecute();

		if (!params.endOfParams() && subcommands.size() > 0) {
			Command subcommand = subcommands.get(params.getCurrent());
			if (subcommand == null) throw new CommandException("Subcommand '" + params.getCurrent() + "' not found");

			params.getThenAdvance();
			subcommand.execute(params);
		}

		postExecute();
	}

	public void execute(String... args) {
		execute(new Parameters(args));
	}

	private void checkOptions(Parameters params) throws CommandException {
		String current;

		while ((current = params.getCurrent()) != null && current.startsWith("-")) {
			params.getThenAdvance();
			String[] split = current.split("=", 2);

			OptionInfo opt = options.get(split[0]);
			if (opt == null) break;

			opt.set(split[1]);
		}
	}

	public static Object parse(String data, Class<?> type) {
		Object val;
		if (type.isAssignableFrom(String.class)) val = data;
		else if (type.isAssignableFrom(long.class)) val = Long.parseLong(data);
		else if (type.isAssignableFrom(int.class)) val = Integer.parseInt(data);
		else if (type.isAssignableFrom(short.class)) val = Short.parseShort(data);
		else if (type.isAssignableFrom(byte.class)) val = Byte.parseByte(data);
		else if (type.isAssignableFrom(double.class)) val = Double.parseDouble(data);
		else if (type.isAssignableFrom(float.class)) val = Float.parseFloat(data);
		else if (type.isAssignableFrom(boolean.class)) val = Boolean.parseBoolean(data);
		else throw new RuntimeException("Can't convert String to " + type);
		return val;
	}
}