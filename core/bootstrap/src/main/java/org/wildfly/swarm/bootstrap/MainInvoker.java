package org.wildfly.swarm.bootstrap;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.wildfly.swarm.bootstrap.modules.BootModuleLoader;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * @author Bob McWhirter
 */
public class MainInvoker {

    private static final String BOOT_MODULE_PROPERTY = "boot.module.loader";

    private static MainInvoker wrapper;

    public MainInvoker(Method mainMethod, String... args) {
        this.mainMethod = mainMethod;
        this.args = args;
        System.setProperty(BOOT_MODULE_PROPERTY, BootModuleLoader.class.getName());
    }

    public MainInvoker(Class<?> mainClass, String... args) throws Exception {
        this(getMainMethod(mainClass), args);
    }

    public MainInvoker(String mainClassName, String... args) throws Exception {
        this(getMainMethod(getMainClass(mainClassName)), args);
    }

    public void invoke() throws Exception {
        this.mainMethod.invoke(null, new Object[]{this.args});
        emitReady();
        /*String processFile = System.getProperty("org.wildfly.swarm.container.processFile");

        System.out.println("Process file " + processFile);
        if (processFile != null) {
            File uuidFile = new File(processFile);
            register(uuidFile.getParentFile(), uuidFile.toPath());
            processEvents(uuidFile.toPath());
            stop();
        }*/
    }

    public void stop() throws Exception {
        Method stopMethod = null;
        Class<?> mainClass = mainMethod.getDeclaringClass();
        //if (this.mainClass.getClass().getName().equals("org.wildfly.swarm.Swarm")) {
        try {
            System.out.println("Declaring class:" + mainClass.getName());
            stopMethod = mainClass.getDeclaredMethod("stopMain");
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        //}

        System.out.println("--------INVOKER STOOOOOOOOOPING");

        if (stopMethod != null) {
            stopMethod.invoke(mainClass, (Object[]) null);
        }

        System.out.println("--------INVOKER STOPPED");

        //System.exit(0);

        /*String processFile = System.getProperty("org.wildfly.swarm.container.processFile");

        System.out.println("Process file " + processFile);
        if (processFile != null) {
            File uuidFile = new File(processFile);
            register(uuidFile.getParentFile(), uuidFile.toPath());
            processEvents(uuidFile.toPath());
            stop();
        }*/
    }

    protected void emitReady() throws Exception {
        Module module = Module.getBootModuleLoader().loadModule(ModuleIdentifier.create("swarm.container"));
        Class<?> messagesClass = module.getClassLoader().loadClass("org.wildfly.swarm.internal.SwarmMessages");
        Field field = messagesClass.getDeclaredField("MESSAGES");

        Object messages = field.get(null);

        Method ready = messages.getClass().getMethod("wildflySwarmIsReady");
        ready.invoke(messages);
    }

    private void register(File directory, Path file) throws IOException {
        System.out.println("REGISTERING DIRECTORY " + directory);
        watcher = FileSystems.getDefault().newWatchService();
        keys = new HashMap<WatchKey,Path>();
        WatchKey key = directory.toPath().register(watcher, ENTRY_DELETE);
        keys.put(key, directory.toPath());
        System.out.println("FILE REGISTERED");
    }

    private void processEvents(Path file) {
        for (;;) {

            System.out.println("PROCESSING EVENTS");
            // wait for key to be signalled
            WatchKey key;
            try {
                key = watcher.take();
                System.out.println("SIGNAL FILE");
            } catch (InterruptedException x) {
                return;
            }

            Path dir = keys.get(key);
            if (dir == null) {
                System.err.println("WatchKey not recognized!!");
                continue;
            }

            for (WatchEvent<?> event: key.pollEvents()) {
                Kind<?> kind = event.kind();

                // Context for directory entry event is the file name of entry
                WatchEvent<Path> ev = cast(event);
                Path name = ev.context();
                Path child = dir.resolve(name);

                // print out event
                System.out.format("%s: %s\n", event.kind().name(), child);

                if (kind == ENTRY_DELETE && child.equals(file)) {
                    System.out.println("FILE DELETED");
                    return;
                }
            }

            // reset key and remove from set if directory no longer accessible
            boolean valid = key.reset();
            if (!valid) {
                keys.remove(key);

                // all directories are inaccessible
                if (keys.isEmpty()) {
                    break;
                }
            }
        }
    }

    public static void main(String... args) throws Exception {
        System.setProperty(BOOT_MODULE_PROPERTY, BootModuleLoader.class.getName());
        List<String> argList = Arrays.asList(args);

        if (argList.isEmpty()) {
            throw new RuntimeException("Invalid usage of MainWrapper; actual main-class must be specified");
        }

        String mainClassName = argList.get(0);
        List<String> actualArgs = argList.subList(1, argList.size());

        Class<?> mainClass = getMainClass(mainClassName);

        Method mainMethod = getMainMethod(mainClass);

        wrapper = new MainInvoker(mainMethod, actualArgs.toArray(new String[]{}));
        wrapper.invoke();
    }

    public static Class<?> getMainClass(String mainClassName) throws IOException, URISyntaxException, ModuleLoadException, ClassNotFoundException {
        Class<?> mainClass = null;
        try {
            Module module = Module.getBootModuleLoader().loadModule(ModuleIdentifier.create("swarm.application"));
            ClassLoader cl = module.getClassLoader();
            mainClass = cl.loadClass(mainClassName);
        } catch (ClassNotFoundException | ModuleLoadException e) {
            ClassLoader cl = ClassLoader.getSystemClassLoader();
            mainClass = cl.loadClass(mainClassName);
        }

        if (mainClass == null) {
            throw new ClassNotFoundException(mainClassName);
        }
        return mainClass;
    }

    public static Method getMainMethod(Class<?> mainClass) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        final Method mainMethod = mainClass.getMethod("main", String[].class);

        if (mainMethod == null) {
            throw new NoSuchMethodException("No method main() found");
        }

        final int modifiers = mainMethod.getModifiers();
        if (!Modifier.isStatic(modifiers)) {
            throw new NoSuchMethodException("Main method is not static for " + mainClass);
        }
        return mainMethod;
    }

    @SuppressWarnings("unchecked")
    private <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>)event;
    }

    private final Method mainMethod;

    private final String[] args;

    private WatchService watcher;

    private Map<WatchKey,Path> keys;
}
