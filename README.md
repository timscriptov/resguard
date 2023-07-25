[![](https://jitpack.io/v/TimScriptov/resguard.svg)](https://jitpack.io/#TimScriptov/resguard)

## Add it in your root build.gradle at the end of repositories:
```groovy
    allprojects {
        repositories {
            //...
            maven { url 'https://jitpack.io' }
        }
    }
```

## Add the dependency
```groovy
    dependencies {
        implementation 'com.github.TimScriptov:resguard:Tag'
    }
```

## Obfuscate resources
```kotlin
    ResGuard(File("path/inFile.apk"), File("path/outFile.apk"), false, null)
```

```java
    ResGuard(new File("path/inFile.apk"), new File("path/outFile.apk"), false, null);
```