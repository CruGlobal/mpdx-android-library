package org.mpdx.android.core.modal

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import org.mpdx.android.R
import org.mpdx.android.features.base.BaseActivity
import org.mpdx.android.features.secure.UnlockFragment

private const val FRAGMENT_CLASS = "FRAGMENT_CLASS"
private const val FRAGMENT_ARGS = "FRAGMENT_ARGS"

@AndroidEntryPoint
class ModalActivity : BaseActivity() {
    // region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isFinishing) return
        createFragmentIfNecessary()
    }

    override fun onResume() {
        super.onResume()
        (fragment as? ModalFragment)?.toolbar?.let {
            setSupportActionBar(it)
            setupToolbar()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        // we just suppress native up navigation for a simpler finish() implementation
        finish()
        return true
    }
    // endregion Lifecycle

    // region Fragment
    private inline val fragmentClass get() = intent?.getSerializableExtra(FRAGMENT_CLASS) as? Class<out Fragment>
    private inline val fragmentArgs get() = intent?.getBundleExtra(FRAGMENT_ARGS)
    private val fragment get() = supportFragmentManager.primaryNavigationFragment

    private fun createFragmentIfNecessary() {
        if (fragment != null) return

        val fragment =
            try {
                fragmentClass?.newInstance()?.apply { arguments = fragmentArgs }
            } catch (e: InstantiationException) {
                throw IllegalStateException(e)
            } catch (e: IllegalAccessException) {
                throw IllegalStateException(e)
            } ?: return finish()

        supportFragmentManager.beginTransaction()
            .setPrimaryNavigationFragment(fragment)
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
    // endregion Fragment

    override fun getPageName() = "ModalActivity"
    override fun layoutId() = R.layout.activity_modal
    override fun showUnlockScreen() =
        super.showUnlockScreen() && fragmentClass?.let { UnlockFragment::class.java.isAssignableFrom(it) } != true

    companion object {
        @JvmStatic
        fun launchActivity(activity: Activity, fragment: Fragment) {
            activity.startActivity(getIntent(activity, fragment, false))
        }

        @JvmStatic
        fun launchActivity(activity: Activity, fragment: Fragment, fromOnboarding: Boolean) {
            activity.startActivity(getIntent(activity, fragment, fromOnboarding))
        }

        @JvmStatic
        fun launchActivityForResult(activity: Activity, fragment: Fragment, requestCode: Int) {
            activity.startActivityForResult(getIntent(activity, fragment, false), requestCode)
        }

        private fun getIntent(context: Context, fragment: Fragment, clearTask: Boolean) =
            Intent(context, ModalActivity::class.java)
                .putExtra(FRAGMENT_CLASS, fragment.javaClass)
                .putExtra(FRAGMENT_ARGS, fragment.arguments)
                .apply { if (clearTask) addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK) }
    }
}
