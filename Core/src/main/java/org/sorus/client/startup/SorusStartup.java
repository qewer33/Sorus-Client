package org.sorus.client.startup;

import com.google.gson.Gson;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.apache.commons.io.IOUtils;
import org.sorus.client.obfuscation.ObfuscationManager;
import org.sorus.client.startup.injection.Hook;
import org.sorus.client.version.IVersion;

public class SorusStartup {

  public static boolean DEV;

  public static ClassLoader classLoader;

  public static TransformerUtility transformer;

  private static Map<String, String> launchArgs = new HashMap<>();

  public static Map<String, String> getArgsMap(String args) {
    Map<String, String> launchArgs = new HashMap<>();
    if (args != null) {
      for (String string : args.split(";")) {
        String[] strings = string.split("=");
        launchArgs.put(strings[0], strings[1]);
      }
    }
    return launchArgs;
  }

  public static void start(
      Class<? extends IVersion> version,
      TransformerUtility transformerUtility,
      ClassLoader classLoader,
      Map<String, String> args,
      boolean dev) {
    launchArgs = args;
    DEV = dev;
    transformer = transformerUtility;
    transformer.initialize();
    SorusStartup.registerTransformers(transformer);
    new Thread(
            () -> {
              ClassLoader classLoader1 = classLoader;
              if (classLoader1 == null) {
                while (SorusStartup.classLoader == null) {
                  System.out.print("");
                }
                classLoader1 = SorusStartup.classLoader;
              }
              try {
                final Class<?> clazz =
                    Class.forName(
                        "org.sorus.client.startup.SorusStartupCustomClassLoader",
                        false,
                        classLoader1);
                final Method mainMethod =
                    clazz.getMethod("start", String.class, Map.class, boolean.class);
                mainMethod.invoke(null, version.getName(), args, dev);
              } catch (ClassNotFoundException
                  | NoSuchMethodException
                  | IllegalAccessException
                  | InvocationTargetException e) {
                e.printStackTrace();
              }
            })
        .start();
  }

  private static void registerTransformers(TransformerUtility transformer) {
    Map<String, List<Class<?>>> injectors = new HashMap<>();
    String version = SorusStartup.getArgsMap().get("version");
    Gson gson = new Gson();
    if (SorusStartup.getArgsMap().get("plugins") != null) {
      for (String string : SorusStartup.getArgsMap().get("plugins").split(",")) {
        String actual = string.substring(2);
        if (string.charAt(0) == 'p') {
          File directory = new File(actual);
          for (File file : Objects.requireNonNull(directory.listFiles())) {
            File file1 = new File(file, version);
            if (file1.exists()) {
              try {
                File file2 = new File(file1, version + ".jar");
                Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                method.setAccessible(true);
                method.invoke(
                    SorusStartup.class.getClassLoader(),
                    (file1.exists()
                            ? new File(file1, file1.getName() + ".jar")
                            : new File(file, file.getName() + ".jar"))
                        .toURI()
                        .toURL());
                JarFile jarFile = new JarFile(file2);
                JarEntry jarEntry = jarFile.getJarEntry(version + ".json");
                InputStream inputStream = jarFile.getInputStream(jarEntry);
                Scanner scanner = new Scanner(inputStream);
                StringBuilder stringBuilder = new StringBuilder();
                while (scanner.hasNextLine()) {
                  stringBuilder.append(scanner.nextLine());
                }
                Info info = gson.fromJson(stringBuilder.toString(), Info.class);
                String location = info.location;
                for (String injectorName : info.injectors) {
                  String fullClassName =
                      (location + (location.isEmpty() ? "" : ".") + injectorName);
                  Class<?> injector = Class.forName(fullClassName);
                  String hookClass =
                      ObfuscationManager.getClassName(
                          injector.getDeclaredAnnotation(Hook.class).value());
                  injectors.computeIfAbsent(hookClass, k -> new ArrayList<>()).add(injector);
                }
              } catch (IOException
                  | ClassNotFoundException
                  | NoSuchMethodException
                  | IllegalAccessException
                  | InvocationTargetException e) {
                e.printStackTrace();
              }
            }
          }
        }
      }
    }
    InputStream pluginVersionJson =
        SorusStartup.class.getClassLoader().getResourceAsStream(version + ".json");
    if (pluginVersionJson != null) {
      try {
        String json = IOUtils.toString(pluginVersionJson, StandardCharsets.UTF_8);
        Info info = gson.fromJson(json, Info.class);
        String location = info.location;
        for (String string : info.injectors) {
          String fullClassName = (location + (location.isEmpty() ? "" : ".") + string);
          Class<?> injector = Class.forName(fullClassName);
          String hookClass =
              ObfuscationManager.getClassName(injector.getDeclaredAnnotation(Hook.class).value());
          injectors.computeIfAbsent(hookClass, k -> new ArrayList<>()).add(injector);
        }
      } catch (IOException | ClassNotFoundException e) {
        e.printStackTrace();
      }
    }
    InputStream inputStream = SorusStartup.class.getClassLoader().getResourceAsStream("sorus.json");
    if (inputStream == null) {
      return;
    }
    try {
      String json = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
      Info info = gson.fromJson(json, Info.class);
      String location = info.location;
      for (String string : info.injectors) {
        String fullClassName = (location + (location.isEmpty() ? "" : ".") + string);
        Class<?> injector = Class.forName(fullClassName);
        String hookClass =
            ObfuscationManager.getClassName(injector.getDeclaredAnnotation(Hook.class).value());
        injectors.computeIfAbsent(hookClass, k -> new ArrayList<>()).add(injector);
      }
    } catch (IOException | ClassNotFoundException e) {
      e.printStackTrace();
    }
    transformer.addTransformer(new MainTransformer(injectors));
  }

  public static Map<String, String> getArgsMap() {
    return launchArgs;
  }

  public static void setLaunchArgs(Map<String, String> args) {
    launchArgs = args;
  }
}
