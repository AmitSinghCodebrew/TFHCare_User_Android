package com.consultantapp.ui.drawermenu

import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.consultantapp.R
import com.consultantapp.data.repos.UserRepository
import com.consultantapp.databinding.ActivityContainerBinding
import com.consultantapp.ui.classes.CategoriesFragment
import com.consultantapp.ui.classes.ClassesDetailFragment
import com.consultantapp.ui.classes.ClassesFragment
import com.consultantapp.ui.dashboard.appointment.AppointmentFragment
import com.consultantapp.ui.dashboard.appointment.detail.AppointmentDetailFragment
import com.consultantapp.ui.dashboard.appointment.rating.AddRatingFragment
import com.consultantapp.ui.dashboard.chat.ChatFragment
import com.consultantapp.ui.dashboard.doctor.listing.DoctorListActivity.Companion.AK_SECOND_OPINION
import com.consultantapp.ui.dashboard.doctor.secondopinion.SecondOpinionFragment
import com.consultantapp.ui.dashboard.doctor.symptom.SymptomDetailFragment
import com.consultantapp.ui.dashboard.feeds.FeedDetailsFragment
import com.consultantapp.ui.dashboard.feeds.FeedsFragment
import com.consultantapp.ui.dashboard.home.questions.QuestionsFragment
import com.consultantapp.ui.dashboard.home.questions.detail.QuestionDetailFragment
import com.consultantapp.ui.dashboard.location.LocationFragment
import com.consultantapp.ui.dashboard.subcategory.SubCategoryFragment
import com.consultantapp.ui.dashboard.subcategory.SubCategoryFragment.Companion.CATEGORY_PARENT_ID
import com.consultantapp.ui.dashboard.subscription.SubscriptionListFragment
import com.consultantapp.ui.drawermenu.addmoney.AddCardFragment
import com.consultantapp.ui.drawermenu.history.HistoryFragment
import com.consultantapp.ui.drawermenu.notification.NotificationFragment
import com.consultantapp.ui.drawermenu.profile.ProfileFragment
import com.consultantapp.ui.drawermenu.wallet.WalletFragment
import com.consultantapp.ui.loginSignUp.changepassword.ChangePasswordFragment
import com.consultantapp.ui.walkthrough.WalkThroughFragment
import com.consultantapp.utils.*
import dagger.android.support.DaggerAppCompatActivity
import javax.inject.Inject

class DrawerActivity : DaggerAppCompatActivity() {

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var prefsManager: PrefsManager

    lateinit var binding: ActivityContainerBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initialise()
    }

    private fun initialise() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_container)

        LocaleHelper.setLocale(this, userRepository.getUserLanguage(), prefsManager)

        when (intent.getStringExtra(PAGE_TO_OPEN)) {
            HISTORY ->
                addFragment(supportFragmentManager,
                        HistoryFragment(), R.id.container)
            PROFILE ->
                addFragment(supportFragmentManager,
                        ProfileFragment(), R.id.container)
            CHANGE_PASSWORD ->
                addFragment(supportFragmentManager,
                        ChangePasswordFragment(), R.id.container)
            NOTIFICATION ->
                addFragment(supportFragmentManager,
                        NotificationFragment(), R.id.container)
            WALLET ->
                addFragment(supportFragmentManager,
                        WalletFragment(), R.id.container)
            REQUEST, SECOND_OPINION  ->
                addFragment(supportFragmentManager,
                        AppointmentFragment(), R.id.container)
            APPOINTMENT_DETAIL ->
                addFragment(supportFragmentManager,
                        AppointmentDetailFragment(), R.id.container)
            RATE ->
                addFragment(supportFragmentManager,
                        AddRatingFragment(), R.id.container)
            USER_CHAT ->
                addFragment(supportFragmentManager,
                        ChatFragment(), R.id.container)
            CLASSES -> {
                val fragment = ClassesFragment()

                val bundle = Bundle()
                bundle.putSerializable(CATEGORY_PARENT_ID, intent.getSerializableExtra(CATEGORY_PARENT_ID))
                fragment.arguments = bundle
                addFragment(supportFragmentManager,
                        fragment, R.id.container)
            }
            CLASSES_DETAILS -> {
                addFragment(supportFragmentManager,
                        ClassesDetailFragment(), R.id.container)
            }
            LOCATION ->
                addFragment(supportFragmentManager,
                        LocationFragment(), R.id.container)
            SUB_CATEGORY ->
                addFragment(supportFragmentManager,
                        SubCategoryFragment(), R.id.container)
            ADD_CARD ->
                addFragment(supportFragmentManager,
                        AddCardFragment(), R.id.container)
            SUBSCRIPTION -> {
                val fragment = SubscriptionListFragment()
                val bundle = Bundle()
                bundle.putSerializable(PAGE_TO_OPEN, SubscriptionListFragment.LIST_ITEM)
                bundle.putSerializable(EXTRA_NAME, getString(R.string.choose_package))
                fragment.arguments = bundle

                addFragment(supportFragmentManager,
                        fragment, R.id.container)
            }
            BlogType.QUESTION ->
                addFragment(supportFragmentManager,
                        FeedsFragment(), R.id.container)
            BLOGS_DETAILS -> {
                val fragment = FeedDetailsFragment()
                val bundle = Bundle()
                bundle.putSerializable(EXTRA_REQUEST_ID, intent.getSerializableExtra(EXTRA_REQUEST_ID))
                fragment.arguments = bundle
                addFragment(supportFragmentManager, fragment, R.id.container)
            }
            WalkThroughFragment.WALK_THROUGH_SCREEN -> {
                addFragment(supportFragmentManager,
                        WalkThroughFragment(), R.id.container)
            }
            MY_QUESTION -> {
                addFragment(supportFragmentManager,
                        QuestionsFragment(), R.id.container)
            }
            QUESTION_DETAILS -> {
                val fragment = QuestionDetailFragment()
                val bundle = Bundle()
                bundle.putString(EXTRA_REQUEST_ID, intent.getStringExtra(EXTRA_REQUEST_ID))
                fragment.arguments = bundle

                addFragment(supportFragmentManager,
                        fragment, R.id.container)
            }
            SYMPTOMS -> {
                addFragment(supportFragmentManager,
                        SymptomDetailFragment(), R.id.container)
            }
            AK_SECOND_OPINION ->
                addFragment(supportFragmentManager,
                        SecondOpinionFragment(), R.id.container)
            CATEGORIES ->
                addFragment(supportFragmentManager,
                        CategoriesFragment(), R.id.container)
        }
    }

    companion object {
        const val HISTORY = "HISTORY"
        const val PROFILE = "PROFILE"
        const val CHANGE_PASSWORD = "CHANGE_PASSWORD"
        const val NOTIFICATION = "NOTIFICATION"
        const val WALLET = "WALLET"
        const val SUBSCRIPTION = "SUBSCRIPTION"
        const val REQUEST = "REQUEST"
        const val APPOINTMENT_DETAIL = "APPOINTMENT_DETAIL"
        const val USER_CHAT = "USER_CHAT"
        const val RATE = "RATE"
        const val CLASSES = "CLASSES"
        const val CLASSES_DETAILS = "CLASSES_DETAILS"
        const val LOCATION = "LOCATION"
        const val SUB_CATEGORY = "SUB_CATEGORY"
        const val ADD_CARD = "ADD_CARD"
        const val BLOGS_DETAILS = "BLOGS_DETAILS"
        const val MY_QUESTION = "MY_QUESTION"
        const val QUESTION_DETAILS = "QUESTION_DETAILS"
        const val SYMPTOMS = "SYMPTOMS"
        const val SECOND_OPINION = "SECOND_OPINION"
        const val CATEGORIES = "CATEGORIES"
    }

}
