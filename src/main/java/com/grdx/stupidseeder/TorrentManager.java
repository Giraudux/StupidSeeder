package com.grdx.stupidseeder;

import com.turn.ttorrent.client.Client;
import com.turn.ttorrent.client.SharedTorrent;

import java.net.InetAddress;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TorrentManager {
    protected static final Logger logger = LoggerFactory.getLogger(TorrentManager.class);
    protected Path outputPath;
    protected Map<Path, Client> torrentClientMap;

    public TorrentManager(Path outputPath) {
        this.outputPath = outputPath;
        this.torrentClientMap = new TreeMap<>();
    }

    public boolean addTorrent(Path torrentPath) {
        Client client;
        try {
            client = new Client(InetAddress.getLocalHost(), SharedTorrent.fromFile(torrentPath.toFile(), outputPath.toFile()));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }
        client.share();
        torrentClientMap.put(torrentPath, client);
        return true;
    }

    public boolean removeTorrent(Path torrentPath) {
        Client client;
        try {
            client = torrentClientMap.get(torrentPath);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }
        if (client == null) {
            return false;
        }
        client.stop();
        torrentClientMap.remove(torrentPath);
        return true;
    }
}
