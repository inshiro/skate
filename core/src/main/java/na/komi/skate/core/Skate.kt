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

    internal fun serializeList(list: ArrayList<SkateFragment>) {
        stack.clear()
        stack.addAll(list)
    }

    override val stack
        get() = SkateSingleton.stack!!

    companion object {
        operator fun invoke(): Skate {
            // Logger info "Skate instance null?: $${SkateSingleton._instance == null}"

            if (SkateSingleton.instance != null)
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

    @IdRes
    override var container: Int = -1
    override var mode = FACTORY
    private val defaultMode
        get () = mode
    private val animationStart = android.R.animator.fade_in
    private val animationEnd = android.R.animator.fade_out
    private val handler by lazy { Handler() }

    override val current: Fragment?
        get() = currentlyVisibleFragment()

    override var fragmentManager: FragmentManager? = null

    private val internalFragmentManager: FragmentManager
        get() = fragmentManager ?: throw NullPointerException("Please set the fragment manager")

    private val Fragment.name
        get() = this::class.java.simpleName


    val back: Boolean
        get() = goBack()

    infix fun back(fragment: Fragment) = hide(fragment)

    /**
     * [Fragment] wrapper to allow real state change listening.
     * @see State
     * @param inBackStack Whether this fragment added to back stack or not.
     * @param modular Hide this fragment as well on [navigate]
     */
    @Parcelize
    data class SkateFragment(
            var tag: String,
            var state: State,
            var inBackStack: Boolean = true,
            var modular: Boolean = false
    ) : Parcelable

    private fun State.isVisible() = this == State.ADDED || this == State.ATTACHED || this == State.SHOWING

    private var currentTransaction: FragmentTransaction? = null

    private var ALLOW_COMMIT = true

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
            currentTransaction = internalFragmentManager.beginTransaction()
    }

    infix fun to(fragment: Fragment) = navigate(fragment)

    override fun navigate(fragment: Fragment) {
        val list = internalFragmentManager.fragments
        list.reverse()

        //SYNCHRONOUS = true
        ALLOW_COMMIT = false
        // Get all modular fragments and hide them. Stop when we reach a non modular one.
        run loop@{
            list.forEach { fragment ->
                if (stack.lastOrNull { it.tag == fragment.name }?.modular == true) {
                    hide(fragment)
                } else {
                    return@loop
                }
            }
        }


        // Hide the current showing fragment
        goBack()

        //SYNCHRONOUS = false
        ALLOW_COMMIT = true

        // Show the destination
        show(fragment)
    }

    private fun Fragment.push(
            mode: Int = defaultMode,
            addToBackStack: Boolean = true,
            modular: Boolean = false,
            index: Int = -1
    ) {
        stack.lastOrNull { it.tag == name }?.also {
            it.state =
                    when (mode) {
                        FACTORY -> State.ADDED
                        SPARING -> State.ATTACHED
                        else -> State.SHOWING
                    }
            it.modular = modular
            it.inBackStack = addToBackStack
            return
        }

        if (index != -1) {
            stack.add(index, SkateFragment(name, State.ADDED, addToBackStack, modular))
            return
        }
        stack.push(
                SkateFragment(
                        name,
                        when (mode) {
                            FACTORY -> State.ADDED
                            SPARING -> State.ATTACHED
                            else -> State.SHOWING
                        },
                        addToBackStack,
                        modular
                )
        )
    }

    private fun Fragment.pop(
            mode: Int = defaultMode,
            addToBackStack: Boolean = true,
            modular: Boolean = false
    ) {
        if (mode == FACTORY) stack.lastOrNull { it.tag == name }?.let {
            stack.remove(it)
        }
        stack.lastOrNull { it.tag == name }?.also {
            it.state =
                    when (mode) {
                        SPARING -> State.DETACHED
                        else -> State.HIDDEN
                    }
            it.modular = modular
            it.inBackStack = addToBackStack
            return
        }
    }


    override infix fun add(fragment: Fragment) = fragment.push(FACTORY)

    override fun add(index: Int, fragment: Fragment) {
        if (index !in 0..stack.size) {
            Logger error "ERROR adding fragment! index out of bounds"
            return
        }
        fragment.push(FACTORY)
    }

    override fun add(
            fragment: Fragment,
            mode: Int,
            addToBackStack: Boolean,
            modular: Boolean
    ) = fragment.push(mode, addToBackStack, modular)


    override fun remove(fragment: Fragment) {
        hide(fragment, FACTORY)
    }

    private fun parseState(frag: Fragment, mode: Int = defaultMode, show: Boolean = true): State {
        val tag = frag::class.java
        val prefix = "Found as"
        var state = State.REMOVED
        if (show) {
            if (!frag.isVisible) {
                when {
                    frag.isDetached -> {
                        Logger assert "$prefix detached"
                        when (mode) {
                            FACTORY -> state = State.ADDED.also { Logger debug "Add $tag" }
                            SPARING -> state = State.ATTACHED.also { Logger debug "Attach $tag" }
                            SINGLETON -> state = State.SHOWING.also { Logger debug "Show $tag" }
                        }
                    }
                    frag.isHidden -> {
                        Logger assert "$prefix hiding"
                        when (mode) {
                            FACTORY -> state = State.ADDED.also { Logger debug "Add $tag" }
                            SPARING -> state = State.ATTACHED.also { Logger debug "Attach $tag" }
                            SINGLETON -> state = State.SHOWING.also { Logger debug "Show $tag" }
                        }
                    }
                    !frag.isAdded -> {
                        Logger assert "$prefix removed"
                        state = State.ADDED
                        Logger debug "Add $tag"
                    }
                    else -> {
                        Logger error "ERROR trying to show $tag"
                        return State.ERROR
                    }
                }
            } else {
                Logger error "[Already showing] $tag"
                return State.ERROR
            }
            return state
        } else {
            if (frag.isAdded) {
                when {
                    !frag.isDetached -> {
                        Logger assert "$prefix attached"
                        when (mode) {
                            FACTORY -> state = State.REMOVED.also { Logger debug "Remove $tag" }
                            SPARING -> state = State.DETACHED.also { Logger debug "Detach $tag" }
                            SINGLETON -> state = State.HIDDEN.also { Logger debug "Hide $tag" }
                        }

                    }
                    !frag.isHidden -> {
                        Logger assert "$prefix showing"
                        when (mode) {
                            FACTORY -> state = State.REMOVED.also { Logger debug "Remove $tag" }
                            SPARING -> state = State.DETACHED.also { Logger debug "Detach $tag" }
                            SINGLETON -> state = State.HIDDEN.also { Logger debug "Hide $tag" }
                        }
                    }
                    else -> {
                        Logger assert "$prefix added"
                        state = State.REMOVED
                        Logger debug "Remove $tag"
                    }
                }

            } else {
                Logger error "[Not Added] $tag"
                return State.ERROR
            }
            return state
        }
    }

    override infix fun show(fragment: Fragment) {
        show(fragment, defaultMode)
    }

    override fun show(fragment: Fragment, mode: Int, addToBackStack: Boolean, modular: Boolean) {
        Logger verbose "== COMMENCE SHOW == "

        val frag = internalFragmentManager.findFragmentByTag(fragment.name) ?: fragment

        val state = parseState(frag, mode)

        if (state == State.ERROR) return

        //val transaction = internalFragmentManager.beginTransaction().setCustomAnimations(animationStart, animationEnd)
        checkAndCreateTransaction()

        frag.push(mode, addToBackStack, modular)

        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (state) {
            State.ADDED -> currentTransaction?.add(container, frag, frag.name)
            State.ATTACHED -> currentTransaction?.attach(frag)
            State.SHOWING -> currentTransaction?.show(frag)
        }

        if (ALLOW_COMMIT) {
            commit()
            listener?.onShow()
        }
    }

    override infix fun hide(fragment: Fragment) {
        hide(fragment, defaultMode)
    }

    override fun hide(fragment: Fragment, mode: Int, addToBackStack: Boolean, modular: Boolean) {
        Logger verbose "== COMMENCE HIDE == "

        val frag = internalFragmentManager.findFragmentByTag(fragment.name) ?: fragment

        val state = parseState(frag, mode, false)

        if (state == State.ERROR) return

        if (!modular)
            ALLOW_COMMIT = true

        //val transaction = internalFragmentManager.beginTransaction().setCustomAnimations(animationStart, animationEnd)

        checkAndCreateTransaction()

        frag.pop(mode, addToBackStack, modular)

        when (state) {
            State.REMOVED -> currentTransaction?.remove(frag)
            State.DETACHED -> currentTransaction?.detach(frag)
            else -> currentTransaction?.hide(frag)
        }

        if (ALLOW_COMMIT) {
            commit()
            listener?.onHide()
        }


    }

    private inline fun currentlyVisibleFragment(
            goingBack: Boolean = false,
            action: (fragment: Fragment, kfragment: SkateFragment) -> Unit = { _, _ -> }
    ): Fragment? {
        stack.lastOrNull { it.state.isVisible() && if (goingBack) it.inBackStack else true }?.also { KFragment ->

            // If it's in this list, it means it's visible.. cause ¯\_(?)_/¯
            internalFragmentManager.fragments.lastOrNull { it.name == KFragment.tag }?.also { fragment ->
                action(fragment, KFragment)
                return fragment
            }
        }
        return null
    }

    override fun goBack(): Boolean {
        // Hide the most recent Fragment added to the stack
        // Hide if visible, else ignore.

        val ret = currentlyVisibleFragment(true) { fragment, KFragment ->
            hide(fragment)
            listener?.onBackPressed(KFragment.modular)
        }

        // Handle back if we have any fragments in our backstack. If not call super.onBackPressed.
        return ret != null

    }

    @Suppress("unused")
    private fun displayFragments() {
        handler.postDelayed({
            internalFragmentManager.fragments.joinToString(", ") { it::class.java.simpleName }
                    .also { Logger verbose "Currently have: [$it]" }
        }, 100)
    }


    private var listener: OnNavigateListener? = null

    fun setOnNavigateListener(listener: OnNavigateListener) {
        this.listener = null
        this.listener = listener
    }

    internal fun clear() {

        fragmentManager = null
        listener = null
        SkateSingleton.clear()
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
        fun onBackPressed(isModular: Boolean) {}
    }

    interface Logger {

        fun debug(msg: String)
        fun info(msg: String)
        fun warn(msg: String)
        fun error(msg: String)
        fun verbose(msg: String)
        fun assert(msg: String)

    }

    enum class State {
        ADDED, ATTACHED, SHOWING, REMOVED, DETACHED, HIDDEN, ERROR
    }

}

