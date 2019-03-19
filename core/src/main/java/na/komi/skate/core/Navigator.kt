package na.komi.skate.core

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import java.util.Stack

interface Navigator {

    /**
     * add-remove (0), attach-detach (1), hide-show (2)
     */
    val mode: Int

    /**
     * Get the current list of fragments added.
     */
    val stack: Stack<Skate.SkateFragment>

    /**
     * Specify the container to house the [Fragment]
     */
    val container:Int

    /**
     * Get the current visible [Fragment].
     */
    val current: Fragment?

    /**
     * Set the [FragmentManager] scoped to the [Activity]. This changes every on configuration change.
     * Be sure to re-set it when that occurs.
     */
    val fragmentManager: FragmentManager?

    /**
     * Hide the currently visible fragment.
     * @return `true` if a [Fragment] was found, else false.
     */
    val back: Boolean

    /**
     * Runs a set of operations. Does not invoke callbacks.
     */
    fun operate(block: Skate.() -> Unit): Skate

    /**
     * Show a [Fragment] based on its [mode].
     * @param fragment to show
     */
    fun show(fragment: Fragment)

    /**
     * Hide a [Fragment] based on its [mode].
     * @param fragment to hide
     */

    fun hide(fragment: Fragment)

    /**
     * Hide the currently visible [Fragment] and then [show] it based on its [mode].
     * @param fragment to navigate to
     */
    fun navigate(fragment: Fragment)

    /**
     * Avoid relying on [FragmentManager]. Only use it to check the currently visible fragments.
     * PopBackStack reverts the last transaction.
     * Recalling the last fragment. If the last fragment is gone, it'll be recreated.
     * Thus we must manage our own backstack because this allows us to properly use
     * detach and attach.
     * This prevents re-adding fragments due to un-intended behavior.
     *
     * We avoid using popBackStack because it reverts the last transaction.
     * Transactions do not deal with concrete implementations such
     * as add, remove, detach, attach, hide, show.
     * https://stackoverflow.com/a/38305887
     */
}