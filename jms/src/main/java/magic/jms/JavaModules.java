package magic.jms;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.Optional;

public final class JavaModules {

    public static void main(String[] args) {
        System.out.println(isEnabled);
        java.util.concurrent.Callable<?> access = () -> {
            Class.forName("java.io.ObjectStreamClass")
                    .getDeclaredField("writeReplaceMethod")
                    .setAccessible(true);
            return true;
        };
        if (isEnabled) {
            JavaModules modules = JavaModules.getInstance();
            try {
                access.call();
                System.out.println("Before openAllToAll: ok");
            } catch (Exception e) {
                System.out.println("Before openAllToAll: failed");
            }
            modules.openAllToAll();
            try {
                access.call();
                System.out.println("After openAllToAll: ok");
            } catch (Exception e) {
                System.out.println("After openAllToAll: failed");
            }
        }
    }

    public static final boolean isEnabled =
            Arrays.stream(Runtime.class.getClasses())
                    .anyMatch(clazz -> "Version".equals(clazz.getSimpleName()));

    public static JavaModules getInstance() {
        return Holder.instance;
    }

    private static class Holder {
        private static final JavaModules instance = new JavaModules();
    }

    private final Object ALL_UNNAMED_MODULE;
    private final Object EVERYONE_MODULE;
    private final MethodHandle implAddExportsOrOpensHH;

    private JavaModules() {
        checkJavaLangOpenToSelf();
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            MethodHandles.Lookup moduleLookup = MethodHandles.privateLookupIn(Module.class, lookup);
            ALL_UNNAMED_MODULE = (Module) moduleLookup.findStaticVarHandle(
                    Module.class,
                    "ALL_UNNAMED_MODULE",
                    Module.class
            ).get();
            EVERYONE_MODULE = (Module) moduleLookup.findStaticVarHandle(
                    Module.class,
                    "EVERYONE_MODULE",
                    Module.class
            ).get();
            implAddExportsOrOpensHH = moduleLookup.findSpecial(
                    Module.class,
                    "implAddExportsOrOpens",
                    MethodType.methodType(void.class, String.class, Module.class, boolean.class, boolean.class),
                    Module.class
            );
        } catch (IllegalAccessException | NoSuchFieldException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkJavaLangOpenToSelf() {
        Module selfModule = getClass().getModule();
        String selfName = "ALL-UNNAMED";
        if (selfModule.isNamed()) {
            selfName = selfModule.getName() + '/' + getClass().getPackageName();
        }
        Optional<Module> module = ModuleLayer.boot().findModule("java.base");
        if (module.isPresent()) {
            if (module.get().isOpen("java.lang", selfModule)) {
                return;
            }
        }
        throw new RuntimeException(
                "magic.jms: use java option `--add-opens java.base/java.lang=" + selfName + "` to use magic.jms");
    }

    private void implAddExportsOrOpens(Object thisModule, String pn, Object otherModule, boolean open, boolean syncVM) {
        try {
            implAddExportsOrOpensHH.invoke(thisModule, pn, otherModule, open, syncVM);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public void openAllToAll() {
        ModuleLayer.boot().modules().forEach(module -> {
            module.getPackages().forEach(pkg -> {
                implAddExportsOrOpens(module, pkg, EVERYONE_MODULE, true, true);
            });
        });
    }

    public void exportAllToAll() {
        ModuleLayer.boot().modules().forEach(module -> {
            module.getPackages().forEach(pkg -> {
                implAddExportsOrOpens(module, pkg, EVERYONE_MODULE, false, true);
            });
        });
    }

    public void openAllToAllUnnamed() {
        ModuleLayer.boot().modules().forEach(module -> {
            module.getPackages().forEach(pkg -> {
                implAddExportsOrOpens(module, pkg, ALL_UNNAMED_MODULE, true, true);
            });
        });
    }

    public void exportAllToAllUnnamed() {
        ModuleLayer.boot().modules().forEach(module -> {
            module.getPackages().forEach(pkg -> {
                implAddExportsOrOpens(module, pkg, ALL_UNNAMED_MODULE, false, true);
            });
        });
    }

    public void openToAll(Object module) {
        ((Module) module).getPackages().forEach(pkg -> {
            implAddExportsOrOpens(module, pkg, EVERYONE_MODULE, true, true);
        });
    }

    public void exportToAll(Object module) {
        ((Module) module).getPackages().forEach(pkg -> {
            implAddExportsOrOpens(module, pkg, EVERYONE_MODULE, false, true);
        });
    }

    public void openToAllUnnamed(Object module) {
        ((Module) module).getPackages().forEach(pkg -> {
            implAddExportsOrOpens(module, pkg, ALL_UNNAMED_MODULE, true, true);
        });
    }

    public void exportToAllUnnamed(Object module) {
        ((Module) module).getPackages().forEach(pkg -> {
            implAddExportsOrOpens(module, pkg, ALL_UNNAMED_MODULE, false, true);
        });
    }

    public void openAllTo(Object targetModule) {
        ModuleLayer.boot().modules().forEach(module -> {
            module.getPackages().forEach(pkg -> {
                implAddExportsOrOpens(module, pkg, targetModule, true, true);
            });
        });
    }

    public void exportAllTo(Object targetModule) {
        ModuleLayer.boot().modules().forEach(module -> {
            module.getPackages().forEach(pkg -> {
                implAddExportsOrOpens(module, pkg, targetModule, false, true);
            });
        });
    }

    public void openTo(Object module, Object targetModule) {
        ((Module) module).getPackages().forEach(pkg -> {
            implAddExportsOrOpens(module, pkg, targetModule, true, true);
        });
    }

    public void exportTo(Object module, Object targetModule) {
        ((Module) module).getPackages().forEach(pkg -> {
            implAddExportsOrOpens(module, pkg, targetModule, false, true);
        });
    }

    public void openTo(Object module, String pkg, Object targetModule) {
        if (((Module) module).getPackages().contains(pkg)) {
            implAddExportsOrOpens(module, pkg, targetModule, true, true);
        }
    }

    public void exportTo(Object module, String pkg, Object targetModule) {
        if (((Module) module).getPackages().contains(pkg)) {
            implAddExportsOrOpens(module, pkg, targetModule, false, true);
        }
    }

}
