package be.maartenvg.io.network;

import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;

public final class LocalIPUtility {
    public static String getLocalIp() throws SocketException {
        return Collections.list(NetworkInterface.getNetworkInterfaces()).stream()
                .flatMap(i -> Collections.list(i.getInetAddresses()).stream())
                .filter(ip -> ip instanceof Inet4Address && ip.isSiteLocalAddress())
                .findFirst().orElseThrow(RuntimeException::new)
                .getHostAddress();
    }
}
