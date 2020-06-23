#### 1. 将`ISay`和`SayHotFix`生成jar
先编译成.class文件，然后在`src`目录下，注意是`src`目录，不然下面生成dex操作会报错
```shell
jar cvf com/cnting/dexclassloaderhotfix/say.jar com/cnting/dexclassloaderhotfix/ISay.class com/cnting/dexclassloaderhotfix/SayHotfix.class
```

#### 2. 将jar文件编译成dex
之前一开始是在.class同级目录下生成jar，会一直报下面这个错误，所以一定要改到`src`目录下去生成
![error1](https://github.com/cnting/LagouCourse/blob/master/course05_ClassLoader/DexClassLoaderHotFix/res/1.jpg)
生成dex命令：
```shell
~/Library/Android/sdk/build-tools/29.0.2/dx --dex --output=say_hotfix.jar com/cnting/dexclassloaderhotfix/say.jar
```

##### 补充：为啥要变成dex
因为Android的虚拟机不认识java打出来的jar，需要转换一下

#### 3. 报错
源代码如下：
```kotlin
btn.setOnClickListener {
            val jarFile = File(getExternalFilesDir(null)!!.path + File.separator + "say_hotfix.jar")
            if (!jarFile.exists()) {
                val say = SayException()
                Toast.makeText(this, say.saySomething(), Toast.LENGTH_SHORT).show()
            } else {
                val dexClassLoader = DexClassLoader(jarFile.absolutePath, getExternalFilesDir(null)!!.absolutePath, null, classLoader)
                //打印jarFile:/storage/emulated/0/Android/data/com.cnting.dexclassloaderhotfix/files/say_hotfix.jar
                Log.d("===>", "classLoader:$dexClassLoader")

                val c = dexClassLoader.loadClass("com.cnting.dexclassloaderhotfix.ISay")
                //打印c:interface com.cnting.dexclassloaderhotfix.ISay,classLoader:dalvik.system.PathClassLoader[DexPathList[[zip file "/data/app/com.cnting.dexclassloaderhotfix-xXZ0i3BN3Ji0DI8DQHAZ6A==/base.apk"]
                Log.d("===>", "c:$c,classLoader:${c.classLoader}")
                //报错
                val clazz = dexClassLoader.loadClass("com.cnting.dexclassloaderhotfix.SayHotFix")
                Log.d("===>", "clazz:$clazz")
                val iSay = clazz.newInstance() as ISay
                Toast.makeText(this, iSay.saySomething(), Toast.LENGTH_SHORT).show()
            }
        }
```
报错如下：
![error2](https://github.com/cnting/LagouCourse/blob/master/course05_ClassLoader/DexClassLoaderHotFix/res/2.jpg)

原因在这里：[Android插件化框架系列之类加载器](https://www.jianshu.com/p/57fc356b9093)

![error3](https://github.com/cnting/LagouCourse/blob/master/course05_ClassLoader/DexClassLoaderHotFix/res/3.jpg)

解决方式：[热修复实现：ClassLoader 方式的实现](https://jaeger.itscoder.com/android/2016/09/20/nuva-source-code-analysis.html)

![error4](https://github.com/cnting/LagouCourse/blob/master/course05_ClassLoader/DexClassLoaderHotFix/res/4.jpg)

主要原理就是将补丁包的dex插到dexElements的最前面，让它优先加载
```java
class DexUtil {
    //合并dex
    public static void injectDexAtFirst(DexClassLoader newClassLoader) throws NoSuchFieldException, IllegalAccessException, ClassNotFoundException {
        Object baseDexElements = getDexElements(getPathList(getPathClassLoader()));
        Object newDexElements = getDexElements(getPathList(newClassLoader));
        Object allDexElements = combineArray(newDexElements, baseDexElements);
        Object pathList = getPathList(getPathClassLoader());
        setField(pathList, pathList.getClass(), "dexElements", allDexElements);
    }

    private static PathClassLoader getPathClassLoader() {
        PathClassLoader pathClassLoader = (PathClassLoader) DexUtil.class.getClassLoader();
        return pathClassLoader;
    }

    private static Object getDexElements(Object paramObject)
            throws IllegalArgumentException, NoSuchFieldException, IllegalAccessException {
        return getField(paramObject, paramObject.getClass(), "dexElements");
    }

    private static Object getPathList(Object baseDexClassLoader)
            throws IllegalArgumentException, NoSuchFieldException, IllegalAccessException, ClassNotFoundException {
        return getField(baseDexClassLoader, Class.forName("dalvik.system.BaseDexClassLoader"), "pathList");
    }

    private static Object combineArray(Object firstArray, Object secondArray) {
        Class<?> localClass = firstArray.getClass().getComponentType();
        int firstArrayLength = Array.getLength(firstArray);
        int allLength = firstArrayLength + Array.getLength(secondArray);
        Object result = Array.newInstance(localClass, allLength);
        for (int k = 0; k < allLength; ++k) {
            if (k < firstArrayLength) {
                Array.set(result, k, Array.get(firstArray, k));
            } else {
                Array.set(result, k, Array.get(secondArray, k - firstArrayLength));
            }
        }
        return result;
    }

    private static Object getField(Object obj, Class<?> cl, String field)
            throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Field localField = cl.getDeclaredField(field);
        localField.setAccessible(true);
        return localField.get(obj);
    }

    private static void setField(Object obj, Class<?> cl, String field, Object value)
            throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Field localField = cl.getDeclaredField(field);
        localField.setAccessible(true);
        localField.set(obj, value);
    }
}

```

