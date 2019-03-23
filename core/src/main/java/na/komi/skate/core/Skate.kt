package na.komi.skate.core

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import kotlinx.android.parcel.Parcelize
import na.komi.skate.core.global.SkateSingleton
import na.komi.skate.core.log.Logger
import java.util.ArrayList

class Skate : Navigator {

    @IdRes
    override var container: Int = -1
    override var mode = FACTORY
    override var fragmentManager: FragmentManager? = null

    private var listener: OnNavigateListener? = null
    private var currentTransaction: FragmentTransaction? = null
    private var ALLOW_COMMIT = true

    private val defaultMode = FACTORY
    private val handler by lazy { Handler() }

    var animationStart = android.R.animator.fade_in
    var animationEnd = android.R.animator.fade_out

    override val current: Fragment?
        get() = internalFragmentManager.fragments.lastOrNull()

    private val internalFragmentManager: FragmentManager
        get() = fragmentManager ?: throw NullPointerException("Please set the fragment manager")

    private val Fragment.uid
        get() = "${javaClass.name}$$container"
    
    private val Fragment.uidSimple
        get() = "${javaClass.simpleName}$$container"
    
    override val stack
        get() = SkateSingleton.stack

    private val modes
        get() = SkateSingleton.modes

    override val back: Boolean
        get() = goBack()

    @Parcelize
    data class SkateFragment(
            var uid: String,
            var mode: Int
    ) : Parcelable

    internal fun restoreList(list: ArrayList<SkateFragment>) {
        stack.clear()
        stack.addAll(list)
    }

    override infix fun show(fragment: Fragment) {
        checkAndCreateTransaction()
        @Suppress("NAME_SHADOWING")
        val fragment = internalFragmentManager.findFragmentByTag(fragment.uid) ?: fragment
        // Get the mode assigned to the fragment
        var mode = defaultMode
        modes[fragment.uid]?.let {
            mode = it
        } ?: modes.put(fragment.uid, mode)

        if (stack.firstOrNull { it.uid == fragment.uid } == null) {
            currentTransaction?.add(container, fragment, fragment.uid)
            stack.push(SkateFragment(fragment.uid, mode))
            Logger assert "Adding ${fragment.uid}"
        } else
            when (mode) {
                FACTORY -> {

                    if (stack.firstOrNull { it.uid == fragment.uid } == null)
                        currentTransaction?.add(
                                container,
                                fragment,
                                fragment.uid
                        ).also { Logger assert "Adding ${fragment.uidSimple}" }
                }
                SPARING -> currentTransaction?.attach(fragment).also { Logger assert "Attaching ${fragment.uidSimple}" }
                SINGLETON -> currentTransaction?.show(fragment).also { Logger assert "Showing ${fragment.uidSimple}" }
            }

        // Logger info stack.toString()

        if (ALLOW_COMMIT) {
            commit()
            listener?.onShow()
        }
    }

    override infix fun hide(fragment: Fragment) {
        checkAndCreateTransaction()
        @Suppress("NAME_SHADOWING")
        val fragment = internalFragmentManager.findFragmentByTag(fragment.uid) ?: fragment

        // Get the mode assigned to the fragment
        var mode = defaultMode
        modes[fragment.uid]?.let {
            mode = it
        } ?: modes.put(fragment.uid, mode)

        when (mode) {
            FACTORY -> {
                stack.firstOrNull { it.uid == fragment.uid }?.let {
                    Logger assert "Removing ${fragment.uidSimple}"
                    currentTransaction?.remove(fragment)
                    stack.remove(it)
                    SkateSingleton.modes.remove(it.uid)
                }
            }
            SPARING -> currentTransaction?.detach(fragment)?.also { Logger assert "Detaching ${fragment.uidSimple}" }
            SINGLETON -> currentTransaction?.hide(fragment)?.also { Logger assert "Hiding ${fragment.uidSimple}" }
        }

        //Logger warn stack.toString()

        if (ALLOW_COMMIT) {
            commit()
            listener?.onShow()
        }
    }

    override infix fun navigate(fragment: Fragment) {
        val list = internalFragmentManager.fragments
        list.reverse()

        currentTransaction = null
        ALLOW_COMMIT = false

        // Hide the current showing fragment
        current?.let {
            hide(it)
        }

        ALLOW_COMMIT = true

        // Show the destination
        show(fragment)

    }

    infix fun to(fragment: Fragment) = navigate(fragment)

    private fun goBack(): Boolean {
        internalFragmentManager.fragments.let {
            it.lastOrNull()?.let { fragment ->
                hide(fragment)
                listener?.onBackPressed(if (it.lastIndex - 1 >= 0) it[it.lastIndex - 1] else null)
                if (it.size <= 1)
                    return false
            }
        }
        return true
    }

    override fun operate(block: Skate.() -> Unit) = apply {
        currentTransaction = null
        ALLOW_COMMIT = false
        block()
        ALLOW_COMMIT = true
        commit()
    }

    fun setOnNavigateListener(listener: OnNavigateListener) {
        this.listener = null
        this.listener = listener
    }

    private fun commit() {
        currentTransaction?.commit()
        currentTransaction = null
    }

    private fun commitNow() {
        currentTransaction?.commitNow()
        currentTransaction = null
    }

    private fun checkAndCreateTransaction() {
        if (currentTransaction == null)
            currentTransaction =
                    internalFragmentManager.beginTransaction().setCustomAnimations(animationStart, animationEnd)
    }

    private fun displayFragments() {
        handler.postDelayed({
            internalFragmentManager.fragments.joinToString(", ") { it::class.java.simpleName }
                    .also { Logger verbose "Currently have: [$it]" }
        }, 100)
    }

    internal fun clear() {

        fragmentManager = null
        listener = null
        SkateSingleton.clear()
    }

    companion object {
        operator fun invoke(): Skate {
            //log w "/SKATE Skate instance null?: ${SkateSingleton._instance == null}"

            if (SkateSingleton.readInstance() != null)
                throw RuntimeException("Use startSkating() method to get the single instance of this class.")

            return SkateSingleton.getInstance()
        }

        /**
         * Pass an implementation of [Logger] here to enable Katana's logging functionality
         */
        var logger: Logger? = null

        /**
         * The mode to Add-Remove.
         *
         * Saves the most memory.
         */
        val FACTORY: Int = 0


        /**
         * The mode to Detach-Attach.
         *
         * A balance between saving memory and speed.
         */
        val SPARING: Int = 1


        /**
         * The mode to Hide-Show.
         *
         * Uses the most memory but also the fastest.
         */
        val SINGLETON: Int = 2
    }

    interface ActivityLifecycleCallbacks {
        fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {}
        fun onActivityStarted(activity: Activity?) {}
        fun onActivityResumed(activity: Activity?) {}
        fun onActivityPaused(activity: Activity?) {}
        fun onActivityStopped(activity: Activity?) {}
        fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {}
        fun onActivityDestroyed(activity: Activity?) {}
    }

    interface OnNavigateListener {
        /**
         * Called after the [Fragment] is shown.
         */
        fun onShow() {}

        /**
         * Called after the [Fragment] is hidden.
         */
        fun onHide() {}

        /**
         * Called after the [Fragment] is hidden.
         */
        fun onBackPressed(current: Fragment?) {}
    }

    interface Logger {
        fun debug(msg: String)
        fun info(msg: String)
        fun warn(msg: String)
        fun error(msg: String)
        fun verbose(msg: String)
        fun assert(msg: String)
    }

}

