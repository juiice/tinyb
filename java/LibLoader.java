package tinyb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper Class used to load native libs without having
 * java.library.path property specified.
 * Native libs will be extracted and loaded automatically, if possible
 *
 * @author Julien Decaen
 */
public class LibLoader
{

    private static final String LIBLOADER_VERSION_NAME = "tinyb-libs-0.5";

    private static final Map<String, String> extractedLibs = new HashMap<String, String>();
    private static final Map<String, Boolean> loadedLibs = new HashMap<String, Boolean>();

    static {

        String osName = System.getProperty("os.name");
        String osArch = System.getProperty("os.arch");

        logStdOut("os.name='"+osName+"'");
        logStdOut("os.arch='"+osArch+"'");

        // check for linux platform ..
        if (osName.toLowerCase().contains("linux")) {
            // check for architecture arm
            if (osArch.toLowerCase().contains("arm")) {
                extractedLibs.put("tinyb", extractLib("/", "libtinyb.so"));
                extractedLibs.put("javatinyb", extractLib("/", "libjavatinyb.so"));
                loadedLibs.put("tinyb", false);
                loadedLibs.put("javatinyb", false);
            }
            // check for architecture 64bit
            else if (osArch.toLowerCase().contains("amd64") || osArch.toLowerCase().contains("x86_64")) {
                extractedLibs.put("tinyb", extractLib("/", "libtinyb.so"));
                extractedLibs.put("javatinyb", extractLib("/", "libjavatinyb.so"));
                loadedLibs.put("tinyb", false);
                loadedLibs.put("javatinyb", false);
            }
            // else 32bit
            else {
                // Not supported
            }

        }
        // other platforms are currently not supported ...
        else {
            logStdOut("Sorry, platform '"+osName+"' not supported by LibLoader");
        }

        logStdOut("Map: "+extractedLibs);
    }


    /**
     * Extracts the specified library from resource dir to a temp folder
     * and returns the path to the extracted file
     *
     * @param folder resource folder of the lib to extract
     * @param libFileName name of the lib file in resource folder
     * @return complete path to the extracted lib
     */
    private static String extractLib(String folder, String libFileName) {

        int written = 0;

        String libSource = folder + libFileName;

        InputStream resourceAsStream = null;

        try {
            resourceAsStream = LibLoader.class.getResourceAsStream(libSource);

            if (resourceAsStream != null) {

                File tempFolder = new File(System.getProperty("java.io.tmpdir"));

                File libDir = new File(tempFolder, LIBLOADER_VERSION_NAME);

                if (!libDir.exists()) {
                    libDir.mkdirs();
                }

                File libFile = new File(libDir, libFileName);

                if(libFile.exists()) {
                    return libFile.getAbsolutePath();
                }

                FileOutputStream fos = new FileOutputStream(libFile);
                int read = 0;
                byte[] data = new byte[512];

                while ((read = resourceAsStream.read(data)) != -1) {
                    fos.write(data, 0, read);
                    written += read;
                }

                fos.close();
                logStdOut("Extracting " + libSource + " to " + libFile.getAbsolutePath() + " *done*. written bytes: " + written);
                return libFile.getAbsolutePath();

            } else {
                logStdOut("Could not find " + libSource + " in resources ...");
            }
        } catch (Exception ex) {
            logStdErr("Error extracting " + libSource  + " to temp... written bytes: " + written);
            logExceptionToStdErr(ex);
        } finally {
            if (resourceAsStream!=null) {
                try {
                    resourceAsStream.close();
                } catch (IOException ex) {
                }
            }
        }
        return null;
    }

    private static void logStdOut(String msg) {
        String property = System.getProperty("tinyb.actbundle.debug", "false");
        boolean log = Boolean.parseBoolean(property);
        if (log) {
            System.out.println(msg);
        }
    }

    private static void logStdErr(String msg) {
        String property = System.getProperty("tinyb.actbundle.suppress_error", "false");
        boolean suppress = Boolean.parseBoolean(property);
        if (!suppress) {
            System.err.println(msg);
        }
    }

    private static void logExceptionToStdErr(Exception ex) {
        String property = System.getProperty("tinyb.actbundle.suppress_error", "false");
        boolean suppress = Boolean.parseBoolean(property);
        if (!suppress) {
            ex.printStackTrace();
        }
    }

    public static void loadLibrary(String name) {

        logStdOut("Trying to load '"+name+"' ...");

        if (loadedLibs.get(name)) {
            logStdOut("Library '"+name+"' is already loaded");
            return;
        }

        String lib = extractedLibs.get(name);

        // check if it's loadable via LibLoader mechanism
        if (lib!=null) {
            File f = new File(lib);
            logStdOut("...Loading via LibLoader mechanism: " + f.getAbsolutePath());
            System.load(f.getAbsolutePath());
        }
        // otherwise load it the old way ...
        else {
            logStdOut("...Loading via System.loadLibrary() call");
            System.loadLibrary(name);
        }
        logStdOut("...*done*");

        loadedLibs.replace(name, true);
    }

}