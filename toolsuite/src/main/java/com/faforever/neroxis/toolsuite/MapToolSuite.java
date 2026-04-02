package com.faforever.neroxis.toolsuite;

import com.faforever.neroxis.cli.VersionProvider;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import static picocli.CommandLine.Command;

@Command(
        name = "toolsuite",
        mixinStandardHelpOptions = true,
        description = "Tools to modify maps",
        versionProvider = VersionProvider.class,
        usageHelpAutoWidth = true,
        synopsisSubcommandLabel = "COMMAND",
        subcommands = {
                MapPopulator.class, MapResizer.class, MapStratumResizer.class, MapForcer.class, MapEvaluator.class, MapInfoTextureExporter.class, MapNormalsTextureExporter.class, PbrTextureGenerator.class}
)
public class MapToolSuite implements Runnable {

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    private MapToolSuite() {
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        Pattern pattern = Pattern.compile(
                "\"[^\"]*\"" +
                "|'[^']*'" +
                "|\\S+"
        );

        spec.commandLine().usage(System.out);
        spec.commandLine().setTrimQuotes(true);
        while (true) {
            System.out.println("Enter command below:");
            List<String> args = new ArrayList<>();
            String arg;
            while ((arg = scanner.findInLine(pattern)) != null) {
                args.add(arg);
            }
            if (args.equals(List.of("exit"))) {
                break;
            }
            int commandExitCode = spec.commandLine().execute(args.toArray(String[]::new));
            if (commandExitCode == 0) {
                System.out.println("Completed");
            } else {
                System.out.println("Command failed see output above");
            }
            scanner.nextLine();

        }
    }

    static void main(String[] args) {
        CommandLine commandLine = new CommandLine(new MapToolSuite());
        commandLine.setAbbreviatedOptionsAllowed(true);
        int exitCode = commandLine.execute(args);
        System.exit(exitCode);
    }
}
