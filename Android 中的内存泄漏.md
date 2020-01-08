# Android 中的内存泄漏 

## 可达性分析 

### 根搜索算法 

根搜索算法是从离散数学中的图论引入的，程序把所有的引用关系看成一张图，从一个节点`GC ROOT`开始，寻找对应的引用节点，找到这个节点以后，继续寻找这个节点的引用节点，当所有的引用节点寻找完毕之后，剩余的节点则被认为是没有被引用到的节点，即无用的节点。 ![根搜索算法](https://images0.cnblogs.com/blog2015/694841/201506/141050566294022.jpg) java 中可以作为`GC ROOT`的对象有：

1. 虚拟机栈中引用的对象**(栈帧-局部变量表)**
2. 方法区中静态属性引用的对象 
3. 方法区中常量引用的对象
4. 本地方法栈中引用的对象(`Native`对象) 

## 内存回收算法 

### 标记-清除算法 

标记-清除算法首先会标记所有可以被回收的对象，在标记完成后统一回收所有被标记的对象。但是这种回收算法在清除后会有大量空间不连续的内存碎片。

 ![标记清除算法](https://images2018.cnblogs.com/blog/1090617/201806/1090617-20180621213112064-1445464345.png) 

### 复制算法 

复制算法将内存分为大小相等的两块，每次使用其中的一块。当这一块内存使用完以后，将所有还存活的对象移到另一块内存区域，然后再把使用的这部分内存空间清理掉，这样每次都是对内存区间的一半进行回收。 

![复制算法](http://img.qingtingip.com/crawler/article/2019427/c71c7276daa838383c23b4bb068ab365) 

## java运行时内存结构 

`java` 运行时内存主要分为：`java栈`，`本地方法栈`，`程序计数器`，`方法区`和`堆`。 ![jvm运行时结构](https://images2015.cnblogs.com/blog/331425/201606/331425-20160623115846235-947282498.png) 

### 栈内存 

栈是线程私有的内存空间，对于一个线程而言，每一个方法被执行就伴随着一个栈帧入栈，方法执行完毕就伴随着 一个栈帧出栈。

![jvm栈](https://www.jiafly.com/image/jvm/jvm-stack.jpg) 

**栈帧：** `栈帧`是支持虚拟机方法执行和调用的一个数据结构，每当一个新的方法被执行就会生成一个新的`栈帧`，`栈帧`存储了方法的`局部变量表`，`操作数栈`，`动态链接`，`方法返回地址`和一些额外信息。 在编译代码的时候，栈帧中需要多大的`局部变量表`和`操作数栈`的深度就已经确定了，被写进字节码的方法表`Code`属性中。

**局部变量表：**`局部变量表`是变量值的存储空间，用于存放参数和方法内部定义的局部变量。`局部变量表`的最小单位是槽(slot)。`jvm`并没有定义一个槽应该占用的大小，但是规定一个槽应该可以存放一个32位以内的数据类型。 

**操作数栈：**`操作数栈`同样是一个栈结构。随着方法的执行，会从`局部变量表`或者对象`实🌰`的字段中复制常量或变量写入操作数栈，也会随着计算将栈中元素出栈到局部变量表或者返回给方法的调用者。一个方法的执行期间往往伴随着多个这样的出栈/入栈操作。 ### 方法区 `jvm`中定义`方法区`是堆的一个逻辑部分。`方法区`中主要存储以下信息： 1. 已经被虚拟机加载的类信息 2. 常量 3. 静态变量 4. 即时编译后的代码 

### 堆内存 

`jvm`运行时，对象分配的区域主要在堆内存中。`jvm堆`是`jvm`所管理的内存中最大的一块，是被所有的线程所共享的一块内存区域，用来存放对象`实🌰`，几乎所有的对象`实🌰`都在这里分配。 

![jvm堆内存区](https://images0.cnblogs.com/blog2015/694841/201506/141332429573819.jpg) 

 jvm堆内存主要分为：`新生代`,`老年代`和`永久代`。 

#### 新生代 

其中，`新生代`又分为`Eden区`，`FromSpace` 和`ToSpace`，其比例一般为`8:1:1`。`jvm`中的新对象一般都出生在`Eden区`，大对象会直接分配进`老年代`。`FromSpace`和`ToSpace`区主要用来做复制算法的存储空间。当新对象生成`Eden`申请内存空间失败时，则会发起一次`Scavenge GC`。 

#### 老年代 

在新生代经历了多次(一般15)`Scavenge GC`仍然存活的对象会被移到老年代，老年代存放的一般都是生命周期比较长的对象，内存也比新生代大很多(1:2)，当老年代内存满时会触发`FULL GC`。 

#### 永久代 

用于存放静态文件(`class`类，方法)和常量等，持久代对垃圾回收并没有显著影响，主要回收废弃类和无效常量。`Java SE8`以后被移除。取代它的是元空间。 

### 垃圾回收 

新生代`gc`会将`Eden区`中存活的对象放入`FromSpace`中，然后清空`Eden区`内存，如果`FromSpace`也存放满了，就将`Eden区`和`FromSpace`中存活的对象移到`ToSpace`,然后清空`Eden区`和`FromSpace`的内存空间，最后将`ToSpace`中对对象再移到`FromSpace`中。 

当`ToSpace`不足以存放`Eden区`和`FromSpace`中的存活对象时，就将存活的对象直接存放到老年代。 

对象每在`FromSpace区`躲过一次`GC`，其对象年龄变会+1，默认情况下，对象年龄达到15岁，就会移动到老年代。如果老年代满了就会触发一次`FULL GC`，则新生代老年代都会回收。如果创建新对象后新生代老年代都无法分配，可能就会触发`oom`。 

## Android 中的内存分配 

在`android`中，一般一个进程对应一个`vm`，一个进程的最大内存有限制，一般超出了内存就会报`oom`。并且对于移动设备而言，可用内存较`pc`也会小很多。 

既然`android`是一个进程对应一个`vm`，不考虑`ams`调度四大组件生命周期的情况，一个进程从`ActivityThread`的`main`方法开始创建了`ActivityThread`实🌰并开启了`Looper循环`。这样也就导致了`ActivityThread`实🌰一直都在当前`main`方法栈帧的局部变量表上。同样的该`ActivityThread`实🌰对象作为`GC ROOT`,被该对象直接/间接引用到的所有对象也都无法释放内存，除非该对象主动释放。 

**ActivityThrad.java**

```java
public static void main(String[] args) {
		...
		Looper.prepareMainLooper();
		...
		ActivityThread thread = new ActivityThread();
        thread.attach(false, startSeq);
        ...
        // 开启looper死循环
        Looper.loop();
	}

final ArrayMap<IBinder, ActivityClientRecord> mActivities = new ArrayMap<>();
```

 其中它的全局变量`mActivities`用来管理`activity`的创建和销毁，管理`activity`内存中的生命周期。 

### Android 中常见的内存泄漏 

#### 被静态类持有引发的内存泄漏 

当一个需要被回收的对象，被静态类持有会导致该对象除了进程死亡否则永远无法被释放。例如：

```kotlin
object Utils {
    private lateinit var context: Context
    
    fun init(context: Context){
        this.context = context
    }
}
```

由于声明了`object`关键字，该类会被声明成单例，如果`context`传入`activity`，则永远无法被释放。因为对象被`方法区`中的静态属性引用，而该引用又为`GC ROOT`，所以除非主动调用释放接口，否则无法被释放。 

#### 全局变量引发的泄漏 

对于某些不是必要全局存储的变量，在方法的局部变量里声明即可，例如`DialogFragment`实现的某些只需要弹出一次的弹窗，在`show`的时候会通过`fragmentManager`调度声明周期进入`onCreateView()`，在`dismiss`之后生命周期就进`onDestory`了，如果这时候有个正在生命周期内在`activity/fragment`有个全局变量持有它，那么它是无法释放的。如果这时候调用它的`requireContext()`,可能会抛出`NullPointException`。 

```java
public void show(@NonNull FragmentManager manager, @Nullable String tag) {
        mDismissed = false;
        mShownByMe = true;
        FragmentTransaction ft = manager.beginTransaction();a
        ft.add(this, tag);
        ft.commit();
}

void dismissInternal(boolean allowStateLoss, boolean fromOnDismiss) {
	...
	FragmentTransaction ft = requireFragmentManager().beginTransaction();
            ft.remove(this);
            if (allowStateLoss) {
                ft.commitAllowingStateLoss();
            } else {
                ft.commit();
            }
}
```

因此，对于生命周期并小于当前对象同步的对象，还是不要去全局持有它，让它们在合理的时候自己被释放掉，才不会引发一系列的问题，也会使内存更好的被管理。 

#### Handler 引发的内存泄漏 

例如： 

```kotlin
class HomeActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        handler.postDelayed({
            btn.text = "点击"
        }, 3000)
    }
    
}
```

对于某些要用到线程通信或者延时任务的情况，有时候需要使用`Handler`,而对于`android`中的内存通信而言，`Handler.post()/Handler.postDelay()`方法会将`post`的对象包装成一个`Message`对象，然后将`Message`对象放到`MessageQueue`队列中去，直到被`looper`从`MessageQueue`中取出来并消费掉，才会被移除掉。 

正确的方式应该是，在`onPause/onDestory`的时候调用`handler.removeCallbacks(runnable)`让该引用从`MessageQueue`中移除掉。

```kotlin
class HomeActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())

    private val r = Runnable {
        btn.text = "点击"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        handler.postDelayed(r, 3000)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(r)
    }

}
```

 这种情况的实际引用如下图： 

![handler 泄漏](https://raw.githubusercontent.com/catdrinker/LeakTest/master/image/handler%20%E6%B3%84%E6%BC%8F.png)

可以看到,由于`MessageQueue`在`Looper.looper()`的方法栈帧中的局部变量表中，因此`MessageQueue`不会被释放，当调用`handler.post/postDelayed(Runnable)`会将该`Runnable`包装成`Message`对象放进队列。由于匿名内部类`Runnable`持有`Activity`的引用，如果只是正常的在主线程`post`是没有问题的，因为马上就会被消费掉，而调用了`postDelay`的`Message`则会被按时间顺序被排在队列靠后的某个位置，等到了时间才会去消费。 

因此在ui调用了`handler.postDelay(Runnable r)`的也要在响应的生命周期去从队列中移除掉这条消息。 

同理：如果定义了匿名内部类`handler`，那么这里的`handler`持有了`activity`的引用，同时由于`handler`也会被每条它`post`出去的`message`持有引用，从而导致除非该`handler post`的每一条消息都被消费结束，否则仍然会导致内存泄漏。

#### 匿名内部类生命周期不一致导致的内存泄漏 

对于一个需要被及时回收的对象而言，如果其因为各种原因，被异步回调的`callback`所引用，那么就会引发该对象的内存泄漏。这里异步回调可能引发内存泄漏的原因是由于，非静态的内部类(包括匿名)会持有外部类的引用，因此一旦该匿名内部类的对象被生命周期比当前被匿名引用的更长，就会导致该对象的生命周期被劫持：

#### 匿名内部类对外部类引用

匿名内部类在编译成字节码的时候遵循以下的规则：

1. 会隐式地持有外部类对象的引用，作为其内部的全局变量`$0`；
2. 对于外部对象而言，如果其或者其内部类对该对象有引用，则会被隐式的加入到该`class`的全局变量`$xxx`。例如：

```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var controller: Controller

    private val callback: Callback = object : Callback {
        override fun onCallback() {
            Log.i("MainActivity","onCallback ")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        controller = Controller()
        controller.start(callback)
    }

}


interface Callback {

    fun onCallback()
}
```

```kotlin
class Controller {

    fun start(callback: Callback){
        // 网络请求，db操作等异步操作
        Thread {
            Thread.sleep(2000)
            callback.onCallback()
        }.start()
    }

}
```

 这种情况下的实际引用如下图：

![callback泄漏](https://raw.githubusercontent.com/catdrinker/LeakTest/master/image/callback%E6%B3%84%E6%BC%8F.png)

可以看到，`ActivityThread`相关方法上的局部变量表会短暂的持有`activity`，伴随着方法出栈又回断开这种引用。 同样的对于`ActivityThread`的`mActivities`这个用来存储正在运行的`activity`的容器，也会伴随着相关`activity`的生命周期来持有和释放`activity`。而上述代码的情况是导致开了一个新的线程，从`native`层持有了该`Thread`实例的引用，而`Thread`又间接的持有了`activity`， 当线程中的方法执行完毕或者被主动中断，才会释放掉这个引用，从而`activity`到`Thread`对象的引用链由于没有`GC ROOT`，会被`GC`回收内存。

这样就引发了内存泄漏。如果该线程是在有限的时间内运行结束，那么`callback/activity`泄漏的时间是有限的，这种泄漏认为是一个**短时间的内存泄漏**。 

如果这个线程设定为不可结束，`native`对该`Thread`对象的引用一直存在，那么`activity`除非`vm`进程被杀否则永远都不会被回收，这种泄漏后果会更加严重，一份无法被回收的内存，尤其是`activity`的内存，会大大增加应用`oom`的风险。这种泄漏是**永久性内存泄漏**。  

#### 动画引发的内存泄漏 

动画之所以会内存泄漏，是因为`AnimatorSet`实际上被一个单例持有引用，即被方法区中的静态变量`GC ROOT`持有引用，这就导致如果不在页面退出的时候调用`cancel()`导致界面被回收的时候该动画的内存还没有被回收，如果该动画为无限循环动画，那么这块动画的内存就永远无法被回收。 

**调用start()**

```java
private void start(boolean inReverse, boolean selfPulse) {
	...
	boolean isEmptySet = isEmptySet(this);
        if (!isEmptySet) {
            startAnimation();
        }
    ...
}

private void startAnimation() {
        addDummyListener();
        // Register animation callback
        addAnimationCallback(0);
		...
}

private void addAnimationCallback(long delay) {
        if (!mSelfPulse) {
            return;
        }
        AnimationHandler handler = AnimationHandler.getInstance();
        handler.addAnimationFrameCallback(this, delay);
}

```

**调用cancel()**

```java
public void cancel() {
	if (isStarted()) {
		...
		endAnimation();
	}
}

private void endAnimation() {
	...
	removeAnimationCallback();
}

private void removeAnimationCallback() {
        if (!mSelfPulse) {
            return;
        }
        AnimationHandler handler = AnimationHandler.getInstance();
        handler.removeCallback(this);
    }
}
```



这种情况实际引用如下图 

![动画泄漏](https://raw.githubusercontent.com/catdrinker/LeakTest/master/image/%E5%8A%A8%E7%94%BB%E6%B3%84%E6%BC%8F.png)

当调用`cancel()`以后，`AnimationHandler`单例对`Animator`引用释放，即使此时`Animator`仍然持有`Activity`的引用，由于从`GC ROOT`找不到它们，仍然会被`GC`回收。 

#### 丢引用导致的内存泄漏 

当一个对象需要被释放但是还没有被释放，之前我们对它的引用被丢了且没有调用对应释放的方法，且该对象还被其它的`GC ROOT` 持有引用，这种情况下就会引发内存泄漏。 

#### 资源未释放导致的引用

 比如文件流等打开了却没有调用`close`释放，`socket`打开了没有释放，音频视频没有释放等。要及时调用响应的接口释放内存。 

### 内存泄漏的解决方式 

从本质上来看，内存泄漏的引发原因即使**在对象生命周期应该结束的时候，还有其它的`GC ROOT`持有该对象的引用。**因此要从本质入手，在对象生命周期结束的时候移除掉任何外部`GC ROOT`对该对象的引用。 

#### WeakReference 避免强引用内存泄漏 

由于匿名内部类会静态持有`activity/fragment`等外部类的引用，因此不能用匿名内部类，内部类要使用静态类，并且通过`WeakReference`包装`activity`来获得其引用，避免内存泄漏。 在`kotlin`中，默认在内部声明的类就是`static`的，要指定非`static`的，就需要加上关键字`inner`。使用`WeakReference`方式如下：

```kotlin
interface Callbacks{
    @MainThread
    fun onCallback()
}

class Callback(activity: Activity):Callbacks{
        private val activityRef = WeakReference(activity)

        @MainThread
        override fun onCallback() {
            val activity = activityRef.get()
            if(activity!=null){
                activity.btn.text = "OnClick"
            }
        }
    }
 
```

#### livedata 避免activit/fragment 内存泄漏 

对于存在的异步回调而言,配合`livedata`的观察者模式，使用如下方式：

```kotlin
livedata.observer(activity,Observer{
	text.text = it?:""
})
```

可以确保不会出现因为异步的`callback`持有`activity`的引用而导致内存泄漏的问题。 在`activity/fragment`中会持有`Lifecycle`的引用，同时把自己的引用传进`LifecycleRegistry`中，其中使用`WeakReference`保证`activity/fragment`实例不会被泄漏。

```java
public class LifecycleRegistry extends Lifecycle {
	...
    /**
     * The provider that owns this Lifecycle.
     * Only WeakReference on LifecycleOwner is kept, so if somebody leaks Lifecycle, they won't leak
     * the whole Fragment / Activity. However, to leak Lifecycle object isn't great idea neither,
     * because it keeps strong references on all other listeners, so you'll leak all of them as
     * well.
     */
  private final WeakReference<LifecycleOwner> mLifecycleOwner;
    
	@Override
	public void addObserver(@NonNull LifecycleObserver observer) {
        State initialState = mState == DESTROYED ? DESTROYED : INITIALIZED;
        ObserverWithState statefulObserver = new ObserverWithState(observer, initialState);
        ObserverWithState previous = mObserverMap.putIfAbsent(observer, statefulObserver);
        ...
	}
}
```

在调用`livedata.observe()`的时候，会包装一个`LifecycleBoundObserver`对象，添加进`mObservers`和`LifecycleBoundObserver`对象里的`mObserverMap`中。 

```java
public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<? super T> observer) {
        assertMainThread("observe");
        if (owner.getLifecycle().getCurrentState() == DESTROYED) {
            // ignore
            return;
        }
        LifecycleBoundObserver wrapper = new LifecycleBoundObserver(owner, observer);
        ObserverWrapper existing = mObservers.putIfAbsent(observer, wrapper);
        ...
        owner.getLifecycle().addObserver(wrapper);
    }
```

 而在`LifecycleBoundObserver`中，在`lifecycle`的`state`为`onDestory`的时候，回调用`removeObserver()`移除相关引用。

```java
class LifecycleBoundObserver extends ObserverWrapper implements GenericLifecycleObserver {
	  @Override
    public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {
            if (mOwner.getLifecycle().getCurrentState() == DESTROYED) {
                removeObserver(mObserver);
                return;
            }
            activeStateChanged(shouldBeActive());
        }
}
	  
```

```java
    @Override
    void detachObserver() {mOwner.getLifecycle().removeObserver(this);}
```

```java
public void removeObserver(@NonNull final Observer<? super T> observer) {
        assertMainThread("removeObserver");
        ObserverWrapper removed = mObservers.remove(observer);
        if (removed == null) {
            return;
        }
        removed.detachObserver();
        removed.activeStateChanged(false);
    }
```

在`removeObserver`的时候，移除了`livedata`中的`mObservers`的引用和`LifecycleRegistry`中`mObserverMap`的引用。 这样其实就是其实就是由于`observer`作为匿名内部类持有`activity/fragment`引用，在需要的时候将`observer`的引用加入到`livedata`模块，们在`activity/fragment`的`destory`中又解除了对`observer`的引用，通过对生命周期的管理来控制引用的指向和释放，来达到避免内存泄漏的目的。

![livedata 内存管理](https://raw.githubusercontent.com/catdrinker/LeakTest/master/image/livedata%20%E5%86%85%E5%AD%98%E7%AE%A1%E7%90%86.png)

##### 关于自定义view 

需要注意的是，`livedata`只负责`activity/fragment`生命周期管理和回调，如果在**自定义view**中有上述内存泄漏的操作，必须主动去释放内存，否则由于`view`持有`context`一般为`Activity`,仍然会引发内存泄漏。 因此在**自定义view**中，尽量明确只做`view`相关的事情，不要做如网络请求等其它操作，如果实在做了，也要在`onDetach`回调中移除掉相关引用。 

####  主动在解除引用 

因为`livedata`是`android`官方提供实现，耦合进了`activity/fragment`中，所以我们不需要自己做额外的处理，而引用其它三方库如`EventBus`，`协程`，`RxJava`等，需要在`onDestroy/onDestroyView`等回调中移除引用或者取消任务来断掉对`activity/fragment`直接或间接的引用。 如果我们自己需要去管理相关的内存不要泄漏，如上述举例异步回调内存泄漏的代码，修改`controller`如下：

```kotlin
class Controller {
    
    private var mCallback:Callback? = null

    fun start(callback: Callback){
        mCallback = callback
        // 网络请求，db操作等异步操作
        Thread {
            Thread.sleep(2000)
            mCallback?.onCallback()
        }.start()
    }
    
    fun removeCallback(){
        mCallback = null
    }

}
```

需要在`activity.onDestroy`的时候调用`removeCallback`的方法来移除掉`callback`，这样，也就是在`onDestroy`的时候移除掉了`controller`对之前那个`callback`的引用。 如果以`list`的方式存储，那么就需要标记属于当前页面的`callback`，并且需要记录每个线程执行的时候对应哪一个`callback`，并且在页面`onDestroy`的时候，移除掉所有的当前页面的`callback`，切断对`callback`的引用。

```kotlin
class Controller {
    private val callbacks = ConcurrentHashMap<String, ConcurrentHashMap<Int, MainCallback>>()

    @Volatile
    private var seq = 0

    private val mainHandler = Handler(Looper.getMainLooper())

    fun start(callback: MainCallback) {
        synchronized(this) {
            seq++
        }
        val key = callback.key
        val mutableMap = callbacks[key]
        if (mutableMap == null) {
            seq = 1
            val map = ConcurrentHashMap<Int, MainCallback>()
            map[seq] = callback
            callbacks[key] = map
        } else {
            mutableMap[seq] = callback
        }
        Thread(object : MyRunnable(seq) {
            override fun run() {
                // 网络请求，db操作等异步操作
                val time = (0..10000L).random()
                Log.i("MainActivity","time $time")
                Thread.sleep(time)
                mainHandler.post {
                    callbacks[key]?.get(mSeq)?.onCallback(mSeq)
                }
            }
        }).start()
    }

    fun removeCallback(key: String) {
        callbacks.remove(key)
    }

    abstract inner class MyRunnable(val mSeq: Int) : Runnable
}
```

```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var controller: Controller

    private lateinit var btn: Button

    private val key = MainActivity::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btn = findViewById(R.id.btn)
        controller = Controller()
        btn.setOnClickListener {
            for (i in 0 until 200) {
                controller.start(object : MainCallback(key) {
                    override fun onCallback(seq: Int) {
                        Log.i("MainActivity", "onCallback $seq")
                    }
                })
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("MainActivity", "remove all callback")
        controller.removeCallback(key)
    }

}


interface Callback {
    fun onCallback(seq: Int)
}

abstract class MainCallback(val key: String) : Callback
```

其引用如下：

![自定义控制生命周期剥离](https://raw.githubusercontent.com/catdrinker/LeakTest/master/image/%E8%87%AA%E5%AE%9A%E4%B9%89%E6%8E%A7%E5%88%B6%E7%94%9F%E5%91%BD%E5%91%A8%E6%9C%9F%E5%89%A5%E7%A6%BB.png)

### 检测内存泄漏

1. 输出`adb`命令检测，快速进入和退出多次可能泄漏的页面和执行泄漏可能产生的操作。最后退回后输出命令`adb shell dumpsys meminfo com.xx.xxx`,这里的`com.xx.xx`是应用的包名。
2. 通过`Android Studio`自带的`Profile`工具，执行可能泄漏的操作，退出页面后快速点击`GC`的按钮，按下`dump`dump出当前的内存，通过`filter`出相关类名，即可看到当前实例的个数，若大于0，则说明发生了泄漏。
3. 通过`mat`，`mat`主要用来分析泄漏的原因，根据可达性分析算法列出整条引用链，即可根据引用链推导出泄漏产生的地方。
4. 应用接入`leakcanary`在开发阶段分析可能产生的内存泄漏，出现泄漏会列出相关的引用链定位泄漏源。