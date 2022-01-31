package com.consultantapp.ui.dashboard.doctor.secondopinion

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.consultantapp.R
import com.consultantapp.data.models.requests.DocImage
import com.consultantapp.data.models.requests.SecondOpinion
import com.consultantapp.data.network.ApisRespHandler
import com.consultantapp.data.network.responseUtil.Status
import com.consultantapp.data.repos.UserRepository
import com.consultantapp.databinding.FragmentSecondOpinionBinding
import com.consultantapp.ui.dashboard.DoctorViewModel
import com.consultantapp.ui.dashboard.chat.UploadFileViewModel
import com.consultantapp.ui.dashboard.doctor.detail.DoctorDetailActivity
import com.consultantapp.utils.*
import com.consultantapp.utils.PermissionUtils
import com.consultantapp.utils.dialogs.ProgressDialog
import com.consultantapp.utils.dialogs.ProgressDialogImage
import dagger.android.support.DaggerFragment
import droidninja.filepicker.FilePickerConst
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import permissions.dispatcher.*
import java.io.File
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.collections.set

@RuntimePermissions
class SecondOpinionFragment : DaggerFragment() {

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var prefsManager: PrefsManager

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: FragmentSecondOpinionBinding

    private var rootView: View? = null

    private lateinit var viewModel: DoctorViewModel

    private lateinit var viewModelUpload: UploadFileViewModel

    private lateinit var progressDialog: ProgressDialog

    private lateinit var progressDialogImage: ProgressDialogImage

    private var imagesAdapter: ImagesAdapter? = null

    private var itemImages = ArrayList<DocImage>()

    private var secondOpinion: SecondOpinion? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (rootView == null) {
            binding =
                    DataBindingUtil.inflate(inflater, R.layout.fragment_second_opinion, container, false)
            rootView = binding.root

            initialise()
            listeners()
            setAdapter()
            bindObservers()
        }
        return rootView
    }

    private fun initialise() {
        viewModelUpload = ViewModelProvider(this, viewModelFactory)[UploadFileViewModel::class.java]
        viewModel = ViewModelProvider(this, viewModelFactory)[DoctorViewModel::class.java]
        progressDialog = ProgressDialog(requireActivity())
        progressDialogImage = ProgressDialogImage(requireActivity())

    }

    private fun setAdapter() {
        imagesAdapter = ImagesAdapter(this, itemImages)
        binding.rvImages.adapter = imagesAdapter
    }

    private fun listeners() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().finish()
        }

        binding.tvSkip.setOnClickListener {
            secondOpinion = SecondOpinion()
            val intent = Intent()
            intent.putExtra(DoctorDetailActivity.EXTRA_SECOND_OPINION, secondOpinion)
            requireActivity().setResult(Activity.RESULT_OK, intent)
            requireActivity().finish()
        }

        binding.tvContinue.setOnClickListener {
            when {
                binding.etRecordDetails.text.toString().trim().isEmpty() -> {
                    binding.etRecordDetails.showSnackBar(getString(R.string.record_title))
                }
                itemImages.isEmpty() -> {
                    binding.etRecordDetails.showSnackBar(getString(R.string.select_image))
                }
                isConnectedToInternet(requireContext(), true) -> {
                    secondOpinion = SecondOpinion()
                    secondOpinion?.title = binding.etRecordDetails.text.toString().trim()

                    secondOpinion?.image = ArrayList()
                    uploadFileOnServer(itemImages[0])
                }
            }
        }
    }

    private fun uploadFileOnServer(docImage: DocImage?) {
        val hashMap = HashMap<String, RequestBody>()
        hashMap["type"] = getRequestBody(docImage?.type)

        val body: RequestBody = docImage?.imageFile?.asRequestBody("image/*".toMediaType())!!
        hashMap["image\"; fileName=\"" + docImage?.imageFile?.name] = body

        viewModelUpload.uploadFile(hashMap)
    }

    private fun bindObservers() {
        viewModelUpload.uploadFile.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialogImage.setLoading(false)

                    secondOpinion?.image?.add(it.data?.image_name ?: "")

                    if (secondOpinion?.image?.size ?: 0 < itemImages.size) {
                        uploadFileOnServer(itemImages[secondOpinion?.image?.size ?: 0])
                    } else {
                        val intent = Intent()
                        intent.putExtra(DoctorDetailActivity.EXTRA_SECOND_OPINION, secondOpinion)
                        requireActivity().setResult(Activity.RESULT_OK, intent)
                        requireActivity().finish()
                    }
                }
                Status.ERROR -> {
                    progressDialogImage.setLoading(false)
                    ApisRespHandler.handleError(it.error, requireActivity(), prefsManager)
                }
                Status.LOADING -> {
                    progressDialogImage.setLoading(true)

                }
            }
        })
    }


    /*Adapter item click*/
    fun clickItem() {
        getStorageWithPermissionCheck()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {

            if (requestCode == AppRequestCode.IMAGE_PICKER) {
                val docPaths = ArrayList<Uri>()
                docPaths.addAll(data?.getParcelableArrayListExtra(FilePickerConst.KEY_SELECTED_MEDIA)
                        ?: emptyList())

                val fileToUpload = File(getPathUri(requireContext(), docPaths[0]))
                val docImage = DocImage()
                docImage.type = DocType.IMAGE
                docImage.imageFile = compressImage(requireActivity(), fileToUpload)

                itemImages.add(docImage)
                imagesAdapter?.notifyDataSetChanged()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }

    @NeedsPermission(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun getStorage() {
        selectImages(this, requireActivity())
    }

    @OnShowRationale(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun showLocationRationale(request: PermissionRequest) {
        PermissionUtils.showRationalDialog(requireContext(), R.string.media_permission, request)
    }

    @OnNeverAskAgain(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun onNeverAskAgainRationale() {
        PermissionUtils.showAppSettingsDialog(
                requireContext(), R.string.media_permission
        )
    }

    @OnPermissionDenied(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun showDeniedForStorage() {
        PermissionUtils.showAppSettingsDialog(
                requireContext(), R.string.media_permission
        )
    }
}
