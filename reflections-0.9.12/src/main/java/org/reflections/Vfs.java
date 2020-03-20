package org.reflections.vfs;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import org.osgi.framework.FrameworkUtil;
import org.reflections.Reflections;
import org.reflections.ReflectionsException;
import org.reflections.util.Utils;


import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarFile;

/**
 * a simple virtual file system bridge
 * <p>use the {@link org.reflections.vfs.Vfs#fromURL(java.net.URL)} to get a {@link org.reflections.vfs.Vfs.Dir}
 * and than use {@link org.reflections.vfs.Vfs.Dir#getFiles()} to iterate over it's {@link org.reflections.vfs.Vfs.File}
 * <p>for example:
 * <pre>
 *      Vfs.Dir dir = Vfs.fromURL(url);
 *      Iterable<Vfs.File> files = dir.getFiles();
 *      for (Vfs.File file : files) {
 *          InputStream is = file.openInputStream();
 *      }
 * </pre>
 * <p>use {@link org.reflections.vfs.Vfs#findFiles(java.util.Collection, com.google.common.base.Predicate)} to get an
 * iteration of files matching given name predicate over given list of urls
 * <p><p>{@link org.reflections.vfs.Vfs#fromURL(java.net.URL)} uses static {@link org.reflections.vfs.Vfs.DefaultUrlTypes} to resolves URLs
 * and it can be plugged in with {@link org.reflections.vfs.Vfs#addDefaultURLTypes(org.reflections.vfs.Vfs.UrlType)} or {@link org.reflections.vfs.Vfs#setDefaultURLTypes(java.util.List)}.
 * <p>for example:
 * <pre>
 *      Vfs.addDefaultURLTypes(new Vfs.UrlType() {
 *          public boolean matches(URL url)         {
 *              return url.getProtocol().equals("http");
 *          }
 *          public Vfs.Dir createDir(final URL url) {
 *              return new HttpDir(url); //implement this type... (check out a naive implementation on VfsTest)
 *          }
 *      });
 *
 *      Vfs.Dir dir = Vfs.fromURL(new URL("http://mirrors.ibiblio.org/pub/mirrors/maven2/org/slf4j/slf4j-api/1.5.6/slf4j-api-1.5.6.jar"));
 * </pre>
 */
public abstract class Vfs {
    private static List<UrlType> defaultUrlTypes = Lists.<UrlType>newArrayList(DefaultUrlTypes.values());

    /** an abstract vfs dir */
    public interface Dir {
        String getPath();
        Iterable<File> getFiles();
        void close();
    }

    /** an abstract vfs file */
    public interface File {
        String getName();
        String getRelativePath();
        InputStream openInputStream() throws IOException;
    }

    /** a matcher and factory for a url */
    public interface UrlType {
        boolean matches(URL url) throws Exception;
        Dir createDir(URL url) throws Exception;
    }

    /** the default url types that will be used when issuing {@link org.reflections.vfs.Vfs#fromURL(java.net.URL)} */
    public static List<UrlType> getDefaultUrlTypes() {
        return defaultUrlTypes;
    }

    /** sets the static default url types. can be used to statically plug in urlTypes */
    public static void setDefaultURLTypes(final List<UrlType> urlTypes) {
        defaultUrlTypes = urlTypes;
    }

    /** add a static default url types. can be used to statically plug in urlTypes */
    public static void addDefaultURLTypes(UrlType urlType) {
        defaultUrlTypes.add(urlType);
    }

    /** tries to create a Dir from the given url, using the defaultUrlTypes */
    public static Dir fromURL(final URL url) {
        return fromURL(url, defaultUrlTypes);
    }

    /** tries to create a Dir from the given url, using the given urlTypes*/
    public static Dir fromURL(final URL url, final List<UrlType> urlTypes) {
        for (UrlType type : urlTypes) {
            try {
                if (type.matches(url)) {
                    Dir dir = type.createDir(url);
                    if (dir != null) return dir;
                }
            } catch (Throwable e) {
                /*if (Reflections.log != null) {
                    Reflections.log.warn("could not create Dir using " + type + " from url " + url.toExternalForm() + ". skipping.", e);
                }*/
            }
        }

        throw new ReflectionsException("could not create Vfs.Dir from url, no matching UrlType was found [" + url.toExternalForm() + "]\n" +
                "either use fromURL(final URL url, final List<UrlType> urlTypes) or " +
                "use the static setDefaultURLTypes(final List<UrlType> urlTypes) or addDefaultURLTypes(UrlType urlType) " +
                "with your specialized UrlType.");
    }

    /** tries to create a Dir from the given url, using the given urlTypes*/
    public static Dir fromURL(final URL url, final UrlType... urlTypes) {
        return fromURL(url, Lists.<UrlType>newArrayList(urlTypes));
    }

    /** return an iterable of all {@link org.reflections.vfs.Vfs.File} in given urls, starting with given packagePrefix and matching nameFilter */
    public static Iterable<File> findFiles(final Collection<URL> inUrls, final String packagePrefix, final Predicate<String> nameFilter) {
        Predicate<File> fileNamePredicate = new Predicate<File>() {
            public boolean apply(File file) {
                String path = file.getRelativePath();
                if (path.startsWith(packagePrefix)) {
                    String filename = path.substring(path.indexOf(packagePrefix) + packagePrefix.length());
                    return !Utils.isEmpty(filename) && nameFilter.apply(filename.substring(1));
                } else {
                    return false;
                }
            }
        };

        return findFiles(inUrls, fileNamePredicate);
    }

    /** return an iterable of all {@link org.reflections.vfs.Vfs.File} in given urls, matching filePredicate */
    public static Iterable<File> findFiles(final Collection<URL> inUrls, final Predicate<File> filePredicate) {
        Iterable<File> result = new ArrayList<File>();

        for (final URL url : inUrls) {
            try {
                result = Iterables.concat(result,
                        Iterables.filter(new Iterable<File>() {
                            public Iterator<File> iterator() {
                                return fromURL(url).getFiles().iterator();
                            }
                        }, filePredicate));
            } catch (Throwable e) {
                /*if (Reflections.log != null) {
                    Reflections.log.error("could not findFiles for url. continuing. [" + url + "]", e);
                }*/
            }
        }

        return result;
    }

    /**try to get {@link java.io.File} from url*/
    static java.io.File getFile(URL url) {
        java.io.File file;
        String path;

        try {
            path = url.toURI().getSchemeSpecificPart();
            if ((file = new java.io.File(path)).exists()) return file;
        } catch (URISyntaxException e) {
        }

        try {
            path = URLDecoder.decode(url.getPath(), "UTF-8");
            if (path.contains(".jar!")) path = path.substring(0, path.lastIndexOf(".jar!") + ".jar".length());
            if ((file = new java.io.File(path)).exists()) return file;

        } catch (UnsupportedEncodingException e) {
        }

        try {
            path = url.toExternalForm();
            if (path.startsWith("jar:")) path = path.substring("jar:".length());
            if (path.startsWith("file:")) path = path.substring("file:".length());
            if (path.contains(".jar!")) path = path.substring(0, path.indexOf(".jar!") + ".jar".length());
            if ((file = new java.io.File(path)).exists()) return file;

            path = path.replace("%20", " ");
            if ((file = new java.io.File(path)).exists()) return file;

        } catch (Exception e) {
        }
        
        try {
            byte[] buffer = new byte[8 * 1024];

            InputStream input = url.openStream();
            java.io.File tmpFile = createTempFile("reflections", ".jar");
            try {
                OutputStream output = new FileOutputStream(tmpFile);
                try {
                    int bytesRead;
                    while ((bytesRead = input.read(buffer)) != -1) {
                        output.write(buffer, 0, bytesRead);
                    }
                } finally {
                    output.close();
                }
            } finally {
                input.close();
            }
            return tmpFile;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
    
    private static java.io.File createTempFile(String prefix, String suffix) throws IOException {
               java.io.File result = null;
               java.io.File parent = createTmpDir();
                   
               if (suffix == null) {
                   suffix = ".tmp";
               }
               if (prefix == null) {
                   prefix = "reflections";
               } else if (prefix.length() < 3) {
                   prefix = prefix + "reflections";
               }
               result = java.io.File.createTempFile(prefix, suffix, parent);

              
               return result;
    }
    
    private static java.io.File createTmpDir() {
        int x = (int)(Math.random() * 1000000);
        String s = System.getProperty("java.io.tmpdir");
        java.io.File checkExists = new java.io.File(s);
        if (!checkExists.exists() || !checkExists.isDirectory()) {
            throw new RuntimeException("The directory " 
                                   + checkExists.getAbsolutePath() 
                                   + " does not exist, please set java.io.tempdir"
                                   + " to an existing directory");
        }
        if (!checkExists.canWrite()) {
            throw new RuntimeException("The directory " 
                                   + checkExists.getAbsolutePath() 
                                   + " is now writable, please set java.io.tempdir"
                                   + " to an writable directory");
        }
        java.io.File f = new java.io.File(s, "reflections-" + x);
        while (!f.mkdir()) {
            x = (int)(Math.random() * 1000000);
            f = new java.io.File(s, "reflections-" + x);
        }
        java.io.File newTmpDir  = f;
        return newTmpDir;
    }
    
    
    
    

    /** default url types used by {@link org.reflections.vfs.Vfs#fromURL(java.net.URL)}
     * <p>
     * <p>jarFile - creates a {@link org.reflections.vfs.ZipDir} over jar file
     * <p>jarUrl - creates a {@link org.reflections.vfs.ZipDir} over a jar url (contains ".jar!/" in it's name)
     * <p>directory - creates a {@link org.reflections.vfs.SystemDir} over a file system directory
     * <p>vfs_jboss7 - creates a {@link org.reflections.vfs.SystemDir} over jboss 7 vfs
     * */
    public static enum DefaultUrlTypes implements UrlType {
        jarFile {
            public boolean matches(URL url) {
                return url.getProtocol().equals("file") && url.toExternalForm().contains(".jar");
            }

            public Dir createDir(final URL url) throws Exception {
                return new ZipDir(new JarFile(getFile(url)));
            }
        },

        jarUrl {
            public boolean matches(URL url) {
                return "jar".equals(url.getProtocol());
            }

            public Dir createDir(URL url) throws Exception {
                URLConnection urlConnection = url.openConnection();
                return urlConnection instanceof JarURLConnection ?
                        new ZipDir(((JarURLConnection) urlConnection).getJarFile()) : null;
            }
        },

        directory {
            public boolean matches(URL url) {
                return url.getProtocol().equals("file") && !url.toExternalForm().contains(".jar");
            }

            public Dir createDir(final URL url) throws Exception {
                return new SystemDir(getFile(url));
            }
        },
        
        bundleUrl {
            public boolean matches(URL url) {
                return url.getProtocol().equals("bundle");
            }

            public Dir createDir(final URL url) throws Exception {
                try {
                    String location = FrameworkUtil.getBundle(Vfs.class).getBundleContext().getBundle(getBundleIdforResourceScan(url.toExternalForm())).getLocation();
                    if (location.startsWith("reference:")) {
                        location = location.substring("reference:".length());
                    }
                    return new ZipDir(new JarFile(getFile(new URL(location))));
                } catch (Exception e) {
                    e.printStackTrace();
                    throw e;
                }
            }
            
            private long getBundleIdforResourceScan(String bundleUrl) {
                int startIdx = bundleUrl.indexOf("://");
                int endIdx = bundleUrl.indexOf(".");
                String retStr = bundleUrl.substring(startIdx + 3, endIdx);
                return Long.valueOf(retStr).longValue();
                
            }
        },
    }
}
