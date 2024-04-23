[![](https://jitpack.io/v/ceclin/magic.jms.svg)](https://jitpack.io/#ceclin/magic.jms)

# magic.jms

Simple magic for Java modules.

## How to use

1. Add jitpack.io maven repo. See [JitPack doc](https://docs.jitpack.io/).
2. Add dependency `com.github.ceclin:magic.jms:v1.0.0`

This library can run on Java 1.8, Java 9 and above.

With Java 9 and above, you should add a java option: `--add-opens java.base/java.lang=ALL-UNNAMED`.

## Example

```java
if (JavaModules.isEnabled) {
    JavaModules.getInstance().openAllToAll();
}
```

