package com.cte.dashboard.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Map;

/**
 * Verwaltet prozessuebergreifende Locks fuer Spring Profile.
 * Verwendet ServerSocket fuer zuverlaessige prozessuebergreifende Locks.
 * Jedes Profil bekommt einen eigenen Port basierend auf dem Profilnamen.
 *
 * Diese Klasse verwendet statische Methoden, damit das Lock VOR dem
 * Spring-Context-Start erworben werden kann.
 */
public class ProfileLockManager {

    private static final int BASE_PORT = 47200;

    // Port-Mapping fuer bekannte Profile
    private static final Map<String, Integer> PROFILE_PORTS = Map.of(
            "default", BASE_PORT,
            "dev", BASE_PORT + 1,
            "prod", BASE_PORT + 2,
            "test", BASE_PORT + 3
    );

    private static ServerSocket lockSocket;
    private static String currentProfile;
    private static boolean shutdownHookRegistered = false;

    /**
     * Ermittelt den Port fuer ein Profil.
     */
    private static int getPortForProfile(String profile) {
        return PROFILE_PORTS.getOrDefault(
                profile.toLowerCase(),
                BASE_PORT + Math.abs(profile.hashCode() % 100)
        );
    }

    /**
     * Versucht einen Lock fuer das angegebene Profil zu erwerben.
     *
     * @param profile Der Name des Profils (z.B. "dev", "prod", "test")
     * @return true wenn Lock erfolgreich erworben, false wenn bereits gesperrt
     */
    public static synchronized boolean acquireLock(String profile) {
        int port = getPortForProfile(profile);

        try {
            // Versuche ServerSocket auf dem Port zu oeffnen
            // Wenn der Port bereits belegt ist, wirft dies eine Exception
            ServerSocket socket = new ServerSocket(port, 1, InetAddress.getLoopbackAddress());

            // Lock erfolgreich erworben
            lockSocket = socket;
            currentProfile = profile;

            System.out.println("[ProfileLock] Lock fuer Profil '" + profile + "' erworben (Port " + port + ")");
            return true;

        } catch (IOException e) {
            // Port ist bereits belegt - Profil ist gesperrt
            System.err.println("[ProfileLock] FEHLER: Profil '" + profile + "' ist bereits von einer anderen Instanz gesperrt (Port " + port + ")");
            return false;
        }
    }

    /**
     * Gibt den aktuellen Lock frei.
     */
    public static synchronized void releaseLock() {
        if (lockSocket != null) {
            try {
                int port = lockSocket.getLocalPort();
                lockSocket.close();
                System.out.println("[ProfileLock] Lock fuer Profil '" + currentProfile + "' freigegeben (Port " + port + ")");
            } catch (IOException e) {
                System.err.println("[ProfileLock] Fehler beim Freigeben des Locks: " + e.getMessage());
            }
            lockSocket = null;
        }
        currentProfile = null;
    }

    /**
     * Prueft ob das angegebene Profil bereits gesperrt ist.
     *
     * @param profile Der Name des Profils
     * @return true wenn gesperrt, false wenn frei
     */
    public static boolean isLocked(String profile) {
        int port = getPortForProfile(profile);

        try (ServerSocket testSocket = new ServerSocket(port, 1, InetAddress.getLoopbackAddress())) {
            // Port ist frei - Profil ist nicht gesperrt
            testSocket.close();
            return false;
        } catch (IOException e) {
            // Port ist belegt - Profil ist gesperrt
            return true;
        }
    }

    /**
     * Gibt den Namen des aktuell gesperrten Profils zurueck.
     *
     * @return Name des Profils oder null wenn keines gesperrt
     */
    public static String getCurrentProfile() {
        return currentProfile;
    }

    /**
     * Prueft ob aktuell ein Lock gehalten wird.
     *
     * @return true wenn ein Lock aktiv ist
     */
    public static boolean hasLock() {
        return lockSocket != null && !lockSocket.isClosed();
    }

    /**
     * Registriert einen Shutdown Hook um den Lock beim Beenden freizugeben.
     */
    public static synchronized void registerShutdownHook() {
        if (!shutdownHookRegistered) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("[ProfileLock] Shutdown Hook: Gebe Lock frei...");
                releaseLock();
            }, "ProfileLockManager-ShutdownHook"));
            shutdownHookRegistered = true;
        }
    }

    /**
     * Ermittelt das aktive Profil aus den Kommandozeilen-Argumenten,
     * System Properties oder Umgebungsvariablen.
     *
     * @param args Die Kommandozeilen-Argumente aus main()
     * @return Das aktive Profil oder "default"
     */
    public static String getActiveProfileFromEnvironment(String[] args) {
        // 1. Kommandozeilen-Argumente (--spring.profiles.active=dev)
        if (args != null) {
            for (String arg : args) {
                if (arg.startsWith("--spring.profiles.active=")) {
                    String profile = arg.substring("--spring.profiles.active=".length());
                    if (!profile.isEmpty()) {
                        return profile.split(",")[0].trim();
                    }
                }
            }
        }

        // 2. Spring Property (-Dspring.profiles.active=dev)
        String profile = System.getProperty("spring.profiles.active");
        if (profile != null && !profile.isEmpty()) {
            return profile.split(",")[0].trim();
        }

        // 3. Umgebungsvariable (SPRING_PROFILES_ACTIVE=dev)
        profile = System.getenv("SPRING_PROFILES_ACTIVE");
        if (profile != null && !profile.isEmpty()) {
            return profile.split(",")[0].trim();
        }

        // 4. Default
        return "default";
    }
}
