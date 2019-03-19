# Skate
[![Release](https://jitpack.io/v/inshiro.skate/Repo.svg)](https://jitpack.io/#inshiro/skate) [![GitHub license](https://img.shields.io/badge/license-Apache_2-blue.svg)](LICENSE) [![Platform](https://img.shields.io/badge/platform-AndroidX-brightgreen.svg)](https://developer.android.com/jetpack/androidx) [![Kotlin](https://img.shields.io/badge/Kotlin-1.3.21-brightgreen.svg)](https://kotlinlang.org)


Skate is a fragment manager controller that allows you to easily navigate from one fragment to another. Simple and Seamless. Just like it should be. 

`FragmentManager` has an unfriendly behavior to work with. 

Skate uses its own stack and backstack to manage fragments. The stack is pushed or popped *only* when a fragment is *added* or *removed*. Otherwise, any modifications are applied accordingly. 

This should be the intended behavior in my opinion. 

- No reflection
- Add/attach/show | Remove/detach/hide fragments
- Commit with state save
- Save and restore flow
- Listen to visibility changes
- Lifecycle aware
- Extremely lightweight and fast

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
    implementation 'com.github.inshiro:skate:0.2.0'
}
```

## Initialize
```kotlin
class MainActivity : AppCompatActivity() {

	private lateinit var skate: Skate
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		...
		skate = startSkating(savedInstanceState)
		skate.fragmentManager = supportFragmentManager
		skate.container = R.id.main_container
		fragment.mode = Skate.SINGLETON // optional
	}
	
	override fun onBackPressed() {
		if (!skate.back)
		    super.onBackPressed()
	}
}
```

## Usages
```kotlin
// Showing
fragment.show()

// Hiding
fragment.hide()

// Navigating
skate to fragment
```

#### Modes
Skate has 3 modes it operates on. The default is `FACTORY`. 
```kotlin
// add-remove
Skate.FACTORY = 0

// attach-detach
Skate.SPARING = 1

// show-hide
Skate.SINGLETON = 2
```
The mode is saved on method call and its corresponding counterpart is used when invoked. 
If you show, Skate will hide. Add, remove. etc..

Setting a mode for a fragment.
```kotlin
fragment.mode = Skate.SINGLETON

mainFragment.mode = Skate.SPARING
```

#### Settings
At any given time you can call these to set the global settings before showing or hiding a fragment.
```kotlin
skate.fragmentManager = supportFragmentManager
skate.container = R.id.main_container // id of your view
```
#### Back stack
By default, fragments are added to the back stack.
#### Listening
To listen to visibility changes, add an implementation of `Skate.OnNavigateListener` to `setOnNavigateListener`.
```kotlin
skate.setOnNavigateListener(object : Skate.OnNavigateListener {
    override fun onHide() { }

    override fun onShow() { }

    override fun onBackPressed(current: Fragment?) { }
})
```
#### Operations
Sometimes you have lots of fragments you want to work with.
```kotlin
skate.operate {
   hide(BillingFragment)
   hide(PaymentFragment)
   hide(DetailFragment1)
   show(MainFragment)
}
```
#### Logging
Skate provides a bit of information about fragment visibility during runtime which might help in debugging visibility-related issues. To enable logging, pass an implementation of `Skate.Logger` to the `Skate.logger` property or use the default `SkateLogger`.
```kotlin
Skate.logger = SkateLogger
```

## Activity Lifecycle
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