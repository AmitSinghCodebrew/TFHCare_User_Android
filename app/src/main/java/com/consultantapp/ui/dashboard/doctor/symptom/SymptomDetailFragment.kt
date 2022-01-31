package com.consultantapp.ui.dashboard.doctor.symptom

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
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.GridLayoutManager
import com.consultantapp.R
import com.consultantapp.data.models.requests.DocImage
import com.consultantapp.data.models.requests.UpdateSymptom
import com.consultantapp.data.models.responses.Filter
import com.consultantapp.data.models.responses.Request
import com.consultantapp.data.network.ApisRespHandler
import com.consultantapp.data.network.PushType
import com.consultantapp.data.network.responseUtil.Status
import com.consultantapp.data.repos.UserRepository
import com.consultantapp.databinding.FragmentSymptomDetailsBinding
import com.consultantapp.ui.adapter.CheckItemAdapter
import com.consultantapp.ui.dashboard.chat.UploadFileViewModel
import com.consultantapp.ui.dashboard.doctor.secondopinion.ImagesAdapter
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
import javax.inject.Inject

@RuntimePermissions
class SymptomDetailFragment : DaggerFragment() {

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var prefsManager: PrefsManager

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: FragmentSymptomDetailsBinding

    private var rootView: View? = null

    private lateinit var progressDialog: ProgressDialog

    private lateinit var progressDialogImage: ProgressDialogImage

    private lateinit var viewModel: PandemicViewModel

    private lateinit var viewModelUpload: UploadFileViewModel

    private val items = ArrayList<Filter>()

    private lateinit var adapter: CheckItemAdapter

    private var symptomIds = ""

    private var request: Request? = null

    private var imagesAdapter: ImagesAdapter? = null

    private var itemImages = ArrayList<DocImage>()

    private var updateSymptom: UpdateSymptom? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        if (rootView == null) {
            binding = DataBindingUtil.inflate(
                    inflater, R.layout.fragment_symptom_details,
                    container, false
            )
            rootView = binding.root

            initialise()
            setAdapter()
            listeners()
            bindObservers()
        }
        return rootView
    }

    private fun initialise() {
        viewModel = ViewModelProvider(this, viewModelFactory)[PandemicViewModel::class.java]
        viewModelUpload = ViewModelProvider(this, viewModelFactory)[UploadFileViewModel::class.java]
        progressDialog = ProgressDialog(requireActivity())
        progressDialogImage = ProgressDialogImage(requireActivity())
        binding.clLoader.setBackgroundResource(R.color.colorWhite)

        binding.tvTitle.gone()
        editTextScroll(binding.etDesc)

        binding.toolbar.title = getString(R.string.select_from_symptom)
        binding.tvDone.visible()
        binding.tvAdd.visible()
        binding.tvAdd.text = getString(R.string.skip)
        binding.etDesc.visible()
        binding.tvDescTitle.visible()
        binding.rvImages.visible()

        hitApi()
    }


    private fun setAdapter() {
        binding.rvListing.layoutManager = GridLayoutManager(requireContext(), 3)

        adapter = CheckItemAdapter(this, true, items)
        binding.rvListing.adapter = adapter

        imagesAdapter = ImagesAdapter(this, itemImages)
        binding.rvImages.adapter = imagesAdapter
    }

    private fun listeners() {
        binding.toolbar.setNavigationOnClickListener {
            onBackClick()
        }

        binding.tvAdd.setOnClickListener {
            onBackClick()
        }

        binding.tvDone.setOnClickListener {
            symptomIds = ""
            items.forEach {
                if (it.isSelected)
                    symptomIds += it.id + ","
            }

            when {
                binding.etDesc.text.toString().trim().isEmpty() && symptomIds.isEmpty() && itemImages.isEmpty() -> {
                    binding.etDesc.showSnackBar(getString(R.string.tell_something_more))
                }
                isConnectedToInternet(requireContext(), true) -> {
                    updateSymptom = UpdateSymptom()
                    if (itemImages.isEmpty()) {
                        updateSymptom?.request_id = requireActivity().intent.getStringExtra(EXTRA_REQUEST_ID)
                                ?: ""
                        if (symptomIds.removePrefix(",").isNotEmpty())
                            updateSymptom?.option_ids = symptomIds.removePrefix(",")
                        if (binding.etDesc.text.toString().trim().isNotEmpty())
                            updateSymptom?.symptom_details = binding.etDesc.text.toString().trim()
                        viewModel.updateSymptom(updateSymptom ?: UpdateSymptom())
                    } else {
                        updateSymptom?.images = ArrayList()
                        uploadFileOnServer(itemImages[0])
                    }
                }
            }

        }
    }

    /*Adapter item click*/
    fun clickItem() {
        getStorageWithPermissionCheck()
    }

    private fun onBackClick() {
        if (requireActivity().supportFragmentManager.backStackEntryCount > 0)
            requireActivity().supportFragmentManager.popBackStack()
        else
            requireActivity().finish()
    }


    private fun hitApi() {
        if (isConnectedToInternet(requireContext(), true)) {
            val hashMap = HashMap<String, String>()
            hashMap["type"] = PandemicType.SYMPTOM_OPTIONS_ALL
            viewModel.symptom(hashMap)
        }
    }

    private fun bindObservers() {
        viewModel.symptom.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    binding.clLoader.gone()

                    items.clear()
                    items.addAll(it.data?.symptoms ?: emptyList())
                    adapter.notifyDataSetChanged()

                }
                Status.ERROR -> {
                    binding.clLoader.gone()
                    ApisRespHandler.handleError(it.error, requireActivity(), prefsManager)
                }
                Status.LOADING -> {
                    binding.clLoader.visible()
                }
            }
        })

        viewModel.updateSymptom.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)
                    val intent = Intent()
                    intent.action = PushType.REQUEST_ACCEPTED
                    LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent)
                    onBackClick()
                }
                Status.ERROR -> {
                    progressDialog.setLoading(false)
                    ApisRespHandler.handleError(it.error, requireActivity(), prefsManager)
                }
                Status.LOADING -> {
                    progressDialog.setLoading(true)
                }
            }
        })

        viewModelUpload.uploadFile.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialogImage.setLoading(false)

                    val docImage = DocImage()
                    docImage.image = it.data?.image_name
                    docImage.type = it.data?.type
                    updateSymptom?.images?.add(docImage)

                    if (updateSymptom?.images?.size ?: 0 < itemImages.size) {
                        uploadFileOnServer(itemImages[updateSymptom?.images?.size ?: 0])
                    } else if (isConnectedToInternet(requireContext(), true)) {
                        updateSymptom?.request_id = requireActivity().intent.getStringExtra(EXTRA_REQUEST_ID)
                                ?: ""
                        if (symptomIds.removePrefix(",").isNotEmpty())
                            updateSymptom?.option_ids = symptomIds.removePrefix(",")
                        if (binding.etDesc.text.toString().trim().isNotEmpty())
                            updateSymptom?.symptom_details = binding.etDesc.text.toString().trim()

                        viewModel.updateSymptom(updateSymptom ?: UpdateSymptom())
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

    private fun uploadFileOnServer(docImage: DocImage?) {
        val hashMap = java.util.HashMap<String, RequestBody>()
        hashMap["type"] = getRequestBody(docImage?.type)

        val body: RequestBody = docImage?.imageFile?.asRequestBody("image/*".toMediaType())!!
        hashMap["image\"; fileName=\"" + docImage?.imageFile?.name] = body

        viewModelUpload.uploadFile(hashMap)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                AppRequestCode.IMAGE_PICKER -> {
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
                AppRequestCode.DOC_PICKER -> {
                    val docPaths = ArrayList<Uri>()
                    docPaths.addAll(data?.getParcelableArrayListExtra(FilePickerConst.KEY_SELECTED_DOCS)
                            ?: emptyList())

                    val fileToUpload = File(getPathUri(requireContext(), docPaths[0]))

                    val docImage = DocImage()
                    docImage.type = DocType.PDF
                    docImage.imageFile = fileToUpload
                    itemImages.add(docImage)

                    imagesAdapter?.notifyDataSetChanged()

                }
            }
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }

    @NeedsPermission(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun getStorage() {
        askForOption(this, requireActivity(), binding.rvImages)
    }

    @OnShowRationale(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun showLocationRationale(request: PermissionRequest) {
        PermissionUtils.showRationalDialog(requireContext(), R.string.media_permission, request)
    }

    @OnNeverAskAgain(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun onNeverAskAgainRationale() {
        PermissionUtils.showAppSettingsDialog(
                requireContext(), R.string.media_permission)
    }

    @OnPermissionDenied(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun showDeniedForStorage() {
        PermissionUtils.showAppSettingsDialog(
                requireContext(), R.string.media_permission)
    }
}
