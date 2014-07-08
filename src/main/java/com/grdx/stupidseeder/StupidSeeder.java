package com.grdx.stupidseeder;

import jargs.gnu.CmdLineParser;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.file.StandardWatchEventKinds.*;


public class StupidSeeder {
    protected static final Logger logger = LoggerFactory.getLogger(StupidSeeder.class);
    protected WatchService watcher;
    protected Path inputPath;
    protected TorrentManager torrentManager;

    StupidSeeder(Path inputPath, Path outputPath) throws IOException {
        this.watcher = FileSystems.getDefault().newWatchService();
        this.inputPath = inputPath;
        this.torrentManager = new TorrentManager(outputPath);
        inputPath.register(watcher, ENTRY_CREATE, ENTRY_DELETE);
    }

    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }

    protected static void printUsage(PrintStream printStream) {
        printStream.print("Usage: StupidSeeder -i [INPUT_DIRECTORY] -o [OUTPUT_DIRECTORY]\n" +
                "Download and seed torrents.\n" +
                "  -h                            print this help\n" +
                "  -i, --input=INPUT_DIRECTORY   folder that contains .torrent files\n" +
                "  -o, --output=OUTPUT_DIRECTORY download directory\n");
    }

    public static void main(String[] args) {
        CmdLineParser parser = new CmdLineParser();
        CmdLineParser.Option help = parser.addBooleanOption('h', "help");
        CmdLineParser.Option input = parser.addStringOption('i', "input");
        CmdLineParser.Option output = parser.addStringOption('o', "output");

        try {
            parser.parse(args);
        } catch (CmdLineParser.OptionException e) {
            logger.error(e.getMessage(), e);
            printUsage(System.err);
            System.exit(1);
        }

        Boolean helpValue = (Boolean) parser.getOptionValue(help, Boolean.FALSE);
        String inputValue = (String) parser.getOptionValue(input);
        String outputValue = (String) parser.getOptionValue(output);

        if (helpValue) {
            printUsage(System.out);
            System.exit(0);
        }

        if ((inputValue == null) || (outputValue == null)) {
            printUsage(System.err);
            System.exit(1);
        }

        File inputFile = new File(inputValue);
        File outputFile = new File(outputValue);

        if (!inputFile.isDirectory()) {
            System.err.print("invalid input directory\n");
            System.exit(1);
        }
        if (!outputFile.isDirectory()) {
            System.err.print("invalid output directory\n");
            System.exit(1);
        }

        try {
            StupidSeeder stupidSeeder = new StupidSeeder(inputFile.toPath(), outputFile.toPath());
            stupidSeeder.run();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            System.exit(1);
        }

        System.exit(0);
    }

    public void run() {
        for (; ; ) {
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
                return;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind kind = event.kind();

                if (kind == OVERFLOW) {
                    continue;
                }

                WatchEvent<Path> watchEvent = cast(event);
                Path path = inputPath.resolve(watchEvent.context());

                logger.info("{}: {}", kind.name(), path.toString());
                if (FilenameUtils.getExtension(path.toString()).equals("torrent")) {
                    if (kind == ENTRY_CREATE) {
                        if (path.toFile().isFile() && torrentManager.addTorrent(path)) {
                            logger.info("ADD: {}", path.toString());
                        }

                    } else if (kind == ENTRY_DELETE) {
                        if (torrentManager.removeTorrent(path)) {
                            logger.info("REMOVE: {}", path.toString());
                        }
                    }
                }
            }
            if (!key.reset()) {
                break;
            }
        }
    }
}
