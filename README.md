# Skate
Seamless fragment navigation made easy.
Skate is a fragment manager controller that allows you to easily navigate from one fragment to another, show, and hide fragments with all the already available methods in FragmentManager.
Currently Skate only has support for Kotlin and AndroidX.
- State retention
- Doesn't use reflection.
- Doesn't hold any references
- Ability to add/attach/show fragments and their respective counterparts.
- Allows commit with state save.
- Lightweight and fast
- Rotation support
- Lifecycle aware

Skate uses its own internal stack that manages fragments with clarity. The stack is pushed or popped only when a fragment is *added* or *removed*. Otherwise, any modification is applied accordingly in the stack. This prevents having to create new objects just to record the state of a fragment. 

## Setup
```gradle
allprojects {
    repositories {
	    maven { url 'https://jitpack.io' }
    }
}
```
```gradle
dependencies {
    implementation 'na.komi.inshiro:skate:0.1.0'
}
```

## Initialisation
```kotlin
class MainActivity : AppCompatActivity() {

	val skate by startSkating()
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)  
		skate.fragmentManager = supportFragmentManager
		skate.container = R.id.main_container
		skate.mode = Skate.FACTORY // optional
	}
	
	override fun onBackPressed() {
		if (!skate.back)
		    super.onBackPressed()
	}
}
```


## Showing a fragment
```kotlin
skate show fragment
```



## Hiding a fragment
```kotlin
skate back fragment
// OR
skate hide fragment
```

## Navigating to another fragment

To hide the current visible fragment and then show the destination
```kotlin
skate to fragment
```
#
Yes it's that simple!
#
## Modular fragments
With Skate, you can define `modular` fragments which will hide alongside the currently visible fragment on navigation.
```kotlin
skate.show(fragment, modular = true)
...
skate to fragment
```

## Syntax
#### Settings
At any given time you can call these to set the global options before showing or hiding your fragment.
```kotlin
skate.fragmentManager = supportFragmentManager
skate.container = R.id.main_container // id of your view
skate.mode = Skate.FACTORY
```
#### Modes
Skate has 3 modes it operates on. The default is `FACTORY`. The initial state is saved and its corresponding counterpart is used on its corresponding method. If you show, Skate will hide. Add, remove. etc..
To override this behavior, specify the mode you want to use.
```kotlin
// == CONSTANTS ==

// add-remove
Skate.FACTORY = 0

// attach-detach
Skate.SPARING = 1

// show-hide
Skate.SINGLETON = 2


// Use it like
skate.show(fragment, Skate.SINGLETON) // or hide

// OR
skate.show(fragment, mode = Skate.SINGLETON) // or hide
```
#### Back stack
By default Skate adds a fragment to the back stack. To disable this, call
```kotlin
// If you're showing
skate.show(fragment, addToBackStack = false) // or hide
```

#### Chaining parameters
```kotlin
skate.show(fragment, Skate.SPARING, true, false)

// OR
skate.show(fragment = fragment,
	   mode = Skate.SPARING,
	   addToBackStack = true,
	   modular = false)
```
#### Operations
Sometimes you have lots of fragments you want to work with.
For working with a long chain of fragments
```kotlin
skate.operate {
   hide(BillingFragment)
   hide(PaymentFragment)
   hide(DetailFragment1)
   show(MainFragment)
}
```
## Handling configuration changes
Fragments should not be declared as singletons and important data should not be stored in them, the same goes for Activities. Setup your fragments in the `onCreate()` method like so
```kotlin
val mainFragment by lazy { MainFragment() }
```


## Fragment Lifecycle
If your application requires a call to any of Skate's methods in an Activity lifecycle method other than `onCreate()`, do it in either `FragmentActivity#onResumeFragments()` or `Activity#onPostResume()`. These two methods are guaranteed to be called after the Activity has been restored to its original state, and therefore avoid the possibility of state loss all together.
 See this [blog](https://www.androiddesignpatterns.com/2013/08/fragment-transaction-commit-state-loss.html) for more detail.


License
-------

    Copyright 2019 inshiro

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.